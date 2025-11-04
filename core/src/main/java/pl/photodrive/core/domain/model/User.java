package pl.photodrive.core.domain.model;


import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.vo.Email;

import java.util.UUID;

public class User {

    private final UUID id;
    private final String name;
    private Email email;
    private final String password;


    public User(UUID id, String name, Email email, String password) {
        if (id == null) throw new UserException("Id cannot be null");
        if (name == null) throw new UserException("Name cannot be null");
        if (email == null) throw new UserException("Email cannot be null");
        if (password == null) throw new UserException("Password cannot be null");
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public static User createNew(String name, Email email, String password) {
        return new User(UUID.randomUUID(), name, email, password);
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

    public String getPassword() {
        return password;
    }
}
