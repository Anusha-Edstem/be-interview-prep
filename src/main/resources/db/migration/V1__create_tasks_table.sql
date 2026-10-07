CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL,
    due_date DATE,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_tasks_status ON tasks (status);
