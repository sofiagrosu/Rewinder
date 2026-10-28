package com.example.myapplication1.data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Hard-coded data used until the real database exists:
 * 4 subjects, 8 questions each, plus a made-up answer history for the last 7 days
 * (so Home and Stats have something to show).
 *
 * The lists are public static so the database person can also use them to fill
 * the database on first launch: SampleData.subjects(), SampleData.questions().
 *
 * IMPORTANT: the questions were written quickly. Check every question and explanation
 * against your course before the app is shown to anyone.
 *
 * Ids: subjects 1..4; questions = subjectId * 100 + number (101..108, 201..208, ...).
 * Option rules (see Question): 2 options (true/false or two choices) or 3 options, correct = 1, 2 or 3.
 */
public final class SampleData {

    public static final long ALGORITHMS = 1;
    public static final long DATABASES = 2;
    public static final long NETWORKS = 3;
    public static final long OOP = 4;

    private static final long HOUR = 60L * 60L * 1000L;

    private SampleData() {
    }

    // ------------------------------------------------------------------ repository

    public static InMemoryQuestionRepository createRepository(long now, ZoneId zone) {
        List<Question> questions = questions();
        return new InMemoryQuestionRepository(
                subjects(),
                questions,
                answerHistory(questions, now, zone),
                momentFeedback(now));
    }

    // ------------------------------------------------------------------ subjects

    public static List<Subject> subjects() {
        List<Subject> list = new ArrayList<>();
        list.add(new Subject(ALGORITHMS, "Algorithms", "Sorting and searching", LocalDate.of(2026, 11, 12)));
        list.add(new Subject(DATABASES, "Databases", "SQL", LocalDate.of(2026, 11, 20)));
        list.add(new Subject(NETWORKS, "Networks", "The TCP/IP model", null));
        list.add(new Subject(OOP, "Programming", "OOP in Java", null));
        return list;
    }

    // ------------------------------------------------------------------ questions

    public static List<Question> questions() {
        List<Question> q = new ArrayList<>();

        // ---- Algorithms · Sorting and searching ----
        q.add(tf(101, ALGORITHMS, "The average-case complexity of Quick Sort is O(n log n).", true,
                "On average Quick Sort is O(n log n); the worst case is O(n²)."));
        q.add(two(102, ALGORITHMS, "Which of these sorting algorithms is stable?",
                "Merge Sort", "Quick Sort", 1,
                "Merge Sort keeps the order of equal elements; classic Quick Sort does not guarantee it."));
        q.add(tf(103, ALGORITHMS, "Insertion Sort runs in O(n) on an array that is already sorted.", true,
                "On a sorted array each element is compared only once, so the time is linear."));
        q.add(three(104, ALGORITHMS, "The complexity of Merge Sort, in every case, is:",
                "O(n log n)", "O(n²)", "O(n)", 1,
                "Merge Sort splits the array in halves (log n levels) and merges in O(n) on each level."));
        q.add(tf(105, ALGORITHMS, "Binary search works correctly on an unsorted array.", false,
                "Binary search requires the elements to be sorted."));
        q.add(two(106, ALGORITHMS, "The complexity of binary search is:",
                "O(log n)", "O(n)", 1,
                "At every step the search space is cut in half."));
        q.add(three(107, ALGORITHMS, "Heap Sort uses which data structure?",
                "Heap", "Stack", "Queue", 1,
                "Heap Sort builds a heap and repeatedly extracts the maximum (or minimum) element."));
        q.add(tf(108, ALGORITHMS, "Selection Sort makes O(n²) comparisons even on an already sorted array.", true,
                "Selection Sort searches for the minimum of the unsorted part at every step, whatever the initial order."));

        // ---- Databases · SQL ----
        q.add(tf(201, DATABASES, "The WHERE clause filters rows before grouping.", true,
                "WHERE applies to rows, before GROUP BY; HAVING filters the groups after grouping."));
        q.add(two(202, DATABASES, "Which clause filters the groups created by GROUP BY?",
                "HAVING", "WHERE", 1,
                "HAVING is applied after grouping and can use aggregate functions, unlike WHERE."));
        q.add(three(203, DATABASES, "Which JOIN returns only the rows that have a match in both tables?",
                "INNER JOIN", "LEFT JOIN", "FULL OUTER JOIN", 1,
                "INNER JOIN keeps only the rows that meet the join condition in both tables."));
        q.add(tf(204, DATABASES, "A primary key can contain NULL values.", false,
                "The primary key uniquely identifies each row, so it cannot be NULL."));
        q.add(three(205, DATABASES, "The SQL command that deletes the rows matching a condition is:",
                "DELETE", "DROP", "REMOVE", 1,
                "DELETE removes rows; DROP removes a whole table; REMOVE does not exist in standard SQL."));
        q.add(two(206, DATABASES, "What does the letter \"A\" in ACID stand for?",
                "Atomicity", "Authorization", 1,
                "ACID = Atomicity, Consistency, Isolation, Durability."));
        q.add(tf(207, DATABASES, "A foreign key usually refers to the primary key of another table.", true,
                "A foreign key links two tables and ensures referential integrity."));
        q.add(three(208, DATABASES, "The SQL function that counts the rows in a group is:",
                "COUNT", "SUM", "AVG", 1,
                "COUNT counts rows; SUM adds up values; AVG computes the average."));

        // ---- Networks · The TCP/IP model ----
        q.add(tf(301, NETWORKS, "TCP guarantees that data is delivered in order.", true,
                "TCP uses sequence numbers and acknowledgements, so data arrives complete and in order."));
        q.add(two(302, NETWORKS, "Which of these protocols is connectionless?",
                "UDP", "TCP", 1,
                "UDP sends datagrams without setting up a connection and without acknowledgements."));
        q.add(three(303, NETWORKS, "The default port for HTTPS is:",
                "443", "80", "21", 1,
                "HTTPS uses port 443; HTTP uses 80 and FTP uses 21."));
        q.add(tf(304, NETWORKS, "The IP address 192.168.1.10 is a private address.", true,
                "The range 192.168.0.0 - 192.168.255.255 is reserved for private networks."));
        q.add(two(305, NETWORKS, "Which protocol translates domain names into IP addresses?",
                "DNS", "ARP", 1,
                "DNS (Domain Name System) resolves names like example.com into IP addresses."));
        q.add(three(306, NETWORKS, "How many bits does an IPv4 address have?",
                "32", "64", "128", 1,
                "IPv4 has 32 bits (4 bytes); IPv6 has 128 bits."));
        q.add(tf(307, NETWORKS, "HTTP encrypts the transmitted data by default.", false,
                "HTTP sends data unencrypted; encryption is provided by HTTPS (HTTP over TLS)."));
        q.add(three(308, NETWORKS, "Which protocol automatically assigns IP addresses to the devices in a network?",
                "DHCP", "DNS", "ARP", 1,
                "DHCP automatically provides the IP address, subnet mask and gateway."));

        // ---- Programming · OOP in Java ----
        q.add(tf(401, OOP, "In Java, a class can directly extend two classes.", false,
                "Java does not allow multiple inheritance of classes; a class can, however, implement several interfaces."));
        q.add(two(402, OOP, "Which keyword prevents a class from being extended in Java?",
                "final", "static", 1,
                "A class declared final cannot be extended."));
        q.add(three(403, OOP, "Which OOP principle hides the internal details of an object?",
                "Encapsulation", "Inheritance", "Polymorphism", 1,
                "Encapsulation hides the internal state and exposes only public methods."));
        q.add(tf(404, OOP, "An abstract class can be instantiated directly.", false,
                "Abstract classes cannot be instantiated; only the concrete classes derived from them can."));
        q.add(two(405, OOP, "Overriding a method means, in the subclass:",
                "Same signature", "Same name, new params", 1,
                "The second option describes overloading."));
        q.add(three(406, OOP, "Which access modifier makes a member visible only inside its own class?",
                "private", "protected", "public", 1,
                "private limits access to the class in which the member is declared."));
        q.add(tf(407, OOP, "A Java interface can declare methods without an implementation.", true,
                "Interface methods are abstract by default, and the classes that implement the interface define them."));
        q.add(two(408, OOP, "What is the default value of an uninitialized object field of a class?",
                "null", "0", 1,
                "Reference-type fields are initialized with null by default."));

        return q;
    }

    private static Question tf(long id, long subjectId, String text, boolean isTrue, String explanation) {
        return new Question(id, subjectId, text, "True", "False", null, isTrue ? 1 : 2, explanation);
    }

    private static Question two(long id, long subjectId, String text,
                                String o1, String o2, int correct, String explanation) {
        return new Question(id, subjectId, text, o1, o2, null, correct, explanation);
    }

    private static Question three(long id, long subjectId, String text,
                                  String o1, String o2, String o3, int correct, String explanation) {
        return new Question(id, subjectId, text, o1, o2, o3, correct, explanation);
    }

    // ------------------------------------------------------------------ made-up history

    /**
     * 71 answers over the last 7 days (6 days ago has none, so the streak is 6).
     * Per subject accuracy: Algorithms 62%, Databases 35%, Networks 80%, Programming 10%.
     * Always looks the same (fixed random seed), relative to "now".
     */
    public static List<AnswerRecord> answerHistory(List<Question> questions, long now, ZoneId zone) {
        long[] subjectIds = {ALGORITHMS, DATABASES, NETWORKS, OOP};
        int[][] plan = {{21, 13}, {20, 7}, {20, 16}, {10, 1}};   // {answers, correct} per subject
        int[] perDay = {0, 10, 14, 8, 17, 8, 14};                // index 0 = 6 days ago ... 6 = today

        // pool of (subject index, correct?) pairs, shuffled in a fixed way
        List<int[]> pool = new ArrayList<>();
        for (int s = 0; s < plan.length; s++) {
            for (int i = 0; i < plan[s][0]; i++) {
                pool.add(new int[]{s, i < plan[s][1] ? 1 : 0});
            }
        }
        Collections.shuffle(pool, new Random(42));

        // questions grouped by subject
        List<List<Question>> bySubject = new ArrayList<>();
        for (long id : subjectIds) {
            List<Question> list = new ArrayList<>();
            for (Question qu : questions) {
                if (qu.getSubjectId() == id) list.add(qu);
            }
            bySubject.add(list);
        }
        int[] cursor = new int[subjectIds.length];

        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        List<AnswerRecord> result = new ArrayList<>();
        int next = 0;
        for (int d = 0; d < perDay.length; d++) {
            int n = perDay[d];
            long dayStart = today.minusDays(6 - d).atStartOfDay(zone).toInstant().toEpochMilli();
            for (int i = 0; i < n; i++) {
                int[] entry = pool.get(next++);
                int s = entry[0];
                boolean correct = entry[1] == 1;

                long ts;
                if (d == perDay.length - 1) {
                    // today: spread between midnight and "now", so nothing is in the future
                    ts = dayStart + (now - dayStart) * (i + 1) / (n + 1);
                } else {
                    ts = dayStart + 8 * HOUR + (10 * HOUR) * i / n;
                }

                List<Question> list = bySubject.get(s);
                Question question = list.get(cursor[s] % list.size());
                cursor[s]++;
                int chosen = correct ? question.getCorrectOption() : wrongOption(question);
                result.add(new AnswerRecord(question.getId(), question.getSubjectId(), ts, chosen, correct));
            }
        }
        return result;
    }

    private static int wrongOption(Question q) {
        for (int n = 1; n <= q.getOptionCount(); n++) {
            if (n != q.getCorrectOption()) return n;
        }
        return q.getCorrectOption();
    }

    /** 7 "Was this a good moment?" answers, 5 of them "Yes" -> 71%. */
    public static List<MomentFeedback> momentFeedback(long now) {
        boolean[] good = {true, true, false, true, true, false, true};
        List<MomentFeedback> list = new ArrayList<>();
        for (int i = 0; i < good.length; i++) {
            long ts = now - (good.length - 1 - i) * 24 * HOUR - HOUR;
            list.add(new MomentFeedback(ts, good[i]));
        }
        return list;
    }
}
