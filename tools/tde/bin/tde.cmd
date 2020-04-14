@rem
@rem  Copyright (c) 2000, 2020, Oracle and/or its affiliates.
@rem
@rem  Licensed under the Universal Permissive License v 1.0 as shown at
@rem  http://oss.oracle.com/licenses/upl.
@rem
@echo off
setlocal

if "%DEV_ROOT%"  == "" call "%~dp0..\..\..\bin\cfgwindows.cmd"

if "%MEM_OPTS%" == "" set MEM_OPTS=-Xms400m -Xmx400m
if "%OPTS%" == "" set OPTS=-showversion %MEM_OPTS% -Dtangosol.taps.repos=file:/%DEV_ROOT%\tde

pushd "%TDE_HOME%"
"%JAVA_HOME%\bin\java" %OPTS% -cp "%TDE_HOME%\lib\tde.jar;%TDE_HOME%\lib\coherence.jar;%CLASSPATH%" com.tangosol.tde.component.application.gUI.desktop.TDE
popd

endlocal
