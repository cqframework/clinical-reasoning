@ECHO OFF

SET "dlurl=https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=org.opencds.cqf&a=tooling&v=1.4.1-SNAPSHOT&c=jar-with-dependencies"
SET tooling_jar=tooling-1.4.1-SNAPSHOT-jar-with-dependencies.jar
SET input_cache_path=%~dp0input-cache\
SET skipPrompts=false
IF "%~1"=="/f" SET skipPrompts=true

FOR %%x IN ("%CD%") DO SET upper_path=%%~dpx

IF NOT EXIST "%input_cache_path%%tooling_jar%" (
   IF NOT EXIST "%upper_path%%tooling_jar%" (
      SET jarlocation=%input_cache_path%%tooling_jar%
      SET jarlocationname=Input Cache
      ECHO IG Refresh is not yet in input-cache or parent folder.
      REM we don't use jarlocation below because it will be empty because we're in a bracketed if statement
      GOTO create
   ) ELSE (
      ECHO IG RefreshFOUND in parent folder
      SET jarlocation=%upper_path%%tooling_jar%
      SET jarlocationname=Parent folder
      GOTO:upgrade
   )
) ELSE (
   ECHO IG Refresh FOUND in input-cache
   SET jarlocation=%input_cache_path%%tooling_jar%
   SET jarlocationname=Input Cache
   GOTO:upgrade
)

:create
ECHO Will place refresh jar here: %input_cache_path%%tooling_jar%
IF "%skipPrompts%"=="false" (
    SET /p create=Ok? [Y/N]
    IF /I "%create%"=="Y" goto:mkdir
) ELSE goto:mkdir

GOTO:done
:mkdir
    mkdir "%input_cache_path%" 2> NUL
GOTO:download

:upgrade
IF "%skipPrompts%"=="false" (
    SET /p overwrite="Overwrite %jarlocation%? (Y/N)"
    IF /I "%overwrite%"=="Y" (
        GOTO:download
    )
) ELSE (
    GOTO:download
)
GOTO:done

:download
ECHO Downloading most recent refresh to %jarlocationname% - it's ~70 MB, so this may take a bit

FOR /f "tokens=4-5 delims=. " %%i IN ('ver') DO SET VERSION=%%i.%%j
IF "%version%" == "10.0" GOTO win10
IF "%version%" == "6.3" GOTO win8.1
IF "%version%" == "6.2" GOTO win8
IF "%version%" == "6.1" GOTO win7
IF "%version%" == "6.0" GOTO vista

ECHO Unrecognized version: %version%
GOTO done

:win10
POWERSHELL -command "if ('System.Net.WebClient' -as [type]) {(new-object System.Net.WebClient).DownloadFile('%dlurl%','%jarlocation%') } else { Invoke-WebRequest -Uri '%dlurl%' -Outfile '%jarlocation%' }"
ECHO Download complete.
GOTO done

:win7
bitsadmin /transfer GetRefresh /download /priority normal "%dlurl%" "%jarlocation%"
ECHO Download complete.
GOTO done

:win8.1
:win8
:vista
ECHO This script does not yet support Windows %winver%.  Please ask for help on https://chat.fhir.org/#narrow/stream/179207-connectathon-mgmt/topic/Clinical.20Reasoning.20Track
GOTO done

:done
IF "%skipPrompts%"=="false" (
    PAUSE
)