package com.ruscello.core;

import org.springframework.security.core.userdetails.User;

// http://www.baeldung.com/spring-security-run-as-auth
// EventStore has a similar "run as" mechanism
public class SystemUser { //extends User {  // implements UserDetails {

//    public static readonly SystemAccount Principal = new SystemAccount();
//
//    public IIdentity Identity { get { return _identity; } }
//
//    private readonly IIdentity _identity = new SystemAccountIdentity();
//
//    private SystemAccount()
//    {
//    }
//
//    public bool IsInRole(string role)
//    {
//        return true;
//    }
//
//    private class SystemAccountIdentity: IIdentity
//    {
//        public string Name { get { return "system"; } }
//        public string AuthenticationType { get { return "system"; } }
//        public bool IsAuthenticated { get { return true; } }
//    }

}
