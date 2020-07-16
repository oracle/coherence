@ECHO OFF
CLS

REM This is an example script to show how to use the Help Library Manager Launcher to remove an MS Help Viewer file.
REM You can use this as an example for creating a script to run from your product's uninstaller.

REM NOTE: If not executed from within the same folder as the executable, a full path is required on the executable.

IF "%1%"=="" GOTO MissingVersion
IF "%1%"=="1.0" GOTO HelpViewer1

GOTO HelpViewer2

:HelpViewer1

REM Help Viewer 1.0
HelpLibraryManagerLauncher.exe /product "VS" /version "100" /locale en-us /uninstall /silent /vendor "Vendor Name" /productName "Oracle® Fusion Middleware .NET API Reference for Oracle Coherence" /mediaBookList "Oracle® Fusion Middleware .NET API Reference for Oracle Coherence"

GOTO Exit

:HelpViewer2

REM Help Viewer 2.x
HelpLibraryManagerLauncher.exe /viewerVersion %1  /locale en-us /wait 0 /operation uninstall /vendor "Vendor Name" /productName "Oracle® Fusion Middleware .NET API Reference for Oracle Coherence" /bookList "Oracle® Fusion Middleware .NET API Reference for Oracle Coherence"

GOTO Exit

:MissingVersion
ECHO A help viewer version parameter is required

:Exit
