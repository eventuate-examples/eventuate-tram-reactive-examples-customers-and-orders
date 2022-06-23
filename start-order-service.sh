#! /bin/bash -e

PROJECT_NAME="${PWD##*/}"

if [ -n "$EVENTUATE_PROJECT_NAME" ] ; then
    PROJECT_NAME="${EVENTUATE_PROJECT_NAME}"
fi

docker start ${PROJECT_NAME}_order-service_1
