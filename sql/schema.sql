-- schema.sql
CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(36) PRIMARY KEY,
  username VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(255),
  bio TEXT,
  profile_pic_path TEXT,
  status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS messages (
  id VARCHAR(36) PRIMARY KEY,
  sender_id VARCHAR(36) REFERENCES users(id),
  receiver_id VARCHAR(36),
  receiver_type VARCHAR(20),
  content TEXT,
  media_type VARCHAR(100),
  media_path TEXT,
  timestamp TIMESTAMP DEFAULT NOW(),
  read_status VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS groups (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  creator_id VARCHAR(36) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS group_members (
  group_id VARCHAR(36) REFERENCES groups(id),
  user_id VARCHAR(36) REFERENCES users(id),
  -- ADDED: Role management for groups
  role VARCHAR(20) NOT NULL DEFAULT 'MEMBER', -- Can be 'MEMBER' or 'ADMIN'
  PRIMARY KEY (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS channels (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  owner_id VARCHAR(36) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS channel_subscribers (
  channel_id VARCHAR(36) REFERENCES channels(id),
  user_id VARCHAR(36) REFERENCES users(id),
  PRIMARY KEY (channel_id, user_id)
);
