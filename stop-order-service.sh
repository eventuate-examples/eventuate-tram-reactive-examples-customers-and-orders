#! /bin/bash

set -e

docker-compose -f docker-compose-mysql-binlog.yml stop order-service
docker-compose -f docker-compose-mysql-binlog.yml rm -f order-service
