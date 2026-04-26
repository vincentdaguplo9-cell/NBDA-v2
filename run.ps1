$projectRoot = "C:\Users\PC-2\Desktop\Naval-Blood-Donation-Archive"
$javaExe = "C:\Program Files\Java\jdk-25.0.2\bin\java.exe"
$fxPath = "C:\Program Files\Java\javafx-sdk-25.0.2\lib"
$modules = "javafx.controls,javafx.graphics,javafx.fxml"
$mainClass = "Main"
$cp = ".;lib\mysql-connector-j-9.6.0.jar;$projectRoot\build\classes"

Write-Host "Running app..."

& $javaExe --enable-native-access=javafx.graphics --module-path $fxPath --add-modules $modules -cp $cp $mainClass

if ($LASTEXITCODE -ne 0) {
    Write-Host "App exited with error."
    pause
    exit 1
}

Write-Host "App exited normally."
pause