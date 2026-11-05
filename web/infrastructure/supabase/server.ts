import { createServerClient } from "@supabase/ssr";
import { cookies } from "next/headers";

import { supabaseConfig } from "@/infrastructure/supabase/config";

export async function createSupabaseServerClient() {
  const config = supabaseConfig();
  if (!config.isConfigured) {
    throw new Error("Supabase no está configurado.");
  }

  const cookieStore = await cookies();

  return createServerClient(config.url, config.publishableKey, {
    cookies: {
      getAll() {
        return cookieStore.getAll();
      },
      setAll(cookiesToSet) {
        try {
          cookiesToSet.forEach(({ name, value, options }) => {
            cookieStore.set(name, value, options);
          });
        } catch {
          // Una Server Component no puede escribir cookies. El proxy refresca la sesión.
        }
      },
    },
  });
}
