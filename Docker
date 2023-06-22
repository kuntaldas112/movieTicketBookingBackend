FROM openjdk:11-jdk
VOLUME /tmp
COPY target/movieapp-0.0.1-SNAPSHOT.jar movie.jar
ENTRYPOINT ["java", "-jar", "movie.jar"]