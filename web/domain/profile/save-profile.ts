export type SaveProfileResult =
  | { type: "success" }
  | { type: "invalid-input" }
  | { type: "missing-migration" }
  | { type: "network" };
