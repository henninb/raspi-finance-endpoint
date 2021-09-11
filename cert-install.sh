#!/usr/bin/env sh

basedir="$HOME/projects/github.com/henninb"

mkdir -p "$HOME/ssl"

stty -echo
printf "Cert Password: "
read -r password
stty echo

if [ ! -f "$HOME/ssl/rootCA.pem" ]; then
  openssl genrsa -out "$HOME/ssl/rootCA.key" 2048
  openssl req -x509 -new -nodes -key "$HOME/ssl/rootCA.key" -sha256 -days 1024 -out "$HOME/ssl/rootCA.pem" -subj "/C=US/ST=Texas/L=Denton/O=Brian LLC/OU=prod/CN=Brian LLC rootCA"
fi

cat > v3.ext << EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = hornsup
DNS.2 = localhost
EOF

COMMON_NAME=hornsup
SUBJECT="/C=US/ST=Texas/L=Denton/O=Brian LLC/OU=None/CN=$COMMON_NAME"
openssl req -new -newkey rsa:2048 -sha256 -nodes -keyout hornsup.key -subj "$SUBJECT" -out hornsup.csr
openssl x509 -req -in hornsup.csr -CA "$HOME/ssl/rootCA.pem" -CAkey "$HOME/ssl/rootCA.key" -CAcreateserial -out hornsup.crt -days 365 -sha256 -extfile v3.ext

openssl pkcs12 -export -out hornsup.p12 -in hornsup.crt -inkey hornsup.key -name hornsup -password "pass:${password}"


# prompts for a password
rm -rf hornsup.jks
keytool -importkeystore -srckeystore hornsup.p12 -srcstoretype PKCS12 -destkeystore hornsup.jks -deststoretype JKS -keypass "${password}" -storepass "${password}"

cp hornsup.p12 "${basedir}/raspi-finance-endpoint/src/main/resources/hornsup-raspi-finance-keystore.p12"
cp hornsup.crt "${basedir}/raspi-finance-react/ssl/hornsup-raspi-finance-cert.pem"
cp hornsup.key "${basedir}/raspi-finance-react/ssl/hornsup-raspi-finance-key.pem"
cp hornsup.jks "${basedir}/raspi-finance-ratpack/ssl/hornsup-raspi-finance.jks"
cp hornsup.jks "${basedir}/example-ktor/hornsup-raspi-finance.jks"

exit 0
