import type { CheckInRepository } from "@/domain/attendance/check-in-repository";

export function registerCheckIn(repository: CheckInRepository) {
  return repository.registerCheckIn();
}
