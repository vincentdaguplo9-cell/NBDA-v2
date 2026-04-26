@echo off
setlocal

rem JavaFX SDK path (set to your local JavaFX lib folder).
if "%PATH_TO_FX%"=="" (
  set "PATH_TO_FX=C:\Program Files\Java\javafx-sdk-25.0.2\lib"
)

set FX_MODULES=javafx.controls,javafx.graphics,javafx.fxml
set COMPILE_CP=.

rem Compile all sources into the build directory.
if not exist build\classes mkdir build\classes

echo Compiling...
javac --module-path "%PATH_TO_FX%" --add-modules %FX_MODULES% ^
  -cp "%COMPILE_CP%" ^
  -d build\classes ^
  src\Main.java src\database\*.java src\model\*.java src\dao\*.java src\ui\*.java

if errorlevel 1 (
  echo Build failed.
  pause
  exit /b 1
)

echo Build succeeded.

set MAIN_CLASS=Main
set RUN_CP=.;lib\mysql-connector-j-9.6.0.jar;build\classes

echo Running app...
java --enable-native-access=javafx.graphics --module-path "%PATH_TO_FX%" --add-modules %FX_MODULES% -cp "%RUN_CP%" %MAIN_CLASS%

if errorlevel 1 (
  echo App exited with an error.
  pause
  exit /b 1
)

echo App exited normally.
pause

endlocal
