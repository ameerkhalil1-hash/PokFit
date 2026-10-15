#!/data/data/com.termux/files/usr/bin/bash
set -e
echo "=== Pokopia Macros: Termux On-Device Build Setup ==="
pkg update -y && pkg install -y openjdk-17 gradle git wget unzip

export JAVA_HOME=$PREFIX/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# Tune JVM memory for phone RAM (2048MB heap for Kotlin KSP + Compose)
sed -i 's/-Xmx4096m/-Xmx2048m/g' gradle.properties

echo "Running unit tests and assembling debug APK..."
gradle testDebugUnitTest assembleDebug --no-daemon
echo "APK compiled at: app/build/outputs/apk/debug/app-debug.apk"
