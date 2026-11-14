package com.example.myapplication1.generation;

import java.util.Set;

/**
 * Everything the generator needs. Material = pasted text and/or a PDF (bytes, may be null).
 * At least one of them is present (UploadMaterialFragment checks this).
 */
public class GenerationRequest {

    public final String text;               // may be empty when a PDF is given
    public final byte[] pdf;                // null if no PDF
    public final long subjectId;
    public final String subjectName;
    public final String chapter;            // may be empty
    public final Set<QuestionType> types;   // never empty
    public final int count;
    public final Difficulty difficulty;

    public GenerationRequest(String text, byte[] pdf, long subjectId, String subjectName,
                             String chapter, Set<QuestionType> types, int count, Difficulty difficulty) {
        this.text = text == null ? "" : text;
        this.pdf = pdf;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.chapter = chapter == null ? "" : chapter;
        this.types = types;
        this.count = count;
        this.difficulty = difficulty;
    }
}
