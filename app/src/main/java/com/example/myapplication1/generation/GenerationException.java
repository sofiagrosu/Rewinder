package com.example.myapplication1.generation;

/** Why generation failed. The screen maps {@link Kind} to a message the user understands. */
public class GenerationException extends Exception {

    public enum Kind {
        /** No internet, server down, timeout. */
        NETWORK,
        /** Gemini is overloaded or its quota is used up (also after trying the fallback models). */
        BUSY,
        /** Our own server's limit: too many requests from this phone in one minute. */
        RATE_LIMITED,
        /** The server refused the app key (APP_TOKEN in AiConfig differs from the server's). */
        UNAUTHORIZED,
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
