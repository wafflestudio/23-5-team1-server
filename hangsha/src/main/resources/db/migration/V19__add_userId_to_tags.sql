ALTER TABLE tags
    ADD COLUMN user_id BIGINT NOT NULL AFTER id;

ALTER TABLE tags
    ADD CONSTRAINT fk_tags_user_id
        FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE CASCADE
            ON UPDATE CASCADE;

ALTER TABLE tags
DROP INDEX uk_tag_name,
  ADD UNIQUE KEY uk_tag_user_name (user_id, name);

CREATE INDEX idx_tags_user_id ON tags(user_id);