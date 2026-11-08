package com.example.myapplication1.data;

/**
 * A question that was sent but never answered: it stayed on screen for its whole
 * protection time (see QuizCoordinator.PROTECTION_MILLIS) and disappeared.
 *
 * Kept apart from AnswerRecord on purpose: an ignored question is NOT a wrong answer,
 * so it must not lower the accuracy. Stats uses it only for "ignored" and "response rate".
 */
public class IgnoredRecord {

    private final long questionId;
    private final long subjectId;
    private final long timestampMillis;   // when the question was shown

    public IgnoredRecord(long questionId, long subjectId, long timestampMillis) {
        this.questionId = questionId;
        this.subjectId = subjectId;
        this.timestampMillis = timestampMillis;
    }

    public long getQuestionId() { return questionId; }
    public long getSubjectId() { return subjectId; }
    public long getTimestampMillis() { return timestampMillis; }
}
