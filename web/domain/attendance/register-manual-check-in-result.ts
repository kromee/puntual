export type RegisterManualCheckInResult =
  | { type: "success" }
  | { type: "invalid-input" }
  | { type: "future-date" }
  | { type: "not-workday" }
  | { type: "outside-active-period" }
  | { type: "already-registered" }
  | { type: "no-period" }
  | { type: "network" };
