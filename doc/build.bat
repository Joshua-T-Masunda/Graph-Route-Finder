@echo off

cd ..

cls

setlocal enabledelayedexpansion

set JAVA_HOME=C:/jdk-21
set PATH=%JAVA_HOME%/bin;%PATH%

set JAVAFX_HOME=C:\javafx-sdk-21
set JAVAFX_MODULES=javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media
set JAVAFX_ARGS=--module-path "%JAVAFX_HOME%\lib" --add-modules=%JAVAFX_MODULES%

set src=./src
set dist=./dist
set bin=./bin
set doc=./doc
set lib=./lib
set javadocs=%doc%/javadocs

if exist %bin% (
  del /s /q "%bin%" >nul 2>&1
  rmdir /s /q "%bin%" >nul 2>&1
)
mkdir "%bin%"

javac %JAVAFX_ARGS% -cp "%dist%\weka.jar" -sourcepath "%src%" -d "%bin%" @%doc%\sources.txt
if %errorLevel% neq 0 (
  echo ~~~ COMPILATION ERROR ~~~
  pause
  exit /b 1
)

dir /s /b "%bin%\*.class"

if exist "%dist%\MiniProject.jar" del /q "%dist%\MiniProject.jar"
jar cvfe "%dist%\MiniProject.jar" user_interface.Main -C "%bin%" .
if %errorLevel% neq 0 (
  echo ~~~ JAR ERROR ~~~
  pause
  exit /b 1
)

rem if exist "%javadocs%" rmdir /q /s "%javadocs%"
rem javadoc -sourcepath "%src%" -d "%javadocs%" -subpackages user_interface algorithm classifier data navigator
rem if %errorLevel% neq 0 (
rem   echo ~~~ JAVADOC ERROR ~~~
rem   pause
rem   exit /b 1
rem )

java %JAVAFX_ARGS% -cp "%dist%\MiniProject.jar";%dist%\weka.jar user_interface.Main
if %errorLevel% neq 0 (
  echo ~~~ RUNNING ERROR ~~~
  pause
  exit /b 1
)

cd %doc%

pause