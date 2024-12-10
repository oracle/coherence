@echo off
rem  Copyright (c) 2000, 2024, Oracle and/or its affiliates.

rem  Licensed under the Universal Permissive License v 1.0 as shown at
rem  https://oss.oracle.com/licenses/upl.

rem
rem This script sets all environment variables necessary to build Coherence.
rem
rem Command line:
rem     cfgwindows [-reset]
rem
rem This script is responsible for the following environment variables:
rem
rem     DEV_ROOT     e.g. c:\dev
rem     MAVEN_HOME   e.g. c:\dev\tools\maven
rem     CLASSPATH    e.g.
rem     PATH         e.g. %ANT_HOME%\bin;%MAVEN_HOME%\bin;%PATH%
rem
rem     _CLASSPATH   saved CLASSPATH
rem     _PATH        saved PATH
rem

rem
rem Reset the build environment if the "-reset" flag was passed
rem
if "%1"=="-reset" (
  if "%DEV_ROOT%"=="" (
    echo Build environment already reset.
    goto exit
  )

  set MAVEN_HOME=%_MAVEN_HOME%
  set CLASSPATH=%_CLASSPATH%
  set PATH=%_PATH%

  set DEV_ROOT=
  set _MAVEN_HOME=
  set _CLASSPATH=
  set _PATH=

  echo Build environment reset.
  goto exit
)

rem
rem Ensure that the JAVA_HOME envirionment variable has been set
rem
if defined JAVA_HOME (
  :: Strip quotes from JAVA_HOME environment variable if present
  set JAVA_HOME=%JAVA_HOME:"=%
) else (
  echo Please set JAVA_HOME appropriately.
  goto exit
)
echo JAVA_HOME  = %JAVA_HOME%
"%JAVA_HOME%\bin\java" -version

rem
rem Determine the root of the dev tree
rem
if not "%DEV_ROOT%"=="" (
  echo Build environment already set.
  goto exit
)
for %%i in ("%~dp0..") do @set DEV_ROOT=%%~fni
echo DEV_ROOT   = %DEV_ROOT%

rem
rem Set the MAVEN_HOME environment variable if %DEV_ROOT%\tools\maven exists
rem
set _MAVEN_HOME=%MAVEN_HOME%
if exist %DEV_ROOT%\tools\maven (
  set MAVEN_HOME=%DEV_ROOT%\tools\maven
  echo MAVEN_HOME = %MAVEN_HOME%
)

rem
rem Set the CLASSPATH environment variable
rem
set _CLASSPATH=%CLASSPATH%
set CLASSPATH=
echo CLASSPATH  = %CLASSPATH%

rem
rem Set the PATH environment variable
rem
set _PATH=%PATH%
echo PATH      = %PATH%

echo Build environment set.

:exit
