package org.ontodia.server.services.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SignUpConfig {
    @Value("${userservice.domain:}")
    public String domain;

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

    @Value("${userservice.activatedLink:/activate/}")
    public String activatedLink;

    @Value("${userservice.recoveryLink:/authenticateByForgotPasswordToken/}")
    public String recoveryLink;

    @Value("${userservice.invitationLink:/authenticateByInvitationToken/}")
    public String invitationLink;
}
