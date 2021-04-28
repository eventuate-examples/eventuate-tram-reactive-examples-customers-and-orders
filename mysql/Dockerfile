ARG baseImageVersion
FROM eventuateio/eventuate-mysql:$baseImageVersion
COPY 8.initialize-database.sql /docker-entrypoint-initdb.d
