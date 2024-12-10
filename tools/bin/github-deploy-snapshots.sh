#!/bin/sh
set -e
#
# Copyright (c) 2000, 2022, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# http://oss.oracle.com/licenses/upl.
#

CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

if [ "${CURRENT_VERSION}" = "" ]; then
  echo "Could not find current version from Maven"
  exit 1
fi

echo "${CURRENT_VERSION}" | grep -q "SNAPSHOT"
if [ $? != 0 ] ; then
  echo "This job only deploys SNAPSHOT versions, skipping version ${CURRENT_VERSION}"
  exit 0
fi

echo "Building version ${CURRENT_VERSION}"
mvn -B clean install -Dproject.official=true -P-modules --file prj/pom.xml -DskipTests -s .github/maven/settings.xml
mvn -B clean install -Dproject.official=true -Pmodules,-coherence,docs -nsu --file prj/pom.xml -DskipTests -s .github/maven/settings.xml

echo "Deploying version ${CURRENT_VERSION}"
mvn -B clean deploy -Dproject.official=true -P-modules -nsu --file prj/pom.xml -DskipTests -s .github/maven/settings.xml
mvn -B clean deploy -Dproject.official=true -Pmodules,-coherence,docs -nsu --file prj/pom.xml -DskipTests -s .github/maven/settings.xml

echo "Deploying docs for version ${CURRENT_VERSION}"
git stash save --keep-index --include-untracked || true
git stash drop || true
git checkout gh-pages
git config pull.rebase true
git pull

rm -rf "${CURRENT_VERSION}" || true
mkdir -p "${CURRENT_VERSION}"/api || true
mv prj/coherence-javadoc/target/javadoc/apidocs "${CURRENT_VERSION}"/api/java
mv prj/docs/target/docs "${CURRENT_VERSION}"/docs
git add -A "${CURRENT_VERSION}"/*

if [ "${HEAD_BRANCH}" = "master" ]; then
  echo "Deploying docs for version ${CURRENT_VERSION} to latest-snapshot"
  rm -rf latest-snapshot || true
  cp -R "${CURRENT_VERSION}" latest-snapshot
  git add -A latest-snapshot/*
fi

echo "Pushing docs for ${CURRENT_VERSION} to gh-pages"
git commit -m "Update ${CURRENT_VERSION} docs"
git push origin gh-pages