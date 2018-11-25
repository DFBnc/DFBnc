#!/bin/sh
set -e

REQUESTOU=${OU:="/C=GB/ST=DFBnc/L=DFBnc/O=DFBnc/OU=DFBnc/CN=dfbnc"}
FILE=".keystore.p12"

if [ ! -f $FILE ]; then
  openssl req -new -passout pass:password -newkey rsa:2048 -keyout key.pem -x509 -days 365 -out certificate.pem -subj "$REQUESTOU"
  openssl pkcs12 -inkey key.pem -in certificate.pem -export -out $FILE -password pass:password -passin pass:password
  rm certificate.pem
  rm key.pem
fi

exec "$@"
