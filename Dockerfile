FROM openjdk:17-alpine

LABEL description="This service is part of E2E-tests implementation example. Check out repository for more details https://github.com/SimonHarmonicMinor/e2e-tests-example"

WORKDIR /app

COPY . /app

CMD ["/app/gradlew", ":e2e-tests:test"]