package org.ontodia.server.services.security.interfaces;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Created by drazdyakonov on 18.05.2015.
 */
public interface UserDetailsWithTokens extends UserDetails,ITokenStorage {
}
