create table auth_attempt_buckets (
 bucket_key varchar(64) primary key,
 window_start bigint not null,
 attempts integer not null
);
create index idx_auth_attempt_window on auth_attempt_buckets(window_start);
