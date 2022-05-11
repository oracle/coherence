# Copyright (c) 2000, 2022, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.

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

unset SCRIPT_PATH
unset SCRIPTS_DIR
