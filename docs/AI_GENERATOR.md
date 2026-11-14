# Generator de întrebări cu AI: decizie și arhitectură

## 1. Alegerea tehnologiei

**Alegere: Gemini API (Google AI Studio), model din familia Flash, prin backend propriu.**

| Criteriu | Gemini Flash / Flash-Lite |
|---|---|
| Cost | Există tier gratuit pentru modelele Flash și Flash-Lite, cu cote pe proiect. Modelele Pro nu mai sunt gratuite din aprilie 2026 (surse terțe, de reverificat). |
| PDF | Acceptă PDF direct în cerere (`inline_data`), deci nu avem nevoie de bibliotecă de extragere text pe telefon. |
| JSON | Suportă `responseSchema`: răspunsul vine într-un format impus, nu text liber. |
| Limite | Cifrele exacte (RPM/RPD) nu sunt publicate în documentație; se văd în contul tău din AI Studio. Surse terțe indică ordinul ~15 cereri/minut pe Flash, dar nu te baza pe asta. |

**Riscuri de cunoscut**
- **Confidențialitate:** pe tier-ul gratuit, Google își rezervă dreptul de a folosi conținutul trimis pentru îmbunătățirea modelelor. Notițele studenților ajung deci la Google. Pentru date sensibile, tier plătit sau alt furnizor.
- **Nume de modele instabile:** modelele se retrag (un 2.5 era programat pentru oprire în octombrie 2026). De aceea modelul este variabilă de mediu (`GEMINI_MODEL`), nu cod.
- **Calitate medicală:** AI-ul poate greși. Ecranul Review rămâne obligatoriu; promptul interzice informații din afara materialului.

## 2. Arhitectura

```
Aplicația Android  --HTTPS-->  Backend (Node, /generate)  --HTTPS-->  Gemini API
   (fără cheie)            cheia în variabilă de mediu,           răspuns JSON impus
                           rate limit, validare întrebări         de schemă
```

- Cheia Gemini există doar pe server (`GEMINI_API_KEY`). În APK nu e nimic secret.
- `APP_TOKEN` din aplicație **nu este secret** (se poate extrage din APK); descurajează doar scanările întâmplătoare. Protecția reală este limita pe client (6 cereri/minut/IP) și, pe viitor, autentificarea utilizatorului.
- Validare în două locuri: `backend/validate.js` și `AiQuestionGenerator.parse()`. O întrebare cu răspuns corect greșit ar învăța utilizatorul ceva fals, deci nu avem încredere oarbă în AI.

## 3. Fluxul în aplicație

1. `UploadMaterialFragment`: text și/sau PDF, materie, capitol, tipuri, număr (5/10/15), dificultate.
2. Generarea rulează pe fir separat (butonul arată „Generating…"); erorile au mesaje distincte: rețea, AI ocupat, material prea mare, răspuns invalid.
3. `AiQuestionGenerator` → `List<Question>` → `PendingReview` → `ReviewFragment` (neschimbat: editare, ștergere).
4. Doar la „Aprobă" se scrie în SQLite (mecanismul existent). Întrebările salvate funcționează offline: AI-ul e folosit doar la generare.

## 4. Maparea tipurilor

| Cerința | Tip în aplicație | Opțiuni |
|---|---|---|
| Adevărat/Fals | `TRUE_FALSE` | „Adevărat" / „Fals" (forțate de validator) |
| Alegere multiplă | `TWO_OPTIONS` | 2 (maxim 3 butoane într-o notificare, deci nu mai multe) |
| Completarea spațiului lipsă | `MISSING_WORD` | 3, cu „____" în enunț |

## 5. Rulare locală

```
cd backend
GEMINI_API_KEY=<cheia ta> APP_TOKEN=<același șir ca în AiConfig.java> node server.js
node test.js        # testele validatorului
```
Emulatorul Android ajunge la calculator pe `http://10.0.2.2:3000` (deja în `AiConfig`). Pentru telefon real adaugă IP-ul calculatorului în `network_security_config.xml`. Pentru producție: hosting cu HTTPS și `BACKEND_URL` actualizat.

## 6. Ce NU a fost testat
- Apelul real către Gemini (nu aveam cheie și acces la rețea). Structura cererii urmează documentația REST, dar prima rulare reală poate cere ajustări (numele modelului, schema).
- Compilarea aplicației Android (nu există SDK în mediul în care am lucrat).
- Testele de calitate pe PDF-uri diferite: trebuie făcute pe materiale reale.
