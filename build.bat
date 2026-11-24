@echo off
setlocal
where java >nul 2>nul
if errorlevel 1 (
  echo Java was not found. Install JDK 17 first.
  pause
  exit /b 1
)
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle was not found.
  echo Install Gradle 8.1.1 or open this project in IntelliJ IDEA and run Tasks ^> build ^> build.
  pause
  exit /b 1
)
gradle build
if errorlevel 1 (
  echo Build failed. Check the Gradle output above.
) else (
  echo Build succeeded. See build\libs\villagerremote-1.0.0.jar
)
pause
