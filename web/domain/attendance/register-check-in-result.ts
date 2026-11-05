import type { CheckIn } from "@/domain/attendance/check-in";

export type RegisterCheckInResult =
  | { type: "success"; checkIn: CheckIn }
  | { type: "not-workday" }
  | { type: "already-registered" }
  | { type: "no-active-period" }
  | { type: "outside-active-period" }
  | { type: "missing-function" }
  | { type: "network" };
