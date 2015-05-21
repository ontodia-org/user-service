package org.ontodia.server.services.security.interfaces;

import org.springframework.security.provisioning.UserDetailsManager;

/**
 * Created by drazdyakonov on 15.05.2015.
 */
public interface IUserDetailManager<DTOClass> extends UserDetailsManager {
    UserDetailsWithTokens createUserFromDTO(DTOClass user);
    UserDetailsWithTokens createUserFromEmail(String email);
    void updateUserFromDTO(DTOClass user);
}
