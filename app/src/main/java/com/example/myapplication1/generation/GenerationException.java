package com.example.myapplication1.generation;

/** Why generation failed. The screen maps {@link Kind} to a message the user understands. */
public class GenerationException extends Exception {

    public enum Kind {
        /** No internet, server down, timeout. */
        NETWORK,
        /** The AI quota is used up or the server is rate-limiting: try again in a minute. */
        BUSY,
        /** The material is too big (text or PDF). */
        TOO_LARGE,
        /** The AI answered, but nothing usable came out of it. */
        INVALID_RESPONSE,
        /** Anything else. */
        FAILED
    }

    private final Kind kind;

    public GenerationException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
