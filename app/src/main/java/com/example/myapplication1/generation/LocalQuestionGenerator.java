package com.example.myapplication1.generation;

import com.example.myapplication1.data.Question;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Rule-based question generator (no internet, no API key). How it works:
 *
 *   1. Split the text into sentences and keep the ones that are neither too short nor too long.
 *   2. In each sentence choose a "key word" = the longest word that is not a common word.
 *   3. Distractors (wrong answers) = other key words taken from the SAME text,
 *      so they look believable (they belong to the same topic).
 *   4. Build the question:
 *        - MISSING_WORD: the key word is replaced by "____", 3 options.
 *        - TWO_OPTIONS:  same, but only 2 options.
 *        - TRUE_FALSE:   every second statement is made false by swapping the key word
 *                        with a distractor.
 *   5. The explanation of every question is the original sentence.
 *
 * It is a simple heuristic: it can produce weak questions. That is exactly why the Review
 * screen exists: nothing enters the quiz before the user checked it.
 */
public class LocalQuestionGenerator implements QuestionGenerator {

    private static final int MAX_QUESTIONS = 12;
    private static final int MIN_WORDS = 6;
    private static final int MAX_WORDS = 35;
    private static final int MIN_KEYWORD_LENGTH = 5;

    /** Very common Romanian + English words that make bad answers. */
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "aceasta", "această", "acesta", "acestea", "aceste", "acestui", "acestei", "pentru",
            "foarte", "care", "între", "printre", "despre", "poate", "pot", "sunt", "fiind",
            "atunci", "deoarece", "pentru", "dintre", "către", "asupra", "unde", "când",
            "there", "their", "these", "those", "which", "where", "while", "between", "about",
            "because", "other", "through", "should", "would", "could"));

    @Override
    public List<Question> generate(String text, long subjectId, Set<QuestionType> types, LongSupplier newId) {
        List<Question> result = new ArrayList<>();
        if (text == null || types == null || types.isEmpty()) return result;

        List<String> sentences = splitSentences(text);
        List<String> pool = keywordPool(sentences);          // all key words of the text
        List<QuestionType> typeCycle = new ArrayList<>(types);
        Collections.sort(typeCycle);                         // always the same order
        Random random = new Random(text.hashCode());         // same text -> same questions

        int typeIndex = 0;
        int trueFalseCount = 0;
        for (String sentence : sentences) {
            if (result.size() >= MAX_QUESTIONS) break;

            String key = pickKeyword(sentence);
            if (key == null) continue;

            QuestionType type = typeCycle.get(typeIndex % typeCycle.size());
            Question q;
            if (type == QuestionType.TRUE_FALSE) {
                boolean makeFalse = (trueFalseCount % 2 == 1);
                q = trueFalse(sentence, key, makeFalse, pool, subjectId, newId);
                if (q != null) trueFalseCount++;
            } else {
                int wrongNeeded = (type == QuestionType.MISSING_WORD) ? 2 : 1;
                q = gap(sentence, key, wrongNeeded, pool, random, subjectId, newId);
            }
            if (q != null) {
                result.add(q);
                typeIndex++;
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ building questions

    private Question trueFalse(String sentence, String key, boolean makeFalse, List<String> pool,
                               long subjectId, LongSupplier newId) {
        String statement = sentence;
        int correct = 1;                                   // option1 = "Adevărat"
        String explanation = null;
        if (makeFalse) {
            List<String> wrong = distractors(key, pool, 1, null);
            if (wrong.isEmpty()) return null;              // cannot make it false -> skip
            statement = replaceFirstWord(sentence, key, wrong.get(0));
            correct = 2;                                   // option2 = "Fals"
            explanation = "Corect: " + sentence;
        }
        return new Question(newId.getAsLong(), subjectId, statement,
                QuestionType.TRUE_TEXT, QuestionType.FALSE_TEXT, null, correct, explanation);
    }

    private Question gap(String sentence, String key, int wrongNeeded, List<String> pool,
                         Random random, long subjectId, LongSupplier newId) {
        List<String> wrong = distractors(key, pool, wrongNeeded, random);
        if (wrong.size() < wrongNeeded) return null;       // not enough believable wrong answers

        List<String> options = new ArrayList<>();
        options.add(key);
        options.addAll(wrong);
        Collections.shuffle(options, random);              // the correct answer is not always first
        int correct = options.indexOf(key) + 1;            // options are numbered 1..3

        String gapText = replaceFirstWord(sentence, key, "____");
        return new Question(newId.getAsLong(), subjectId, gapText,
                options.get(0), options.get(1), options.size() > 2 ? options.get(2) : null,
                correct, "Propoziția completă: " + sentence);
    }

    // ------------------------------------------------------------------ text helpers

    /** Splits at . ! ? and new lines; keeps sentences with MIN_WORDS..MAX_WORDS words. */
    static List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        for (String part : text.replace("\r", "\n").split("(?<=[.!?])\\s+|\\n+")) {
            String s = part.trim().replaceAll("^[-•*\\d.)\\s]+", "").trim();   // remove "1." / "- " bullets
            int words = s.isEmpty() ? 0 : s.split("\\s+").length;
            if (words >= MIN_WORDS && words <= MAX_WORDS) sentences.add(s);
        }
        return sentences;
    }

    /** The word without punctuation around it: "(nervul)," -> "nervul". */
    private static String clean(String word) {
        return word.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");
    }

    private static boolean isGoodKeyword(String word) {
        if (word.length() < MIN_KEYWORD_LENGTH) return false;
        if (STOP_WORDS.contains(word.toLowerCase(Locale.ROOT))) return false;
        for (int i = 0; i < word.length(); i++) {
            if (Character.isDigit(word.charAt(i))) return false;   // numbers/codes make bad answers
        }
        return true;
    }

    /** The longest good word of the sentence, or null. Words in the first position are skipped. */
    private static String pickKeyword(String sentence) {
        String[] words = sentence.split("\\s+");
        String best = null;
        for (int i = 1; i < words.length; i++) {
            String w = clean(words[i]);
            if (isGoodKeyword(w) && (best == null || w.length() > best.length())) best = w;
        }
        return best;
    }

    /** Every key word of the text, without repeats (case-insensitive), in order of appearance. */
    private static List<String> keywordPool(List<String> sentences) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> pool = new ArrayList<>();
        for (String sentence : sentences) {
            for (String raw : sentence.split("\\s+")) {
                String w = clean(raw);
                if (isGoodKeyword(w) && seen.add(w.toLowerCase(Locale.ROOT))) pool.add(w);
            }
        }
        return pool;
    }

    /**
     * Wrong answers for `key`: words of the pool that are different from it, preferably of a
     * similar length (so the options look alike). `random` may be null (then the order is stable).
     */
    private static List<String> distractors(String key, List<String> pool, int count, Random random) {
        List<String> candidates = new ArrayList<>();
        for (String w : pool) {
            if (!w.equalsIgnoreCase(key)) candidates.add(w);
        }
        if (random != null) Collections.shuffle(candidates, random);
        candidates.sort((a, b) -> Integer.compare(
                Math.abs(a.length() - key.length()), Math.abs(b.length() - key.length())));
        return new ArrayList<>(candidates.subList(0, Math.min(count, candidates.size())));
    }

    /** Replaces the first whole-word occurrence of `word` in the sentence. */
    private static String replaceFirstWord(String sentence, String word, String replacement) {
        String regex = "(?<![\\p{L}\\p{N}])" + java.util.regex.Pattern.quote(word) + "(?![\\p{L}\\p{N}])";
        return sentence.replaceFirst(regex, java.util.regex.Matcher.quoteReplacement(replacement));
    }
}
