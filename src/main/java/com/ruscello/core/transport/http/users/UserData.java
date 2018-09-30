package com.ruscello.core.transport.http.users;

public class UserData {

    public final String loginName;
    public final String fullName;
    public final String salt;
    public final String hash;
    public final boolean disabled;
    public final String[] groups;

    public UserData(String loginName, String fullName, String[] groups, String hash, String salt, boolean disabled) {
        this.loginName = loginName;
        this.fullName = fullName;
        this.groups = groups;
        this.salt = salt;
        this.hash = hash;
        this.disabled = disabled;
    }

    public String getLoginName() {
        return loginName;
    }

    public UserData setFullName(String fullName) {
        return new UserData(loginName, fullName, groups, hash, salt, disabled);
    }

    public String getFullName() {
        return fullName;
    }

    public UserData setGroups(String[] groups) {
        return new UserData(loginName, fullName, groups, hash, salt, disabled);
    }

    public String[] getGroups() {
        return groups;
    }

    public String getSalt() {
        return salt;
    }

    public String getHash() {
        return hash;
    }

    public UserData setPassword(String hash, String salt) {
        return new UserData(loginName, fullName, groups, hash, salt, disabled);
    }

    public boolean isDisabled() {
        return disabled;
    }

    public UserData setEnabled() {
        return new UserData(loginName, fullName, groups, hash, salt, false);
    }

    public UserData setDisabled() {
        return new UserData(loginName, fullName, groups, hash, salt, true);
    }


}
