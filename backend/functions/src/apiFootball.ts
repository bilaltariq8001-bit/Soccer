import { API_BASE, API_KEY_HEADER } from "./config";

export class ApiError extends Error {
  constructor(message: string, readonly status?: number) {
    super(message);
    this.name = "ApiError";
  }
}

export interface ApiClient {
  get<T>(path: string, params: Record<string, string | number>): Promise<T[]>;
}

interface ApiEnvelope<T> {
  errors?: unknown;
  response?: T[];
}

function hasErrors(errors: unknown): boolean {
  if (Array.isArray(errors)) return errors.length > 0;
  if (errors && typeof errors === "object") return Object.keys(errors).length > 0;
  return false;
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

/**
 * Every call first reserves one unit of the daily budget. Only 429 and 5xx are retried (3 attempts, exponential backoff).
 * API-Football can answer HTTP 200 with an `errors` object (for example when the rate limit is hit), so that is checked too.
 */
export function createApiClient(
  key: string,
  budget: { reserve(n?: number): Promise<boolean> },
  fetchImpl: typeof fetch = fetch,
  backoffMs = 500,
): ApiClient {
  return {
    async get<T>(path: string, params: Record<string, string | number>): Promise<T[]> {
      if (!(await budget.reserve(1))) throw new ApiError("Daily API budget exhausted");
      const search = new URLSearchParams();
      for (const [k, v] of Object.entries(params)) search.set(k, String(v));
      const qs = search.toString();
      const url = `${API_BASE}${path}${qs ? `?${qs}` : ""}`;
      let lastError: Error = new ApiError("No attempt made");
      for (let attempt = 0; attempt < 3; attempt++) {
        try {
          const res = await fetchImpl(url, { headers: { [API_KEY_HEADER]: key } });
          if (res.status === 429 || res.status >= 500) {
            lastError = new ApiError(`HTTP ${res.status}`, res.status);
            await sleep(backoffMs * 2 ** attempt);
            continue;
          }
          if (!res.ok) throw new ApiError(`HTTP ${res.status}`, res.status);
          const body = (await res.json()) as ApiEnvelope<T>;
          if (hasErrors(body.errors)) throw new ApiError(`API error: ${JSON.stringify(body.errors)}`);
          return body.response ?? [];
        } catch (e) {
          if (e instanceof ApiError) throw e;
          lastError = e as Error;
          await sleep(backoffMs * 2 ** attempt);
        }
      }
      throw lastError;
    },
  };
}
