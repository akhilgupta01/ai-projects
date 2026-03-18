CREATE TABLE IF NOT EXISTS attribute_definitions (
    id BIGSERIAL PRIMARY KEY,
    attribute_name VARCHAR(255) NOT NULL,
    jurisdiction VARCHAR(255) NOT NULL,
    definition_json TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_attribute_jurisdiction UNIQUE (attribute_name, jurisdiction)
);
