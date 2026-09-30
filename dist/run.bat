@echo off
echo.
echo =================================================
echo   Hazard-Aware Navigation System
echo   AllStar Team - CS3A 2026
echo =================================================
echo.

java --module-path "C:\Dev\Java\javafx21\lib" ^
     --add-modules javafx.controls,javafx.fxml,javafx.graphics ^
     --enable-native-access=javafx.graphics ^
     -jar allStar.jar

pause