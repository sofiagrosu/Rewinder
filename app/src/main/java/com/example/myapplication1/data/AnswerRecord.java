package com.example.myapplication1.data;

/**
 * One answer given by the user. This is the history that Home and Stats are built from.
 *
 * Whoever shows a question (QuizEngine, notification, watch) must save one of these:
 *   repo.saveAnswer(new AnswerRecord(q.getId(), q.getSubjectId(),
 *           System.currentTimeMillis(), chosen, q.isCorrect(chosen)));
 */
public class AnswerRecord {

    private final long questionId;
    private final long subjectId;
    private final long timestampMillis;
    private final int chosenOption;     // 1, 2 or 3
    private final boolean correct;

    public AnswerRecord(long questionId, long subjectId, long timestampMillis,
                        int chosenOption, boolean correct) {
        this.questionId = questionId;
        this.subjectId = subjectId;
        this.timestampMillis = timestampMillis;
        this.chosenOption = chosenOption;
        this.correct = correct;
    }

    public long getQuestionId() {
        return questionId;
    }

    public long getSubjectId() {
        return subjectId;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public int getChosenOption() {
        return chosenOption;
    }

    public boolean isCorrect() {
        return correct;
    }
}

