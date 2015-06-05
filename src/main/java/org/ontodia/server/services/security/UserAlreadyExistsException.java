package org.ontodia.server.services.security;

/**
 * Created by drazdyakonov on 05.06.2015.
 */
public class UserAlreadyExistsException extends Exception {
    public UserAlreadyExistsException(String e) {
        super(e);
    }
}
