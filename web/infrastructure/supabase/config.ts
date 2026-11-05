export const SUPABASE_SCHEMA = "puntuall";

export function supabaseConfig() {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL?.trim() ?? "";
  const publishableKey = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY?.trim() ?? "";

  return {
    url,
    publishableKey,
    isConfigured: url.length > 0 && publishableKey.length > 0,
  };
}
