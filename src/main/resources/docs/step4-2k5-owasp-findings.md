# Steg 4 - Identifierade OWASP-sårbarheter

## A01 - Broken Access Control
Endpointen `/api/ai/analyze` är helt öppen, det betyder att vem som helst kan anropa den utan
att identifiera sig, vilket i sin tur betyder att om någon hittar min URL kan de bara börja 
skicka requests och använda upp min GROQ API-free budget utan att jag får en chans att stoppa
dem.

## A06 - Vulnerable and Outdated Components
När jag körde OWASP Dependency-Check med kommandot:

`mvn dependency-check:check "-Dnvd.api.key=secret_key"`

Så fick jag `BUILD FAILURE`, vilket var meningen och verktyget hittade då `57`-sårbarheter i
mina beroenden. Det värsta var `spring-core`, `spring-web` och `tomcat-embed-core` dem alla 
fick `CVSS 9.8 (CRITICAL)` som man kan se i terminal dokumentationen, jag har ju inte lagt till 
dessa själv utan de kom automatiskt via `Spring Boot`.


## A07 - Identification and Authentication Failures
I mitt projekt finns det ingen autentisering alls, ingen behöver logga in eller skicka med någon
nyckel för att använda API:et. Min `Groq`-nyckel ligger som en miljövariabel men det skyddar 
bara servern inte själva `endpointen` som är publik.