FROM clojure:temurin-21-tools-deps-alpine AS deps
WORKDIR /build
COPY deps.edn ./
RUN clojure -P -X:build

FROM deps AS build
WORKDIR /build
COPY . .
RUN clojure -T:build uber

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /build/target/*-standalone.jar /app/ledger.jar
USER app
EXPOSE 8080 9090
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/ledger.jar"]
