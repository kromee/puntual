"use client";

import { useActionState, useState } from "react";

import { signInAction, type SignInFormState } from "@/app/auth/actions";

const initialState: SignInFormState = { errorMessage: null };

export function LoginForm() {
  const [state, formAction, pending] = useActionState(signInAction, initialState);
  const [passwordVisible, setPasswordVisible] = useState(false);

  return (
    <form action={formAction} className="flex flex-col gap-4">
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Correo
        <input
          name="email"
          type="email"
          autoComplete="email"
          required
          disabled={pending}
          className="h-12 rounded-xl border border-puntual-muted bg-white px-3 text-base font-normal outline-none focus:border-puntual-green"
        />
      </label>
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Contraseña
        <span className="relative">
          <input
            name="password"
            type={passwordVisible ? "text" : "password"}
            autoComplete="current-password"
            required
            disabled={pending}
            className="h-12 w-full rounded-xl border border-puntual-muted bg-white px-3 pr-24 text-base font-normal outline-none focus:border-puntual-green"
          />
          <button
            type="button"
            className="absolute top-1/2 right-3 -translate-y-1/2 text-sm font-medium text-puntual-secondary"
            onClick={() => setPasswordVisible((visible) => !visible)}
          >
            {passwordVisible ? "Ocultar" : "Mostrar"}
          </button>
        </span>
      </label>
      {state.errorMessage ? (
        <p role="alert" className="text-sm text-puntual-error">
          {state.errorMessage}
        </p>
      ) : null}
      <button
        type="submit"
        disabled={pending}
        className="h-12 rounded-xl bg-puntual-green text-base font-semibold text-white disabled:opacity-60"
      >
        {pending ? "Validando..." : "Entrar"}
      </button>
    </form>
  );
}
