#!/bin/sh
set -e
#
# Copyright (c) 2000, 2025, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# http://oss.oracle.com/licenses/upl.
#

REVISION_POM=prj/pom.xml
CURRENT_VERSION=$(grep -E "<revision>" ${REVISION_POM} | sed 's/.*<revision>\(.*\)<\/revision>/\1/')

if [ "${CURRENT_VERSION}" = "" ]; then
  echo "Could not find current version from ${REVISION_POM}"
  exit 1
fi

echo "${CURRENT_VERSION}" | grep -q "SNAPSHOT"
if [ $? != 0 ] ; then
  echo "This job only deploys SNAPSHOT versions, skipping version ${CURRENT_VERSION}"
  exit 0
fi

echo "Building version ${CURRENT_VERSION}"
mvn -B clean install -Dproject.official=true --file prj/pom.xml -DskipTests -s .github/maven/settings.xml

echo "Deploying version ${CURRENT_VERSION}"
mvn -B deploy --file prj/pom.xml -DskipTests -s .github/maven/settings.xml -P-examples
