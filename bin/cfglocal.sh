# Copyright (c) 2000, 2022, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# http://oss.oracle.com/licenses/upl.

#
# This script by default uses JDK 11 and set JAVA_HOME pointing to
# that JDK version.
#
# To use JDK 17 locally, use command: RBT_JV=17 ./bin/cfglocal.sh
#
# And to run on RQ with JDK 17, specify -j option as shown below
# enqueue -j 17 [-c changelist]

SCRIPT_PATH="${BASH_SOURCE[0]}"
JAVA_VERSION_TO_USE=${RBT_JV:-11}

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
