create table order_items(
    order_id bigint not null,
    product_id bigint not null,
    quantity int not null,
    reserved int not null,
    product_name varchar(255) not null,
    product_price numeric not null,

    primary key (order_id, product_id),

    constraint fk_order foreign key (order_id) references orders(id) on delete cascade,
    constraint fk_product foreign key (product_id) references products(id)
)