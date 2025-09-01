CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  profile_name VARCHAR(100),
  profile_pic TEXT,
  bio TEXT,
  status VARCHAR(50),
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS private_chats (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user1 UUID NOT NULL REFERENCES users(id),
  user2 UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMP DEFAULT now(),
  CONSTRAINT unique_pair UNIQUE (user1, user2)
);

CREATE TABLE IF NOT EXISTS groups (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name VARCHAR(150) NOT NULL,
  creator UUID NOT NULL REFERENCES users(id),
  profile_pic TEXT,
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS channels (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name VARCHAR(150) NOT NULL,
  owner UUID NOT NULL REFERENCES users(id),
  profile_pic TEXT,
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS messages (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  sender_id UUID NOT NULL REFERENCES users(id),
  receiver_private_chat UUID NULL REFERENCES private_chats(id),
  receiver_group UUID NULL REFERENCES groups(id),
  receiver_channel UUID NULL REFERENCES channels(id),
  content TEXT,
  image_path TEXT,
  timestamp TIMESTAMP DEFAULT now(),
  read_status VARCHAR(20) DEFAULT 'SENT'
);

-- Group membership
CREATE TABLE IF NOT EXISTS group_members (
  group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
  user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role     VARCHAR(20) DEFAULT 'member',
  PRIMARY KEY (group_id, user_id)
);

-- Channel subscribers
CREATE TABLE IF NOT EXISTS channel_subscribers (
  channel_id UUID NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
  user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  PRIMARY KEY (channel_id, user_id)
);

-- Typing indicator table
CREATE TABLE IF NOT EXISTS typing_status (
  chat_id UUID NOT NULL,
  user_id UUID NOT NULL,
  last_ts TIMESTAMP NOT NULL,
  PRIMARY KEY (chat_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp);
