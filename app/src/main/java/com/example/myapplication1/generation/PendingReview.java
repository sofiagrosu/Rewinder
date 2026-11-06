package com.example.myapplication1.generation;

import com.example.myapplication1.data.Question;
import com.example.myapplication1.data.Subject;

import java.util.ArrayList;
import java.util.List;

/**
 * The generated questions that wait for the user's check (the Review screen).
 *
 * They live ONLY here, in memory: the database is not touched until the user taps
 * "Aprobă toate". So a question that was not checked can never appear in the quiz,
 * and no database change was needed.
 * (If the app process is killed while reviewing, the pending questions are lost:
 * the user simply generates them again. Acceptable for this stage of the project.)
 */
public final class PendingReview {

    private static Subject subject;                 // may be new (not saved yet)
    private static final List<Question> questions = new ArrayList<>();

    private PendingReview() {
    }

    public static synchronized void set(Subject newSubject, List<Question> newQuestions) {
        subject = newSubject;
        questions.clear();
        questions.addAll(newQuestions);
    }

    public static synchronized Subject getSubject() {
        return subject;
    }

    /** The live list: the Review screen edits/removes items directly in it. */
    public static synchronized List<Question> getQuestions() {
        return questions;
    }

    public static synchronized void clear() {
        subject = null;
        questions.clear();
    }
}
