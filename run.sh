#!/bin/bash

# JavaFX SDK path
if [ -z "$PATH_TO_FX" ]; then
  PATH_TO_FX="/opt/javafx-sdk-26.0.1/lib"
fi

FX_MODULES="javafx.controls,javafx.graphics,javafx.fxml"

# Clean and create build directory
mkdir -p build/classes

echo "Compiling..."
javac --module-path "$PATH_TO_FX" --add-modules "$FX_MODULES" \
  -d build/classes \
  src/Main.java src/database/*.java src/model/*.java src/dao/*.java src/ui/*.java

if [ $? -ne 0 ]; then
  echo "Build failed."
  exit 1
fi

echo "Build succeeded."

# CONFIGURATION
# If Main.java has "package com.example;", change this to "com.example.Main"
MAIN_CLASS="Main" 

# Linux uses : to separate paths. Windows uses ;
# We include the build/classes folder so java can find your compiled .class files
RUN_CP="build/classes:lib/mysql-connector-j-9.6.0.jar"

echo "Running app..."
java --enable-native-access=javafx.graphics \
     --module-path "$PATH_TO_FX" \
     --add-modules "$FX_MODULES" \
     -cp "$RUN_CP" \
     "$MAIN_CLASS"

if [ $? -ne 0 ]; then
  echo "App exited with an error."
  exit 1
fi

echo "App exited normally."
