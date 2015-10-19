package org.ontodia.server.services.security;

import org.ontodia.server.services.security.interfaces.MessageProviderForMail;
import org.ontodia.server.services.security.interfaces.UserDetailsWithTokens;
import org.springframework.stereotype.Component;

@Component
public class MessageProviderForMVCControllerBase implements MessageProviderForMail {
    @Override
    public String getConfirmationSubject() {
        return "Sign Up Request";
    }

    @Override
    public String getConfirmationMessage(UserDetailsWithTokens user, String link) {
        return "To activate your account click on the following link:\r\n"+ link +user.getConfirmationToken()+"\r\n\r\n";
    }

    @Override
    public String getChangePasswordSubject() {
        return "Change Password Request";
    }

    @Override
    public String getChangePasswordMessage(UserDetailsWithTokens user, String link) {
        return "To change password click on the following link:\r\n"+ link +user.getForgotPasswordToken()+"\r\n\r\n";
    }

    @Override
    public String getInvitationSubject() {
        return "Invitation Request";
    }

    @Override
    public String getInvitationMessage(UserDetailsWithTokens user, String link, String senderEmail) {
        return String.format(
                "User %s send invitation to you. Click on the following link:\r\n%s\r\n\r\n",
                senderEmail, link+user.getInvitationToken());
    }
}
