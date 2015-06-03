package org.ontodia.server.services.security;

import org.ontodia.server.services.security.interfaces.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * To use this as rest controller, just extend @RestController @RequestMapping SignUpRest extends SignUpController<DTOType> and you're done!
 * @param <DTOUserType>
 */

public class SignUpController<DTOUserType extends IUserDTO> {

    private IUserDetailManager userRepository;
    private PasswordEncoder passwordEncoder;
    private UserDetailsService userDetailsService;
    private IUserSearcherByToken userSearcherByToken;

    private MailSender mailSender;
    private String mailFromAddress;
    private String mailDomain;
    private MessageProviderForMail messageProvider;

    public SignUpController(
            IUserDetailManager userRepository,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService,
            MailSender mailSender,
            String mailFromAddress,
            String mailDomain,
            MessageProviderForMail messageProvider,
            IUserSearcherByToken userSearcherByToken) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.mailSender = mailSender;
        this.mailFromAddress = mailFromAddress;
        this.mailDomain = mailDomain;
        this.messageProvider = messageProvider;
        this.userSearcherByToken = userSearcherByToken;
    }

    // Registry
    //==================================================================
    //==========================================================

    public boolean register(DTOUserType user) throws Exception {

        if (userRepository.userExists(user.getUsername())) {
            return false;
        } else {
            UserDetailsWithTokens newUser = userRepository.createUserFromDTO(user);
            createConfirmationToken(newUser);
            userRepository.createUser(newUser);
            sendConfirmationEmailTo(newUser);

            this.authenticateUser(newUser);
            return true;
        }
    }

    private void createConfirmationToken(ITokenStorage user) {
        if (user.getConfirmationToken() == null) {
            user.setConfirmationToken(UUID.randomUUID().toString());
        }
    }

    private void sendConfirmationEmailTo(UserDetailsWithTokens user) throws Exception {

        String subj = this.messageProvider.getConfirmationSubject();
        String msg = this.messageProvider.getConfirmationMessage(user, mailDomain);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFromAddress);
        message.setTo(user.getUsername());
        message.setSubject(subj);
        message.setText(msg);

        mailSender.send(message);
    }

    // Recovery password
    //==================================================================
    //==========================================================

    public boolean forgotPassword(String email) throws Exception {
        if (!userRepository.userExists(email)) {
            return false;
        } else {
            UserDetails existingUser = userRepository.loadUserByUsername(email);
            createForgotPasswordToken((ITokenStorage)existingUser);
            userRepository.updateUser(existingUser);
            sendRequestForChangePassword((UserDetailsWithTokens) existingUser);
            return true;
        }
    }

    private void createForgotPasswordToken(ITokenStorage user) {
        if (user.getForgotPasswordToken() == null) {
            user.setForgotPasswordToken(UUID.randomUUID().toString());
        }
    }

    private void sendRequestForChangePassword(UserDetailsWithTokens user) throws Exception {
        String subj = this.messageProvider.getChangePasswordSubject();
        String msg = this.messageProvider.getChangePasswordMessage(user, mailDomain);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFromAddress);
        message.setTo(user.getUsername());
        message.setSubject(subj);
        message.setText(msg);

        mailSender.send(message);
        user.setEmailConfirmed(false);
        userRepository.updateUser(user);
    }

    // Invitation
    //==================================================================
    //==========================================================

    public boolean inviteUser(String email) throws Exception {
        User user = getUser();
        if(user==null) return false;

        UserDetailsWithTokens userDetails = userRepository.createUserFromEmail(email);
        createInvitationToken(userDetails);
        userRepository.createUser(userDetails);
        sendInvitationTo(userDetails, user.getUsername());
        return true;
    }

    private void createInvitationToken(ITokenStorage user) {
        if (user.getInvitationToken() == null) {
            user.setInvitationToken(UUID.randomUUID().toString());
        }
    }

    private void sendInvitationTo(UserDetailsWithTokens user, String sender) throws Exception {


        String subj = this.messageProvider.getInvitationSubject();
        String msg = this.messageProvider.getInvitationMessage(user, mailDomain, sender);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFromAddress);
        message.setTo(user.getUsername());
        message.setSubject(subj);
        message.setText(msg);

        mailSender.send(message);
        userRepository.updateUser(user);
    }


    public boolean setPassword(String password) throws Exception {
        User user = getUser();

        if(user!=null) {
            UserDetailsWithTokens existingUser = (UserDetailsWithTokens)user;
            if(!existingUser.isEmailConfirmed()) {
                existingUser.setEmailConfirmed(true);
                userRepository.updateUser(existingUser);
                userRepository.changePassword(existingUser.getPassword(), passwordEncoder.encode(password));
            }
            return true;
        }else {
            return false;
        }
    }

    // Authentication
    //==================================================================
    //==========================================================

    public boolean activate(String token, HttpServletRequest request) {
        UserDetailsWithTokens userDetails = userSearcherByToken.findByConfirmationToken(token);
        if (userDetails != null && !userDetails.isEmailConfirmed()) {
            userDetails.setEmailConfirmed(true);
            userRepository.updateUser(userDetails);
            authenticateUser(userDetails, request);
            return true;
        }else{
            return false;
        }
    }

    public boolean authenticateByForgotPasswordToken(String token, HttpServletRequest request) {
        UserDetailsWithTokens userInfo = userSearcherByToken.findByForgotPasswordToken(token);
        if (userInfo != null && !userInfo.isEmailConfirmed()) {
            authenticateUser(userInfo, request);
            return true;
        }else{
            return false;
        }
    }

    public void authenticateByInvitationToken(String token, HttpServletRequest request) {
        UserDetailsWithTokens userDetails = userSearcherByToken.findByInvitationToken(token);
        if (userDetails != null && !userDetails.isEmailConfirmed()) {
            userRepository.updateUser(userDetails);
            authenticateUser(userDetails, request);
        }
    }

    private void authenticateUser(UserDetails user, HttpServletRequest request) {
        request.getSession(); // generate session if one doesn't exist
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null,
                userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void authenticateUser(UserDetails user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null,
                userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(!authentication.getPrincipal().equals("anonymousUser")) {
            return (User) authentication.getPrincipal();
        }else{
            return null;
        }
    }
}
