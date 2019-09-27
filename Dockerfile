FROM openjdk:8

RUN useradd henninb
RUN mkdir -p /opt/raspi_finance_endpoint/bin /opt/raspi_finance_endpoint/ssl /opt/raspi_finance_endpoint/logs /opt/raspi_finance_endpoint/json_in
RUN chown -R henninb /opt/raspi_finance_endpoint/*

COPY ./build/libs/raspi_finance_endpoint.jar /opt/raspi_finance_endpoint/bin/raspi_finance_endpoint.jar
WORKDIR /opt/raspi_finance_endpoint/bin
#CMD ["java", "-jar" "/opt/raspi_finance_endpoint/raspi_finance_endpoint.jar"]

#RUN echo "172.17.0.1 hornsup" | tee -a /etc/hosts
#RUN ping $(ip route|awk '/default/ { print $3 }')
#hornsup:9092
USER henninb

CMD java -jar raspi_finance_endpoint.jar
