create sequence e_explain_it_back_seq start with 1 increment by 1;

CREATE TABLE e_explain_it_back (
                                   id           BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('e_explain_it_back_seq'),
                                   user_id      BIGINT,
                                   message_id   BIGINT,
                                   created_at   TIMESTAMP
);

CREATE INDEX idx_e_explain_it_back_user_id ON e_explain_it_back(user_id);
CREATE INDEX idx_e_explain_it_back_created_at ON e_explain_it_back(created_at);
