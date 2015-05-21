package org.ontodia.server.services.security.interfaces;

/**
 * Created by drazdyakonov on 18.05.2015.
 */
public interface IUserSearcherByToken {
    UserDetailsWithTokens findByConfirmationToken(String token);
    UserDetailsWithTokens findByInvitationToken(String token);
    UserDetailsWithTokens findByForgotPasswordToken(String token);
}
