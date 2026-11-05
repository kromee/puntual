import type { CheckInRepository } from "@/domain/attendance/check-in-repository";

export function getTodayEntry(repository: CheckInRepository) {
  return repository.getTodayEntry();
}
