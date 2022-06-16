FROM openjdk:17-alpine

LABEL description="This service is part of E2E-tests implementation example. Check out repository for more details https://github.com/SimonHarmonicMinor/e2e-tests-example"

WORKDIR /app

COPY build/libs/gain-service-0.0.1-SNAPSHOT.jar /app/application.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/application.jar"]