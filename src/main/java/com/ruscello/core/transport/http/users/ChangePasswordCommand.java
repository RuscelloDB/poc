package com.ruscello.core.transport.http.users;

import com.google.common.base.MoreObjects;

public class ChangePasswordCommand {

    public String user;
    public String oldPassword;
    public String newPassword;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("user", user)
                .toString();
    }
}
