package com.example.myapplication1.data;

/**
 * The user's progress on ONE question (Leitner box, when it was last seen, counts).
 * Separate from Question: the question content is the same for everyone,
 * the progress belongs to this user and changes after every answer.
 * Times are in milliseconds (System.currentTimeMillis()); 0 means "never".
 */
public class QuestionProgress {

    public static final int MIN_BOX = 1;   // new or recently wrong: comes back soon
    public static final int MAX_BOX = 5;   // well known: comes back rarely

    private final long questionId;
    private int box;
    private long lastShownAt;      // when it was last sent as a notification
    private long lastAnsweredAt;   // when the user last answered it
    private int timesCorrect;
    private int timesWrong;

    public QuestionProgress(long questionId, int box, long lastShownAt, long lastAnsweredAt,
                            int timesCorrect, int timesWrong) {
        this.questionId = questionId;
        this.box = box;
        this.lastShownAt = lastShownAt;
        this.lastAnsweredAt = lastAnsweredAt;
        this.timesCorrect = timesCorrect;
        this.timesWrong = timesWrong;
    }

    /** Progress for a question the user has never seen. */
    public static QuestionProgress newFor(long questionId) {
        return new QuestionProgress(questionId, MIN_BOX, 0, 0, 0, 0);
    }

    /** True if the user has never answered this question. */
    public boolean isNew() {
        return lastAnsweredAt == 0;
    }

    // ---- getters ----

    public long getQuestionId() { return questionId; }
    public int getBox() { return box; }
    public long getLastShownAt() { return lastShownAt; }
    public long getLastAnsweredAt() { return lastAnsweredAt; }
    public int getTimesCorrect() { return timesCorrect; }
    public int getTimesWrong() { return timesWrong; }

    // ---- setters (used by ordering, e.g. LeitnerUpdater) ----

    public void setBox(int box) {
        // keep the box between MIN_BOX and MAX_BOX, whatever value comes in
        this.box = Math.max(MIN_BOX, Math.min(MAX_BOX, box));
    }

    public void setLastShownAt(long lastShownAt) { this.lastShownAt = lastShownAt; }
    public void setLastAnsweredAt(long lastAnsweredAt) { this.lastAnsweredAt = lastAnsweredAt; }
    public void setTimesCorrect(int timesCorrect) { this.timesCorrect = timesCorrect; }
    public void setTimesWrong(int timesWrong) { this.timesWrong = timesWrong; }
}
