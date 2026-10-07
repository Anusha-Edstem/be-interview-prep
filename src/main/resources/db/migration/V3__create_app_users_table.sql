CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE app_users ADD CONSTRAINT uq_app_users_email UNIQUE (email);
