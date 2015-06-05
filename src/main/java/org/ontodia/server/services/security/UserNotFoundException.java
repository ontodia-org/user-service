package org.ontodia.server.services.security;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Created by drazdyakonov on 05.06.2015.
 */
public class UserNotFoundException extends Exception {
    String message = null;
    Exception exception = null;

    UserNotFoundException(Exception exception){
        this.exception = exception;
    }

    UserNotFoundException(String exception){
        super(exception);
        this.message = exception;
    }

    @Override
    public String toString() {
        return ""+(message!=null?message:"")+(this.exception!=null?this.exception:"");
    }
}
