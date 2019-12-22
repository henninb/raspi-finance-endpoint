FROM openjdk:8

ARG TIMEZONE="set the time zone at build time"
ENV TIMEZONE ${TIMEZONE}
ARG APP="set the app at build time"
ENV APP ${APP}
ARG USERNAME="set the username as build time"
ENV USERNAME=${USERNAME}
RUN useradd ${USERNAME}

RUN cp /usr/share/zoneinfo/${TIMEZONE} /etc/localtime
RUN mkdir -p -m 0775 /opt/${APP}/bin
RUN mkdir -p -m 0775 /opt/${APP}/logs/archive
RUN mkdir -p -m 0775 /opt/${APP}/ssl
RUN mkdir -p -m 0775 /opt/${APP}/json_in
RUN mkdir -p -m 0775 /opt/${APP}/excel_in
RUN mkdir -p -m 0775 /opt/${APP}/config
RUN mkdir -p -m 0775 /opt/${APP}/json_out
ADD ./build/libs/${APP}*.jar /opt/${APP}/bin/${APP}.jar
RUN chown -R ${USERNAME}:${USERNAME} /opt/${APP}/*

WORKDIR /opt/${APP}/bin
USER ${USERNAME}

CMD java -Duser.timezone=${TIMEZONE} -jar /opt/${APP}/bin/${APP}.jar
