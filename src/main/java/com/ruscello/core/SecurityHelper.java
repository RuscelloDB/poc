package com.ruscello.core;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

// TODO: really would like to have some of these methods on the user object instead of this helper
public class SecurityHelper {

    private SecurityHelper() {
        // statics only
    }

    public static boolean hasAuthority(UserDetails user, String authority) {
        if (user == null) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        if (authorities == null) {
            return false;
        }

        for (GrantedAuthority grantedAuthority : authorities) {
            if (authority.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAdmin(UserDetails user) {
        return user != null && hasAuthority(user, ""); //SystemRoles.Admins);
    }

}
