USE eventuate;

CREATE TABLE customer (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(1024),
  version BIGINT,
  creation_time BIGINT,
  credit_limit DECIMAL
);

CREATE TABLE credit_reservation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT,
  order_id VARCHAR(128),
  reservation DECIMAL
);

CREATE TABLE ordert (
  id VARCHAR(128) PRIMARY KEY,
  version BIGINT,
  state VARCHAR(50),
  customer_id BIGINT,
  order_total DECIMAL
);