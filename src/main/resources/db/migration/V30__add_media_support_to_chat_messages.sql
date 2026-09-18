-- V30: Add media support to chat_messages table

ALTER TABLE chat_messages 
ADD COLUMN message_type VARCHAR(50) NULL,
ADD COLUMN attachment_url TEXT NULL;
