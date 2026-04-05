#!/bin/sh
#
# Copyright (c) 2000, 2026, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#
echo "Checking copyright for ${1}"
BASEDIR=$(dirname "$0")
JAR=${DEV_ROOT}/tools/copyright/glassfish-copyright-maven-plugin-2.4-SNAPSHOT.jar
EXCLUDE="@${BASEDIR}/copyright-exclude.txt"
CMD="java -cp ${JAR} org.glassfish.copyright.Copyright -c -X ${EXCLUDE} -C ${BASEDIR}/header.txt -A ${BASEDIR}/header-2.txt -A ${BASEDIR}/header-3.txt -q -p4 -noP4fstat $DEV_ROOT/${1}"

case "${1}" in
    *.md)
        CURRENT_YEAR=$(date +%Y)
        if [ -f "${FILE}" ] && CURRENT_YEAR="${CURRENT_YEAR}" perl -0ne 'my $year = $ENV{CURRENT_YEAR}; exit(!m{\A<!--\n  Copyright \(c\) (?:$year|\d{4}, $year), Oracle and/or its affiliates\.\n\n  Licensed under the Universal Permissive License v 1\.0 as shown at\n  https?://oss\.oracle\.com/licenses/upl\.\n  -->\n\n}s)' "${FILE}"; then
            exit 0
        fi
        ;;
esac

exec $CMD
