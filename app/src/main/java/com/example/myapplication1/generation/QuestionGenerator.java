package com.example.myapplication1.generation;

import com.example.myapplication1.data.Question;

import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Turns study material (plain text) into quiz questions.
 *
 * Today the only implementation is LocalQuestionGenerator (rules, works offline).
 * For the planned AI version (Version 2.0 of the plan) write an "AiQuestionGenerator"
 * that calls the API and returns the same List<Question>, then change ONE line in
 * UploadMaterialFragment. Review, saving and the quiz stay exactly the same.
 */
public interface QuestionGenerator {

    /**
     * @param text      the material (pasted notes)
     * @param subjectId the subject the questions belong to
     * @param types     which kinds of questions the user wants (never empty)
     * @param newId     gives a fresh question id on every call (repository.nextQuestionId)
     * @return the generated questions; empty if the text has no usable sentences
     */
    List<Question> generate(String text, long subjectId, Set<QuestionType> types, LongSupplier newId);
}
