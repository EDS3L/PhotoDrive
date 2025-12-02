package pl.photodrive.core.domain.model;

import pl.photodrive.core.domain.exception.FileException;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;

import java.time.Instant;

public class File {

    private final FileId fileId;
    private final long sizeBytes;
    private final String contentType;
    private final Instant uploadedAt;
    private FileName fileName;
    boolean isVisible;
    boolean hasWatermark;


    public File(FileId fileId, FileName fileName, long sizeBytes, String contentType, Instant uploadedAt, boolean isVisible,boolean hasWatermark) {
        if (sizeBytes <= 0) throw new FileException("Size cannot be null!");
        if (contentType == null) throw new FileException("Content type cannot be null!");
        if (uploadedAt == null || uploadedAt.isAfter(Instant.now()))
            throw new FileException("Uploaded at is either empty or before today");
        this.fileId = fileId;
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
        this.uploadedAt = uploadedAt;
        this.isVisible = isVisible;
        this.hasWatermark = hasWatermark;
    }

    public static File create(FileName fileName, long sizeBytes, String contentType ) {
        if (sizeBytes <= 0) throw new FileException("Size cannot be negative");
        return new File(new FileId(FileId.newId()), fileName, sizeBytes, contentType, Instant.now(),  false, false);
    }


    public void rename(FileName newFileName) {
        if (newFileName == null) throw new FileException("New file name cannot be null");
        this.fileName = newFileName;
    }

    public void setViable() {
        if(this.isVisible) throw  new FileException("File is already viable");
        this.isVisible = true;
    }

    public void setUnviable() {
        if(!this.isVisible) throw  new FileException("File is already unviable");
        this.isVisible = false;
    }

    public void setWaterMark() {
        if(this.hasWatermark) throw new FileException("File is already watermarked");
        this.hasWatermark = true;
    }

    public void disableWatermark() {
        if(!this.hasWatermark) throw new FileException("File is not watermarked");
        this.hasWatermark = false;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public boolean isHasWatermark() {
        return hasWatermark;
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
