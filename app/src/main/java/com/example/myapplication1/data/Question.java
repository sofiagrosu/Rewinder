package com.example.myapplication1.data;

/**
 * One quiz question. Has 2 options (true/false) or 3 options (multiple choice),
 * so it always fits in a notification (max 3 action buttons).
 */
public class Question {

    private final long id;
    private final long subjectId;
    private final String text;
    private final String option1;
    private final String option2;
    private final String option3;        // null for true/false questions
    private final int correctOption;     // 1, 2 or 3 (same numbering as option1..option3)
    private final String explanation;    // optional, shown after answering; may be null

    public Question(long id, long subjectId, String text,
                    String option1, String option2, String option3,
                    int correctOption, String explanation) {
        this.id = id;
        this.subjectId = subjectId;
        this.text = text;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.correctOption = correctOption;
        this.explanation = explanation;
    }

    public long getId() {
        return id;
    }

    public long getSubjectId() {
        return subjectId;
    }

    public String getText() {
        return text;
    }

    public int getCorrectOption() {
        return correctOption;
    }

    public String getExplanation() {
        return explanation;
    }

    /** How many options this question has: 2 or 3. */
    public int getOptionCount() {
        if (option3 != null && !option3.isEmpty()) return 3;
        else return 2;
    }

    /** Text of option number n (1, 2 or 3). */
    public String getOption(int n) {
        switch (n) {
            case 1: return option1;
            case 2: return option2;
            case 3: return option3;
            default: return null;
        }
    }

    /** True if the chosen option number is the correct one. */
    public boolean isCorrect(int chosenOption) {
        return chosenOption == this.correctOption;
    }
}
