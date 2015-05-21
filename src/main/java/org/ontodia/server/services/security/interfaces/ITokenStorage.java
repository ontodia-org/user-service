package org.ontodia.server.services.security.interfaces;

/**
 * Created by drazdyakonov on 15.05.2015.
 */
public interface ITokenStorage {
    void setConfirmationToken(String token);
    void setInvitationToken(String token);
    void setForgotPasswordToken(String token);

    String getConfirmationToken();
    String getInvitationToken();
    String getForgotPasswordToken();

    boolean isEmailConfirmed();
    void setEmailConfirmed(boolean isEmailConfirmed);
}
