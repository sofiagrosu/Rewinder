package com.example.myapplication1.generation;

/**
 * Where the backend lives. The Gemini API key is NOT here: it stays on the server.
 *
 * BACKEND_URL:
 *   - Android emulator -> the computer running `node server.js`:  http://10.0.2.2:3000
 *   - real phone on the same Wi-Fi: http://<computer IP>:3000  (add it in network_security_config.xml)
 *   - production: an https:// URL (cleartext is only allowed for local addresses)
 *
 * APP_TOKEN is NOT a secret (anyone can unpack the APK). It only keeps random scanners away;
 * the real protection is the per-client rate limit on the server.
 */
public final class AiConfig {

    private AiConfig() {
    }

    public static final String BACKEND_URL = "https://rewinder-l2y1.onrender.com";
    public static final String APP_TOKEN = "rw-2589-zwkjl-2619pmg";

    public static final int MAX_PDF_BYTES = 10 * 1024 * 1024;
    public static final int[] COUNT_CHOICES = {5, 10, 15};
    public static final int DEFAULT_COUNT = 10;
}
