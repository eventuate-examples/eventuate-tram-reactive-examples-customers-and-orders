ARG baseImageVersion
FROM eventuateio/eventuate-mysql8:$baseImageVersion
COPY 8.initialize-database.sql /docker-entrypoint-initdb.d
