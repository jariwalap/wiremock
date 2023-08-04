# RUN
FROM eclipse-temurin:11.0.19_7-jre

WORKDIR /home/wiremock

ADD build/libs/wiremock-standalone-3.0.0-beta-8.jar /var/wiremock/lib/wiremock-jre8-standalone.jar

COPY docker-entrypoint.sh /
RUN chmod a+x /docker-entrypoint.sh

# Init WireMock files structure
RUN mkdir -p /home/wiremock/mappings && \
	mkdir -p /home/wiremock/__files && \
	mkdir -p /var/wiremock/extensions

EXPOSE 8080 8443

ENTRYPOINT ["/docker-entrypoint.sh"]