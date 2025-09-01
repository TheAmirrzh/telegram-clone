INSERT INTO users (id, username, password_hash, profile_name)
VALUES (uuid_generate_v4(), 'alice', '$2a$10$testhashalice', 'Alice'),
       (uuid_generate_v4(), 'bob',   '$2a$10$testhashbob', 'Bob');
