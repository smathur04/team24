#!/bin/bash

cd "$(dirname "$0")"

timestamp=$(date +"%Y%m%d_%H%M%S")
mkdir -p logs/archive
for log in *_errors.log; do
    [ -f "$log" ] && mv "$log" "logs/archive/${log%.log}_$timestamp.log"
done

# Build full classpath including MySQL driver
mvn -f ../../pom.xml dependency:build-classpath -Dmdep.outputFile=classpath.txt > /dev/null
CLASSPATH="../../target/classes:$(cat ../../classpath.txt)"

echo "Running ActorXMLParser..."
java -cp "$CLASSPATH" PARSE.ActorXMLParser

echo "Running MovieXMLParser..."
java -cp "$CLASSPATH" PARSE.MovieXMLParser

echo "Running CastXMLParser..."
java -cp "$CLASSPATH" PARSE.CastXMLParser

echo "Parsing complete. New logs written to *_errors.log"
