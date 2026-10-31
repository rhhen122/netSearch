FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /data
RUN mkdir -p /app /data && \
    useradd --system --uid 10001 --create-home app && \
    chown app:app /data
COPY --from=build /build/target/netSearch-1.1.0.jar /app/netSearch.jar

VOLUME ["/data"]
USER app
ENTRYPOINT ["java", "-jar", "/app/netSearch.jar"]
CMD ["help"]
