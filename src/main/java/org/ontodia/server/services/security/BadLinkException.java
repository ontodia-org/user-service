package org.ontodia.server.services.security;

public class BadLinkException extends Exception {
    public BadLinkException(String message) {
        super(message);
    }

    public BadLinkException(String message, Throwable cause) {
        super(message, cause);
    }
}
