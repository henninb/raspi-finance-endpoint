# FROM openjdk:11.0.9.1
FROM docker.io/openjdk:11.0.9.1

ARG TIMEZONE="set the time zone at build time"
ENV TIMEZONE ${TIMEZONE}
ARG APP="set the app at build time"
ENV APP ${APP}
ARG USERNAME="set the username as build time"
ENV USERNAME ${USERNAME}

RUN useradd -M ${USERNAME}

RUN cp /usr/share/zoneinfo/${TIMEZONE} /etc/localtime
RUN mkdir -p -m 0755 /opt/${APP}/bin
RUN mkdir -p -m 0755 /opt/${APP}/logs/archive
RUN mkdir -p -m 0755 /opt/${APP}/ssl
RUN mkdir -p -m 0755 /opt/${APP}/excel_in
RUN mkdir -p -m 0755 /opt/${APP}/json_in
COPY ./ssl /opt/${APP}/ssl
ADD ./build/libs/${APP}*.jar /opt/${APP}/bin/${APP}.jar
RUN chown -R ${USERNAME}:${USERNAME} /opt/${APP}/*
RUN apt -y update 2> /dev/null
RUN apt install -y netcat 2> /dev/null

WORKDIR /opt/${APP}/bin
USER ${USERNAME}

# RUN sleep 10

# default on OSX was 522m, so increased to 2048
# RUN echo while ! nc -z hornsup 1521; do sleep 1; done >  run
# RUN echo java -Duser.timezone=${TIMEZONE} -Xmx2048m -jar /opt/${APP}/bin/${APP}.jar > run
# RUN chmod 755 run
CMD java -Duser.timezone=${TIMEZONE} -Xmx2048m -jar /opt/${APP}/bin/${APP}.jar
