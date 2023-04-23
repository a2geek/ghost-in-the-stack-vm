#!/bin/bash

set -eu

GHOST="java -jar build/libs/GhostBasic-1.0-SNAPSHOT.jar"
ACX_JAR="AppleCommander-acx-1.8.0.jar"
ACX_URL="https://github.com/AppleCommander/AppleCommander/releases/download/1.8.0/${ACX_JAR}"
if [ ! -f build/libs/${ACX_JAR} ]
then
  echo "Pulling copy of 'acx'..."
  curl -L -o build/libs/${ACX_JAR} --silent ${ACX_URL}
fi
ACX="java -jar build/libs/${ACX_JAR}"

export ACX_DISK_NAME=disk1.po
cp src/main/asm/template.po ${ACX_DISK_NAME}

for dir in $(ls src/main/basic)
do
  ${ACX} mkdir ${dir}

  for source in $(find src/main/basic/${dir} -name "*.bas" -o -name "*.int")
  do
    echo "Building file ${source} in ${dir}..."

    filename=$(basename ${source})
    extension="${filename##*.}"
    filename="${filename%.*}"
    filename="${filename//-/}"

    FLAGS=""
    [ "${extension}" == "int" ] && FLAGS="--integer"

    ${GHOST} $source ${FLAGS} --output=${filename} --quiet
    ${ACX} import --dir=${dir} ${filename} --as -a 0x803 -f
    rm ${filename}
  done
done
