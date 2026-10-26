FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests || mvn clean package -DskipTests
EXPOSE 7070
CMD ["java", "-jar", "target/mavenproject1-1.0-SNAPSHOT.jar"]
