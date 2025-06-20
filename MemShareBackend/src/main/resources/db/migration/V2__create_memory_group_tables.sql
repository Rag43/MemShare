-- Create memory_group table
CREATE TABLE memory_group (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1000),
    created_by INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES _user(id)
);

-- Create user_memory_group join table
CREATE TABLE user_memory_group (
    user_id INTEGER NOT NULL,
    group_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, group_id),
    FOREIGN KEY (user_id) REFERENCES _user(id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES memory_group(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_memory_group_created_by ON memory_group(created_by);
CREATE INDEX idx_user_memory_group_user_id ON user_memory_group(user_id);
CREATE INDEX idx_user_memory_group_group_id ON user_memory_group(group_id); 