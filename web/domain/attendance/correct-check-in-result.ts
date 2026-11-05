import type { CheckIn } from "@/domain/attendance/check-in";

export type CorrectCheckInResult =
  | { type: "success"; checkIn: CheckIn }
  | { type: "invalid-input" }
  | { type: "not-registered" }
  | { type: "missing-function" }
  | { type: "network" };
