# Skriftlig Utvärdering av Tillförlitlighet

---
## Introduktion och varför jag valde Groq

Jag testade först med `ChatGPT OpenAI API`, vilket jag senare
fick reda på hade kostat 5$, jag försökte sen med `Google Gemini API`, men
den "token" jag fick där tog slut direkt så jag fick inte chansen att få testa
min kod, vilket var frustrerande, sist men inte minst så testade jag
`Groq AI API` vilket är gratis och har tillräckligt med tokens, och det funkade
klockrent för och gjorde att jag kunde testa mitt project flertalet gånger och
slutligen göra klart det, jag testade med samma *Curl* kommando hela tiden, jag
använde mig utav denna:

```bash
curl.exe -X POST http://localhost:8080/api/ai/analyze -H 
"Content-Type: text/plain" -d "I love this product, it is absolutely amazing!"
```

Denna prompt är ju en väldigt positiv prompt och AIn har ju tre stycken
sentiments att gå efter `"POSITIVE|NEGATIVE|NEUTRAL"` så i denna prompt fick
jag svaret:

```json
{"sentiment":"POSITIVE",
  "score":0.95,
  "summary":"The user has a very strong positive opinion about the product."}
```
AIn ger då alltså ett väldigt kort men precist svar, precis som uppgiften
efterfrågar.

---

## 1. Promptstrategi

Jag valde att skriva systeminstruktionerna väldigt strikt och rakt på sak för
att tvinga modellen att bara svara med `JSON`, ingen markdown eller annan extra
text runt om. Anledningen till detta är att LLM:er ofta lägger till saker som 
*"Här är ditt svar:"* eller att det omsluter hela resultatet i ett `JSON`-kodblock,
vilket gör att svaret inte går att parsa direkt som en ren `JSON`.

Jag definierade då exakt ett schema som jag ville ha tillbaka:
```json
 {"sentiment": "POSITIVE|NEGATIVE|NEUTRAL",
  "score": 0.0-1.0,
  "summary": "one sentence explanation"}
```

Jag satte även temperaturens värde till 0.1 istället för standardvärdet,
eftersom jag ville ha så deterministiska svar jag kunde få som möjligt. Desto
högre temperatur desto mer kreativ blir modellen vilket är motsatsen till det
jag ville åstadkomma, jag vill ha korta och strikta svar som respons som
uppgiften sa.

---

## 2. Felhantering

**Timeouts**

Jag satte *ConnectTimeout* till 2000ms och *ReadTimeout* till 8000ms på 
**RestClienten**, om `Groq` nu skulle ta för lång tid på sig att ge ett svar
så kastas ett *timeout-fel* istället för att den ska fastna och hänga sig för
evigt. 

Jag testade detta genom att tillfälligt sätta *ReadTimeout* till 10ms,
vilket garanterat ger ett *timeout-fel* eftersom inget svar hinner komma
tillbaka så snabbt, detta har jag dokumenterat med hjälp av en skärmdump.

**429 Rate Limit**

Jag byggde ihop en loop som försöker max 3 gånger, om Groq svarar så skickas
`429 (för många requests)`. Mellan varje försök så väntar koden längre och
längre såhär: `1s. 2s.. 4s....` vilket kallas för exponentiell backoff. Om alla
3 försöken misslyckas så kastas ett fel: 

`RuntimeException("Failed after 3 retries due to rate limiting."`.

För att testa att detta verkligen fungerar så behövde jag bygga en 
`fake-controller` som alltid svarar `429`, så att jag kunde se att loopen körs
utan att behöva spamma Groqs riktiga API.

**Hallucination / Parsningsfel**

Om AI:n skulle svara med en trasig eller konstig `JSON` som inte går att läsa
in i min DTO så fångar jag det felet med hjälp utav `JsonProcessingException`
och returnerar då ett *fallback*-svar istället för att hela programmet ska
krascha:

```json
{"sentiment": "NEUTRAL",
"score": 0.0,
"summary": "Could not analyze sentiment."}
```

För att se att `fallbacken` faktiskt triggas så gjorde jag även en 
`fake-endpoint` som då returnerar ett påhittat svar som inte är i giltigt 
`JSON` svar.

---

## 3. Tillförlitlighetsbedömning

Det är väldigt viktigt att man inte förlitar sig blint på LLM:er, man behöver
vara självmedveten och kritisk, samt att man bör ifrågasätta dem ordentligt om
det verkligen stämmer, och gärna be om källanvisningar för att själv bekräfta
att det den säger faktiskt stämmer.

Även när man försöker styra dem med låg temperatur och strikta instruktioner
så kan modellen fortfarande hitta på saker eller bryta formatet helt utan
någon förvarning, speciellt om t.ex Groq skulle byta eller uppdatera modellen
i bakgrunden.

En annan begränsning som jag har nu är att jag är helt beroende av en gratis
tredjepartstjänst, om Groq har driftstopp eller om jag når deras rate limit
så fungerar inte min applikation alls, oavsett hur bra felhantering jag har
skrivit.

Jag har täckt en del viktiga risker i detta projekt:

- `Timeout` hantering så att appen inte hänger sig
- `Backoff` loop så den inte ger upp direkt vid rate limit
- `Fail-fast` validering av API nycklar vid uppstart
- `Fallback` svar om AI:n hallucinerar eller svarar med en trasig `JSON`
- `Bean Validation` som ett extra skyddsnät om `JSON` skulle vara giltig men
innehållet i den är fel, till exempel score över 1.0.

I en riktigt produktionsmiljö hade jag velat använda mig utav en betald
`API-nyckel` istället för en gratis version för att säkerställa att jag inte
når en rate limit men också för att ha en bättre AI överlag, men för den här
skoluppgiften så räcker det med denna gratis version för att demonstrera och
jag känner att jag täcker dem viktigaste scenarierna med felhantering som
efterfrågades i uppgiften.

---

*Skriven av:*

Robin Lindholm