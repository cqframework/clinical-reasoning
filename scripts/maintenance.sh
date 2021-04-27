#!/usr/bin/env bash

# Fail if any command fails or if there are unbound variables and check syntax
set -euxo pipefail
bash -n "$0"

# caffeine 3+ requires Java 11
mvn versions:update-properties versions:use-releases versions:use-latest-releases -Dexcludes=com.github.ben-manes.caffeine:caffeine:jar:2.9.0,org.slf4j:\*
