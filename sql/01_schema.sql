-- =====================================================
-- TELEGRAM APP - COMPLETE DATABASE SCHEMA
-- Enhanced with Contact Management System
-- =====================================================

-- Drop existing tables if they exist (for clean setup)
DROP TABLE IF EXISTS typing_status CASCADE;
DROP TABLE IF EXISTS channel_subscribers CASCADE;
DROP TABLE IF EXISTS channels CASCADE;
DROP TABLE IF EXISTS group_members CASCADE;
DROP TABLE IF EXISTS groups CASCADE;
DROP TABLE IF EXISTS messages CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =====================================================
-- CORE TABLES
-- =====================================================

-- Users table (enhanced with contact fields)
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    bio TEXT,
    profile_pic_path TEXT,
    status VARCHAR(50) DEFAULT 'Offline',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_verified BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    
    -- Contact-related fields (kept for backward compatibility)
    contacts TEXT DEFAULT ''
);

-- Groups table
CREATE TABLE groups (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    creator_id VARCHAR(36) REFERENCES users(id) ON DELETE CASCADE,
    description TEXT DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    max_members INT DEFAULT 200
);

-- Channels table
CREATE TABLE channels (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner_id VARCHAR(36) REFERENCES users(id) ON DELETE CASCADE,
    description TEXT DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    is_public BOOLEAN DEFAULT TRUE
);

-- =====================================================
-- MESSAGING TABLES
-- =====================================================

-- Messages table (enhanced)
CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY,
    sender_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    receiver_id VARCHAR(36),
    receiver_type VARCHAR(20),
    content TEXT,
    media_type VARCHAR(100) DEFAULT 'TEXT',
    media_path TEXT,
    timestamp TIMESTAMP WITHOUT TIME ZONE DEFAULT (NOW() AT TIME ZONE 'UTC'),
    read_status VARCHAR(20) DEFAULT 'UNREAD',
    reply_to_message_id VARCHAR(36) REFERENCES messages(id) ON DELETE SET NULL,
    forwarded_from_id VARCHAR(36) REFERENCES messages(id) ON DELETE SET NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    edited_at TIMESTAMP NULL
);

-- =====================================================
-- MEMBERSHIP TABLES
-- =====================================================

-- Group members
CREATE TABLE group_members (
    group_id VARCHAR(36) REFERENCES groups(id) ON DELETE CASCADE,
    user_id VARCHAR(36) REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER', -- CREATOR, ADMIN, MEMBER
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (group_id, user_id)
);

-- Channel subscribers
CREATE TABLE channel_subscribers (
    channel_id VARCHAR(36) REFERENCES channels(id) ON DELETE CASCADE,
    user_id VARCHAR(36) REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'SUBSCRIBER', -- OWNER, ADMIN, SUBSCRIBER
    subscribed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (channel_id, user_id)
);

-- =====================================================
-- REAL-TIME FEATURES
-- =====================================================

-- Typing status
CREATE TABLE typing_status (
    chat_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    last_typed TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (chat_id, user_id)
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Basic indexes for users table
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_display_name ON users (display_name);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_last_seen ON users (last_seen);

-- Basic indexes for groups table
CREATE INDEX idx_groups_creator ON groups (creator_id);
CREATE INDEX idx_groups_name ON groups (name);

-- Basic indexes for channels table
CREATE INDEX idx_channels_owner ON channels (owner_id);
CREATE INDEX idx_channels_name ON channels (name);
CREATE INDEX idx_channels_public ON channels (is_public);

-- Basic indexes for messages table
CREATE INDEX idx_messages_sender ON messages (sender_id);
CREATE INDEX idx_messages_receiver ON messages (receiver_id, receiver_type);
CREATE INDEX idx_messages_timestamp ON messages (timestamp);
CREATE INDEX idx_messages_read_status ON messages (read_status);

-- Basic indexes for group_members table
CREATE INDEX idx_group_members_group ON group_members (group_id);
CREATE INDEX idx_group_members_user ON group_members (user_id);
CREATE INDEX idx_group_members_role ON group_members (role);

-- Basic indexes for channel_subscribers table
CREATE INDEX idx_channel_subscribers_channel ON channel_subscribers (channel_id);
CREATE INDEX idx_channel_subscribers_user ON channel_subscribers (user_id);
CREATE INDEX idx_channel_subscribers_role ON channel_subscribers (role);

-- Basic indexes for typing_status table
CREATE INDEX idx_typing_status_chat ON typing_status (chat_id);
CREATE INDEX idx_typing_status_user ON typing_status (user_id);
CREATE INDEX idx_typing_status_last_typed ON typing_status (last_typed);

-- =====================================================
-- SAMPLE DATA (FOR TESTING)
-- =====================================================

-- Sample users
INSERT INTO users (id, username, password_hash, display_name, bio, status) VALUES
('user-1', 'john_doe', '$2a$10$example_hash_1', 'John Doe', 'Software Developer', 'Online'),
('user-2', 'jane_smith', '$2a$10$example_hash_2', 'Jane Smith', 'Designer', 'Away'),
('user-3', 'bob_wilson', '$2a$10$example_hash_3', 'Bob Wilson', 'Marketing Manager', 'Offline'),
('user-4', 'alice_johnson', '$2a$10$example_hash_4', 'Alice Johnson', 'Project Manager', 'Busy'),
('user-5', 'charlie_brown', '$2a$10$example_hash_5', 'Charlie Brown', 'Student', 'Online');

-- Sample messages
INSERT INTO messages (id, sender_id, receiver_id, receiver_type, content, timestamp, read_status) VALUES
('msg-1', 'user-1', 'user-2', 'USER', 'Hey Jane, how are you?', NOW() - INTERVAL '1 HOUR', 'READ'),
('msg-2', 'user-2', 'user-1', 'USER', 'Hi John! I am good, thanks!', NOW() - INTERVAL '30 MINUTE', 'READ'),
('msg-3', 'user-1', 'user-3', 'USER', 'Want to grab lunch tomorrow?', NOW() - INTERVAL '15 MINUTE', 'UNREAD');

-- =====================================================
-- END OF SCHEMA
-- =====================================================

SELECT 'Database schema created successfully!' as Status;