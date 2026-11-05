-- Perfil web: nombre visible y hora esperada compartida en Supabase.
-- Esquema puntuall. Ejecutar en el SQL editor de Supabase después de register_check_in.

create table if not exists puntuall.user_profiles (
  user_id uuid primary key references auth.users (id) on delete cascade,
  display_name text not null default '',
  has_expected_time boolean not null default false,
  expected_hour integer,
  expected_minute integer,
  updated_at timestamptz not null default now(),
  constraint user_profiles_display_name_length check (char_length(display_name) <= 80),
  constraint user_profiles_expected_time_consistency check (
    (
      not has_expected_time
      and expected_hour is null
      and expected_minute is null
    )
    or (
      has_expected_time
      and expected_hour between 0 and 23
      and expected_minute between 0 and 59
    )
  )
);

alter table puntuall.user_profiles enable row level security;

drop policy if exists user_profiles_select_own on puntuall.user_profiles;
create policy user_profiles_select_own
  on puntuall.user_profiles
  for select
  to authenticated
  using (user_id = auth.uid());

drop policy if exists user_profiles_insert_own on puntuall.user_profiles;
create policy user_profiles_insert_own
  on puntuall.user_profiles
  for insert
  to authenticated
  with check (user_id = auth.uid());

drop policy if exists user_profiles_update_own on puntuall.user_profiles;
create policy user_profiles_update_own
  on puntuall.user_profiles
  for update
  to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

grant select, insert, update on puntuall.user_profiles to authenticated;

create or replace function puntuall.register_check_in()
returns puntuall.check_ins
language plpgsql
security invoker
set search_path = puntuall, public
as $$
declare
  v_now timestamptz := now();
  v_local timestamp := v_now at time zone 'America/Mexico_City';
  v_date date := v_local::date;
  v_period puntuall.attendance_periods;
  v_profile puntuall.user_profiles;
  v_row puntuall.check_ins;
  v_expected_hour integer;
  v_expected_minute integer;
  v_delay integer := 0;
  v_expected_at timestamp;
begin
  if auth.uid() is null then
    raise exception 'not_authenticated';
  end if;

  if extract(isodow from v_date) > 5 then
    raise exception 'not_workday';
  end if;

  select *
  into v_period
  from puntuall.attendance_periods
  where user_id = auth.uid()
    and is_active = true
  order by id desc
  limit 1;

  if not found then
    raise exception 'no_active_period';
  end if;

  if v_date < v_period.start_date
    or (v_period.end_date is not null and v_date > v_period.end_date) then
    raise exception 'outside_active_period';
  end if;

  select *
  into v_profile
  from puntuall.user_profiles
  where user_id = auth.uid();

  if found and v_profile.has_expected_time then
    v_expected_hour := v_profile.expected_hour;
    v_expected_minute := v_profile.expected_minute;
    v_expected_at := v_date + make_time(v_expected_hour, v_expected_minute, 0);
    v_delay := greatest(0, floor(extract(epoch from (v_local - v_expected_at)) / 60))::int;
  else
    v_expected_hour := extract(hour from v_local)::int;
    v_expected_minute := extract(minute from v_local)::int;
    v_delay := 0;
  end if;

  insert into puntuall.check_ins (
    user_id,
    period_id,
    work_date,
    checked_in_at,
    expected_hour,
    expected_minute,
    delay_minutes
  )
  values (
    auth.uid(),
    v_period.id,
    v_date,
    v_now,
    v_expected_hour,
    v_expected_minute,
    v_delay
  )
  returning * into v_row;

  return v_row;
exception
  when unique_violation then
    raise exception 'already_registered';
end;
$$;

revoke all on function puntuall.register_check_in() from public;
grant execute on function puntuall.register_check_in() to authenticated;
