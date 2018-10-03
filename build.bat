@echo off
SETLOCAL
rem cls

set JAVA_HOME=C:\Development\j2sdk1.4.2_04
set ANT_HOME=
set CLASSPATH=
if not exist tools\lib\junit.jar copy lib\ext\junit.jar tools\lib\
set path=.;tools\bin;%JAVA_HOME%\bin;%PATH%
ant %*

ENDLOCAL
