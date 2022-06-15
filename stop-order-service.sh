#! /bin/bash

set -e

if [ -n "$EVENTUATE_PROJECT_NAME" ] ; then
    ARGS="-p $EVENTUATE_PROJECT_NAME"
fi

docker-compose $ARGS -f docker-compose-mysql-binlog.yml stop order-service
docker-compose $ARGS -f docker-compose-mysql-binlog.yml rm -f order-service
