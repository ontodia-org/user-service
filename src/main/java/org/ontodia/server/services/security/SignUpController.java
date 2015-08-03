package org.ontodia.server.services.security;

import org.ontodia.server.services.security.interfaces.*;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * To use this as rest controller, just extend @RestController @RequestMapping SignUpRest extends SignUpController<DTOType> and you're done!
 * @param <DTOUserType>
 */

public abstract class SignUpController<DTOUserType extends IUserDTO> {

    private final SignUpConfig config;
    private IUserDetailManager userRepository;
    private PasswordEncoder passwordEncoder;
    private UserDetailsService userDetailsService;
    private IUserSearcherByToken userSearcherByToken;

    private MailSender mailSender;
    private String mailFromAddress;
    private MessageProviderForMail messageProvider;

    public SignUpController(
            IUserDetailManager userRepository,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService,
            MailSender mailSender,
            String mailFromAddress,
            MessageProviderForMail messageProvider,
            IUserSearcherByToken userSearcherByToken,
            SignUpConfig config) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.mailSender = mailSender;
        this.mailFromAddress = mailFromAddress;
        this.messageProvider = messageProvider;
        this.userSearcherByToken = userSearcherByToken;
        this.config = config;
    }

    // Registry
    //==================================================================
    //==========================================================

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @ResponseBody
    public void register(@RequestBody DTOUserType user) throws Exception {
        if (userRepository.userExists(user.getUsername()))
            throw new UserAlreadyExistsException("User with username "+user.getUsername()+" already exists!");

        UserDetailsWithTokens newUser = userRepository.createUserFromDTO(user);
        if(newUser==null) throw new Exception("var newUser is null!");
        createConfirmationToken(newUser);
        userRepository.createUser(newUser);
        sendConfirmationEmailTo(newUser);
        this.authenticateUser(newUser);
    }

    private void createConfirmationToken(ITokenStorage user) {
        if (user.getConfirmationToken() == null) {
            user.setConfirmationToken(UUID.randomUUID().toString());
        }
    }

    private void sendConfirmationEmailTo(UserDetailsWithTokens user) throws Exception {

        String subj = this.messageProvider.getConfirmationSubject();
        String msg = this.messageProvider.getConfirmationMessage(user, config.domain + config.activatedLink);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFromAddress);
        message.setTo(user.getUsername());
        message.setSubject(subj);
        message.setText(msg);

        mailSender.send(message);
    }

    @RequestMapping(value = "/activate", method = RequestMethod.POST)
    @ResponseBody
    public void activate(@RequestParam("token")String token, HttpServletRequest request) throws Exception {
        UserDetailsWithTokens userDetails = userSearcherByToken.findByConfirmationToken(token);
        if (userDetails == null) throw new UserNotFoundException("Bad token!");
        if (userDetails.isEmailConfirmed()) throw new BadLinkException("User already activated!");
        userDetails.setEmailConfirmed(true);
        userRepository.updateUser(userDetails);
        authenticateUser(userDetails, request);
    }

    // Recovery password
    //==================================================================
    //==========================================================
    @RequestMapping(value = "/recoverPassword", method = RequestMethod.POST)
    @ResponseBody
    public void forgotPassword(@RequestParam("email")String email) throws Exception {
        try {
            UserDetails existingUser = userRepository.loadUserByUsername(email);
            createForgotPasswordToken((ITokenStorage) existingUser);
            userRepository.updateUser(existingUser);
            sendRequestForChangePassword((UserDetailsWithTokens) existingUser);
        }catch (Exception e){
            throw new UserNotFoundException(e);
        }
    }

    private void createForgotPasswordToken(ITokenStorage user) {
        if (user.getForgotPasswordToken() == null) {
            user.setForgotPasswordToken(UUID.randomUUID().toString());
        }
    }

    private void sendRequestForChangePassword(UserDetailsWithTokens user) throws Exception {
        String subj = this.messageProvider.getChangePasswordSubject();
        String msg = this.messageProvider.getChangePasswordMessage(user, config.domain + config.recoveryLink);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFromAddress);
        message.setTo(user.getUsername());
        message.setSubject(subj);
        message.setText(msg);

        mailSender.send(message);
        user.setEmailConfirmed(false);
        userRepository.updateUser(user);
    }

    @RequestMapping(value = "/authenticateByForgotPasswordToken", method = RequestMethod.POST)
    @ResponseBody
    public void authenticateByForgotPasswordToken(@RequestParam("token")String token, HttpServletRequest request) throws Exception  {
        UserDetailsWithTokens userDetails = userSearcherByToken.findByForgotPasswordToken(token);
        if (userDetails == null) throw new UserNotFoundException("Bad token!");
        if (userDetails.isEmailConfirmed()) throw new BadLinkException("Link already used!");
            authenticateUser(userDetails, request);
    }

    // Invitation
    //==================================================================
    //==========================================================
    //@RequestMapping(value = "/inviteUser", method = RequestMethod.POST)
    public boolean inviteUser(String email) throws Exception {
        User user = getUser();
        if(user==null) return false;

        UserDetailsWithTokens userDetails = userRepository.createUserFromEmail(email);
        createInvitationToken(userDetails);
        userRepository.createUser(userDetails);
        sendInvitationTo(userDetails, user.getUsername());
        return true;
    }

    //@RequestMapping(value = "/simpleInviteUser", method = RequestMethod.POST)
    public boolean inviteExistingUser(@RequestParam("email") String email) throws Exception {
        User user = getUser();
        if(user==null) return false;

        sendSimpleInvitationTo(email, user.getUsername());
        return true;
    }

    private void createInvitationToken(ITokenStorage user) {
        if (user.getInvitationToken() == null) {
            user.setInvitationToken(UUID.randomUUID().toString());
        }
    }

    private void sendInvitationTo(UserDetailsWithTokens user, String sender) throws Exception {
        String subj = this.messageProvider.getInvitationSubject();
        String msg = this.messageProvider.getInvitationMessage(user, config.domain + config.invitationLink, sender);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFromAddress);
        message.setTo(user.getUsername());
        message.setSubject(subj);
        message.setText(msg);

        mailSender.send(message);
        userRepository.updateUser(user);
    }

    private void sendSimpleInvitationTo(String user, String sender) throws Exception {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFromAddress);
        message.setTo(user);
        message.setSubject("OntoDia Invitation");
        message.setText(String.format(
                "Welcome to OntoDia, the fist and only online OWL diagramming tool for everyone.\r\n\r\n" +
                        "User %s has shared a data with you." +
                        "To view the data source please sign in and find it under Diagrams tab.:\r\n" +
                        "%s\r\n\r\n" +
                        "Best Regards,\r\nOntoDia Team\r\n",
                sender, config.domain));

        mailSender.send(message);
    }

    @RequestMapping(value = "/setPassword", method = RequestMethod.POST)
    public void setPassword(@RequestParam("newPassword")String password) throws Exception {
        User user = getUser();
        if(user==null) new AccessDeniedException("User not authenticated!");
        UserDetailsWithTokens existingUser = (UserDetailsWithTokens)user;
        if (existingUser.isEmailConfirmed()) throw new BadLinkException("Password already changed by this link!");
        existingUser.setEmailConfirmed(true);
        userRepository.updateUser(existingUser);
        userRepository.changePassword(existingUser.getPassword(), passwordEncoder.encode(password));
    }

    @RequestMapping(value = "/authenticateByInvitationToken", method = RequestMethod.POST)
    @ResponseBody
    public void authenticateByInvitationToken(@RequestParam("token")String token, HttpServletRequest request) throws Exception  {
        UserDetailsWithTokens userDetails = userSearcherByToken.findByInvitationToken(token);
        if (userDetails == null) throw new UserNotFoundException("Bad token!");
        if (userDetails.isEmailConfirmed()) throw new BadLinkException("Link already used!");
        userRepository.updateUser(userDetails);
        authenticateUser(userDetails, request);
    }

    // Authentication
    //==================================================================
    //==========================================================


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
