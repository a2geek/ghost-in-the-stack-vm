#!/bin/bash

set -eu

GHOST="java -jar build/libs/GhostBasic-1.0-SNAPSHOT.jar"
ACX_JAR="AppleCommander-acx-1.9.0.jar"
ACX_URL="https://github.com/AppleCommander/AppleCommander/releases/download/1.9.0/${ACX_JAR}"
if [ ! -f build/libs/${ACX_JAR} ]
then
  echo "Pulling copy of 'acx'..."
  curl -L -o build/libs/${ACX_JAR} --silent ${ACX_URL}
fi
ACX="java -jar build/libs/${ACX_JAR}"

export ACX_DISK_NAME=disk1.po
${ACX} mkdisk --format=src/main/asm/template.po --name=MYDISK --size=800k --prodos --prodos-order

DIRS=$(ls src/main/basic)
if [ $# -ge 1 ]
then
    case "$1" in
      --sample)
          DIRS="sample"
          shift
          ;;
      --integer)
          DIRS="integer"
          shift
          ;;
    esac
fi
echo "Building from these directories: ${DIRS}"

for dir in ${DIRS}
do
  ${ACX} mkdir ${dir}

  for source in $(find src/main/basic/${dir} -name "*.bas" -o -name "*.int" -o -name "*.as" | sort)
  do
    echo "Building file ${source} in ${dir}..."

    filename=$(basename "${source}")
    extension="${filename##*.}"
    filename="${filename%.*}"
    filename="${filename//-/}"
    asfilename="${filename}.as"
    case "$(uname -s)" in
      Linux*)   optname="${source/\.${extension}/\.opt}";;
      Darwin*)  optname="${source/\.${extension}/.opt}";;
      *)        echo "Unknown OS"; exit 1;;
    esac

    if [ "${extension}" == "as" ]
    then
      ${ACX} import --dir="${dir}" "${source}" --applesingle
    else
      FLAGS="--fix-control-chars"
      [ "${extension}" == "int" ] && FLAGS+=" --integer"
      [ -f "${optname}" ] && FLAGS+=" @${optname}"

      ${GHOST} "$source" ${FLAGS} "$@" --output="${asfilename}" --quiet
      ${ACX} import --dir="${dir}" --name="${filename}" "${asfilename}" --applesingle
      rm "${asfilename}"
    fi
  done
done
