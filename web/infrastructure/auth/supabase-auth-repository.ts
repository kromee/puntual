import type { AuthRepository } from "@/domain/auth/auth-repository";
import type { AuthSession } from "@/domain/auth/auth-session";
import type { SignInResult } from "@/domain/auth/sign-in-result";
import { createSupabaseServerClient } from "@/infrastructure/supabase/server";
import { supabaseConfig } from "@/infrastructure/supabase/config";

export function createSupabaseAuthRepository(): AuthRepository {
  return {
    signIn(email, password) {
      return signInWithSupabase(email, password);
    },
    signOut() {
      return signOutFromSupabase();
    },
    getSession() {
      return readSession();
    },
  };
}

async function signInWithSupabase(email: string, password: string): Promise<SignInResult> {
  if (!supabaseConfig().isConfigured) {
    return { type: "missing-config" };
  }

  try {
    const supabase = await createSupabaseServerClient();
    const { data, error } = await supabase.auth.signInWithPassword({ email, password });

    if (error) {
      if (error.status === 400 || error.status === 401) {
        return { type: "invalid-credentials" };
      }
      return { type: "error", message: error.message };
    }

    const user = data.user;
    if (!user) {
      return { type: "error", message: "No se pudo iniciar sesión." };
    }

    return {
      type: "success",
      session: toSession(user.id, user.email, email),
    };
  } catch (error) {
    return {
      type: "error",
      message: error instanceof Error ? error.message : "No se pudo iniciar sesión.",
    };
  }
}

async function signOutFromSupabase(): Promise<void> {
  if (!supabaseConfig().isConfigured) {
    return;
  }
  const supabase = await createSupabaseServerClient();
  await supabase.auth.signOut();
}

async function readSession(): Promise<AuthSession | null> {
  if (!supabaseConfig().isConfigured) {
    return null;
  }

  const supabase = await createSupabaseServerClient();
  const { data } = await supabase.auth.getUser();
  if (!data.user) {
    return null;
  }

  return toSession(data.user.id, data.user.email, "");
}

function toSession(userId: string, email: string | undefined, fallbackEmail: string): AuthSession {
  return {
    userId,
    email: email || fallbackEmail,
  };
}
