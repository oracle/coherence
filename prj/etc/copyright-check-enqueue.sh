#!/bin/sh
#
# Copyright (c) 2000, 2021, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# http://oss.oracle.com/licenses/upl.
#
echo "Checking copyright for ${1}"
BASEDIR=$(dirname "$0")
JAR=${DEV_ROOT}/tools/copyright/glassfish-copyright-maven-plugin-2.4-SNAPSHOT.jar
EXCLUDE="@${BASEDIR}/copyright-exclude.txt"
CMD="java -cp ${JAR} org.glassfish.copyright.Copyright -c -X ${EXCLUDE} -C ${BASEDIR}/header.txt -A ${BASEDIR}/header-2.txt -A ${BASEDIR}/header-3.txt -q -p4 -noP4fstat $DEV_ROOT/${1}"

exec $CMD