package org.ontodia.server.services.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by yuricus on 19.05.15.
 */
@Component
public class SignUpMVCControllerConfig {
    @Value("${userservice.view.register:user/register}")
    public String registerView;

    @Value("${userservice.redirect.activated:redirect:/dashboard}")
    public String activatedRedirect;

    @Value("${userservice.redirect.setPwd:redirect:/setPassword}")
    public String setPwdRedirect;

    @Value("${userservice.view.setPwd:user/setPassword}")
    public String setPwdView;

    @Value("${userservice.view.restorePwd:user/forgotPassword}")
    public String restorePwdView;
}
