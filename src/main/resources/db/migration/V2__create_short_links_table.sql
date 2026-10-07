CREATE TABLE short_links (
    id UUID PRIMARY KEY,
    code VARCHAR(8) NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    visit_count BIGINT NOT NULL DEFAULT 0,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE short_links ADD CONSTRAINT uq_short_links_code UNIQUE (code);
