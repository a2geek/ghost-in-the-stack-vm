#!/bin/bash

# Capture output for BASIC programs
find src/main/basic -name "*.bas" |
  xargs -n 1 -P 3 -I {} -t sh -c  "java -jar build/libs/GhostBasic-1.0-SNAPSHOT.jar --quiet -tl={}.lst {} || true"

# Capture output for INTEGER programs, some have OPT files, so let them crash
find src/main/basic -name "*.int" |
    xargs -n 1 -P 3 -I {} -t sh -c "java -jar build/libs/GhostBasic-1.0-SNAPSHOT.jar --quiet -tl={}.lst {} --integer || true "

# Keep the instructions that have 100+ usage
find src/main/basic -name "*.lst" |
  xargs cat |
  grep -v ":$" |
  cut -c20- |
  grep -E "^[A-Z]" |
  sort |
  uniq -c |
  grep -vE "^  [ 0-9]\d "
