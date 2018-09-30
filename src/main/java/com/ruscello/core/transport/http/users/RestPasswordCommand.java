package com.ruscello.core.transport.http.users;

import com.google.common.base.MoreObjects;

public class RestPasswordCommand {

    private String login;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("login", login)
                .toString();
    }
}
