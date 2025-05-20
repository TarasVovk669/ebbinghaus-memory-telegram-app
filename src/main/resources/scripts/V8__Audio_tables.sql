create sequence e_audio_text_seq start with 1 increment by 1;

CREATE TABLE e_audio_text (
                              id           BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('e_audio_text_seq'),
                              user_id      BIGINT,
                              message_id   BIGINT,
                              audio_id      VARCHAR(255),
                              description  VARCHAR(255),
                              created_at   TIMESTAMP
);

-- Optional indexes for common lookups
CREATE INDEX idx_e_audio_text_user_id    ON e_audio_text(user_id);
CREATE INDEX idx_e_audio_text_created_at ON e_audio_text(created_at);

