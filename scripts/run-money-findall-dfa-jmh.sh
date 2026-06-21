#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

mvn -q -DskipTests test-compile dependency:build-classpath -Dmdep.outputFile=target/jmh.cp

CP="$(cat target/jmh.cp):target/test-classes:target/classes"

if [ "$#" -eq 0 ]; then
  set -- io.github.ukman.priluka.benchmark.MoneyFindAllDfaBenchmark -p sizeMiB=10,20,100 -wi 3 -i 5 -f 1 -t 1
fi

java -cp "$CP" org.openjdk.jmh.Main "$@"
