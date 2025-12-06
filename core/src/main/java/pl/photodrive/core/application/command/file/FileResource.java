package pl.photodrive.core.application.command.file;

import org.springframework.core.io.Resource;

public record FileResource(Resource resource, String contentType) {
}
