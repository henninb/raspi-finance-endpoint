#!/bin/sh

APP=raspi-finance
SERVERNAME=hornsup

[ -z "$KEYSTORE_PASSWORD" ] && { echo "please set KEYSTORE_PASSWORD"; exit 1; }
TRUSTSTORE_PASSWORD="${KEYSTORE_PASSWORD}"

mkdir -p "$HOME/ssl"

echo generate private key
openssl genrsa -out "$HOME/ssl/ca.key.pem" 4096

rm -rf "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks"
#-storepass
keytool -genkey -keyalg RSA -alias "${SERVERNAME}-${APP}" -keystore "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" -storepass "${TRUSTSTORE_PASSWORD}" -keypass "${KEYSTORE_PASSWORD}" -validity 365 -keysize 4096 -dname "CN=$SERVERNAME, OU=$SERVERNAME, O=Brian LLC, L=Denton, ST=Texas, C=US"
keytool -export -alias "${SERVERNAME}-${APP}" -file "$HOME/ssl/${SERVERNAME}-${APP}.der" -keystore "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" -keypass "${KEYSTORE_PASSWORD}" -storepass "${TRUSTSTORE_PASSWORD}"
keytool -export -rfc -alias "${SERVERNAME}-${APP}" -file "$HOME/ssl/${SERVERNAME}-${APP}.pem" -keystore "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" -keypass "${KEYSTORE_PASSWORD}" -storepass "${TRUSTSTORE_PASSWORD}"

echo convert the cert to PEM
openssl x509 -inform der -in "$HOME/ssl/${SERVERNAME}-${APP}.der" -out "$HOME/ssl/${SERVERNAME}-${APP}-cert"

# keytool -exportcert -rfc -keystore server.jks -storepass password -alias server > server.pem
#keytool -list -v -keystore keystore.jks
#-dname "CN=$SERVERNAME, OU=$SERVERNAME, O=Brian LLC, L=Denton, ST=Texas, C=US"

echo generate CSR - certificate signing request
openssl req -new -key "$HOME/ssl/ca.key.pem" -out "$HOME/ssl/ca.csr" -subj "/C=US/ST=Texas/L=Denton/O=Brian LLC/OU=$SERVERNAME/CN=$SERVERNAME"

openssl req -new -key "$HOME/ssl/ca.key.pem" -out "$HOME/ssl/${SERVERNAME}-${APP}.csr.pem" -subj "/C=US/ST=Texas/L=Denton/O=Brian LLC/OU=$SERVERNAME/CN=$SERVERNAME"

echo Generate Self Signed Key
openssl x509 -req -days 365 -in "$HOME/ssl/ca.csr" -signkey "$HOME/ssl/ca.key.pem" -out "$HOME/ssl/ca.crt.pem"

openssl x509 -req -days 365 -in "$HOME/ssl/${SERVERNAME}-${APP}.csr.pem" -signkey "$HOME/ssl/ca.key.pem" -out "$HOME/ssl/${SERVERNAME}-${APP}.crt.pem"
cp -v "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" "$HOME/projects/${APP}-convert/ssl"

cp -v "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" "$HOME/projects/${APP}-endpoint/ssl"

# mkdir -p "$HOME/projects/raspi-finance-react/node_modules/webpack-dev-server/ssl"

echo keytool -list -v -keystore "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" -keypass "${KEYSTORE_PASSWORD}"
#keytool -list -v -keystore "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" -keypass "${KEYSTORE_PASSWORD}"

keytool -noprompt -importkeystore -srckeystore "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" -destkeystore "$HOME/ssl/${SERVERNAME}-${APP}-keystore.p12" -deststoretype pkcs12 -keypass "${KEYSTORE_PASSWORD}"

openssl pkcs12 -in "$HOME/ssl/${SERVERNAME}-${APP}-keystore.p12" -nokeys -out "$HOME/ssl/${SERVERNAME}-${APP}-cert.pem"

openssl pkcs12 -in "$HOME/ssl/${SERVERNAME}-${APP}-keystore.p12" -nocerts -nodes -out "$HOME/ssl/${SERVERNAME}-${APP}-key.pem"

#openssl x509 -outform der -in your-cert.pem -out your-cert.crt

cp -v "$HOME/ssl/${SERVERNAME}-${APP}-cert.pem" "$HOME/projects/raspi-finance-react/ssl/"

cp -v "$HOME/ssl/${SERVERNAME}-${APP}-key.pem" "$HOME/projects/raspi-finance-react/ssl/"

cp -v "$HOME/ssl/${SERVERNAME}-${APP}-keystore.p12" "$HOME/projects/raspi-finance-endpoint/src/main/resources/"

echo curl --cacert archlinux-raspi-finance.pem https://archlinux:8080
echo curl --cacert hornsup-raspi-finance.pem https://hornsup:8080

exit 0

#concert PKCS12 key to unencrypted PEM:
openssl pkcs12 -in keystore.p12 -nodes -nocerts -out mydomain.key
