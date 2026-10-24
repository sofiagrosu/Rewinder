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

    public static final long ALGORITMI = 1;
    public static final long BAZE_DE_DATE = 2;
    public static final long RETELE = 3;
    public static final long POO = 4;

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
        list.add(new Subject(ALGORITMI, "Algoritmi", "Sortare și căutare", LocalDate.of(2026, 11, 12)));
        list.add(new Subject(BAZE_DE_DATE, "Baze de date", "SQL", LocalDate.of(2026, 11, 20)));
        list.add(new Subject(RETELE, "Rețele", "Modelul TCP/IP", null));
        list.add(new Subject(POO, "Programare", "POO în Java", null));
        return list;
    }

    // ------------------------------------------------------------------ questions

    public static List<Question> questions() {
        List<Question> q = new ArrayList<>();

        // ---- Algoritmi · Sortare și căutare ----
        q.add(tf(101, ALGORITMI, "Complexitatea medie a algoritmului Quick Sort este O(n log n).", true,
                "În cazul mediu Quick Sort este O(n log n); cazul cel mai defavorabil este O(n²)."));
        q.add(two(102, ALGORITMI, "Care dintre acești algoritmi de sortare este stabil?",
                "Merge Sort", "Quick Sort (varianta clasică)", 1,
                "Merge Sort păstrează ordinea elementelor egale; Quick Sort în varianta obișnuită nu o garantează."));
        q.add(tf(103, ALGORITMI, "Insertion Sort are complexitatea O(n) pe un tablou deja sortat.", true,
                "Pe un tablou sortat, fiecare element este comparat o singură dată, deci timpul este liniar."));
        q.add(three(104, ALGORITMI, "Complexitatea algoritmului Merge Sort, în toate cazurile, este:",
                "O(n log n)", "O(n²)", "O(n)", 1,
                "Merge Sort împarte tabloul în jumătăți (log n niveluri) și interclasează în O(n) pe fiecare nivel."));
        q.add(tf(105, ALGORITMI, "Căutarea binară funcționează corect pe un tablou nesortat.", false,
                "Căutarea binară cere ca elementele să fie sortate."));
        q.add(two(106, ALGORITMI, "Căutarea binară are complexitatea:",
                "O(log n)", "O(n)", 1,
                "La fiecare pas, spațiul de căutare se înjumătățește."));
        q.add(three(107, ALGORITMI, "Algoritmul Heap Sort folosește structura de date:",
                "Heap (ansamblu)", "Stivă", "Coadă", 1,
                "Heap Sort construiește un heap și extrage repetat elementul maxim (sau minim)."));
        q.add(tf(108, ALGORITMI, "Selection Sort face O(n²) comparații chiar și pe un tablou deja sortat.", true,
                "Selection Sort caută minimul în partea nesortată la fiecare pas, indiferent de ordinea inițială."));

        // ---- Baze de date · SQL ----
        q.add(tf(201, BAZE_DE_DATE, "Clauza WHERE filtrează rândurile înainte de grupare.", true,
                "WHERE se aplică rândurilor, înainte de GROUP BY; HAVING filtrează grupurile după grupare."));
        q.add(two(202, BAZE_DE_DATE, "Care clauză filtrează grupurile create de GROUP BY?",
                "HAVING", "WHERE", 1,
                "HAVING se aplică după grupare și poate folosi funcții de agregare, spre deosebire de WHERE."));
        q.add(three(203, BAZE_DE_DATE, "Ce tip de JOIN returnează doar rândurile care au corespondent în ambele tabele?",
                "INNER JOIN", "LEFT JOIN", "FULL OUTER JOIN", 1,
                "INNER JOIN păstrează doar rândurile care îndeplinesc condiția de legătură în ambele tabele."));
        q.add(tf(204, BAZE_DE_DATE, "O cheie primară poate conține valori NULL.", false,
                "Cheia primară identifică unic fiecare rând, deci nu poate fi NULL."));
        q.add(three(205, BAZE_DE_DATE, "Comanda SQL care șterge rândurile ce respectă o condiție este:",
                "DELETE", "DROP", "REMOVE", 1,
                "DELETE șterge rânduri; DROP șterge un tabel întreg; REMOVE nu există în SQL standard."));
        q.add(two(206, BAZE_DE_DATE, "Ce înseamnă litera „A” din ACID?",
                "Atomicitate", "Autorizare", 1,
                "ACID = Atomicitate, Consistență, Izolare, Durabilitate."));
        q.add(tf(207, BAZE_DE_DATE, "O cheie străină face referire, de obicei, la cheia primară a altui tabel.", true,
                "Cheia străină leagă două tabele și asigură integritatea referențială."));
        q.add(three(208, BAZE_DE_DATE, "Funcția SQL care numără rândurile dintr-un grup este:",
                "COUNT", "SUM", "AVG", 1,
                "COUNT numără rândurile; SUM adună valori; AVG calculează media."));

        // ---- Rețele · Modelul TCP/IP ----
        q.add(tf(301, RETELE, "TCP garantează livrarea ordonată a datelor.", true,
                "TCP folosește numere de secvență și confirmări, deci datele ajung complete și în ordine."));
        q.add(two(302, RETELE, "Care dintre aceste protocoale este fără conexiune?",
                "UDP", "TCP", 1,
                "UDP trimite datagrame fără a stabili o conexiune și fără confirmări."));
        q.add(three(303, RETELE, "Portul implicit pentru HTTPS este:",
                "443", "80", "21", 1,
                "HTTPS folosește portul 443; HTTP folosește 80, iar FTP folosește 21."));
        q.add(tf(304, RETELE, "Adresa IP 192.168.1.10 este o adresă privată.", true,
                "Intervalul 192.168.0.0 - 192.168.255.255 este rezervat pentru rețele private."));
        q.add(two(305, RETELE, "Ce protocol traduce numele de domenii în adrese IP?",
                "DNS", "ARP", 1,
                "DNS (Domain Name System) rezolvă nume precum example.com în adrese IP."));
        q.add(three(306, RETELE, "Câți biți are o adresă IPv4?",
                "32", "64", "128", 1,
                "IPv4 are 32 de biți (4 octeți); IPv6 are 128 de biți."));
        q.add(tf(307, RETELE, "Protocolul HTTP criptează implicit datele transmise.", false,
                "HTTP transmite datele necriptat; criptarea este oferită de HTTPS (HTTP peste TLS)."));
        q.add(three(308, RETELE, "Ce protocol atribuie automat adrese IP dispozitivelor dintr-o rețea?",
                "DHCP", "DNS", "ARP", 1,
                "DHCP oferă automat adresa IP, masca de rețea și gateway-ul."));

        // ---- Programare · POO în Java ----
        q.add(tf(401, POO, "În Java, o clasă poate moșteni direct de la două clase.", false,
                "Java nu permite moștenirea multiplă de clase; o clasă poate însă implementa mai multe interfețe."));
        q.add(two(402, POO, "Ce cuvânt cheie împiedică moștenirea unei clase în Java?",
                "final", "static", 1,
                "O clasă declarată final nu poate fi extinsă."));
        q.add(three(403, POO, "Ce principiu POO ascunde detaliile interne ale unui obiect?",
                "Încapsularea", "Moștenirea", "Polimorfismul", 1,
                "Încapsularea ascunde starea internă și expune doar metode publice."));
        q.add(tf(404, POO, "O clasă abstractă poate fi instanțiată direct.", false,
                "Clasele abstracte nu pot fi instanțiate; se instanțiază doar clasele concrete derivate din ele."));
        q.add(two(405, POO, "Suprascrierea (override) unei metode presupune:",
                "Aceeași semnătură, în clasa derivată", "Același nume, dar alți parametri", 1,
                "A doua variantă descrie supraîncărcarea (overload)."));
        q.add(three(406, POO, "Ce modificator de acces face un membru vizibil doar în propria clasă?",
                "private", "protected", "public", 1,
                "private limitează accesul la clasa în care este declarat membrul."));
        q.add(tf(407, POO, "Într-o interfață Java se pot declara metode fără implementare.", true,
                "Metodele unei interfețe sunt abstracte în mod implicit, iar clasele care o implementează le definesc."));
        q.add(two(408, POO, "Ce valoare implicită are un câmp de tip obiect, neinițializat, al unei clase?",
                "null", "0", 1,
                "Câmpurile de tip referință sunt inițializate implicit cu null."));

        return q;
    }

    private static Question tf(long id, long subjectId, String text, boolean isTrue, String explanation) {
        return new Question(id, subjectId, text, "Adevărat", "Fals", null, isTrue ? 1 : 2, explanation);
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
     * Per subject accuracy: Algoritmi 62%, Baze de date 35%, Rețele 80%, Programare 10%.
     * Always looks the same (fixed random seed), relative to "now".
     */
    public static List<AnswerRecord> answerHistory(List<Question> questions, long now, ZoneId zone) {
        long[] subjectIds = {ALGORITMI, BAZE_DE_DATE, RETELE, POO};
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

    /** 7 "A fost un moment bun?" answers, 5 of them "Da" -> 71%. */
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
