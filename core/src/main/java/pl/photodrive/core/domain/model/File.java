package pl.photodrive.core.domain.model;

import pl.photodrive.core.domain.exception.FileException;
import pl.photodrive.core.domain.port.FileUniquenessChecker;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.domain.vo.FileId;

import java.time.Instant;

public class File {

    private final FileId fileId;
    private FileName fileName;
    private final long sizeBytes;
    private final String contentType;
    private final Instant uploadedAt;

    public File(FileId fileId, FileName fileName, long sizeBytes, String contentType, Instant uploadedAt) {
        if (sizeBytes <= 0) throw new FileException("Size cannot be null!");
        if (contentType == null) throw new FileException("Content type cannot be null!");
        if (uploadedAt == null || uploadedAt.isAfter(Instant.now()))
            throw new FileException("Uploaded at is either empty or before today");
        this.fileId = fileId;
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
        this.uploadedAt = uploadedAt;
    }

    public static File create(FileName fileName, long sizeBytes, String contentType, FileUniquenessChecker fileUniquenessChecker) {
        if (sizeBytes < 0) throw new FileException("Size cannot be negative");

        FileName uniqueName = ensureUnique(fileName, fileUniquenessChecker);
        return new File(new FileId(FileId.newId()), uniqueName, sizeBytes, contentType, Instant.now());
    }

    private static FileName ensureUnique(FileName name, FileUniquenessChecker checker) {
        if (!checker.isFileNameTaken(name)) return name;

        String full = name.value();
        int dot = full.lastIndexOf('.');

        String base = (dot > 0) ? full.substring(0, dot) : full;
        String ext = (dot > 0) ? full.substring(dot) : "";

        int i = 1;
        FileName candidate;
        do {
            candidate = new FileName(base + " (" + i++ + ")" + ext);
        } while (checker.isFileNameTaken(candidate));

        return candidate;
    }

    public void rename(FileName newFileName) {
        if (newFileName == null) throw new FileException("New file name cannot be null");
        this.fileName = newFileName;
    }

    public FileId getFileId() {
        return fileId;
    }

    public FileName getFileName() {
        return fileName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
