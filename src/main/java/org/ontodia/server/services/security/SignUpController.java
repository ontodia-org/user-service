package org.ontodia.server.services.security;

import org.ontodia.server.services.security.interfaces.*;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * To use this as rest controller, just extend
 * <code>@RestController @RequestMapping SignUpRest extends SignUpController<DTOType> and you're done.</code>
 * @param <DTOUserType>
 */
public abstract class SignUpController<DTOUserType extends IUserDTO> {

    private final SignUpConfig config;
    private IUserDetailManager<? super DTOUserType> userRepository;
    private PasswordEncoder passwordEncoder;
    private UserDetailsService userDetailsService;
    private IUserSearcherByToken userSearcherByToken;

    private MailSender mailSender;
    private String mailFromAddress;
    private MessageProviderForMail messageProvider;

    public SignUpController(
            IUserDetailManager<? super DTOUserType> userRepository,
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
        ComposedMessage composedMessage = messageProvider.composeConfirmation(
                user, config.domain + config.activatedLink);
        sendMailMessage(user, composedMessage);
    }

    @RequestMapping(value = "/activate", method = RequestMethod.POST)
    @ResponseBody
    public void activate(@RequestParam("token") String token, HttpServletRequest request) throws Exception {
        UserDetailsWithTokens userDetails = userSearcherByToken.findByConfirmationToken(token);
        if (userDetails == null) { throw new UserNotFoundException("Invalid activation token"); }
        if (userDetails.isEmailConfirmed()) { throw new BadLinkException("User email already confirmed"); }
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
        } catch (Exception e){
            throw new UserNotFoundException(e);
        }
    }

    private void createForgotPasswordToken(ITokenStorage user) {
        if (user.getForgotPasswordToken() == null) {
            user.setForgotPasswordToken(UUID.randomUUID().toString());
        }
    }

    private void sendRequestForChangePassword(UserDetailsWithTokens user) throws Exception {
        ComposedMessage composedMessage = this.messageProvider.composeChangePassword(
                user, config.domain + config.recoveryLink);
        sendMailMessage(user, composedMessage);
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

    public void inviteUser(String userEmail, String senderEmail) throws MailException {
        UserDetailsWithTokens userDetails = createUserByEmail(userEmail);
        sendInvitationTo(userDetails, senderEmail);
    }

    public UserDetailsWithTokens createUserByEmail(String userEmail){
        UserDetailsWithTokens userDetails = userRepository.createUserFromEmail(userEmail);
        createInvitationToken(userDetails);
        userRepository.createUser(userDetails);
        return userDetails;
    }

    public void inviteExistingUser(String userEmail, ComposedMessage message) throws MailException {
        UserDetails user = userRepository.loadUserByUsername(userEmail);
        sendMailMessage(user, message);
    }

    private void createInvitationToken(ITokenStorage user) {
        if (user.getInvitationToken() == null) {
            user.setInvitationToken(UUID.randomUUID().toString());
        }
    }

    public void sendInvitationTo(UserDetailsWithTokens user, String senderEmail) throws MailException {
        ComposedMessage composedMessage = this.messageProvider.composeInvitation(
                user, config.domain + config.invitationLink, senderEmail);
        sendMailMessage(user, composedMessage);
    }

    private void sendMailMessage(UserDetails user, ComposedMessage message) {
        ComposedMessage formattedMessage = messageProvider.formatMailStructure(user, message);

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(mailFromAddress);
        mailMessage.setTo(user.getUsername());
        mailMessage.setSubject(formattedMessage.getSubject());
        mailMessage.setText(formattedMessage.getBody());

        mailSender.send(mailMessage);
    }

    @RequestMapping(value = "/setPassword", method = RequestMethod.POST)
    public void setPassword(@RequestParam("newPassword") String password) throws Exception {
        User user = getUser();
        UserDetailsWithTokens existingUser = (UserDetailsWithTokens)user;
        if (existingUser.isEmailConfirmed()) throw new BadLinkException("Password already changed by this link!");
        existingUser.setEmailConfirmed(true);
        userRepository.updateUser(existingUser);
        userRepository.changePassword(existingUser.getPassword(), passwordEncoder.encode(password));
    }

    @RequestMapping(value = "/authenticateByInvitationToken", method = RequestMethod.POST)
    @ResponseBody
    public void authenticateByInvitationToken(@RequestParam("token") String token, HttpServletRequest request) throws Exception  {
        UserDetailsWithTokens userDetails = userSearcherByToken.findByInvitationToken(token);
        if (userDetails == null) throw new UserNotFoundException("Bad token!");
        if (userDetails.isEmailConfirmed()) throw new BadLinkException("Link already used!");
        userRepository.updateUser(userDetails);
        authenticateUser(userDetails, request);
    }

    // Authentication
    //==================================================================

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
        if (authentication == null) {
            throw new AccessDeniedException("Cannot get Authentication (user isn't signed in?)");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) authentication.getPrincipal();
        } else {
            throw new AccessDeniedException("Cannot get User object because user is anonymous");
        }
    }
}
