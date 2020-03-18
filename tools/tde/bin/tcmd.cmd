@rem
@rem  Copyright (c) 2000, 2020, Oracle and/or its affiliates.
@rem
@rem  Licensed under the Universal Permissive License v 1.0 as shown at
@rem  http://oss.oracle.com/licenses/upl.
@rem
@rem *****************************************
@rem Default project is "core-net" (Coherence)
@rem
@rem Useful commands:
@rem    tcmd -dump Component...
@rem    tcmd -rawdump Component...
@rem    tcmd -depend -store Component
@rem ****************************************

@echo off
setlocal

if "%DEV_ROOT%"  == "" call "%~dp0..\..\..\bin\cfgwindows.cmd"

if "%PROJECT%" == "" set PROJECT=core-net

if "%MEM_OPTS%" == "" set MEM_OPTS=-Xms800m -Xmx800m
if "%OPTS%" == "" set OPTS=-showversion %MEM_OPTS% -Dtangosol.taps.repos=file:/%DEV_ROOT%\tde -Dtangosol.taps.prj=%PROJECT%

pushd %TDE_HOME%
"%JAVA_HOME%\bin\java" %OPTS% -cp "%TDE_HOME%\lib\tde.jar;%TDE_HOME%\lib\coherence.jar;%CLASSPATH%" com.tangosol.tde.component.application.console.Tcmd %*
popd

endlocal
