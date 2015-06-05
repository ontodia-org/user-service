package org.ontodia.server.services.security.interfaces;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Created by drazdyakonov on 25.05.2015.
 */
public interface MessageProviderForMail {
    String getConfirmationSubject();
    String getConfirmationMessage(UserDetailsWithTokens user, String link);

    String getChangePasswordSubject();
    String getChangePasswordMessage(UserDetailsWithTokens user, String link);

    String getInvitationSubject();
    String getInvitationMessage(UserDetailsWithTokens user, String link, String from);
}
