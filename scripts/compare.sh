#!/bin/bash

set -eux

if [ ! -f "build/libs/GhostBasic-1.0-SNAPSHOT.jar" ] || [ ! -f "${HOME}/Downloads/build-artifacts/build/libs/GhostBasic-1.0-SNAPSHOT.jar" ]
then
  echo "Must have current and prior GhostBasic JAR available."
  exit 1
fi

CURRGHOST="java -jar build/libs/GhostBasic-1.0-SNAPSHOT.jar"
PREVGHOST="java -jar ${HOME}/Downloads/build-artifacts/build/libs/GhostBasic-1.0-SNAPSHOT.jar"

if [ $# -ne 1 ]
then
  echo "Please include file to compile"
  exit 2
fi

FILE="$1"
filename=$(basename "${FILE}")
extension="${filename##*.}"
filename="${filename%.*}"

case "$(uname -s)" in
  Linux*)   optname="${FILE/\.${extension}/\.opt}";;
  Darwin*)  optname="${FILE/\.${extension}/.opt}";;
  *)        echo "Unknown OS"; exit 1;;
esac

FLAGS="--fix-control-chars --trace"
[ "${extension}" == "int" ] && FLAGS+=" --integer"
[ -f "${optname}" ] && FLAGS+=" @${optname}"

${CURRGHOST} "${FILE}" ${FLAGS} -il="il-${filename}-curr.lst" -tl="tl-${filename}-curr.lst" --symbols="symbols-${filename}-curr.lst" -o curr.out
${PREVGHOST} "${FILE}" ${FLAGS} -il="il-${filename}-prev.lst" -tl="tl-${filename}-prev.lst" --symbols="symbols-${filename}-prev.lst" -o prev.out
