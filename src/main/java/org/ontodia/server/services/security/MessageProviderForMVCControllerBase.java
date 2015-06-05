package org.ontodia.server.services.security;

import org.ontodia.server.services.security.interfaces.MessageProviderForMail;
import org.ontodia.server.services.security.interfaces.UserDetailsWithTokens;
import org.springframework.stereotype.Component;

/**
 * Created by drazdyakonov on 03.06.2015.
 */
@Component
public class MessageProviderForMVCControllerBase implements MessageProviderForMail {
    @Override
    public String getConfirmationSubject() {
        return new String("SignUp request");
    }

    @Override
    public String getConfirmationMessage(UserDetailsWithTokens user, String link) {
        return "To activate your account click on the following link:\r\n"+ link +user.getConfirmationToken()+"\r\n\r\n";
    }

    @Override
    public String getChangePasswordSubject() {
        return new String("Change password Request");
    }

    @Override
    public String getChangePasswordMessage(UserDetailsWithTokens user, String link) {
        return "To change password click on the following link:\r\n"+ link +user.getForgotPasswordToken()+"\r\n\r\n";
    }

    @Override
    public String getInvitationSubject() {
        return new String("Invitation Request");
    }

    @Override
    public String getInvitationMessage(UserDetailsWithTokens user, String link, String from) {
        return "User " + from + " send invitation to you. Click on the following link:\r\n"+link+user.getInvitationToken()+"\r\n\r\n";
    }
}
