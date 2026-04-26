$ErrorActionPreference = "Stop"
$projectRoot = "C:\Users\PC-2\Desktop\Naval-Blood-Donation-Archive"
$javaExe = "C:\Program Files\Java\jdk-25.0.2\bin\javac.exe"
$fxPath = "C:\Program Files\Java\javafx-sdk-25.0.2\lib"
$modules = "javafx.controls,javafx.graphics,javafx.fxml"

$srcFiles = Get-ChildItem -Path "$projectRoot\src" -Include "*.java" -Recurse | Select-Object -ExpandProperty FullName

Write-Host "Compiling..."

& $javaExe --module-path $fxPath --add-modules $modules -cp . -d "$projectRoot\build\classes" @srcFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed."
    exit 1
}

Write-Host "Build succeeded."