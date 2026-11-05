import { LoginForm } from "@/components/auth/login-form";

export default function LoginPage() {
  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col px-4 py-10 lg:max-w-lg lg:px-8 lg:py-16">
      <p className="text-3xl font-bold text-puntual-green lg:text-4xl">Puntual</p>
      <p className="mt-1 text-lg text-puntual-secondary">Acceso personal</p>
      <section className="mt-6 rounded-2xl bg-white p-5 shadow-sm lg:p-8">
        <h1 className="text-xl font-semibold text-puntual-text">Iniciar sesión</h1>
        <div className="mt-4">
          <LoginForm />
        </div>
      </section>
    </main>
  );
}
