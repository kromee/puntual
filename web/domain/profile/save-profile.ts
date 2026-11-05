export type SaveProfileResult =
  | { type: "success" }
  | { type: "invalid-input" }
  | { type: "network" };
