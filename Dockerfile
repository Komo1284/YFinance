FROM openjdk:17-jdk-slim
ADD /build/libs/finance-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/templates /templates
COPY src/main/resources/static /static
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
