import { describe, expect, it } from "vitest";
import { ApiError } from "../src/errors";
import type { Env, Fetcher } from "../src/types";
import { normalizeTaskResult, safeMediaUrl, YouCamClient } from "../src/youcam";

describe("official feature-cost lookup", () => {
  const env: Env = { YOUCAM_API_KEY: "test-api-key", VTO_PROVIDER: "scarf" };

  it("matches the local task path and returns the live amount", async () => {
    const fetcher: Fetcher = async (input) => {
      expect(String(input)).toContain("/s2s/v2.0/credit/feature-cost?page_size=20");
      return new Response(
        JSON.stringify({
          status: 200,
          result: {
            next_token: null,
            skus: [
              {
                description: "Scarf",
                amount: 2,
                unit: "image",
                proc_unit: 1,
                run_task_url: "/s2s/v2.0/task/scarf"
              }
            ]
          }
        }),
        { headers: { "content-type": "application/json" } }
      );
    };
    await expect(new YouCamClient(env, fetcher).featureCost("try-on")).resolves.toBe(2);
  });

  it("invokes fetch with the global receiver required by the Workers runtime", async () => {
    const fetcher: Fetcher = async function (this: unknown) {
      expect(this).toBe(globalThis);
      return new Response(
        JSON.stringify({
          status: 200,
          result: {
            next_token: null,
            skus: [{ amount: 2, run_task_url: "/s2s/v2.0/task/scarf" }]
          }
        })
      );
    };

    await expect(new YouCamClient(env, fetcher).featureCost("try-on")).resolves.toBe(2);
  });

  it("paginates using starting_token until the matching SKU is found", async () => {
    let call = 0;
    const fetcher: Fetcher = async (input) => {
      call += 1;
      if (call === 1) {
        return new Response(
          JSON.stringify({
            status: 200,
            result: { next_token: "next-20", skus: [{ amount: 9, run_task_url: "/other" }] }
          })
        );
      }
      expect(String(input)).toContain("starting_token=next-20");
      return new Response(
        JSON.stringify({
          status: 200,
          result: {
            next_token: null,
            skus: [{ amount: 3, run_task_url: "https://yce-api-01.makeupar.com/s2s/v2.0/task/scarf" }]
          }
        })
      );
    };
    await expect(new YouCamClient(env, fetcher).featureCost("try-on")).resolves.toBe(3);
    expect(call).toBe(2);
  });

  it("fails closed for missing or non-integral costs", async () => {
    const fetcher: Fetcher = async () =>
      new Response(
        JSON.stringify({
          status: 200,
          result: {
            next_token: null,
            skus: [{ amount: 2.5, run_task_url: "/s2s/v2.0/task/scarf" }]
          }
        })
      );
    await expect(new YouCamClient(env, fetcher).featureCost("try-on")).rejects.toMatchObject({
      code: "feature_cost_unavailable"
    });
  });
});

describe("result normalization", () => {
  it("bounds upstream error messages and codes", () => {
    const normalized = normalizeTaskResult("facial-colors", "task_1234567890abcdef", {
      taskStatus: "error",
      error: "error_face_position_invalid",
      errorMessage: "Face is not centered."
    });
    expect(normalized).toEqual({
      taskId: "task_1234567890abcdef",
      feature: "facial-colors",
      status: "error",
      error: { code: "error_face_position_invalid", message: "Face is not centered." }
    });
  });

  it("requires skin_color in a successful facial response", () => {
    expect(() =>
      normalizeTaskResult("facial-colors", "task_1234567890abcdef", {
        taskStatus: "success",
        results: { color: { eye_color: "#123456" } }
      })
    ).toThrowError(ApiError);
  });

  it("drops invalid hex fields but retains bounded color names", () => {
    const normalized = normalizeTaskResult("facial-colors", "task_1234567890abcdef", {
      taskStatus: "success",
      results: {
        color: {
          skin_color: "#abcdef",
          eye_color: "javascript:alert(1)",
          eye_color_name: "Other"
        }
      }
    });
    expect(normalized).toMatchObject({
      result: { colors: { skin_color: "#abcdef", eye_color_name: "Other" } }
    });
    expect(JSON.stringify(normalized)).not.toContain("javascript");
  });
});

describe("trusted media URLs", () => {
  it("accepts approved HTTPS subdomains", () => {
    expect(safeMediaUrl("https://cdn.makeupar.com/result.jpg")).toBe("https://cdn.makeupar.com/result.jpg");
    expect(safeMediaUrl("https://bucket.s3.amazonaws.com/result.jpg?x=1")).toContain("amazonaws.com");
  });

  it("rejects HTTP, embedded credentials, and suffix-confusion hosts", () => {
    expect(() => safeMediaUrl("http://cdn.makeupar.com/result.jpg")).toThrowError(ApiError);
    expect(() => safeMediaUrl("https://user:pass@cdn.makeupar.com/result.jpg")).toThrowError(ApiError);
    expect(() => safeMediaUrl("https://makeupar.com.evil.example/result.jpg")).toThrowError(ApiError);
  });
});
