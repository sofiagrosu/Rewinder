package com.example.myapplication1.data;

import java.util.List;

/** Where quiz questions come from. Hard-coded for now, local database later. */
public interface QuestionSource {

    /** All questions of one subject. Empty list if there are none. */
    List<Question> getQuestionsForSubject(long subjectId);

    /** One question by its id, or null if it doesn't exist. */
    Question getQuestionById(long questionId);
}