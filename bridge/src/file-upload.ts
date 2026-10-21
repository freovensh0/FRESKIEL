import { randomUUID } from "node:crypto";
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";

export interface UploadedFile {
    path: string;
    name: string;
    sizeBytes: number;
}

const MAX_NAME_LENGTH = 120;

export function sanitizeUploadName(rawName: string): string {
    const base = path.basename(rawName.trim()).replace(/[^A-Za-z0-9._-]/g, "_");
    return base.length > 0 ? base.slice(0, MAX_NAME_LENGTH) : "upload.bin";
}

export async function storeUploadedFile(options: {
    stateDirectory: string;
    name: string;
    dataBase64: string;
    maxBytes: number;
}): Promise<UploadedFile> {
    const bytes = Buffer.from(options.dataBase64, "base64");

    if (bytes.byteLength === 0) {
        throw new Error("file_empty");
    }

    if (bytes.byteLength > options.maxBytes) {
        throw new Error("file_too_large");
    }

    const name = sanitizeUploadName(options.name);
    const directory = path.join(options.stateDirectory, "uploads");
    await mkdir(directory, { recursive: true });

    const target = path.join(directory, `${randomUUID()}-${name}`);
    await writeFile(target, bytes);

    return { path: target, name, sizeBytes: bytes.byteLength };
}
