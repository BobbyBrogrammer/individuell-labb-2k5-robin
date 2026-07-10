# Säkerhetsrapport - Individuell Labb 2k5

## 1. Identifierade Sårbarheter

### A01 - Broken Access Control

Min endpoint `/api/ai/analyze` var helt öppen vilket jag inte tänkte på från början.
Det betyder att vem som helst som känner till URL:en kan bara börja skicka requests
rakt in utan att behöva identifiera sig på något sätt, och jag hade inte lagt märke till
det förrän Groq-budgeten var slut.

### A06 - Vulnerable and Outdated Components

När jag körde `OWASP Dependency-Check` blev jag lite chockad, verktyget hittade hela
57 sårbarheter i mina beroenden. Det värsta var att `spring-core`, `spring-web` och
`tomcat-embed-core` alla fick CVSS 9.8 vilket är nästan max och klassas som **CRITICAL**.

Det roliga är att jag aldrig lagt till dessa själv, utan de följde med automatiskt när
jag lade till `Spring Boot`.

### A07 - Identification and Authentication Failures

Kopplat till A01, det fanns ingen autentisering alls i mitt projekt. Min Groq-nyckel
låg visserligen som en miljövariabel vilket är bättre än att hårdkoda den, men det
skyddade bara servern. Själva endpointen var fortfarande helt publik.

---

## 2. Åtgärder

### A01 & A07 - ApiKeyFilter

Jag skapade `ApiKeyFilter.java` som ett filter som körs på varje inkommande request.
Den kollar om rätt `X-API-KEY`-header skickas med, och om den saknas eller är fel
så stoppas requesten direkt med `HTTP 401`. 

Nyckeln läses från miljövariabeln
`APP_API_KEY` via `application.properties` så den aldrig ligger hårdkodad i koden.

### A05 - ObjectMapper som @Bean

Det här är en fix som jag faktiskt förstod värdet av under labben, eftersom vi tidigare har
följt reglerna av separation. Från början skapade jag `ObjectMapper` direkt i 
`AiClientService` med `new ObjectMapper()` vilket inte är optimalt. 

Jag flyttade den till `AppConfig.java` som en Spring `@Bean` istället,
vilket gör att `Spring` hanterar instansen och samma objekt återanvänds, det är
både säkrare och följer `Separation of Concerns` regeln mer korrekt.

### A06 - Dependency-Check i CI-pipelinen

Att uppdatera alla beroenden manuellt är inte realistiskt, så istället lade jag till
ett `security-scan`-job i `GitHub Actions` pipelinen. Så nu körs `OWASP Dependency-Check`
automatiskt vid varje push och rapporten sparas som en artefakt i GitHub så man kan
ladda ner och kolla igenom den.

---

## 3. Analys & Prioritering

**A01/A07 var det mest akuta att fixa**, det är den enklaste attacken att utföra och
kräver inga som helst kunskaper. Vem som helst med URL:en kunde ha börjat använda
min applikation gratis på min bekostnad. En API-nyckel är inte ett perfekt skydd men
det är ett stort steg bättre än ingenting.

**A06 går inte att helt lösa** eftersom sårbarheterna sitter i Spring Boots egna
beroenden och inte i kod jag skrivit själv. Vad jag kan göra är att automatiskt
hålla koll på nya CVE:er via CI-pipelinen och uppdatera Spring Boot-versionen när
patchar släpps.

**Om jag aldrig hade åtgärdat A01** och någon hittat endpointen hade de kunnat
anropa `/api/ai/analyze` hur många gånger som helst utan att jag kunde stoppa dem.
Men med `ApiKeyFilter` implementerat krävs en giltig nyckel som bara jag känner till.

---

*Skriven av:*

Robin Lindholm