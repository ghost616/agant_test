CREATE TABLE IF NOT EXISTS model_config (
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    platform_type VARCHAR(64),
    api_key     VARCHAR(255),
    base_url    VARCHAR(512),
    model_name  VARCHAR(255),
    temperature DOUBLE,
    max_tokens  INTEGER,
    status      VARCHAR(32),
    description TEXT,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tool_config (
    id               BIGINT PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    tool_type        VARCHAR(32),
    description      TEXT,
    parameter_schema TEXT,
    return_schema    TEXT,
    impl_path        VARCHAR(512),
    auth_config      TEXT,
    sub_tool_type    VARCHAR(32),
    tool_script      TEXT,
    status           VARCHAR(32),
    create_time      TIMESTAMP,
    update_time      TIMESTAMP,
    deleted          INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS session (
    id               BIGINT PRIMARY KEY,
    agent_id         BIGINT,
    model_id         BIGINT,
    title            VARCHAR(200),
    system_prompt    TEXT,
    parent_session_id BIGINT,
    is_child         TINYINT(1) DEFAULT 0,
    description      VARCHAR(500),
    thinking         TINYINT(1),
    total_token_used BIGINT,
    create_time      TIMESTAMP,
    update_time      TIMESTAMP,
    deleted          INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_session_agent_id ON session(agent_id);
CREATE INDEX IF NOT EXISTS idx_session_model_id ON session(model_id);

CREATE TABLE IF NOT EXISTS message (
    id           BIGINT PRIMARY KEY,
    session_id   BIGINT,
    role         VARCHAR(20),
    content      MEDIUMTEXT,
    reasoning    MEDIUMTEXT,
    sequence_num INT,
    tool_call_id VARCHAR(100),
    token_usage  TEXT,
    rollback     TINYINT NOT NULL DEFAULT 0,
    create_time  TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_message_session_id ON message(session_id);
CREATE INDEX IF NOT EXISTS idx_message_tool_call_id ON message(tool_call_id);

CREATE TABLE IF NOT EXISTS message_tool_call (
    id                  BIGINT PRIMARY KEY,
    message_id          BIGINT,
    tool_call_id        VARCHAR(100),
    tool_call_name      VARCHAR(100),
    tool_call_arguments TEXT
);
CREATE INDEX IF NOT EXISTS idx_message_tool_call_message_id ON message_tool_call(message_id);
CREATE INDEX IF NOT EXISTS idx_message_tool_call_tool_call_id ON message_tool_call(tool_call_id);

CREATE TABLE IF NOT EXISTS session_tool (
    id           BIGINT PRIMARY KEY,
    session_id   BIGINT,
    tool_id      BIGINT,
    session_auth INT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_session_tool_session_id ON session_tool(session_id);
CREATE INDEX IF NOT EXISTS idx_session_tool_tool_id ON session_tool(tool_id);

CREATE TABLE IF NOT EXISTS agent_config (
    id            BIGINT PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    system_prompt TEXT,
    model_id      BIGINT,
    status        VARCHAR(32),
    create_time             TIMESTAMP,
    update_time             TIMESTAMP,
    recent_message_count    INTEGER DEFAULT 10,
    deleted                 INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_agent_config_model_id ON agent_config(model_id);

CREATE TABLE IF NOT EXISTS agent_tool (
    id           BIGINT PRIMARY KEY,
    agent_id     BIGINT NOT NULL,
    tool_id      BIGINT NOT NULL,
    session_auth INT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_agent_tool_agent_id ON agent_tool(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_tool_tool_id ON agent_tool(tool_id);

CREATE TABLE IF NOT EXISTS agent_skill (
    id           BIGINT PRIMARY KEY,
    agent_id     BIGINT NOT NULL,
    skill_id     BIGINT NOT NULL,
    session_auth INT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_agent_skill_agent_id ON agent_skill(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_skill_skill_id ON agent_skill(skill_id);

CREATE TABLE IF NOT EXISTS skill_config (
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    prompt      TEXT,
    status      VARCHAR(32),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS skill_tool (
    id       BIGINT PRIMARY KEY,
    skill_id BIGINT NOT NULL,
    tool_id  BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_skill_tool_skill_id ON skill_tool(skill_id);
CREATE INDEX IF NOT EXISTS idx_skill_tool_tool_id ON skill_tool(tool_id);

CREATE TABLE IF NOT EXISTS session_variable (
    id             BIGINT PRIMARY KEY,
    session_id     BIGINT,
    variable_key   VARCHAR(255),
    variable_value TEXT,
    create_time    TIMESTAMP,
    update_time    TIMESTAMP,
    deleted        INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_session_variable_session_id ON session_variable(session_id);
CREATE INDEX IF NOT EXISTS idx_session_variable_key ON session_variable(session_id, variable_key);

CREATE TABLE IF NOT EXISTS session_skill (
    id           BIGINT PRIMARY KEY,
    session_id   BIGINT,
    skill_id     BIGINT,
    session_auth INT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_session_skill_session_id ON session_skill(session_id);
CREATE INDEX IF NOT EXISTS idx_session_skill_skill_id ON session_skill(skill_id);
