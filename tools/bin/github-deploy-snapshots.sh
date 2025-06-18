#!/bin/sh
#
# Copyright (c) 2000, 2025, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#

REVISION_POM=prj/coherence-bom/pom.xml
CURRENT_VERSION=$(grep -E "<revision>" ${REVISION_POM} | sed 's/.*<revision>\(.*\)<\/revision>/\1/')

if [ "${CURRENT_VERSION}" = "" ]; then
  echo "Could not find current version from Maven"
  exit 1
fi

if [ -z $(echo $CURRENT_VERSION | grep SNAPSHOT) ]; then
  echo "This job only deploys SNAPSHOT versions, skipping version ${CURRENT_VERSION}"
  exit 0
fi

echo "Building version ${CURRENT_VERSION}"
mvn -B clean install -Dproject.official=true -P-modules --file prj/pom.xml -DskipTests -s .github/maven/settings.xml
mvn -B clean install -Dproject.official=true -Pmodules,-coherence,docs -nsu --file prj/pom.xml -DskipTests -s .github/maven/settings.xml

echo "Deploying version ${CURRENT_VERSION}"
mvn -B deploy -Dproject.official=true -P-modules -nsu --file prj/pom.xml -DskipTests -s .github/maven/settings.xml
mvn -B deploy -Dproject.official=true -Pmodules,-coherence,docs -nsu --file prj/pom.xml -DskipTests -s .github/maven/settings.xml
