alter table user_sessions
    alter column created_at type timestamptz using created_at at time zone 'UTC',
    alter column expires_at type timestamptz using expires_at at time zone 'UTC';

alter table refresh_tokens
    alter column issued_at type timestamptz using issued_at at time zone 'UTC';