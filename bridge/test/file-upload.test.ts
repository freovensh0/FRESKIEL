import { mkdtemp, readFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";

import { describe, expect, it } from "vitest";

import { sanitizeUploadName, storeUploadedFile } from "../src/file-upload.js";

describe("file upload", () => {
    it("keeps only the safe part of a name", () => {
        expect(sanitizeUploadName("../../etc/passwd")).toBe("passwd");
        expect(sanitizeUploadName("report 2026.pdf")).toBe("report_2026.pdf");
        expect(sanitizeUploadName("   ")).toBe("upload.bin");
    });

    it("stores the bytes under the state directory and reports the path", async () => {
        const stateDirectory = await mkdtemp(path.join(os.tmpdir(), "freskiel-upload-"));
        const data = Buffer.from("hello bridge", "utf8").toString("base64");

        const uploaded = await storeUploadedFile({
            stateDirectory,
            name: "notes.txt",
            dataBase64: data,
            maxBytes: 1024,
        });

        expect(uploaded.name).toBe("notes.txt");
        expect(uploaded.sizeBytes).toBe(12);
        expect(uploaded.path.startsWith(path.join(stateDirectory, "uploads"))).toBe(true);
        expect(await readFile(uploaded.path, "utf8")).toBe("hello bridge");
    });

    it("refuses empty and oversized files", async () => {
        const stateDirectory = await mkdtemp(path.join(os.tmpdir(), "freskiel-upload-"));

        await expect(
            storeUploadedFile({ stateDirectory, name: "a.txt", dataBase64: "", maxBytes: 1024 }),
        ).rejects.toThrow("file_empty");

        await expect(
            storeUploadedFile({
                stateDirectory,
                name: "a.txt",
                dataBase64: Buffer.from("0123456789", "utf8").toString("base64"),
                maxBytes: 4,
            }),
        ).rejects.toThrow("file_too_large");
    });
});
