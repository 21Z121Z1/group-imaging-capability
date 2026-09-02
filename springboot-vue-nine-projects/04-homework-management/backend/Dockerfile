FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system app && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app
WORKDIR /app
COPY --chown=app:app target/*.jar /app/app.jar
USER app
EXPOSE 8104
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-Djava.security.egd=file:/dev/urandom","-jar","/app/app.jar"]
