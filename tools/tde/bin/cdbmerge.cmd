@rem
@rem  Copyright (c) 2000, 2020, Oracle and/or its affiliates.
@rem
@rem  Licensed under the Universal Permissive License v 1.0 as shown at
@rem  http://oss.oracle.com/licenses/upl.
@rem
@echo off
@rem convert CDBs to XML and then MERGE the XML

setlocal

if "%DEV_ROOT%"  == "" call "%~dp0..\..\..\bin\cfgwindows.cmd"

if "%P4MERGE%"=="" set P4MERGE=C:\Program Files\Perforce\p4merge

set XML1=%TEMP%\%~n1%~x1%.1.xml
set XML2=%TEMP%\%~n2%~x2%.2.xml
set XML3=%TEMP%\%~n3%~x3%.3.xml
set XML4=%TEMP%\%~n4%~x4%.4.xml

copy /Y NUL "%XML4%"

set CDB1=%~s1
set CDB2=%~s2
set CDB3=%~s3

@rem convert to XML
call "%TDE_HOME%\bin\cdb2xml.cmd" "%CDB1%" "%XML1%"
call "%TDE_HOME%\bin\cdb2xml.cmd" "%CDB2%" "%XML2%"
call "%TDE_HOME%\bin\cdb2xml.cmd" "%CDB3%" "%XML3%"

@rem merge
"%P4MERGE%" "%XML1%" "%XML2%" "%XML3%" "%XML4%"
del "%~s4"
call "%TDE_HOME\bin\%cdb2xml.cmd" "%XML4%" "%~s4"

@rem remove temporary conversions
del "%XML1%"
del "%XML2%"
del "%XML3%"
del "%XML4%"

endlocal
