#!/bin/bash
#
# Copyright (c) 2000-2021 Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# http://oss.oracle.com/licenses/upl.
#

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ALL_GUIDES=$(find ${DIR}/guides -type f -name 'gradlew')

echo  ${ALL_GUIDES}
for f in ${ALL_GUIDES}
do
  EXAMPLE=${f%/*}
	echo "Building ${EXAMPLE}"
  ${f} clean build -p ${EXAMPLE}
done
