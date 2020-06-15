@rem
@rem  Copyright (c) 2000, 2020, Oracle and/or its affiliates.
@rem
@rem  Licensed under the Universal Permissive License v 1.0 as shown at
@rem  http://oss.oracle.com/licenses/upl.
@rem
@echo off
@rem convert two CDBs to XML and then diff the XML

setlocal

if "%DEV_ROOT%" == "" call "%~dp0..\..\..\bin\cfgwindows.cmd"

if "%P4DIFF%" == "" set "P4DIFF=C:\Program Files\Perforce\p4merge"

set XML1=%TEMP%\%~n1%~x1%.1.xml
set XML2=%TEMP%\%~n2%~x2%.2.xml

set CDB1=%~s1
set CDB2=%~s2

@rem P4 can generate a synthetic second name that contains "=", which splits the arguments
if not "%3" == "" set CDB2=%CDB2%=%3

@rem convert to XML
call "%TDE_HOME%\bin\cdb2xml.cmd" "%CDB1%" "%XML1%"
call "%TDE_HOME%\bin\cdb2xml.cmd" "%CDB2%" "%XML2%"

@rem diff
"%P4DIFF%" "%XML1%" "%XML2%"

@rem remove temporary conversions
del "%XML1%"
del "%XML2%"

endlocal
