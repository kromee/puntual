export function delayMinutesSameDay(
  hour: number,
  minute: number,
  expectedHour: number,
  expectedMinute: number,
): number {
  const arrival = hour * 60 + minute;
  const expected = expectedHour * 60 + expectedMinute;
  return Math.max(0, arrival - expected);
}
