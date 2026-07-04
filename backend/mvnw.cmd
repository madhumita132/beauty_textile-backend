@REM Maven Wrapper launch script (Windows)
@SET WRAPPER_JAR=.mvn\wrapper\maven-wrapper.jar
@SET WRAPPER_PROPS=.mvn\wrapper\maven-wrapper.properties

@IF EXIST "%WRAPPER_JAR%" GOTO runWrapper

@REM Download wrapper jar
@FOR /F "tokens=1,2 delims==" %%A IN (%WRAPPER_PROPS%) DO @IF "%%A"=="wrapperUrl" SET DOWNLOAD_URL=%%B
@MKDIR .mvn\wrapper 2>NUL
@CALL curl -fsSL "%DOWNLOAD_URL%" -o "%WRAPPER_JAR%"

:runWrapper
@java -jar "%WRAPPER_JAR%" %*
