package org.ontodia.server.services.security;

import org.ontodia.server.services.security.interfaces.ComposedMessage;
import org.ontodia.server.services.security.interfaces.MessageProviderForMail;
import org.ontodia.server.services.security.interfaces.UserDetailsWithTokens;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

public class BasicMessageProvider implements MessageProviderForMail {
    @Override
    public ComposedMessage formatMailStructure(UserDetails user, ComposedMessage content) {
        return content;
    }

    @Override
    public ComposedMessage composeConfirmation(UserDetailsWithTokens user, String link) {
        return ComposedMessage.create()
            .setSubject("Sign Up Request")
            .setBody(String.format(
                    "To activate your account click on the following link:\r\n%s",
                    link + user.getConfirmationToken()))
            .compose();
    }

    @Override
    public ComposedMessage composeChangePassword(UserDetailsWithTokens user, String link) {
        return ComposedMessage.create()
            .setSubject("Change Password Request")
            .setBody(String.format(
                    "To change password click on the following link:\r\n%s",
                    link + user.getForgotPasswordToken()))
            .compose();
    }

    @Override
    public ComposedMessage composeInvitation(UserDetailsWithTokens user, String link, String senderEmail) {
        return ComposedMessage.create()
            .setSubject("Invitation Request")
            .setBody(String.format(
                    "User %s send invitation to you. Click on the following link to activate your account:\r\n%s",
                    senderEmail, link + user.getInvitationToken()))
            .compose();
    }
}
