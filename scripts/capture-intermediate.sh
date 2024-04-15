#!/bin/bash

cflags=()
prework=""
while [[ $# -gt 0 ]]
do
  case $1 in
    -h)
      echo "Usage: $0 [ --keep ] [ -X <compiler flag> ]"
      exit 0
      ;;
    --keep)
      prework="[ -f {} ] || "
      shift
      ;;
    -X)
      cflags+=("$2")
      shift
      shift
      ;;
    -*)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

compilerFlags="${cflags[*]}"

# Capture output for BASIC programs
find src/main/basic -name "*.bas" -print0 |
  xargs -0 -n 1 -P 3 -I {} -t sh -c "${prework}java -jar build/libs/GhostBasic-1.0-SNAPSHOT.jar --quiet -il={}-intermediate.lst ${compilerFlags} {} || true"

# Capture output for INTEGER programs, some have OPT files, so let them crash
find src/main/basic -name "*.int" -print0 |
  xargs -0 -n 1 -P 3 -I {} -t sh -c "${prework}java -jar build/libs/GhostBasic-1.0-SNAPSHOT.jar --quiet -il={}-intermediate.lst ${compilerFlags} {} --integer || true "
