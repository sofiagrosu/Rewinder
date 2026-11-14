package com.example.myapplication1.generation;

import android.util.Base64;

import com.example.myapplication1.data.Question;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Generates questions with the AI, through our backend (see /backend/server.js).
 * Replaces the old rule-based generator.
 *
 * generate() does network I/O: call it on a background thread, never on the main thread.
 *
 * The backend already validates the AI output; this class checks it AGAIN before building
 * Question objects, because a Question with a wrong correct-option would silently teach
 * the user a wrong answer.
 */
public class AiQuestionGenerator implements QuestionGenerator {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 100_000;   // the AI can take a while with a PDF

    private final String baseUrl;
    private final String appToken;

    public AiQuestionGenerator() {
        this(AiConfig.BACKEND_URL, AiConfig.APP_TOKEN);
    }

    public AiQuestionGenerator(String baseUrl, String appToken) {
        this.baseUrl = baseUrl;
        this.appToken = appToken;
    }

    @Override
    public List<Question> generate(GenerationRequest request, LongSupplier newId) throws GenerationException {
        String responseBody = post(buildBody(request));
        return parse(responseBody, request, newId);
    }

    // ---------------------------------------------------------------- request

    private static String buildBody(GenerationRequest r) throws GenerationException {
        try {
            JSONObject body = new JSONObject();
            if (!r.text.isEmpty()) body.put("text", r.text);
            if (r.pdf != null) {
                if (r.pdf.length > AiConfig.MAX_PDF_BYTES) {
                    throw new GenerationException(GenerationException.Kind.TOO_LARGE, "PDF too large");
                }
                body.put("pdfBase64", Base64.encodeToString(r.pdf, Base64.NO_WRAP));
            }
            JSONArray types = new JSONArray();
            for (QuestionType t : r.types) types.put(t.name());
            body.put("types", types);
            body.put("count", r.count);
            body.put("difficulty", r.difficulty.name());
            body.put("subject", r.subjectName);
            body.put("chapter", r.chapter);
            return body.toString();
        } catch (JSONException e) {
            throw new GenerationException(GenerationException.Kind.FAILED, e.getMessage());
        }
    }

    private String post(String json) throws GenerationException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(baseUrl + "/generate").openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("X-App-Key", appToken);

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(bytes);
            }

            int status = conn.getResponseCode();
            if (status == 200) {
                return readAll(conn.getInputStream());
            }
            String errorCode = errorCodeOf(conn.getErrorStream());
            throw new GenerationException(kindFor(status, errorCode), "HTTP " + status + " " + errorCode);
        } catch (IOException e) {
            throw new GenerationException(GenerationException.Kind.NETWORK, e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static GenerationException.Kind kindFor(int status, String code) {
        if (status == 413 || "TOO_LARGE".equals(code)) return GenerationException.Kind.TOO_LARGE;
        if (status == 429 || status == 503 || "RATE_LIMITED".equals(code) || "AI_BUSY".equals(code)) {
            return GenerationException.Kind.BUSY;
        }
        if ("AI_INVALID".equals(code)) return GenerationException.Kind.INVALID_RESPONSE;
        return GenerationException.Kind.FAILED;
    }

    private static String errorCodeOf(InputStream errorStream) {
        if (errorStream == null) return "";
        try {
            return new JSONObject(readAll(errorStream)).optString("error", "");
        } catch (IOException | JSONException e) {
            return "";
        }
    }

    private static String readAll(InputStream in) throws IOException {
        try (InputStream stream = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = stream.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toString("UTF-8");
        }
    }

    // ---------------------------------------------------------------- response

    /** JSON from the backend -> Question objects (invalid items are skipped). */
    static List<Question> parse(String responseBody, GenerationRequest request, LongSupplier newId)
            throws GenerationException {
        List<Question> result = new ArrayList<>();
        try {
            JSONArray items = new JSONObject(responseBody).getJSONArray("questions");
            for (int i = 0; i < items.length() && result.size() < request.count; i++) {
                Question q = toQuestion(items.getJSONObject(i), request, newId);
                if (q != null) result.add(q);
            }
        } catch (JSONException e) {
            throw new GenerationException(GenerationException.Kind.INVALID_RESPONSE, e.getMessage());
        }
        if (result.isEmpty()) {
            throw new GenerationException(GenerationException.Kind.INVALID_RESPONSE, "no usable questions");
        }
        return result;
    }

    private static Question toQuestion(JSONObject o, GenerationRequest request, LongSupplier newId) {
        QuestionType type;
        try {
            type = QuestionType.valueOf(o.optString("type"));
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (!request.types.contains(type)) return null;

        String text = o.optString("text", "").trim();
        JSONArray options = o.optJSONArray("options");
        int correct = o.optInt("correctIndex", -1);
        if (text.isEmpty() || options == null) return null;

        int wanted = (type == QuestionType.MISSING_WORD) ? 3 : 2;
        if (options.length() != wanted || correct < 0 || correct >= wanted) return null;

        String[] opt = new String[wanted];
        for (int i = 0; i < wanted; i++) {
            opt[i] = options.optString(i, "").trim();
            if (opt[i].isEmpty()) return null;
        }
        if (type != QuestionType.TRUE_FALSE && !text.contains("____")) return null;

        String explanation = o.optString("explanation", "").trim();
        return new Question(
                newId.getAsLong(),
                request.subjectId,
                text,
                opt[0],
                opt[1],
                wanted == 3 ? opt[2] : null,
                correct + 1,                                 // the app numbers options 1..3
                explanation.isEmpty() ? null : explanation);
    }
}
