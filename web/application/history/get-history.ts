import { formatCheckInTime, formatDelay, formatWorkDate } from "@/application/attendance/format-entry";
import { formatHoursMinutes, formatMonthTitle, formatShortMonth } from "@/application/history/format-history";
import {
  absenceStatusLabel,
  absenceTypeLabel,
  isActiveAbsence,
  type Absence,
} from "@/domain/attendance/absence";
import { isWorkday } from "@/domain/attendance/attendance-clock";
import type { AttendancePeriod } from "@/domain/attendance/attendance-period";
import type { CheckIn } from "@/domain/attendance/check-in";
import type { HistoryRepository } from "@/domain/attendance/history-repository";
import {
  availableYears,
  canGoToNextMonth,
  canGoToPreviousMonth,
  clampYearMonth,
  eachDate,
  endOfMonth,
  firstSelectableMonth,
  intersectRange,
  isoDate,
  lastSelectableMonth,
  yearMonthOf,
  type YearMonth,
} from "@/domain/attendance/period-date-rules";

export type HistoryView = {
  periods: AttendancePeriod[];
  periodId: number;
  periodTitle: string;
  periodRange: string;
  years: number[];
  year: number;
  month: number;
  monthTitle: string;
  canGoPrevious: boolean;
  canGoNext: boolean;
  previous: YearMonth | null;
  next: YearMonth | null;
  monthLateDays: number;
  monthTotalLabel: string;
  yearLateDays: number;
  yearTotalLabel: string;
  months: {
    label: string;
    lateDays: number;
    totalLabel: string;
    hasData: boolean;
  }[];
  rows: {
    key: string;
    dayLabel: string;
    timeLabel: string;
    delayLabel: string;
    tone: "ontime" | "late" | "absence";
  }[];
  isEmpty: boolean;
  isCurrentMonth: boolean;
};

export async function getHistory(
  repository: HistoryRepository,
  input: {
    today: string;
    periodId: number | null;
    year: number | null;
    month: number | null;
  },
): Promise<HistoryView | null> {
  const periods = await repository.listPeriods();
  if (!periods || periods.length === 0) {
    return null;
  }

  const period = pickPeriod(periods, input.periodId);
  const years = availableYears(period, input.today);
  const today = yearMonthOf(input.today);
  const requestedYear = input.year && years.includes(input.year) ? input.year : years[0];
  const requestedMonth = input.month ?? (requestedYear === today.year ? today.month : 12);
  const selected = clampYearMonth(requestedYear, requestedMonth, period, input.today);
  const yearEndMonth = lastSelectableMonth(period, selected.year, input.today);
  const yearSlice = yearEndMonth.year === selected.year
    ? intersectRange(
      period,
      isoDate(selected.year, 1, 1),
      endOfMonth(selected.year, yearEndMonth.month),
      input.today,
    )
    : null;
  const monthSlice = intersectRange(
    period,
    isoDate(selected.year, selected.month, 1),
    endOfMonth(selected.year, selected.month),
    input.today,
  );
  const checkIns = yearSlice
    ? await repository.listCheckIns(period.id, yearSlice.start, yearSlice.end)
    : [];
  const absences = monthSlice
    ? await repository.listAbsences(period.id, monthSlice.start, monthSlice.end)
    : [];
  const monthCheckIns = checkIns.filter((checkIn) => sameMonth(checkIn.workDate, selected));
  const canGoPrevious = canGoToPreviousMonth(selected, period);
  const canGoNext = canGoToNextMonth(selected, period, input.today);
  const rows = historyRows(monthCheckIns, absences, monthSlice);

  return {
    periods,
    periodId: period.id,
    periodTitle: period.title,
    periodRange: `${formatWorkDate(period.startDate)} – ${period.endDate ? formatWorkDate(period.endDate) : "actualidad"}`,
    years,
    year: selected.year,
    month: selected.month,
    monthTitle: formatMonthTitle(selected.year, selected.month),
    canGoPrevious,
    canGoNext,
    previous: canGoPrevious ? { year: selected.year, month: selected.month - 1 } : null,
    next: canGoNext ? { year: selected.year, month: selected.month + 1 } : null,
    monthLateDays: lateDays(monthCheckIns),
    monthTotalLabel: formatHoursMinutes(totalMinutes(monthCheckIns)),
    yearLateDays: lateDays(checkIns),
    yearTotalLabel: formatHoursMinutes(totalMinutes(checkIns)),
    months: breakdown(checkIns, period, selected.year, input.today),
    rows,
    isEmpty: rows.length === 0,
    isCurrentMonth: selected.year === today.year && selected.month === today.month,
  };
}

function historyRows(
  checkIns: CheckIn[],
  absences: Absence[],
  monthSlice: { start: string; end: string } | null,
): HistoryView["rows"] {
  const checkInDates = new Set(checkIns.map((checkIn) => checkIn.workDate));
  const rows = [
    ...checkIns.map((checkIn) => ({
      key: `check-${checkIn.id}`,
      sort: checkIn.workDate,
      dayLabel: formatWorkDate(checkIn.workDate),
      timeLabel: formatCheckInTime(checkIn.checkedInAt),
      delayLabel: formatDelay(checkIn.delayMinutes),
      tone: checkIn.delayMinutes > 0 ? "late" as const : "ontime" as const,
    })),
    ...absences.filter(isActiveAbsence).flatMap((absence) => absenceDays(absence, monthSlice, checkInDates)),
  ].sort((left, right) => left.sort.localeCompare(right.sort));

  return rows.map((row) => ({
    key: row.key,
    dayLabel: row.dayLabel,
    timeLabel: row.timeLabel,
    delayLabel: row.delayLabel,
    tone: row.tone,
  }));
}

function absenceDays(
  absence: Absence,
  monthSlice: { start: string; end: string } | null,
  checkInDates: Set<string>,
) {
  if (!monthSlice) {
    return [];
  }
  const start = absence.startDate > monthSlice.start ? absence.startDate : monthSlice.start;
  const end = absence.endDate < monthSlice.end ? absence.endDate : monthSlice.end;
  if (start > end) {
    return [];
  }
  return eachDate(start, end)
    .filter((date) => isWorkday(date) && !checkInDates.has(date))
    .map((date) => ({
      key: `absence-${absence.id}-${date}`,
      sort: date,
      dayLabel: formatWorkDate(date),
      timeLabel: absenceTypeLabel(absence.type),
      delayLabel: absence.reason.trim() || absenceStatusLabel(absence.status),
      tone: "absence" as const,
    }));
}

function breakdown(
  checkIns: CheckIn[],
  period: AttendancePeriod,
  year: number,
  today: string,
): HistoryView["months"] {
  const first = firstSelectableMonth(period, year);
  const last = lastSelectableMonth(period, year, today);
  if (first.year !== year || last.year !== year || first.month > last.month) {
    return [];
  }
  const months: HistoryView["months"] = [];
  for (let month = first.month; month <= last.month; month += 1) {
    const monthCheckIns = checkIns.filter((checkIn) => yearMonthOf(checkIn.workDate).month === month);
    const minutes = totalMinutes(monthCheckIns);
    const days = lateDays(monthCheckIns);
    const hasData = days > 0 || minutes > 0;
    months.push({
      label: formatShortMonth(year, month),
      lateDays: days,
      totalLabel: hasData ? formatHoursMinutes(minutes) : "—",
      hasData,
    });
  }
  return months;
}

function pickPeriod(periods: AttendancePeriod[], periodId: number | null): AttendancePeriod {
  return periods.find((period) => period.id === periodId)
    ?? periods.find((period) => period.isActive)
    ?? periods[0];
}

function sameMonth(iso: string, selected: YearMonth): boolean {
  const month = yearMonthOf(iso);
  return month.year === selected.year && month.month === selected.month;
}

function lateDays(checkIns: CheckIn[]): number {
  return checkIns.filter((checkIn) => checkIn.delayMinutes > 0).length;
}

function totalMinutes(checkIns: CheckIn[]): number {
  return checkIns.reduce((sum, checkIn) => sum + checkIn.delayMinutes, 0);
}
