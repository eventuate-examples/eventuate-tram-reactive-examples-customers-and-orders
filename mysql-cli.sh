#! /bin/bash -e

. ./_network-env.sh

docker run ${1:--it} \
   --name mysqlterm --network=$NETWORK_NAME --rm \
   -e MYSQL_HOST=mysql \
   mysql/mysql-server:8.0.27-1.2.6-server \
   sh -c 'exec mysql -h"$MYSQL_HOST"  -uroot -prootpassword -o eventuate'
