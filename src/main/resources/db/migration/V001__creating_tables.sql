CREATE TABLE countries (
  id INT IDENTITY(1,1) NOT NULL,
  name VARCHAR(50) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE users (
  id UNIQUEIDENTIFIER NOT NULL,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255),
  country_id INT,
  PRIMARY KEY (id),
  FOREIGN KEY (country_id)
    REFERENCES countries (id)
);