package pl.photodrive.core.application.command.user;


import java.util.List;
import java.util.UUID;

public record AssignUserCommand(List<UUID> assignedUser, UUID userId) {
}
