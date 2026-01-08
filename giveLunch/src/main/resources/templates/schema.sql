CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username varchar(50) not null ,
    password varchar(255) not null,
    email varchar(50) not null
)