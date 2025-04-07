FROM gradle:jdk17-alpine as builder
WORKDIR /
COPY . /
RUN gradle clean build --no-daemon

FROM openjdk:17-slim-buster
WORKDIR /
COPY --from=builder build/libs/gateway-0.0.1-SNAPSHOT.jar ./gateway.jar
EXPOSE 18080
ENTRYPOINT ["java", "-jar", "gateway.jar"]
