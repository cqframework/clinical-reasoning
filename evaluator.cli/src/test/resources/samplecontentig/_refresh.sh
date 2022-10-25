#!/bin/bash
#DO NOT EDIT WITH WINDOWS
tooling_jar=tooling-1.4.1-SNAPSHOT-jar-with-dependencies.jar
input_cache_path=./input-cache
ig_ini_path=$PWD/ig.ini

set -e
echo Checking internet connection...
wget -q --spider tx.fhir.org

if [ $? -eq 0 ]; then
	echo "Online"
	fsoption="-fs https://cds-sandbox.alphora.com/cqf-ruler-r4/fhir/"
else
	echo "Offline"
	fsoption=""
fi

echo "$fsoption"

tooling=$input_cache_path/$tooling_jar
if test -f "$tooling"; then
	java -jar $tooling -RefreshIG -ini="$ig_ini_path" -d -p -t $fsoption
else
	tooling=../$tooling_jar
	echo $tooling
	if test -f "$tooling"; then
		java -jar $tooling -RefreshIG -ini="$ig_ini_path" -d -p -t $fsoption
	else
		echo IG Refresh NOT FOUND in input-cache or parent folder.  Please run _updateCQFTooling.  Aborting...
	fi
fi
