export const SUPABASE_SCHEMA = "puntuall";

function readEnv(primary: string, fallback: string): string {
  return (process.env[primary] ?? process.env[fallback] ?? "").trim();
}

export function supabaseConfig() {
  const url = readEnv("NEXT_PUBLIC_SUPABASE_URL", "SUPABASE_URL");
  const publishableKey = readEnv("NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY", "SUPABASE_PUBLISHABLE_KEY");

  return {
    url,
    publishableKey,
    isConfigured: url.length > 0 && publishableKey.length > 0,
  };
}
