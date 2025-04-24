#!/usr/bin/env bash
#
# Copyright (c) 2000, 2025, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#

BASEDIR=$(dirname "$0")

PRINCIPAL_NAME=${1}
KEYSTORE_NAME=${2}
PERMISSIONS_XML=${3}
TRUST_STORE=test-ca1.p12

if [ "${PERMISSIONS_XML}" == "" ]
then
  PERMISSIONS_XML=cert-permissions.xml
fi


java -cp ${BASEDIR}/target/libs/coherence.jar:${BASEDIR}/target/classes:${BASEDIR}/target/test-classes \
    -Xms64M -Xmx128M \
    -Djava.net.preferIPv4Stack \
    -Dcoherence.client=remote \
    -Dcoherence.cacheconfig=cert-cache.config.xml \
    -Dcoherence.override=cert-override.xml \
    -Djava.security.auth.login.config=${BASEDIR}/target/test-classes/cert-login.config \
    -Dcoherence.member=${PRINCIPAL_NAME} \
    -Dcoherence.security.keystore=${BASEDIR}/certs/${KEYSTORE_NAME} \
    -Dcoherence.security.keystore.password=pa55w0rd \
    -Dcoherence.security.permissions=${BASEDIR}/target/test-classes/${PERMISSIONS_XML} \
    -Dcoherence.security.truststore=${BASEDIR}/certs/${TRUST_STORE} \
    -Dcoherence.security.truststore.password=s3cr37 \
    -Dcoherence.storage.authorizer=capture \
    -Dcoherence.cluster=CertSecurityTests \
    -Dcoherence.wka=127.0.0.1 \
    -Dcoherence.localhost=127.0.0.1 \
    -Dcoherence.ttl=0 \
    com.oracle.coherence.guides.security.SecureClient
