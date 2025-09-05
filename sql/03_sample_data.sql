-- This script inserts sample users into the database for testing.
-- The password for all users is 'password'.
-- It has been hashed using BCrypt.

-- Make sure to use different UUIDs if you run this manually multiple times
-- to avoid primary key conflicts if the table isn't cleared first.

INSERT INTO users (id, username, password_hash, display_name, bio, status) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'alice', '$2a$10$Vp4z7A5A.g2f/i2Jz.h22uL2bIF4R4G.uX.j1/t.z6C3d3B5E4A7m', 'Alice Smith', 'Digital artist and coffee enthusiast.', 'Offline'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'bob', '$2a$10$Vp4z7A5A.g2f/i2Jz.h22uL2bIF4R4G.uX.j1/t.z6C3d3B5E4A7m', 'Bob Johnson', 'Loves hiking and exploring new trails.', 'Offline'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'charlie', '$2a$10$Vp4z7A5A.g2f/i2Jz.h22uL2bIF4R4G.uX.j1/t.z6C3d3B5E4A7m', 'Charlie Brown', 'Frontend developer and UI/UX designer.', 'Offline'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'diana', '$2a$10$Vp4z7A5A.g2f/i2Jz.h22uL2bIF4R4G.uX.j1/t.z6C3d3B5E4A7m', 'Diana Prince', 'Curator of ancient artifacts.', 'Offline');
