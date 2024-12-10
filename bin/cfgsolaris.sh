#!/bin/bash

#
#  Copyright (c) 2000, 2020, Oracle and/or its affiliates.
#
#  Licensed under the Universal Permissive License v 1.0 as shown at
#  http://oss.oracle.com/licenses/upl.
#

#
# This script sets all environment variables necessary to build Coherence.
#
# Command line:
#     . ./cfgsolaris.sh [-reset]
#
# see cfgcommon.sh

#
# Global Variables
#

# The platform specific command used to locate the correct version of java
# Note: this command is evaluated when required
_JAVA_HOME_CMD="ls -td /usr/java/jdk$_VERSION_REQUIRED* | head -1"

# determine the scripts directory, assuming all scripts are in the same directory
SCRIPT_PATH="${BASH_SOURCE[0]}"
while [ -h "${SCRIPT_PATH}" ]; do
  LS=`ls -ld "${SCRIPT_PATH}"`
  LINK=`expr "${LS}" : '.*-> \(.*\)$'`
  if [ `expr "${LINK}" : '/.*'` > /dev/null ]; then
    SCRIPT_PATH="${LINK}"
  else
    SCRIPT_PATH="`dirname "${SCRIPT_PATH}"`/${LINK}"
  fi
done

cd `dirname $SCRIPT_PATH`
SCRIPTS_DIR=`pwd`
cd - &>/dev/null

source $SCRIPTS_DIR/cfgcommon.sh

unset SCRIPT_PATH
unset SCRIPTS_DIR