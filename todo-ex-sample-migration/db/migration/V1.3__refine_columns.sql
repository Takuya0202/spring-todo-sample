alter table todos rename column todo to title;

alter table todos
  alter column created_at set not null,
  alter column updated_at set not null;
