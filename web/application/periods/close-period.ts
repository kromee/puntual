import { defaultPeriodTitle } from "@/application/periods/period-title";
import type { ClosePeriodResult } from "@/domain/attendance/close-period";
import { validateClosePeriod } from "@/domain/attendance/close-period";
import type { PeriodRepository } from "@/domain/attendance/period-repository";

export async function closePeriod(
  repository: PeriodRepository,
  input: { endDate: string; newStartDate: string; newTitle: string },
  today: string,
): Promise<ClosePeriodResult> {
  const active = await repository.getActivePeriod();
  if (!active) {
    return { type: "no-active-period" };
  }

  const error = validateClosePeriod(active.startDate, input.endDate, input.newStartDate, today);
  if (error) {
    return error;
  }

  const customTitle = input.newTitle.trim();
  return repository.closeActivePeriod({
    periodId: active.id,
    closedTitle: defaultPeriodTitle(active.startDate, input.endDate),
    endDate: input.endDate,
    openedTitle: customTitle || defaultPeriodTitle(input.newStartDate, null),
    newStartDate: input.newStartDate,
  });
}
