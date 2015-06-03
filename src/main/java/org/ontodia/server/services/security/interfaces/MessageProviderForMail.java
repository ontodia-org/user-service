package org.ontodia.server.services.security.interfaces;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Created by drazdyakonov on 25.05.2015.
 */
public interface MessageProviderForMail {
    String getConfirmationSubject();
    String getConfirmationMessage(UserDetailsWithTokens user, String domain);

    String getChangePasswordSubject();
    String getChangePasswordMessage(UserDetailsWithTokens user, String domain);

    String getInvitationSubject();
    String getInvitationMessage(UserDetailsWithTokens user, String domain, String from);
}
