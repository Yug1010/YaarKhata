@echo off
REM Creates yaarkhata.jks in this folder. You will be asked to choose passwords.
REM Use the SAME password for keystore and key to keep things simple. WRITE IT DOWN.
set KT="C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
if not exist %KT% set KT=keytool
%KT% -genkeypair -v -keystore yaarkhata.jks -keyalg RSA -keysize 2048 -validity 10000 -alias yaarkhata
echo.
echo Done. Now copy keystore.properties.example to keystore.properties and fill in the passwords.
echo BACK UP yaarkhata.jks and your passwords in two places.
pause
