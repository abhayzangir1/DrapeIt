export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly details?: unknown,
    readonly headers?: HeadersInit
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export function errorResponse(error: unknown, requestId: string): Response {
  const apiError =
    error instanceof ApiError
      ? error
      : new ApiError(500, "internal_error", "The request could not be completed.");

  const body: Record<string, unknown> = {
    error: {
      code: apiError.code,
      message: apiError.message,
      requestId,
      ...(apiError.details === undefined ? {} : { details: apiError.details })
    }
  };

  const headers = new Headers(apiError.headers);
  headers.set("content-type", "application/json; charset=utf-8");
  return new Response(JSON.stringify(body), { status: apiError.status, headers });
}
