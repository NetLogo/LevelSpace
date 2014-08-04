#!/bin/sh

LAUNCH=$HOME/.sbt/sbt-launch-0.13.5.jar
URL='http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/0.13.5/sbt-launch.jar'

if [ ! -f $LAUNCH ] ; then
  echo "downloading" $URL
  mkdir -p $HOME/.sbt
  curl -s -S -f $URL -o $LAUNCH || exit
fi

java \
  $JAVA_OPTS \
  -Xmx1536m \
  -XX:MaxPermSize=256m \
  -classpath $LAUNCH \
  xsbt.boot.Boot "$@"
