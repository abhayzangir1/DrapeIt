import { z, type ZodType } from "zod";
import { MAX_IMAGE_BYTES, SCARF_STYLES } from "./config";
import { ApiError } from "./errors";

const safeFileName = /^[A-Za-z0-9][A-Za-z0-9._-]{0,127}\.(?:jpe?g|png)$/i;
const fileId = z
  .string()
  .min(8)
  .max(512)
  .regex(/^[A-Za-z0-9_+\-/=]+$/, "Invalid file ID");
const taskId = z.string().min(16).max(512).regex(/^[A-Za-z0-9_-]+$/, "Invalid task ID");
const operationId = z
  .string()
  .regex(
    /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
    "Operation ID must be a UUID v4"
  )
  .transform((value) => value.toLowerCase());
const templateId = z.string().min(1).max(256).regex(/^[A-Za-z0-9_.-]+$/, "Invalid template ID");

export const sessionSchema = z
  .object({ accessCode: z.string().min(1).max(512) })
  .strict();

const uploadFileSchema = z
  .object({
    contentType: z.enum(["image/jpeg", "image/jpg", "image/png"]),
    fileName: z.string().min(5).max(132).regex(safeFileName, "Invalid file name"),
    fileSize: z.number().int().positive().max(MAX_IMAGE_BYTES)
  })
  .strict()
  .superRefine((file, context) => {
    const extension = file.fileName.split(".").pop()?.toLowerCase();
    const isJpeg = file.contentType === "image/jpeg" || file.contentType === "image/jpg";
    if ((isJpeg && extension !== "jpg" && extension !== "jpeg") || (!isJpeg && extension !== "png")) {
      context.addIssue({ code: "custom", message: "File extension does not match content type" });
    }
  });

export const uploadSchema = z
  .object({
    feature: z.enum(["facial-colors", "try-on"]),
    files: z.array(uploadFileSchema).min(1).max(2)
  })
  .strict()
  .superRefine((body, context) => {
    if (body.feature === "facial-colors") {
      if (body.files.length !== 1) {
        context.addIssue({ code: "custom", message: "Facial color upload requires exactly one file" });
      }
      if (body.files.some((file) => file.contentType === "image/png")) {
        context.addIssue({ code: "custom", message: "Facial color analysis accepts JPEG files only" });
      }
    }
  });

export const facialTaskSchema = z
  .object({
    operationId,
    sourceFileId: fileId,
    faceAngleStrictness: z.enum(["strict", "high", "medium", "low", "flexible"]).default("high")
  })
  .strict();

export const clothesVtoTaskSchema = z
  .object({
    operationId,
    sourceFileId: fileId,
    referenceFileId: fileId.optional(),
    templateId: templateId.optional(),
    garmentCategory: z.enum(["auto", "upper_body", "lower_body", "full_body", "shoes"]).default("auto")
  })
  .strict()
  .superRefine((body, context) => {
    if (Boolean(body.referenceFileId) === Boolean(body.templateId)) {
      context.addIssue({
        code: "custom",
        message: "Provide exactly one of referenceFileId or templateId"
      });
    }
  });

export const scarfVtoTaskSchema = z
  .object({
    operationId,
    sourceFileId: fileId.optional(),
    referenceFileId: fileId.optional(),
    sourceImageUrl: z.string().url().max(2_048).optional(),
    referenceImageUrl: z.string().url().max(2_048).optional(),
    gender: z.enum(["female", "male"]).optional(),
    style: z.enum(SCARF_STYLES).default("style_modern_chic")
  })
  .strict()
  .superRefine((body, context) => {
    const hasFilePair = Boolean(body.sourceFileId && body.referenceFileId);
    const hasUrlPair = Boolean(body.sourceImageUrl && body.referenceImageUrl);
    const hasPartialFilePair = Boolean(body.sourceFileId) !== Boolean(body.referenceFileId);
    const hasPartialUrlPair = Boolean(body.sourceImageUrl) !== Boolean(body.referenceImageUrl);
    if (hasPartialFilePair || hasPartialUrlPair || hasFilePair === hasUrlPair) {
      context.addIssue({
        code: "custom",
        message: "Provide exactly one complete source/reference file pair or URL pair"
      });
    }
  });

export function validateAllowedImageUrl(value: string, allowedHosts: ReadonlySet<string>): string {
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    throw new ApiError(400, "invalid_image_url", "The direct image URL is invalid.");
  }
  const host = url.hostname.toLowerCase();
  const allowed = Array.from(allowedHosts).some((entry) =>
    entry.startsWith("*.")
      ? host.endsWith(entry.slice(1)) && host !== entry.slice(2)
      : host === entry
  );
  if (
    url.protocol !== "https:" ||
    url.username ||
    url.password ||
    (url.port && url.port !== "443") ||
    !allowed ||
    host === "localhost" ||
    host.endsWith(".local") ||
    /^\d{1,3}(?:\.\d{1,3}){3}$/.test(host) ||
    host.includes(":")
  ) {
    throw new ApiError(
      400,
      "image_url_not_allowed",
      "Direct image URLs must use an explicitly allowed public HTTPS host."
    );
  }
  return url.toString();
}

export function parseWith<T>(schema: ZodType<T>, input: unknown): T {
  const result = schema.safeParse(input);
  if (!result.success) {
    throw new ApiError(
      400,
      "validation_error",
      "The request body is invalid.",
      result.error.issues.map((issue) => ({
        path: issue.path.join("."),
        message: issue.message
      }))
    );
  }
  return result.data;
}

export function validateTaskId(value: string): string {
  const result = taskId.safeParse(value);
  if (!result.success) throw new ApiError(400, "invalid_task_id", "The task ID is invalid.");
  return result.data;
}
