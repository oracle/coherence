#!/bin/sh
set -e
#
# Copyright (c) 2000, 2026, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#

pwd
cd prj
CURRENT_VERSION=$(./mvnw --file pom.xml help:evaluate -Dexpression=project.version -DforceStdout -q -nsu)

if [ "${CURRENT_VERSION}" = "" ]; then
  echo "Could not find current version from Maven"
  exit 1
fi

if ! echo "${CURRENT_VERSION}" | grep -q "SNAPSHOT"; then
  echo "This job only deploys SNAPSHOT versions, skipping version ${CURRENT_VERSION}"
  exit 0
fi

echo "Building version ${CURRENT_VERSION}"
./mvnw -B clean install -Dproject.official=true -P-modules --file pom.xml -DskipTests -s ../.github/maven/settings.xml
./mvnw -B clean install -Dproject.official=true -Pmodules,-coherence,docs -nsu --file pom.xml -DskipTests -s ../.github/maven/settings.xml

echo "Deploying version ${CURRENT_VERSION}"
./mvnw -B clean deploy -Dproject.official=true -P-modules -nsu --file pom.xml -DskipTests -s ../.github/maven/settings.xml
./mvnw -B clean deploy -Dproject.official=true -Pmodules,-coherence,docs -nsu --file pom.xml -DskipTests -s ../.github/maven/settings.xml
