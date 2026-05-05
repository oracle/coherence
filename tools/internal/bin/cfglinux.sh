#!/bin/bash

#
# Copyright (c) 2000, 2026, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#

#
# Oracle internal script to pull down and unzip a JDK locally
#
# Command line:
#     . <workspace>/bin/cfglocal.sh [-reset]
#
# see <workspace>/bin/cfglocal.sh

# The platform specific command used to locate the correct version of java
# Note: this command is evaluated when required
function get_java_home
  {
  _CURRENT_DIR=`pwd`
  cd ${SCRIPTS_DIR}
  cd ..

  if [ 25 -eq $_VERSION_REQUIRED ]; then
    _JDK_VER=25.0.3
    _JDK_BUILD=9
  elif [ 21 -eq $_VERSION_REQUIRED ]; then
    _JDK_VER=21.0.11
    _JDK_BUILD=9
  elif [ 17 -eq $_VERSION_REQUIRED ]; then
    _JDK_VER=17.0.19
    _JDK_BUILD=9
  fi

  _JAVA_HOME=`pwd`/jdk/jdk-${_JDK_VER}

  case $(uname -m) in
    aarch*)
      _JDK_URL=http://slciajo.us.oracle.com/artifactory/re-release-local/jdk/${_JDK_VER}/${_JDK_BUILD}/bundles/linux-aarch64/jdk-${_JDK_VER}_linux-aarch64_bin.tar.gz
      _JDK_FILE=jdk-${_JDK_VER}_linux-aarch64_bin.tar.gz
     ;;
    *)
      _JDK_URL=http://slciajo.us.oracle.com/artifactory/re-release-local/jdk/${_JDK_VER}/${_JDK_BUILD}/bundles/linux-x64/jdk-${_JDK_VER}_linux-x64_bin.tar.gz
      _JDK_FILE=jdk-${_JDK_VER}_linux-x64_bin.tar.gz
     ;;
  esac

  if [ ! -f $_JAVA_HOME/bin/java ]; then
    rm -f $_JDK_FILE
    wget --no-proxy --tries=5 --wait=60 $_JDK_URL

    mkdir -p jdk
    cd jdk
    tar xzf ../$_JDK_FILE
    rm ../$_JDK_FILE
  fi

  cd $_CURRENT_DIR
  unset _CURRENT_DIR
  unset _JDK_BUILD
  unset _JDK_FILE
  unset _JDK_URL
  unset _JDK_VER
  echo $_JAVA_HOME
  }
