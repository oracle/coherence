#!/bin/sh
#
# Copyright (c) 2000, 2021, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# http://oss.oracle.com/licenses/upl.
#

# Example
#
# ./etc/copyright-check.sh -c -X @etc/copyright-exclude.txt -C etc/header.txt -A etc/header-2.txt -A etc/header-3.txt -y

REPO=~/.m2/repository/org/glassfish/copyright/glassfish-copyright-maven-plugin

#VERSION="ls ${REPO} | grep '^[1-9]' | tail -1"
VERSION=2.4

java -cp "${REPO}/${VERSION}/glassfish-copyright-maven-plugin-${VERSION}.jar" \
    org.glassfish.copyright.Copyright "$@"
