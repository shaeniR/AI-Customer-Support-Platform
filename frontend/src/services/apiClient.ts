export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export async function getBackendHealth(): Promise<string> {
  try {
    const res = await fetch(`${API_BASE_URL}/actuator/health`, { cache: "no-store" });
    const body = (await res.json()) as { status?: string };
    return body.status ?? "UNKNOWN";
  } catch {
    return "UNREACHABLE";
  }
}
