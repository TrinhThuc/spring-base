FROM openjdk:21-jdk
WORKDIR /app
COPY ./target/*.jar ./app.jar
ENTRYPOINT ["java", "-jar", "./app.jar"]
EXPOSE 8081