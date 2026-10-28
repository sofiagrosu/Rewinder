package com.example.myapplication1.data;

import java.util.List;

/**
 * Bridge between the two "roads to the data" that existed:
 *   - QuestionSource     (used by the quiz)
 *   - QuestionRepository (used by Home, Stats, Subjects, AI)
 *
 * Now the quiz reads its questions from the SAME repository as everybody else,
 * so a question created by the AI/upload screen can appear in a notification,
 * and HardcodedQuestionSource is no longer needed.
 */
public class RepositoryQuestionSource implements QuestionSource {

    private final QuestionRepository repository;

    public RepositoryQuestionSource(QuestionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Question> getQuestionsForSubject(long subjectId) {
        return repository.getQuestionsForSubject(subjectId);
    }

    @Override
    public Question getQuestionById(long questionId) {
        return repository.getQuestion(questionId);
    }
}
