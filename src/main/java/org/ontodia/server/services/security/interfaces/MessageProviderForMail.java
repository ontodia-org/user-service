package org.ontodia.server.services.security.interfaces;

public interface MessageProviderForMail {
    String getConfirmationSubject();
    String getConfirmationMessage(UserDetailsWithTokens user, String link);

    String getChangePasswordSubject();
    String getChangePasswordMessage(UserDetailsWithTokens user, String link);

    String getInvitationSubject();
    String getInvitationMessage(UserDetailsWithTokens user, String link, String senderEmail);
}
