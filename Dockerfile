FROM openjdk:8
MAINTAINER haridas <haridas.kakunje@tarento.com>
ADD target/analytics-0.0.1-SNAPSHOT.jar analytics-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "/analytics-0.0.1-SNAPSHOT.jar"]
EXPOSE 8089
