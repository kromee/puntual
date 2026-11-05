export type UserProfile = {
  displayName: string;
  hasExpectedTime: boolean;
  expectedHour: number;
  expectedMinute: number;
};

export const DEFAULT_EXPECTED_HOUR = 8;
export const DEFAULT_EXPECTED_MINUTE = 0;

export function defaultUserProfile(): UserProfile {
  return {
    displayName: "",
    hasExpectedTime: false,
    expectedHour: DEFAULT_EXPECTED_HOUR,
    expectedMinute: DEFAULT_EXPECTED_MINUTE,
  };
}
