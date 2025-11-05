package pl.photodrive.core.domain.model;

import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.Password;
import pl.photodrive.core.infrastructure.security.BCryptPasswordEncoderAdapter;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class User {

    private final UUID id;
    private final String name;
    private Email email;
    private Password password;
    private final Set<Role> roles;


    public User(UUID id, String name, Email email, Password password, Set<Role> roles) {
        if (id == null) throw new UserException("Id cannot be null");
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

    public static User createNew(String name, Email email, Password password, Set<Role> roles) {
        return new User(UUID.randomUUID(), name, email, password, roles);
    }

    public void addRole(Role role) {
        if(this.roles.contains(role)) throw new UserException("User already has role: " + role);
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        if(!this.roles.contains(role)) throw new UserException("This set role not contains " + role);
        if(this.roles.size() == 1) throw new UserException("You cannot remove all user role");
        this.roles.remove(role);
    }

    public void changePassword(String currentPassword, String newPassword, BCryptPasswordEncoderAdapter passwordEncoder) {
        if (!passwordEncoder.matches(currentPassword, this.password.value())) {
            throw new UserException("Incorrect current password");
        }

        if (passwordEncoder.matches(newPassword, this.password.value())) {
            throw new UserException("New password cannot be the same as the current password");
        }

        Password password = new Password(newPassword);
        this.password = new Password(passwordEncoder.encode(password));
    }

    public void changeEmail(String newEmail) {
        Email email = new Email(newEmail);
        if (this.email.equals(email)) {
            throw new UserException("New email cannot be the same as the current email");
        }
        this.email = email;
    }


    public UUID getId() {
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
