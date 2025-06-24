-- Create memories table
CREATE TABLE memories (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    title VARCHAR(255) NOT NULL,
    memory_date TIMESTAMP NOT NULL,
    location VARCHAR(500),
    is_public BOOLEAN DEFAULT FALSE,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_memories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create media table
CREATE TABLE media (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    s3_bucket VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    media_type VARCHAR(20) NOT NULL CHECK (media_type IN ('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT')),
    content_type VARCHAR(100),
    width INTEGER,
    height INTEGER,
    duration BIGINT,
    thumbnail_s3_key VARCHAR(500),
    memory_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_media_memory FOREIGN KEY (memory_id) REFERENCES memories(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_memories_user_id ON memories(user_id);
CREATE INDEX idx_memories_memory_date ON memories(memory_date);
CREATE INDEX idx_memories_created_at ON memories(created_at);
CREATE INDEX idx_memories_is_public ON memories(is_public);

CREATE INDEX idx_media_memory_id ON media(memory_id);
CREATE INDEX idx_media_media_type ON media(media_type);
CREATE INDEX idx_media_s3_key ON media(s3_key);
CREATE INDEX idx_media_created_at ON media(created_at);
CREATE INDEX idx_media_file_size ON media(file_size);

-- Create unique constraint on s3_key to prevent duplicates
CREATE UNIQUE INDEX idx_media_s3_key_unique ON media(s3_key);

-- Create composite indexes for common queries
CREATE INDEX idx_memories_user_date ON memories(user_id, memory_date);
CREATE INDEX idx_media_memory_type ON media(memory_id, media_type);

-- Add comments for documentation
COMMENT ON TABLE memories IS 'Stores user memories with text content and metadata';
COMMENT ON TABLE media IS 'Stores media files associated with memories, with S3 metadata';

COMMENT ON COLUMN memories.content IS 'The main text content of the memory';
COMMENT ON COLUMN memories.title IS 'Optional title for the memory';
COMMENT ON COLUMN memories.memory_date IS 'When the memory occurred';
COMMENT ON COLUMN memories.location IS 'Optional location where the memory occurred';
COMMENT ON COLUMN memories.is_public IS 'Whether the memory is public or private';

COMMENT ON COLUMN media.file_name IS 'Generated unique filename';
COMMENT ON COLUMN media.original_file_name IS 'Original filename from upload';
COMMENT ON COLUMN media.file_type IS 'MIME type of the file';
COMMENT ON COLUMN media.s3_key IS 'S3 object key for the file';
COMMENT ON COLUMN media.s3_bucket IS 'S3 bucket name';
COMMENT ON COLUMN media.file_size IS 'File size in bytes';
COMMENT ON COLUMN media.media_type IS 'Type of media (IMAGE, VIDEO, AUDIO, DOCUMENT)';
COMMENT ON COLUMN media.width IS 'Width for images/videos';
COMMENT ON COLUMN media.height IS 'Height for images/videos';
COMMENT ON COLUMN media.duration IS 'Duration for videos/audio in milliseconds';
COMMENT ON COLUMN media.thumbnail_s3_key IS 'S3 key for video thumbnail'; 