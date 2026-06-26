-- uuidカラム追加。既存はrandom_uuidを使ってETL
alter table todos
  add column new_id uuid not null default gen_random_uuid();

-- 既存のauto_increament pkを制約解除、カラム削除
alter table todos drop constraint todos_pkey;
alter table todos drop column id;

-- new_idをidにrenameして、pkに設定
alter table todos rename column new_id to id;
alter table todos add primary key (id);


drop sequence if exsits todos_id_seq;
