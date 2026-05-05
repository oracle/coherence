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
  _CURRENT_DIR=${DEV_ROOT:-`pwd`}
  # echo "_CURRENT_DIR: $_CURRENT_DIR"
  cd $_CURRENT_DIR
  rm -fr jdk

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
  _JDK_BIN_FILE=jdk-${_JDK_VER}_windows-x64_bin.zip
  _JDK_DOWNLOAD_URL="https://slciajo.us.oracle.com/artifactory/re-release-local/jdk/${_JDK_VER}/${_JDK_BUILD}/bundles/windows-x64/${_JDK_BIN_FILE}"
  HTTP_PROXY="" HTTPS_PROXY="" $_CURRENT_DIR/tools/wls/infra/curl.exe -k -o ${_JDK_BIN_FILE} $_JDK_DOWNLOAD_URL

  mkdir -p jdk
  cd jdk
  unzip -q ../$_JDK_BIN_FILE
  rm -f ../$_JDK_BIN_FILE

  cd $_CURRENT_DIR
  unset _CURRENT_DIR
  unset _JDK_BIN_FILE
  unset _JDK_BUILD
  unset _JDK_VER
  echo $_JAVA_HOME
  }

# download and add OpenSSL-Win64 to the PATH
function get_openssl_home
  {
  _CURRENT_DIR=${DEV_ROOT:-`pwd`}
  cd ${_CURRENT_DIR}
  _OPENSSL_HOME=`pwd`/openssl/openssl-1.1.1j-win64-mingw

  if [ ! -f ${_OPENSSL_HOME}/openssl.exe ]; then
    mkdir -p openssl
    cd openssl
    if [ ! -f openssl-1.1.1j_4-win64-mingw.zip ]; then
      # this version of curl does not support NO_PROXY or no_proxy
      HTTP_PROXY= HTTPS_PROXY= $_CURRENT_DIR/tools/wls/infra/curl.exe -k -o openssl-1.1.1j_4-win64-mingw.zip http://tangosol-build.us.oracle.com:8081/artifactory/third-party-release/openssl/openssl/1.1.1j-win64/openssl-1.1.1j_4-win64-mingw.zip
    fi
    unzip -q openssl-1.1.1j_4-win64-mingw.zip
    rm -f openssl-1.1.1j_4-win64-mingw.zip
  fi

  cd $_CURRENT_DIR
  unset _CURRENT_DIR
  echo ${_OPENSSL_HOME}
  }
