-- Hora de registro y corrección auditada para la web.
-- Esquema de la app: puntuall. Ejecutar en el SQL editor de Supabase.
-- No revoca los insert directos: la app Android todavía registra por su cuenta.

create table if not exists puntuall.check_in_corrections (
  id bigint generated always as identity primary key,
  user_id uuid not null,
  check_in_id bigint not null references puntuall.check_ins (id),
  previous_checked_in_at timestamptz not null,
  new_checked_in_at timestamptz not null,
  previous_delay_minutes integer not null,
  new_delay_minutes integer not null,
  reason text not null,
  created_at timestamptz not null default now(),
  constraint check_in_corrections_reason_length check (char_length(btrim(reason)) between 3 and 200)
);

alter table puntuall.check_in_corrections enable row level security;

drop policy if exists check_in_corrections_select_own on puntuall.check_in_corrections;
create policy check_in_corrections_select_own
  on puntuall.check_in_corrections
  for select
  to authenticated
  using (user_id = auth.uid());

drop policy if exists check_in_corrections_insert_own on puntuall.check_in_corrections;
create policy check_in_corrections_insert_own
  on puntuall.check_in_corrections
  for insert
  to authenticated
  with check (user_id = auth.uid());

grant select, insert on puntuall.check_in_corrections to authenticated;

create unique index if not exists check_ins_one_per_user_period_day
  on puntuall.check_ins (user_id, period_id, work_date);

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
  v_row puntuall.check_ins;
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
    extract(hour from v_local)::int,
    extract(minute from v_local)::int,
    0
  )
  returning * into v_row;

  return v_row;
exception
  when unique_violation then
    raise exception 'already_registered';
end;
$$;

create or replace function puntuall.correct_check_in(
  p_hour integer,
  p_minute integer,
  p_reason text
)
returns puntuall.check_ins
language plpgsql
security invoker
set search_path = puntuall, public
as $$
declare
  v_now timestamptz := now();
  v_date date := (v_now at time zone 'America/Mexico_City')::date;
  v_period puntuall.attendance_periods;
  v_row puntuall.check_ins;
  v_reason text := btrim(coalesce(p_reason, ''));
  v_new_at timestamptz;
  v_expected timestamp;
  v_actual timestamp;
  v_delay integer;
begin
  if auth.uid() is null then
    raise exception 'not_authenticated';
  end if;

  if p_hour is null or p_minute is null
    or p_hour < 0 or p_hour > 23
    or p_minute < 0 or p_minute > 59
    or char_length(v_reason) < 3
    or char_length(v_reason) > 200 then
    raise exception 'invalid_input';
  end if;

  select *
  into v_period
  from puntuall.attendance_periods
  where user_id = auth.uid()
    and is_active = true
  order by id desc
  limit 1;

  if not found then
    raise exception 'not_registered';
  end if;

  select *
  into v_row
  from puntuall.check_ins
  where user_id = auth.uid()
    and period_id = v_period.id
    and work_date = v_date;

  if not found then
    raise exception 'not_registered';
  end if;

  v_actual := v_date + make_time(p_hour, p_minute, 0);
  v_expected := v_date + make_time(v_row.expected_hour, v_row.expected_minute, 0);
  v_new_at := v_actual at time zone 'America/Mexico_City';
  v_delay := greatest(0, floor(extract(epoch from (v_actual - v_expected)) / 60))::int;

  insert into puntuall.check_in_corrections (
    user_id,
    check_in_id,
    previous_checked_in_at,
    new_checked_in_at,
    previous_delay_minutes,
    new_delay_minutes,
    reason
  )
  values (
    auth.uid(),
    v_row.id,
    v_row.checked_in_at,
    v_new_at,
    v_row.delay_minutes,
    v_delay,
    v_reason
  );

  update puntuall.check_ins
  set
    checked_in_at = v_new_at,
    delay_minutes = v_delay
  where id = v_row.id
    and user_id = auth.uid()
  returning * into v_row;

  return v_row;
end;
$$;

revoke all on function puntuall.register_check_in() from public;
revoke all on function puntuall.correct_check_in(integer, integer, text) from public;
grant execute on function puntuall.register_check_in() to authenticated;
grant execute on function puntuall.correct_check_in(integer, integer, text) to authenticated;
