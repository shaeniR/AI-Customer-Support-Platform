import { getBackendHealth } from "@/services/apiClient";

export const dynamic = "force-dynamic";

export default async function Home() {
  const status = await getBackendHealth();
  const healthy = status === "UP";

  return (
    <main className="flex flex-1 items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-md rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="text-2xl font-semibold text-slate-900">LankaMart Support</h1>
        <p className="mt-2 text-slate-600">AI Customer Support Platform</p>
        <div className="mt-6 flex items-center gap-2 text-sm">
          <span
            className={`h-2.5 w-2.5 rounded-full ${healthy ? "bg-emerald-500" : "bg-red-500"}`}
          />
          <span className="text-slate-700">
            Backend: <strong>{status}</strong>
          </span>
        </div>
      </div>
    </main>
  );
}
