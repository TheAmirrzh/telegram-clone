-- This is the complete and final schema.
-- It will DROP existing tables to ensure a clean start.

DROP TABLE IF EXISTS typing_status, channel_subscribers, channels, group_members, groups, messages, users CASCADE;

CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(36) PRIMARY KEY,
  username VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(255),
  bio TEXT,
  profile_pic_path TEXT,
  status VARCHAR(50) DEFAULT 'Offline'
);

CREATE TABLE IF NOT EXISTS messages (
  id VARCHAR(36) PRIMARY KEY,
  sender_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
  receiver_id VARCHAR(36),
  receiver_type VARCHAR(20),
  content TEXT,
  media_type VARCHAR(100),
  media_path TEXT,
  timestamp TIMESTAMP WITHOUT TIME ZONE DEFAULT (NOW() AT TIME ZONE 'UTC'),
  read_status VARCHAR(20),
  reply_to_message_id VARCHAR(36) REFERENCES messages(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS groups (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  creator_id VARCHAR(36) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS group_members (
  group_id VARCHAR(36) REFERENCES groups(id) ON DELETE CASCADE,
  user_id VARCHAR(36) REFERENCES users(id) ON DELETE CASCADE,
  role VARCHAR(20) NOT NULL DEFAULT 'MEMBER', -- CREATOR, ADMIN, MEMBER
  PRIMARY KEY (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS channels (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  owner_id VARCHAR(36) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS channel_subscribers (
  channel_id VARCHAR(36) REFERENCES channels(id) ON DELETE CASCADE,
  user_id VARCHAR(36) REFERENCES users(id) ON DELETE CASCADE,
  role VARCHAR(20) NOT NULL DEFAULT 'SUBSCRIBER', -- OWNER, ADMIN, SUBSCRIBER
  PRIMARY KEY (channel_id, user_id)
);

CREATE TABLE IF NOT EXISTS typing_status (
    chat_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    last_typed TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (chat_id, user_id)
);

