package org.ontodia.server.services.security;

import org.ontodia.server.services.security.interfaces.IUserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

public abstract class SignUpMVCControllerBase<UserDTO extends IUserDTO> {

    private final SignUpController<? super UserDTO> signUpController;
    private final String mailFromAddress;
    private final SignUpConfig config;

    @Autowired
    public SignUpMVCControllerBase(
            SignUpController<? super UserDTO> signUpController,
            @Value("${mailsender.from}") String mailFromAddress,
            SignUpConfig config) {
        this.mailFromAddress = mailFromAddress;
        this.signUpController = signUpController;
        this.config = config;
    }

    // Registry
    //==================================================================
    //==========================================================

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public ModelAndView register() {
        ModelAndView model = new ModelAndView(config.registerView);
        return model;
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ModelAndView register(UserDTO user) throws Exception {
        ModelAndView model = new ModelAndView(config.registerView);

        try{
            signUpController.register(user);
            model.addObject("emailConfirmation", true);
            model.addObject("fromEmailAddress", mailFromAddress);
        }catch (Exception e){
            e.printStackTrace();
            model.addObject("emailAlreadyInUse", true);
            model.addObject("user", user);
        }

        return model;
    }

    @RequestMapping(value = "/activate/{token}", method = RequestMethod.GET)
    public String activate(@PathVariable String token, HttpServletRequest request) throws Exception {
        signUpController.activate(token,request);
        return config.activatedRedirect;
    }


    // Recovery password
    //==================================================================
    //==========================================================

    @RequestMapping(value = "/forgotPassword", method = RequestMethod.GET)
    public ModelAndView forgotPassword() {
        ModelAndView model = new ModelAndView(config.restorePwdView);
        return model;
    }

    @RequestMapping(value = "/forgotPassword", method = RequestMethod.POST)
    public ModelAndView forgotPassword(@RequestParam("email") String email) throws Exception {
        ModelAndView model = new ModelAndView(config.restorePwdView);

        try {
            signUpController.forgotPassword(email);
            model.addObject("recoverPassword", true);
            model.addObject("fromEmailAddress", mailFromAddress);
        }catch (Exception e){
            e.printStackTrace();
            model.addObject("userNotFound", true);
            model.addObject("email", email);
        }

        return model;
    }

    @RequestMapping(value = "/authenticateByForgotPasswordToken/{token}", method = RequestMethod.GET)
    public String authenticateByForgotPasswordToken(@PathVariable String token, HttpServletRequest request) throws Exception {
        signUpController.authenticateByForgotPasswordToken(token,request);
        return config.setPwdRedirect;
    }

    // Send invitation
    //==================================================================
    //==========================================================

    @RequestMapping(value = "/authenticateByInvitationToken/{token}", method = RequestMethod.GET)
    public String authenticateByInvitationToken(@PathVariable String token, HttpServletRequest request) throws Exception {
        signUpController.authenticateByInvitationToken(token,request);
        return config.setPwdRedirect;
    }

    @RequestMapping(value = "/setPassword", method = RequestMethod.GET)
    public ModelAndView setPassword() {
        ModelAndView model = new ModelAndView(config.setPwdView);
        return model;
    }

    @RequestMapping(value = "/setPassword", method = RequestMethod.POST)
    public ModelAndView setPassword(@RequestParam("newPassword") String password) throws Exception {
        try{
            signUpController.setPassword(password);
            return new ModelAndView("dashboard");
        }catch (Exception e){
            e.printStackTrace();
            return new ModelAndView("register");
        }

    }
}
