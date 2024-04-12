FROM yti-docker-java-base:corretto-17.0.10

ADD build/libs/yti-datamodel-api.jar yti-datamodel-api.jar

ENTRYPOINT ["/bootstrap.sh", "yti-datamodel-api.jar", "-j", "-Djava.security.egd=file:/dev/./urandom"]
