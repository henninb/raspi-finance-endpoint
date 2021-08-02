#!/bin/sh

APP=raspi-finance
SERVERNAME=hornsup

[ -z "$KEYSTORE_PASSWORD" ] && { echo "please set KEYSTORE_PASSWORD"; exit 1; }
TRUSTSTORE_PASSWORD="${KEYSTORE_PASSWORD}"

mkdir -p "$HOME/ssl/"
mkdir -p "$HOME/projects/raspi-finance-react/ssl/"
mkdir -p "$HOME/projects/raspi-finance-endpoint/ssl/"

# generate private key (change to a file extention of key)
echo openssl genrsa -out "ca.key.pem" 4096
openssl genrsa -out "ca.key.pem" 4096

# generate a root certificate (5 years)
echo openssl req -x509 -new -nodes -key "ca.key.pem" -sha256 -days 1825 -out root.ca.pem -subj "/C=US/ST=Texas/L=Denton/O=Brian LLC/OU=$SERVERNAME/CN=$SERVERNAME"
openssl req -x509 -new -nodes -key "ca.key.pem" -sha256 -days 1825 -out root.ca.pem -subj "/C=US/ST=Texas/L=Denton/O=Brian LLC/OU=$SERVERNAME/CN=$SERVERNAME"

cp -v ca.key.pem "$HOME/projects/raspi-finance-react/ssl/${SERVERNAME}-${APP}-key.pem"
cp -v root.ca.pem "$HOME/projects/raspi-finance-react/ssl/${SERVERNAME}-${APP}-cert.pem"

# converting root Certificate from PEM to PKCS12 Format - for browsers
# openssl pkcs12 -export -in root.ca.pem -out root.ca.pfx
echo openssl pkcs12 -export -out root.ca.pem.p12 -in root.ca.pem -inkey "ca.key.pem" -password "pass:${KEYSTORE_PASSWORD}"
openssl pkcs12 -export -out root.ca.pem.p12 -in root.ca.pem -inkey "ca.key.pem" -password "pass:${KEYSTORE_PASSWORD}"

cp -v root.ca.pem.p12 "src/main/resources/${SERVERNAME}-${APP}-keystore.p12"

rm -rf "${SERVERNAME}-${APP}-keystore.jks"
#echo delete alias from keystore
# keytool -delete -alias hornsup -keystore ${SERVERNAME}-${APP}-keystore.jks" -storepass "${KEYSTORE_PASSWORD}"
echo import into keytool
echo keytool -import -alias hornsup -trustcacerts -file root.ca.pem -keystore "${SERVERNAME}-${APP}-keystore.jks" -keypass "${KEYSTORE_PASSWORD}" -storepass "${TRUSTSTORE_PASSWORD}"

keytool -noprompt -import -alias hornsup -trustcacerts -file root.ca.pem -keystore "${SERVERNAME}-${APP}-keystore.jks" -keypass "${KEYSTORE_PASSWORD}" -storepass "${TRUSTSTORE_PASSWORD}"


# generate a private key for raspi-finance
#openssl genrsa -out raspi-finance.key 4096

# create the CSR (certificate signing request)
#openssl req -new -key raspi-finance.key -out raspi-finance.csr


#How import PEM file to keystore?
#To convert the PEM-format keys to Java KeyStores:
#Convert the certificate from PEM to PKCS12, using the following command

exit 0

rm -rf "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks"
# generate a keystore
keytool -genkey -keyalg RSA -alias "${SERVERNAME}-${APP}" -keystore "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" -storepass "${TRUSTSTORE_PASSWORD}" -keypass "${KEYSTORE_PASSWORD}" -validity 365 -keysize 4096 -dname "CN=$SERVERNAME, OU=$SERVERNAME, O=Brian LLC, L=Denton, ST=Texas, C=US"

# exporting to a der (cert)
keytool -export -alias "${SERVERNAME}-${APP}" -file "$HOME/ssl/${SERVERNAME}-${APP}.der" -keystore "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" -keypass "${KEYSTORE_PASSWORD}" -storepass "${TRUSTSTORE_PASSWORD}"

# exporting to a pem
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


# mkdir -p "$HOME/projects/raspi-finance-react/node_modules/webpack-dev-server/ssl"

echo keytool -list -v -keystore "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" -keypass "${KEYSTORE_PASSWORD}"
#keytool -list -v -keystore "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" -keypass "${KEYSTORE_PASSWORD}"

echo TODO: remove password prompt
keytool -noprompt -importkeystore -srckeystore "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" -destkeystore "$HOME/ssl/${SERVERNAME}-${APP}-keystore.p12" -deststoretype pkcs12 -keypass "${KEYSTORE_PASSWORD}" -storepass "${TRUSTSTORE_PASSWORD}"

openssl pkcs12 -in "$HOME/ssl/${SERVERNAME}-${APP}-keystore.p12" -nokeys -out "$HOME/ssl/${SERVERNAME}-${APP}-cert.pem" -password "pass:${KEYSTORE_PASSWORD}"

openssl pkcs12 -in "$HOME/ssl/${SERVERNAME}-${APP}-keystore.p12" -nocerts -nodes -out "$HOME/ssl/${SERVERNAME}-${APP}-key.pem" -password "pass:${KEYSTORE_PASSWORD}"

mkdir -p "$HOME/projects/raspi-finance-react/ssl/"
mkdir -p "$HOME/projects/raspi-finance-ncurses/ssl/"
sudo mkdir -p /etc/pki/tls/certs

sudo cp -v "$HOME/ssl/${SERVERNAME}-${APP}-cert.pem" "/etc/pki/tls/certs/"
cp -v "$HOME/ssl/${SERVERNAME}-${APP}-cert.pem" "$HOME/projects/raspi-finance-react/ssl/"
cp -v "$HOME/ssl/${SERVERNAME}-${APP}-cert.pem" "$HOME/projects/raspi-finance-ncurses/ssl/"
cp -v "$HOME/ssl/${SERVERNAME}-${APP}-key.pem" "$HOME/projects/raspi-finance-react/ssl/"
cp -v "$HOME/ssl/${SERVERNAME}-${APP}-keystore.p12" "$HOME/projects/raspi-finance-endpoint/src/main/resources/"
cp -v "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" "$HOME/projects/${APP}-convert/ssl"
cp -v "$HOME/ssl/${SERVERNAME}-${APP}-keystore.jks" "$HOME/projects/${APP}-endpoint/ssl"

echo sudo trust anchor --remove --verbose "pkcs11:id=%9F%AE%4A%84%68%2A%6E%A0%6A%79%CC%DA%F4%89%51%20%9B%C4%29%2B;type=cert"


echo curl-config --ca
echo export CURL_CA_BUNDLE=/path/to/cacert/ca-certificates.crt
sudo trust anchor "/etc/pki/tls/certs/${SERVERNAME}-${APP}-cert.pem"
echo curl --cacert "${SERVERNAME}-${APP}-cert.pem" https://hornsup:8443

exit 0

#concert PKCS12 key to unencrypted PEM:
openssl pkcs12 -in keystore.p12 -nodes -nocerts -out mydomain.key
