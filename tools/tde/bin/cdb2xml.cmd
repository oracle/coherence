@rem
@rem  Copyright (c) 2000, 2020, Oracle and/or its affiliates.
@rem
@rem  Licensed under the Universal Permissive License v 1.0 as shown at
@rem  http://oss.oracle.com/licenses/upl.
@rem
@echo off

@rem convert a CDB to XML

setlocal

if "%DEV_ROOT%"  == "" call "%~dp0..\..\..\bin\cfgwindows.cmd"

"%JAVA_HOME%\bin\java" -cp "%DEV_ROOT%\tools\tde\lib\coherence.jar;%CLASSPATH%" com.tangosol.dev.component.Component %*

endlocal
