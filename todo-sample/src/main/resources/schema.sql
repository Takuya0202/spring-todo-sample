drop table if exists todos;

create table todos (
  id serial primary key,
  todo varchar(255) not null,
  detail text,
  created_at timestamp without time zone,
  updated_at timestamp without time zone
)
