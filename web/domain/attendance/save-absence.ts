export type SaveAbsenceResult =
  | { type: "success" }
  | { type: "invalid-input" }
  | { type: "no-active-period" }
  | { type: "end-before-start" }
  | { type: "outside-active-period" }
  | { type: "overlaps-existing" }
  | { type: "network" };
