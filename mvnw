#!/bin/sh
# Maven Wrapper launch script (Unix/macOS)
# This is a minimal wrapper; the real wrapper jar is downloaded on first use.
MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES=".mvn/wrapper/maven-wrapper.properties"

if [ -r "$MAVEN_WRAPPER_JAR" ]; then
  exec java -jar "$MAVEN_WRAPPER_JAR" "$@"
fi

# Fallback: download wrapper jar
DOWNLOAD_URL=$(grep -m1 wrapperUrl "$MAVEN_WRAPPER_PROPERTIES" | cut -d= -f2)
mkdir -p .mvn/wrapper
curl -fsSL "$DOWNLOAD_URL" -o "$MAVEN_WRAPPER_JAR"
exec java -jar "$MAVEN_WRAPPER_JAR" "$@"
