package pl.photodrive.core.domain.model;

import lombok.extern.slf4j.Slf4j;
import pl.photodrive.core.application.exception.SecurityException;
import pl.photodrive.core.domain.event.user.UserCreated;
import pl.photodrive.core.domain.event.user.UserRemindedPassword;
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
    private boolean changePasswordOnNextLogin;
    private boolean isActive;
    private List<UserId> assignedUsers = new ArrayList<>();


    private transient final List<Object> domainEvents = new ArrayList<>();

    public User(UserId id, String name, Email email, Password password, Set<Role> roles, boolean changePasswordOnNextLogin, boolean isActive, List<UserId> assignedUsers) {
        if (name == null) throw new UserException("Name cannot be null");
        if (email == null) throw new UserException("Email cannot be null");
        if (password == null) throw new UserException("Password cannot be null");
        if (roles.isEmpty()) throw new UserException("Roles cannot be null");
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.changePasswordOnNextLogin = changePasswordOnNextLogin;
        this.isActive = isActive;
        this.assignedUsers = assignedUsers;
    }

    public static User create(String name, Email email, Password password, Role role, String rawPassword) {
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = new User(UserId.newId(), name, email, password, roles, true, true, null);

        user.registerEvent(new UserCreated(user.getId().value(), user.getEmail().value(), user.getRoles(), rawPassword));

        return user;
    }

    public void login() {
        if(!isActive) {
            throw new UserException("User is not active!");
        }

        if(changePasswordOnNextLogin) {
            throw new UserException("You should change your password!");
        }
    }

    public void addRole(Role role) {
        if (this.roles.contains(role)) throw new UserException("User already has role: " + role);
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        if(role.equals(Role.ADMIN)) throw new SecurityException("Cannot remove admin role");
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

    public void changePasswordWithToken(UUID token, String newPassword, PasswordHasher passwordHasher) {
        if(token == null) throw new UserException("Token cannot be null");

        if (passwordHasher.matches(newPassword, this.password.value())) {
            throw new UserException("New password cannot be the same as the current password");
        }

        Password password = new Password(newPassword);
        this.registerEvent(new UserRemindedPassword(this.getEmail().value()));

        this.password = new Password(passwordHasher.encode(password.value()));
    }

    public void changeEmail(String newEmail) {
        Email email = new Email(newEmail);
        if (this.email.equals(email)) {
            throw new UserException("New email cannot be the same as the current email");
        }
        this.email = email;
    }

    public void activeUser(boolean active, User user) {
        hasAccessToSetActive(user);

        if(isActive) {
            throw  new UserException("User is already active");
        }
        this.isActive = active;
    }

    public void assignUsers(List<UserId> assignedUsers) {
        if(!this.roles.contains(Role.PHOTOGRAPHER)) throw  new UserException("Users can only assigned to Photograph");
        this.assignedUsers = assignedUsers;
    }

    public void disconnectUsers(List<UserId> disconnectedUsers) {
        if (disconnectedUsers == null || disconnectedUsers.isEmpty()) {
            return;
        }
        if (this.assignedUsers == null) {
            throw new UserException("Assigned users list is null!");
        }

        List<UserId> usersToVerify = new ArrayList<>(disconnectedUsers);

        usersToVerify.retainAll(this.assignedUsers);

        if (usersToVerify.size() != disconnectedUsers.size()) {
            throw new UserException("Some of the users listed are not assigned to a photographer!");
        }

        this.assignedUsers.removeAll(disconnectedUsers);

    }

    public boolean hasAccessToReadAllAlbums(User loggedUser) {
        return loggedUser.getRoles().contains(Role.ADMIN);

    }

    public boolean hasAccessToReadUserAlbums(User loggedUser) {
        return loggedUser.getRoles().contains(Role.PHOTOGRAPHER);
    }

    public boolean hasAccessToReadAssignedAlbums(User loggedUser) {
        return loggedUser.getRoles().contains(Role.CLIENT);

    }

    public List<UserId> getPhotographUsers(User currentUser) {
        if(!currentUser.getRoles().contains(Role.PHOTOGRAPHER)) throw new UserException("Access denied!");
        if (!currentUser.getId().equals(this.getId())) throw new UserException("Cannot access unassigned customer list!");

        return this.assignedUsers;
    }

    public void detectiveUser(boolean active,User user) {
        hasAccessToSetActive(user);

        if(!isActive) {
            throw  new UserException("User is already detective");
        }
        this.isActive = active;
    }

    public void shouldChangePasswordOnNextLogin() {
        if(changePasswordOnNextLogin) {
            throw  new UserException("You must change the password!");
        }
    }

    public void verifyPassword(String rawPassword, PasswordHasher passwordHasher) {
        if (!passwordHasher.matches(rawPassword, this.password.value())) {
            throw new UserException("Incorrect password");
        }
    }

    public boolean hasAccessToReadAllUsers(User currentUser) {
        boolean isAdmin = currentUser.getRoles().contains(Role.ADMIN);

        if(!isAdmin) {
            throw new UserException("Access denied!");
        } else {
            return true;
        }

    }


    private void hasAccessToSetActive(User user) {
        if(!(user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.PHOTOGRAPHER))) {
            throw new UserException("Access denied!");
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

    public void setChangePasswordOnNextLogin(boolean changePasswordOnNextLogin) {
        this.changePasswordOnNextLogin = changePasswordOnNextLogin;
    }

    public List<UserId> getAssignedUsers() {
        return assignedUsers;
    }

    public boolean isChangePasswordOnNextLogin() {
        return changePasswordOnNextLogin;
    }

    public boolean isActive() {
        return isActive;
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
