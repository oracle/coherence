#!/bin/bash

#
#  Copyright (c) 2000, 2020, Oracle and/or its affiliates.
#
#  Licensed under the Universal Permissive License v 1.0 as shown at
#  http://oss.oracle.com/licenses/upl.
#

#
# This script sets all environment variables necessary to build Coherence,
# however should be sourced by other scripts. These scripts should define
# _JAVA_HOME_CMD as a platform specific means of locating the correct
# JAVA_HOME.
#
# This script is responsible for the following environment variables:
#
#     DEV_ROOT     e.g. /dev
#     JAVA_HOME    e.g. /usr/java/jdk1.6
#     MAVEN_HOME   e.g. /dev/tools/maven
#     P4IGNORE     e.g. /dev/.p4ignore
#     TDE_HOME     e.g. /dev/tools/tde
#     CLASSPATH    e.g.
#
#     _P4IGNORE    saved P4IGNORE
#     _TDE_HOME    saved TDE_HOME
#     _CLASSPATH   saved CLASSPATH
#     _PATH        saved PATH
#

#
# Reset the build environment if the "-reset" flag was passed.
#
function reset
  {
  if [ -z $DEV_ROOT ]; then
    echo Build environment already reset.
    return 0
  fi

  if [ -z $_JAVA_HOME ]; then
    unset JAVA_HOME
  else
    export JAVA_HOME=$_JAVA_HOME
  fi

  if [ -z $_MAVEN_HOME ]; then
    unset MAVEN_HOME
  else
    export MAVEN_HOME=$_MAVEN_HOME
  fi

  if [ -z $_P4IGNORE ]; then
    unset P4IGNORE
  else
    export P4IGNORE=$_P4IGNORE
  fi

  if [ -z $_TDE_HOME ]; then
    unset TDE_HOME
  else
    export TDE_HOME=$_TDE_HOME
  fi

  if [ -z $_CLASSPATH ]; then
    unset CLASSPATH
  else
    export CLASSPATH=$_CLASSPATH
  fi

  if [ -z $_PATH ]; then
    unset PATH
  else
    export PATH=$_PATH
  fi

  unset DEV_ROOT
  unset _JAVA_HOME
  unset _MAVEN_HOME
  unset _P4IGNORE
  unset _TDE_HOME
  unset _CLASSPATH
  unset _PATH

  echo Build environment reset.
  }

#
# Setup the shell for Coherence builds.
#
function setup
  {
  #
  # Determine the root of the dev tree
  #
  cd $SCRIPTS_DIR/..
  DEV_ROOT=`pwd`
  cd - > /dev/null

  #
  # Ensure proper Java version, attempt selection if necessary
  #
  _JAVA_HOME=$JAVA_HOME
  _VERSION_REQUIRED=11

  if [ -z $JAVA_HOME ] || [ "$($JAVA_HOME/bin/java -version 2>&1 | sed 's/.*version "\([0-9]*\).*/\1/; 1q')" != "$_VERSION_REQUIRED" ]; then
    # Try to find the correct version
    JAVA_HOME=`eval $_JAVA_HOME_CMD`

    unset _ERROR

    if [ ! -d "$JAVA_HOME" ]; then
      _ERROR="Set JAVA_HOME as Java version $_VERSION_REQUIRED could not be located using command: $_JAVA_HOME_CMD"
    else
      # Ensure that it has been found
      _VERSION=$($JAVA_HOME/bin/java -version 2>&1 | sed 's/.*version "\([0-9]*\).*/\1/; 1q')

      if [ "$_VERSION" != "$_VERSION_REQUIRED" ]; then
        _ERROR="Incorrect JDK version $_VERSION at $JAVA_HOME; $_VERSION_REQUIRED is required, please export JAVA_HOME appropriately."
      fi
    fi

    if [ ! -z "$_ERROR" ]; then

      if [ -z $_JAVA_HOME ]; then
        unset JAVA_HOME
      else
        JAVA_HOME=$_JAVA_HOME
      fi

      echo "$_ERROR"

      unset _ERROR
      unset _JAVA_HOME
      unset _VERSION
      unset _VERSION_REQUIRED
      unset DEV_ROOT

      return 1

    fi
    unset _VERSION
  fi

  unset _VERSION_REQUIRED
  export DEV_ROOT
  echo DEV_ROOT = $DEV_ROOT
  export JAVA_HOME
  echo JAVA_HOME = $JAVA_HOME
  $JAVA_HOME/bin/java -version

  #
  # Save the PATH environment variable
  #
  _PATH=$PATH

  #
  # Set the MAVEN_HOME environment variable if tools/maven exists
  #
  _MAVEN_HOME=$MAVEN_HOME
  if [ -d $DEV_ROOT/tools/maven ]; then
    MAVEN_HOME=$DEV_ROOT/tools/maven
    PATH=$MAVEN_HOME/bin:$PATH
    export MAVEN_HOME
    echo MAVEN_HOME = $MAVEN_HOME
  fi

  #
  # Set the P4IGNORE environment variable
  #
  _P4IGNORE=$P4IGNORE
  if [ -f $DEV_ROOT/.p4ignore ]; then
    P4IGNORE=$DEV_ROOT/.p4ignore
    export P4IGNORE
    echo P4IGNORE = $P4IGNORE
  fi

  #
  # Set the TDE_HOME environment variable
  #
  _TDE_HOME=$TDE_HOME
  TDE_HOME=$DEV_ROOT/tools/tde
  PATH=$TDE_HOME/bin:$PATH
  export TDE_HOME
  echo TDE_HOME = $TDE_HOME

  #
  # Add the RQ executables to the PATH environment variable
  #
  PATH=$PATH:$DEV_ROOT/tools/wls/infra

  #
  # Set the CLASSPATH environment variable
  #
  _CLASSPATH=$CLASSPATH
  CLASSPATH=""
  export CLASSPATH
  echo CLASSPATH = $CLASSPATH

  export PATH
  echo PATH = $PATH
  echo Build environment set.
  }

# main
if [ "$1" = "-reset" ]; then
    reset
elif [ -z $DEV_ROOT ]; then
    setup
else
    echo Build environment already set.
fi

