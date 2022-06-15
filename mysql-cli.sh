#! /bin/bash -e

NETWORK_NAME="${PWD##*/}_default"

if [ -n "$EVENTUATE_PROJECT_NAME" ] ; then
    NETWORK_NAME="${EVENTUATE_PROJECT_NAME}_default"
fi


docker run ${1:--it} \
   --name mysqlterm --network=$NETWORK_NAME --rm \
   -e MYSQL_HOST=mysql \
   mysql/mysql-server:8.0.27-1.2.6-server \
   sh -c 'exec mysql -h"$MYSQL_HOST"  -uroot -prootpassword -o eventuate'
