# Copyright (c) 2000, 2023, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.

#
# This script by default uses JDK 21 and set JAVA_HOME pointing to
# that JDK version.
#
# To use a newer version locally, use command: JV=<version> ./bin/cfglocal.sh
#
# To run on RQ with a newer Java version, specify -j option as shown below
# enqueue -j <version> [-c changelist]

SCRIPT_PATH="${BASH_SOURCE[0]}"
JAVA_VERSION_TO_USE=${JV:-21}

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
  if [ -e `pwd`/cfglocal.sh ]; then
    SCRIPTS_DIR=`pwd`
  else
    SCRIPTS_DIR=`pwd`/bin
  fi
else
  cd `dirname $SCRIPT_PATH`
  SCRIPTS_DIR=`pwd`
fi
cd - &>/dev/null

case $(uname) in
  Darwin*)
    . $SCRIPTS_DIR/cfgosx.sh
     ;;
  SunOS*)
    . $SCRIPTS_DIR/cfgsolaris.sh
     ;;
  Windows*)
    . $SCRIPTS_DIR/cfgwindows.sh
     ;;
  *)
    . $SCRIPTS_DIR/cfglinux.sh
     ;;
esac

unset JAVA_VERSION_TO_USE
unset SCRIPT_PATH
unset SCRIPTS_DIR
