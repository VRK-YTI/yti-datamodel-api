FROM yti-docker-java-base:corretto-11.0.22

ADD build/libs/yti-datamodel-api.jar yti-datamodel-api.jar

ENTRYPOINT ["/bootstrap.sh", "yti-datamodel-api.jar", "-j", "-Djava.security.egd=file:/dev/./urandom"]
