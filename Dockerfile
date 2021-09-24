FROM yti-docker-java11-base:alpine

ADD build/libs/yti-datamodel-api.jar yti-datamodel-api.jar

ENTRYPOINT ["/bootstrap.sh", "yti-datamodel-api.jar", "-j", "-Djava.security.egd=file:/dev/./urandom"]