package com.supportportal.service.impl;

import com.supportportal.domain.User;
import com.supportportal.domain.UserPrincipal;
import com.supportportal.enumeration.Role;
import com.supportportal.exception.domain.EmailExistException;
import com.supportportal.exception.domain.EmailNotFoundException;
import com.supportportal.exception.domain.UserNotFoundException;
import com.supportportal.exception.domain.UsernameExistException;
import com.supportportal.repository.UserRepository;
import com.supportportal.service.EmailService;
import com.supportportal.service.LoginAttemptService;
import com.supportportal.service.UserService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.mail.MessagingException;
import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.supportportal.constant.FileConstant.*;
import static com.supportportal.constant.UserImplConstant.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Transactional
@Qualifier("userDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {

    private Logger LOGGER = LoggerFactory.getLogger(getClass());

    private UserRepository userRepository;

    private BCryptPasswordEncoder passwordEncoder;

    private LoginAttemptService loginAttemptService;

    private EmailService emailService;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder,
            LoginAttemptService loginAttemptService,
            EmailService emailService
            ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.emailService = emailService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByUsername(username);
        if(user == null){
            LOGGER.error(NO_USER_FOUND_BY_USERNAME + username);
            throw new UsernameNotFoundException(NO_USER_FOUND_BY_USERNAME + username);
        }else{
            validateLoginAttempt(user);
            user.setLLastLoginDateDisplay(user.getLastLoginDate());
            user.setLastLoginDate(new Date());
            userRepository.save(user);
            UserPrincipal userPrincipal = new UserPrincipal(user);
            LOGGER.info(FOUND_USER_BY_USERNAME + username);
            return userPrincipal;
        }
    }

    private void validateLoginAttempt(User user)  {
        if(user.isNotLocked()){
            if(loginAttemptService.hasExceededMaxAttempts(user.getUsername())){
                user.setNotLocked(false);
            }else{
                user.setNotLocked(true);
            }
        }else{
            loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
        }
    }


    @Override
    public List<User> getUsers() {
        // Check current user's permissions
        String currentUsername = getCurrentUsername();
        if (currentUsername == null) {
            return new ArrayList<>(); // Return empty list if no authenticated user
        }

        User currentUser = userRepository.findUserByUsername(currentUsername);
        if (currentUser == null) {
            return new ArrayList<>();
        }

        Role currentUserRole = getRoleEnumName(currentUser.getRole());

        // USER role can only see their own profile
        if (currentUserRole == Role.ROLE_USER) {
            return Collections.singletonList(currentUser);
        }

        // HR, MANAGER, ADMIN, SUPER_ADMIN can see all users
        return userRepository.findAll();
    }

    @Override
    public User findUserByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }


    @Override
    public User register(String firstName, String lastName, String username, String email) throws UserNotFoundException, UsernameExistException, EmailExistException, MessagingException {
        validateNewUsernameAndEmail(StringUtils.EMPTY, username, email);
        User user = new User();
        user.setUserId(generateUserId());
        String password = generatePassword();
        String encodedPassword = encodePassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setJoinDate(new Date());
        user.setPassword(encodedPassword);
        user.setActive(true);
        user.setNotLocked(true);
        user.setRole(Role.ROLE_USER.name());
        user.setAuthorities(Role.ROLE_USER.getAuthorities());
        user.setProfileImageUrl(getTemporaryProfileImageUrl(username));
        userRepository.save(user);
        LOGGER.info("New User Password " + password);
       // emailService.sendNewPasswordEmail(firstName, password, email);
        return user;
    }

    @Override
    public User addNewUser(String firstName, String lastName, String username, String email, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException {
        // Validate create permissions
        validateCreatePermission(role);

        validateNewUsernameAndEmail(StringUtils.EMPTY, username, email);
        User user = new User();
        user.setUserId(generateUserId());
        String password = generatePassword();
        String encodedPassword = encodePassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setJoinDate(new Date());
        user.setPassword(encodedPassword);
        user.setActive(isActive);
        user.setNotLocked(isNonLocked);
        user.setRole(getRoleEnumName(role).name());
        user.setAuthorities(getRoleEnumName(role).getAuthorities());
        user.setProfileImageUrl(getTemporaryProfileImageUrl(username));
        userRepository.save(user);
        LOGGER.info("New User Password " + password);
        saveProfileImage(user, profileImage);
        return user;
    }


    @Override
    public User updateUser(String currentUsername, String newFirstName, String newLastName, String newUsername, String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException {
        User currentUser = validateNewUsernameAndEmail(currentUsername, newUsername, newEmail);

        // Validate update permissions
        validateUpdatePermission(currentUsername, role);

        currentUser.setFirstName(newFirstName);
        currentUser.setLastName(newLastName);
        currentUser.setUsername(newUsername);
        currentUser.setEmail(newEmail);
        currentUser.setActive(isActive);
        currentUser.setNotLocked(isNonLocked);
        currentUser.setRole(getRoleEnumName(role).name());
        currentUser.setAuthorities(getRoleEnumName(role).getAuthorities());
        userRepository.save(currentUser);
        saveProfileImage(currentUser, profileImage);
        return currentUser;
    }

    @Override
    public void deleteUser(String username) throws UserNotFoundException {
        // Check if user exists
        User userToDelete = userRepository.findUserByUsername(username);
        if (userToDelete == null) {
            throw new UserNotFoundException("User not found with username: " + username);
        }

        // Prevent deleting the super admin user
        if ("supportPortal".equals(userToDelete.getUsername())) {
            throw new UserNotFoundException("Cannot delete super admin user");
        }

        // Prevent users from deleting themselves (get current user from security context)
        try {
            String currentUsername = getCurrentUsername();
            if (currentUsername != null && currentUsername.equals(userToDelete.getUsername())) {
                throw new UserNotFoundException("Users cannot delete their own account");
            }
        } catch (Exception e) {
            // If we can't get current user, continue with deletion (for admin operations)
            LOGGER.warn("Could not determine current user context for delete operation");
        }

        userRepository.delete(userToDelete);
        LOGGER.info("User deleted successfully: {}", userToDelete.getUsername());
    }

    @Override
    public void resetPassword(String email) throws EmailNotFoundException {
        User user = userRepository.findUserByEmail(email);
        if(user == null){
            throw new EmailNotFoundException(NO_USER_FOUND_BY_EMAIL + email);
        }
        String password = generatePassword();
        user.setPassword(encodePassword(password));
        userRepository.save(user);
        LOGGER.info("New User Password " + password);
    }


    @Override
    public User updateProfileImage(String username, MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException {
        User user = validateNewUsernameAndEmail(username, null, null);
        saveProfileImage(user, profileImage);
        return user;
    }



    private void saveProfileImage(User user, MultipartFile profileImage) throws IOException {
        if(profileImage != null){
            Path userFolder = Paths.get(USER_FOLDER + user.getUsername()).toAbsolutePath().normalize();
            if(!Files.exists(userFolder)){
                Files.createDirectories(userFolder);
                LOGGER.info(DIRECTORY_CREATED + userFolder);
            }
            Files.deleteIfExists(Paths.get(userFolder + user.getUsername() + DOT + JPG_EXTENSION));
            Files.copy(profileImage.getInputStream(), userFolder.resolve(user.getUsername() + DOT + JPG_EXTENSION ), REPLACE_EXISTING);
            user.setProfileImageUrl(setProfileImageUrl(user.getUsername()));
            userRepository.save(user);
            LOGGER.info(FILE_SAVED_IN_FILE_SYSTEM + profileImage.getOriginalFilename());
        }
    }

    private String setProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(USER_IMAGE_PATH + username + FORWARD_SLASH + username + DOT + JPG_EXTENSION).toUriString();
    }


    private String getTemporaryProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH + username).toUriString();
    }

    private Role getRoleEnumName(String role) {
        return Role.valueOf(role.toUpperCase());
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(10);
    }

    private User validateNewUsernameAndEmail(String currentUsername, String newUsername, String newEmail) throws UsernameExistException, EmailExistException, UserNotFoundException {
        User userByNewUsername = findUserByUsername(newUsername);
        User userByNewEmail = findUserByEmail(newEmail);
        if(StringUtils.isNotBlank(currentUsername)){
            User currentUser = findUserByUsername(currentUsername);
            if(currentUser == null){
                throw new UserNotFoundException(NO_USER_FOUND_BY_USERNAME + currentUsername);
            }

            if(userByNewUsername != null && !currentUser.getId().equals(userByNewUsername.getId())){
                throw new UsernameExistException(USERNAME_ALREADY_EXISTS);
            }

            if(userByNewEmail != null && !currentUser.getId().equals(userByNewEmail.getId())){
                throw new EmailExistException(EMAIL_ALREADY_EXISTS);
            }
            return currentUser;
        }else{
            if(userByNewUsername != null){
                throw new UsernameExistException(USERNAME_ALREADY_EXISTS);
            }
            if(userByNewEmail != null){
                throw new EmailExistException(EMAIL_ALREADY_EXISTS);
            }
            return null;
        }
    }

    private String getCurrentUsername() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                return (String) principal;
            }
        } catch (Exception e) {
            // Return null if we can't get the current user
        }
        return null;
    }

    private void validateCreatePermission(String role) throws UserNotFoundException {
        String currentUsername = getCurrentUsername();
        if (currentUsername == null) {
            throw new UserNotFoundException("Unable to determine current user");
        }

        User currentUser = userRepository.findUserByUsername(currentUsername);
        if (currentUser == null) {
            throw new UserNotFoundException("Current user not found");
        }

        Role currentUserRole = getRoleEnumName(currentUser.getRole());
        Role newUserRole = getRoleEnumName(role);

        // Check if current user has create permission
        if (!hasAuthority(currentUserRole, "user:create")) {
            throw new UserNotFoundException("Insufficient privileges to create users");
        }

        // ADMIN cannot create SUPER_ADMIN users
        if (currentUserRole == Role.ROLE_ADMIN && newUserRole == Role.ROLE_SUPER_ADMIN) {
            throw new UserNotFoundException("ADMIN users cannot create SUPER_ADMIN users");
        }

        // Users cannot create users with higher roles than themselves (except SUPER_ADMIN)
        if (currentUserRole != Role.ROLE_SUPER_ADMIN && getRoleLevel(newUserRole) > getRoleLevel(currentUserRole)) {
            throw new UserNotFoundException("Cannot create users with higher role than yourself. Your role: " +
                currentUserRole + ", Requested role: " + newUserRole);
        }
    }

    private void validateUpdatePermission(String targetUsername, String newRole) throws UserNotFoundException {
        String currentUsername = getCurrentUsername();
        if (currentUsername == null) {
            throw new UserNotFoundException("Unable to determine current user");
        }

        User currentUser = userRepository.findUserByUsername(currentUsername);
        if (currentUser == null) {
            throw new UserNotFoundException("Current user not found");
        }

        Role currentUserRole = getRoleEnumName(currentUser.getRole());

        // Check if current user has update permission
        if (!hasAuthority(currentUserRole, "user:update")) {
            throw new UserNotFoundException("Insufficient privileges to update users");
        }

        // If updating someone else (not themselves)
        if (!currentUsername.equals(targetUsername)) {
            // Only ADMIN and SUPER_ADMIN can update other users
            if (getRoleLevel(currentUserRole) < getRoleLevel(Role.ROLE_ADMIN)) {
                throw new UserNotFoundException("Only ADMIN and SUPER_ADMIN can update other users");
            }

            // ADMIN cannot promote users to SUPER_ADMIN
            if (currentUserRole == Role.ROLE_ADMIN && getRoleEnumName(newRole) == Role.ROLE_SUPER_ADMIN) {
                throw new UserNotFoundException("ADMIN users cannot promote users to SUPER_ADMIN");
            }

            // Cannot assign roles higher than current user's role (except SUPER_ADMIN)
            if (currentUserRole != Role.ROLE_SUPER_ADMIN && getRoleLevel(getRoleEnumName(newRole)) > getRoleLevel(currentUserRole)) {
                throw new UserNotFoundException("Cannot assign roles higher than your own. Your role: " +
                    currentUserRole + ", Requested role: " + newRole);
            }
        } else {
            // User updating themselves - cannot escalate privileges
            User targetUser = userRepository.findUserByUsername(targetUsername);
            if (targetUser != null) {
                Role targetUserRole = getRoleEnumName(targetUser.getRole());
                Role requestedRole = getRoleEnumName(newRole);

                if (getRoleLevel(requestedRole) > getRoleLevel(targetUserRole)) {
                    throw new UserNotFoundException("Users cannot escalate their own privileges. Current role: " +
                        targetUserRole + ", Requested role: " + requestedRole);
                }
            }
        }
    }

    private boolean hasAuthority(Role role, String authority) {
        String[] authorities = role.getAuthorities();
        for (String auth : authorities) {
            if (auth.equals(authority)) {
                return true;
            }
        }
        return false;
    }

    private int getRoleLevel(Role role) {
        switch (role) {
            case ROLE_USER: return 1;
            case ROLE_HR: return 2;
            case ROLE_MANAGER: return 3;
            case ROLE_ADMIN: return 4;
            case ROLE_SUPER_ADMIN: return 5;
            default: return 0;
        }
    }

}
