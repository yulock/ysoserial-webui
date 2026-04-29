FROM eclipse-temurin:8-jdk

WORKDIR /app
COPY target/test-target.jar /app/app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
