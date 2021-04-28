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
  order_id BIGINT,
  reservation DECIMAL,
  FOREIGN KEY (customer_id) REFERENCES customer (id)
);