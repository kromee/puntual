/** Hora local de llegada en Ciudad de México como ISO (offset fijo UTC−6). */
export function mexicoCheckInTimestamp(workDate: string, hour: number, minute: number): string {
  const hh = String(hour).padStart(2, "0");
  const mm = String(minute).padStart(2, "0");
  return `${workDate}T${hh}:${mm}:00-06:00`;
}
