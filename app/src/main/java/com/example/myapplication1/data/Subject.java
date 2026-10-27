package com.example.myapplication1.data;

import java.time.LocalDate;

/**
 * A study subject + chapter, e.g. "Algoritmi · Sortare și căutare".
 * The number of questions and the progress are NOT stored here:
 * they are computed from the repository (see StatsCalculator).
 */
public class Subject {

    private final long id;
    private final String name;      // "Algoritmi"
    private final String topic;     // "Sortare și căutare"
    private final LocalDate examDate; // may be null (no exam set)

    public Subject(long id, String name, String topic, LocalDate examDate) {
        this.id = id;
        this.name = name;
        this.topic = topic;
        this.examDate = examDate;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTopic() {
        return topic;
    }

    /** May be null. */
    public LocalDate getExamDate() {
        return examDate;
    }

    /** "Algoritmi · Sortare și căutare" - the way it is shown on screen. */
    public String getTitle() {
        if (topic == null || topic.isEmpty()) return name;
        return name + " \u00B7 " + topic;
    }
}
