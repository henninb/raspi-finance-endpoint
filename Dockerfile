FROM openjdk:8

RUN useradd henninb

RUN mkdir -p /opt/raspi_finance_endpoint/bin
RUN mkdir -p /opt/raspi_finance_endpoint/ssl
RUN mkdir -p /opt/raspi_finance_endpoint/logs
RUN mkdir -p /opt/raspi_finance_endpoint/json_in
RUN chown -R henninb /opt/raspi_finance_endpoint/*

COPY ./build/libs/raspi-finance-endpoint.jar /opt/raspi_finance_endpoint/bin/raspi-finance-endpoint.jar
WORKDIR /opt/raspi_finance_endpoint/bin
USER henninb

CMD java -jar raspi-finance-endpoint.jar
