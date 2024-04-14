#!/bin/bash

n=2
cflags=()
prework=""
while [[ $# -gt 0 ]]
do
  case $1 in
    -h)
      echo "Usage: $0 [ --keep ] [ -n <number> ] [ -X <compiler flag> ]"
      exit 0
      ;;
    --keep)
      prework="[ -f {} ] || "
      shift
      ;;
    -n)
      n=$2
      shift
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
  xargs -0 -n 1 -P 3 -I {} -t sh -c "${prework}java -jar build/libs/GhostBasic-1.0-SNAPSHOT.jar --quiet -tl={}.lst ${compilerFlags} {} || true"

# Capture output for INTEGER programs, some have OPT files, so let them crash
find src/main/basic -name "*.int" -print0 |
  xargs -0 -n 1 -P 3 -I {} -t sh -c "${prework}java -jar build/libs/GhostBasic-1.0-SNAPSHOT.jar --quiet -tl={}.lst ${compilerFlags} {} --integer || true "

read -d '' CODE << EOF
BEGIN {
  max=$n
  base=1
  idx=1
}
{
  if (idx-base >= max) {
    out=""
    for (n=0; n<=max; n++) {
      out = (out data[base+n] " ")
    }
    print out
    delete data[base]
    base++
  }
  data[idx++]=\$0
}
EOF
echo "CODE=${CODE}"

# Print instruction sequences...
find src/main/basic -name "*.lst" -print0 |
  xargs -0 cat |
  cut -c20- |
  grep -E --only-matching "^[A-Z][_A-Z0-9]*( |$)" |
  awk "${CODE}" |
  sort |
  uniq -c |
  sort -nb
