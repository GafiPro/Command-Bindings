@echo off
set "DIRNAME=%~dp0"
java %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=gradlew" -jar "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" %*
