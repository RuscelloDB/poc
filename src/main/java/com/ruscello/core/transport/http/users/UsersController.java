package com.ruscello.core.transport.http.users;

import org.springframework.web.bind.annotation.*;

// TODO: do we want to consider making auth pluggable?
// Default would be internal auth but could allow for ldap/auth0/etc?
@RestController
public class UsersController {

    public UsersController() {

    }

    @GetMapping("/users")
    public void getUsers() {

    }

    @PutMapping("/users/{login}")
    public void putUser() {

    }

    @PostMapping("/users/{login}/enable")
    public void enable() {

    }

    @PostMapping("/users/{login}/disable")
    public void disable() {

    }

    @DeleteMapping("/users/{login}")
    public void delete() {

    }

    @PostMapping("/users/{login}/change-password")
    public void changePassword() {

    }

    @PostMapping("/users/{login}/reset-password")
    public void resetPassword() {

    }
}
