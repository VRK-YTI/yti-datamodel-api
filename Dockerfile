FROM openjdk:8-jdk-alpine

ADD build/libs/yti-datamodel-api.jar yti-datamodel-api.jar

ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar yti-datamodel-api.jar" ]
