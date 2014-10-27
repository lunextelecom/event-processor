DEPLOY_DIR=$(dirname $0)/..
echo DEPLOY_DIR
while [[ $# > 1 ]]
do
key="$1"
shift

case $key in
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
        CONF=$DEPLOY_DIR/conf/configuration.yaml
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

exec java -jar -Xms2500m -Xmx2500m -Dlog4j.configuration=file:$LOG $DEPLOY_DIR/lib/event-processor-web-1.0-SNAPSHOT.jar -c $CONF

