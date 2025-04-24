#!/usr/bin/env bash
#
# Copyright (c) 2000, 2025, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#
set -e

CERT_DIR=certs
KEY_PASS="pa55w0rd"
STORE_PASS="pa55w0rd"
TRUST_PASS="s3cr37"

rm -rf ${CERT_DIR}
mkdir ${CERT_DIR}

create_ca() {
  echo "Generating CA key ${1}.key"
  openssl genrsa -passout pass:1111 -aes256 \
      -out ${CERT_DIR}/${1}.key 4096

  echo "Generating CA certificate ${1}.crt"
  openssl req -passin pass:1111 -new -x509 -days 3650 \
      -key ${CERT_DIR}/${1}.key \
      -out ${CERT_DIR}/${1}.crt \
      -subj "/CN=${1}"

  echo "Generating CA PKCS12 trust store ${1}.p12"
  keytool -import -storepass ${TRUST_PASS} -noprompt -trustcacerts \
      -alias "${1}" -file ${CERT_DIR}/${1}.crt \
      -keystore ${CERT_DIR}/${1}.p12 \
      -deststoretype PKCS12

  echo "Importing CA cert into PKCS12 all CA trust store trust-all.p12"
  keytool -import -storepass ${TRUST_PASS} -noprompt -trustcacerts \
      -alias "${1}" -file ${CERT_DIR}/${1}.crt \
      -keystore ${CERT_DIR}/trust-all.p12 \
      -deststoretype PKCS12
}

create_key_and_cert() {
  echo "Generating private key ${1}.key"
  openssl genrsa -passout pass:1111 -aes256 \
    -out ${CERT_DIR}/${1}.key 4096

  echo "Generating signing request ${1}.csr"
  openssl req -passin pass:1111 -new -key \
    ${CERT_DIR}/${1}.key \
    -out ${CERT_DIR}/${1}.csr \
    -subj "/CN=${2}"

  # Create the signed certificate:
  echo "Generating signed certificate ${1}.crt"
  openssl x509 -req -passin pass:1111 -days 3650 \
    -in ${CERT_DIR}/${1}.csr \
    -CA ${CERT_DIR}/${3}.crt \
    -CAkey ${CERT_DIR}/${3}.key \
    -set_serial 01 \
    -out ${CERT_DIR}/${1}.crt

  echo "Remove passphrase from ${1} key"
  openssl rsa -passin pass:1111 \
      -in ${CERT_DIR}/${1}.key \
      -out ${CERT_DIR}/${1}.key

  echo "Generating PEM file ${1}.pem"
  openssl pkcs8 -topk8 -nocrypt \
      -in ${CERT_DIR}/${1}.key \
      -passout pass:${KEY_PASS} \
      -out ${CERT_DIR}/${1}.pem

  echo "Generating PKCS12 key store member${1}.p12"
  openssl pkcs12 -export -passout pass:${STORE_PASS} \
      -inkey ${CERT_DIR}/${1}.pem \
      -name ${2} -in ${CERT_DIR}/${1}.crt \
      -out ${CERT_DIR}/${1}.p12
}

# Create the first CA cert
create_ca "test-ca1"
# Create five keys and certs signed by the first CA
create_key_and_cert "member-1" "member-1" "test-ca1"
create_key_and_cert "member-2" "member-2" "test-ca1"
create_key_and_cert "member-3" "member-3" "test-ca1"
create_key_and_cert "member-4" "member-4" "test-ca1"
create_key_and_cert "member-5" "member-5" "test-ca1"

# Create a second CA cert
create_ca "test-ca2"
# Create a key and cert with the CN member-1 signed by the second CA
create_key_and_cert "untrusted" "member-1" "test-ca2"

