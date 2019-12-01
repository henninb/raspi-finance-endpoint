FROM openjdk:8

ARG APP="raspi-finance-enpoint"
ENV APP raspi-finance-endpoint
RUN useradd henninb

RUN mkdir -p -m 0775 /opt/${APP}/bin
RUN mkdir -p -m 0775 /opt/${APP}/logs/archive
RUN mkdir -p -m 0775 /opt/${APP}/ssl
RUN mkdir -p -m 0775 /opt/${APP}/json_in
ADD ./build/libs/${APP}*.jar /opt/${APP}/bin/${APP}.jar
RUN chown -R henninb /opt/${APP}/*

WORKDIR /opt/${APP}/bin
USER henninb

CMD java -jar -Xmx1g /opt/${APP}/bin/${APP}.jar
