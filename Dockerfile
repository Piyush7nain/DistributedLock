FROM openjdk:17
WORKDIR /app
COPY target/dist-lock-app-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

#
#COPY target/DistributedSystem-0.0.1-SNAPSHOT.jar app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]