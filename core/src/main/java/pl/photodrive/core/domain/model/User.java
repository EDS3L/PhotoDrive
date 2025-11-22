package pl.photodrive.core.domain.model;

import lombok.extern.slf4j.Slf4j;
import pl.photodrive.core.domain.event.user.UserCreated;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.application.port.password.PasswordHasher;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.Password;
import pl.photodrive.core.domain.vo.UserId;

import java.util.*;

@Slf4j
public class User {

    private final UserId id;
    private final String name;
    private Email email;
    private Password password;
    private final Set<Role> roles;

    private transient final List<Object> domainEvents = new ArrayList<>();

    public User(UserId id, String name, Email email, Password password, Set<Role> roles) {
        if (name == null) throw new UserException("Name cannot be null");
        if (email == null) throw new UserException("Email cannot be null");
        if (password == null) throw new UserException("Password cannot be null");
        if (roles.isEmpty()) throw new UserException("Roles cannot be null");
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

    public static User create(String name, Email email, Password password, Role role, String rawPassword) {
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = new User(UserId.newId(), name, email, password, roles);

        user.registerEvent(new UserCreated(user.getId().value(), user.getEmail().value(), user.getRoles(), rawPassword));

        return user;
    }

    public void addRole(Role role) {
        if (this.roles.contains(role)) throw new UserException("User already has role: " + role);
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        if (!this.roles.contains(role)) throw new UserException("This set role not contains " + role);
        if (this.roles.size() == 1) throw new UserException("You cannot remove all user role");
        this.roles.remove(role);
    }

    public void changePassword(String currentPassword, String newPassword, PasswordHasher passwordHasher) {
        if (!passwordHasher.matches(currentPassword, this.password.value())) {
            throw new UserException("Incorrect current password");
        }

        if (passwordHasher.matches(newPassword, this.password.value())) {
            throw new UserException("New password cannot be the same as the current password");
        }

        Password password = new Password(newPassword);
        this.password = new Password(passwordHasher.encode(password.value()));
    }

    public void changeEmail(String newEmail) {
        Email email = new Email(newEmail);
        if (this.email.equals(email)) {
            throw new UserException("New email cannot be the same as the current email");
        }
        this.email = email;
    }

    public void verifyPassword(String rawPassword, PasswordHasher passwordHasher) {
        if (!passwordHasher.matches(rawPassword, this.password.value())) {
            throw new UserException("Incorrect password");
        }
    }

    private void registerEvent(Object event) {
        this.domainEvents.add(event);
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public UserId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public Password getPassword() {
        return password;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}
