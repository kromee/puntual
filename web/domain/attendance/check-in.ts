export type CheckIn = {
  id: number;
  periodId: number;
  workDate: string;
  checkedInAt: string;
  expectedHour: number;
  expectedMinute: number;
  delayMinutes: number;
};
