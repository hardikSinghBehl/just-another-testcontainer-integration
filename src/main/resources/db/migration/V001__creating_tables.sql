CREATE TABLE countries (
  id INTEGER NOT NULL AUTO_INCREMENT, 
  name VARCHAR(50) NOT NULL UNIQUE, 
  PRIMARY KEY (id)
);

CREATE TABLE users (
  id BINARY(16) NOT NULL, 
  first_name VARCHAR(255) NOT NULL, 
  last_name VARCHAR(255), 
  country_id INTEGER,
  PRIMARY KEY (id),
  CONSTRAINT `users_fkey_countries` FOREIGN KEY (country_id) 
  REFERENCES countries (id)
);