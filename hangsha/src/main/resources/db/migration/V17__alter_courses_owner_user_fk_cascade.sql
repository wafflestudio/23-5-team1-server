ALTER TABLE courses
    DROP FOREIGN KEY fk_courses_owner_user_id;
ALTER TABLE courses
    ADD CONSTRAINT fk_courses_owner_user_id
    FOREIGN KEY (owner_user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE;