FROM gradle:7.4.1-jdk11-alpine as builder
WORKDIR /
COPY . /
RUN gradle clean build --no-daemon

FROM openjdk:11-jre-slim
WORKDIR /
COPY --from=builder build/libs/discovery-0.0.1-SNAPSHOT.jar ./discovery.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "discovery.jar"]
