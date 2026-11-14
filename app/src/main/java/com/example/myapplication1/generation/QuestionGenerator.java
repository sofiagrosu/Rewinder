package com.example.myapplication1.generation;

import com.example.myapplication1.data.Question;

import java.util.List;
import java.util.function.LongSupplier;

/**
 * Turns study material (text and/or a PDF) into quiz questions.
 *
 * The only implementation is AiQuestionGenerator (Gemini through our backend).
 * The rule-based LocalQuestionGenerator was removed. Review, saving and the quiz did not
 * change: they receive the same List<Question> as before.
 */
public interface QuestionGenerator {

    /**
     * Does network I/O: call it from a background thread.
     *
     * @param request the material and the settings chosen by the user
     * @param newId   gives a fresh question id on every call (repository.nextQuestionId)
     * @return the generated questions (never empty)
     * @throws GenerationException when no questions could be produced; see its Kind
     */
    List<Question> generate(GenerationRequest request, LongSupplier newId) throws GenerationException;
}
