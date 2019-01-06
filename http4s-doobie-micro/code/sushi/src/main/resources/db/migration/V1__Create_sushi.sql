CREATE TABLE sushi_kinds
(
  id       BIGSERIAL PRIMARY KEY,
  name     varchar(30) not null,
  set_size int         not null,
  price    int         not null
);

insert into sushi_kinds(name, set_size, price)
values ('nigiri', 2, 12),
       ('hosomaki', 6, 18),
       ('uramaki', 8, 14)