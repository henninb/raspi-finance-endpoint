FROM openjdk:11.0.14

ARG TIMEZONE="set the time zone at build time"
ENV TIMEZONE ${TIMEZONE}
ARG APP="set the app at build time"
ENV APP ${APP}
ARG USERNAME="set the username as build time"
ENV USERNAME ${USERNAME}
ARG CURRENT_GID="set the gid"
ENV CURRENT_GID ${CURRENT_GID}
ARG CURRENT_UID="set the uid"
ENV CURRENT_UID ${CURRENT_UID}

RUN groupadd -g ${CURRENT_GID} ${USERNAME}
RUN useradd -l ${USERNAME} -u ${CURRENT_UID} -g ${CURRENT_GID}

RUN cp /usr/share/zoneinfo/${TIMEZONE} /etc/localtime
RUN mkdir -p -m 0755 /opt/${APP}/bin
RUN mkdir -p -m 0755 /opt/${APP}/logs/archive
RUN mkdir -p -m 0755 /opt/${APP}/ssl
RUN mkdir -p -m 0755 /opt/${APP}/excel_in
RUN mkdir -p -m 0755 /opt/${APP}/json_in
COPY ./ssl /opt/${APP}/ssl
ADD ./build/libs/${APP}.jar /opt/${APP}/bin/${APP}.jar
RUN chown -R ${USERNAME}:${USERNAME} /opt/${APP}/*
# RUN apt -y update 2> /dev/null

WORKDIR /opt/${APP}/bin
USER ${USERNAME}

CMD java -Duser.timezone=${TIMEZONE} -Xmx2048m -jar /opt/${APP}/bin/${APP}.jar
