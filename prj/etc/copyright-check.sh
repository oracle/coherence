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
#
# ./etc/copyright-check.sh -c -X @etc/copyright-exclude.txt -C etc/header.txt -A etc/header-2.txt -A etc/header-3.txt -p4
BASEDIR=$(dirname "$0")
JAR=${BASEDIR}/../../tools/internal/copyright/glassfish-copyright-maven-plugin-2.4-SNAPSHOT.jar

java -cp ${JAR} org.glassfish.copyright.Copyright "$@"
