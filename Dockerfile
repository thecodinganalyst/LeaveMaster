FROM eclipse-temurin:25-jdk
WORKDIR /app
# Build the backend artifact first with ./backend/gradlew bootJar before running docker build.
COPY backend/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
