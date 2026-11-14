# Ce s-a schimbat față de arhiva primită

## Home
- `HomeFragment`: „De ce acum?" deschide `WhyNowFragment` (prin `MainActivity.openScreen`); obiectivul zilnic vine din Settings (`QuizSettings.getDailyGoal`, implicit 20) în loc de constanta 20.
- `quiz/whynow/WhyNowFragment` + `fragment_why_now.xml` (NOU): afișează regulile reale ale quiz-ului (pași, ore de liniște, calendar), ultima întrebare trimisă și feedback „A fost un moment bun? Da/Nu" (salvat cu `saveMomentFeedback`, deci apare în Statistics).
- `quiz/trigger/TriggerStatus` (NOU) + 5 linii în `BreakDetector`: starea mișcării, doar pentru afișare.
- Settings (ecranul temporar): card „Daily goal" cu − / +, pași de 5, între 5 și 100.
- Calendarul afișează doar ora de final, nu titlul evenimentului.

## Statistics
- Afișate: răspunsuri, corecte, greșite, rata de răspuns, întrebări ignorate (7 zile). Acuratețea se calculează doar din `AnswerRecord`, deci ignoratele NU o afectează.

## ATENȚIE: suprapunere cu task-ul Sofiei (salvarea întrebărilor ignorate)
Pentru ca Statistics să funcționeze, am adăugat o variantă minimă. Dacă Sofia are una proprie, păstrați-o pe a ei și adaptați doar `StatsCalculator`:
- `data/IgnoredRecord` (NOU), `QuestionRepository.saveIgnored/getIgnoredSince` + implementările lor.
- `RewinderDbHelper`: versiunea 2, tabel `ignored_questions`; migrarea 1→2 păstrează datele existente.
- `QuizCoordinator`: o întrebare rămasă fără răspuns după `PROTECTION_MILLIS` (3 min) se salvează ca ignorată. Funcționează doar cât procesul aplicației e viu.
- Conflicte probabile la merge: `DB_VERSION`, `Rows`, `QuestionRepository`.

## Generatorul AI
- ȘTERS: `LocalQuestionGenerator`.
- NOU: `AiQuestionGenerator`, `GenerationRequest`, `GenerationException`, `Difficulty`, `AiConfig`; `QuestionGenerator` schimbat (cerere completă + excepție).
- `UploadMaterialFragment`: PDF, număr, dificultate, generare pe fundal. `AndroidManifest`: `INTERNET` + `network_security_config` (cleartext doar pentru 10.0.2.2/localhost).
- Backend în `/backend` (Node, fără dependențe).

## Probleme din "Probleme_si_rezolvari_actualizat.docx" (toate cele 9)

1. **Pași cu telefonul sau cu ceasul:** rămâne senzorul de pași al TELEFONULUI (`TYPE_STEP_DETECTOR`, timp real). Un ceas fără aplicație proprie nu poate trimite pași; varianta Health Connect are întârzieri de minute. Limită: telefonul uitat pe masă nu vede mersul. Neschimbat în cod.
2. **Wake lock:** nu mai expiră la 2 ore. Se reînnoiește la fiecare minut (limită de siguranță 5 min) și se eliberează cât monitorizarea e suspendată (calendar ocupat, ore de liniște) sau oprită de utilizator. Reluarea se face printr-o alarmă la sfârșitul intervalului (inexactă, în Doze poate întârzia câteva minute). `QuizService`, `TriggerConfig`.
3. **„Nu știu”:** buton în notificare (doar la întrebări cu 2 opțiuni: Android afișează maxim 3 butoane) și în banner (la toate). Afișează răspunsul corect și explicația. Se salvează ca răspuns NEcorect (opțiunea 0), deci apare în statistici la „greșite” și întrebarea revine mai devreme. Dacă preferați alt comportament, se schimbă în `QuizCoordinator.onAnswer`.
4. **Swipe:** închide definitiv; întrebarea se salvează ca ignorată (nu greșită) și nu mai reapare.
5. **„Este un moment nepotrivit pentru întrebări?”:** apare după swipe la prima întrebare din set. Da → următoarele două nu se trimit, reîncercare după 15 min. Nu → ciclul continuă.
6. **Repornire telefon:** `QuizBootReceiver` (+ `RECEIVE_BOOT_COMPLETED`), doar dacă monitorizarea nu fusese oprită. Oprirea din notificare sau din Settings (switch „Monitoring”) e păstrată.
7. **Calendar:** dezactivat implicit.
8. **Titluri:** nu se mai citesc; doar ora de început și de sfârșit.
9. **Mers detectat:** întrebarea afișată și promptul „moment nepotrivit” sunt anulate (și de pe ceas). Întrebarea retrasă NU se numără ca ignorată.

Test rapid reboot: `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p com.example.myapplication1`
