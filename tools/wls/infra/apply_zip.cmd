@echo off

REM Look for perl.  See if PERL is set and exists.
if exist "%PERL%" goto run

REM PERL didn't work, so try the "usual" place
set PERL=c:\Perl\bin\perl.exe
if not exist "%PERL%" set PERL=perl

:run

%PERL% %~d0%~p0/apply_zip.pl %*

