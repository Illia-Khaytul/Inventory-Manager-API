alter table products
    add deleted_at timestamptz,
    add deleted_by varchar(255),
    drop constraint products_name_key,
    add constraint unique_not_deleted_name unique nulls not distinct (name, deleted_at);