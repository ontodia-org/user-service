package org.ontodia.server.services.security.interfaces;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public interface MessageProviderForMail {
    ComposedMessage formatMailStructure(UserDetails user, ComposedMessage content);

    ComposedMessage composeConfirmation(UserDetailsWithTokens user, String link);
    ComposedMessage composeChangePassword(UserDetailsWithTokens user, String link);
    ComposedMessage composeInvitation(UserDetailsWithTokens user, String link, String senderEmail);
}
