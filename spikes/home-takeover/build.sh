#!/usr/bin/env bash
# Builds the spike APK without Gradle: aapt2, javac, d8, zipalign, apksigner.
# Tools are the per-user install from the 2026-11-09 spike (see README.md).
set -euo pipefail
cd "$(dirname "$0")"

T="${ANDROID_BUILD:-$LOCALAPPDATA/AndroidBuild}"
BT="$T/sdk/build-tools/34.0.0"
JAR="$T/sdk/platforms/android-34/android.jar"
export JAVA_HOME="$T/jdk"
export PATH="$JAVA_HOME/bin:$PATH"

rm -rf build && mkdir -p build/gen build/obj build/dex

"$BT/aapt2.exe" compile --dir res -o build/res.zip
"$BT/aapt2.exe" link -I "$JAR" --manifest AndroidManifest.xml -o build/unsigned.apk \
  build/res.zip --java build/gen

javac --release 11 -classpath "$JAR" -d build/obj \
  $(find src build/gen -name '*.java')

cmd //c "$(cygpath -w "$BT/d8.bat")" --release --min-api 26 --lib "$(cygpath -w "$JAR")" \
  --output "$(cygpath -w build/dex)" $(find build/obj -name '*.class' -exec cygpath -w {} \;)

(cd build/dex && jar uf ../unsigned.apk classes.dex)

"$BT/zipalign.exe" -f 4 build/unsigned.apk build/aligned.apk

KS="$T/spike-debug.keystore"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias spike \
    -keyalg RSA -keysize 2048 -validity 3650 -dname "CN=Quiet spike" >/dev/null
fi
cmd //c "$(cygpath -w "$BT/apksigner.bat")" sign --ks "$(cygpath -w "$KS")" --ks-pass pass:android \
  --out "$(cygpath -w build/quiet-spike.apk)" "$(cygpath -w build/aligned.apk)"

echo "built $(pwd)/build/quiet-spike.apk"
