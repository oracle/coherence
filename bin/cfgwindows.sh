#!/bin/bash

# Copyright (c) 2000, 2025, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.

# This script sets all environment variables necessary to build Coherence.
#
# Command line:
#     . ./cfgwindows.sh [-reset]
#
# see cfgcommon.sh

#
# Global Variables
#

# The platform specific command used to locate the correct version of java
# Note: this command is evaluated when required
_JAVA_HOME_CMD="get_java_home"

function get_java_home
  {
  echo Not Supported
  }

function get_openssl_home
  {
  echo Not Supported
  }

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

if [ -z "$SCRIPT_PATH" ]; then
  SCRIPTS_DIR=`pwd`/bin
  if [ -e `pwd`/cfgwindows.sh ]; then
    SCRIPTS_DIR=`pwd`
  else
    SCRIPTS_DIR=`pwd`/bin
  fi
else
  cd `dirname $SCRIPT_PATH`
  SCRIPTS_DIR=`pwd`
fi
cd - &>/dev/null

if [ -f $SCRIPTS_DIR/../tools/internal/bin/cfgwindows.sh ]; then
  source $SCRIPTS_DIR/../tools/internal/bin/cfgwindows.sh
fi

source $SCRIPTS_DIR/cfgcommon.sh

# configure OpenSSL for building test certificates
if [ -z $OPENSSL_HOME ]; then
  OPENSSL_HOME=`eval get_openssl_home`
  echo OPENSSL_HOME evaluated to $OPENSSL_HOME
fi

export PATH=${OPENSSL_HOME}:${PATH}
export OPENSSL_CONF=${OPENSSL_HOME}/openssl.cnf
echo openssl: `which openssl`
openssl version

unset SCRIPT_PATH
unset SCRIPTS_DIR
