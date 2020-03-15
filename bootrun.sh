#!/usr/bin/env bash

if [ "$OSTYPE" = "linux-gnu" ]; then
#  export JAVA_HOME=$(dirname $(dirname $(readlink $(readlink $(which javac)))))
  JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$(readlink -f "$(which javac)")" || readlink -f "$(which javac)")")")
else
  # macos
  JAVA_HOME=$(/usr/libexec/java_home)
#  JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/
  #JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-11.0.2.jdk/Contents/Home
fi

export JAVA_HOME
export PATH=${JAVA_HOME}/bin:${PATH}

touch env.secrets
touch env.console

set -a
# shellcheck disable=SC1091
source env.console
# shellcheck disable=SC1091
source env.secrets
set +a

./gradlew clean build bootRun

exit 0
