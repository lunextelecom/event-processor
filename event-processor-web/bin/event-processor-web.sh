#!/bin/bash
DEPLOY_DIR=$(dirname $0)/..
while [[ $# > 1 ]]
do
key="$1"
shift
case $key in
-a)
APP="$1"
shift
;;
-p)
PROXY="$1"
shift
;;
-c)
CONF="$1"
shift
;;
-l)
LOG="$1"
shift
;;
--default)
DEFAULT=YES
shift
;;
*)
# unknown option
;;
esac
done
if [ -z "$CONF" ]; then
CONF=$DEPLOY_DIR/conf/config.yaml
fi
if [ -z "$LOG" ]; then
LOG=$DEPLOY_DIR/conf/log4j.properties
fi
f=$CONF
if [ -d "$f" ]; then f=$f/.; fi
absolute=$(cd "$(dirname -- "$f")"; printf %s. "$PWD")
absolute=${absolute%?}
absolute=$absolute/${f##*/}
CONF=$absolute
f=$LOG
if [ -d "$f" ]; then f=$f/.; fi
absolute=$(cd "$(dirname -- "$f")"; printf %s. "$PWD")
absolute=${absolute%?}
absolute=$absolute/${f##*/}
LOG=$absolute
exec java -jar -Xms2500m -Xmx2500m -Dlog4j.configuration=file:$LOG $DEPLOY_DIR/lib/event-processor-reader-1.0-SNAPSHOT.jar -c $CONF