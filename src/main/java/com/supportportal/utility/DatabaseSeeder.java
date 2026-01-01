package com.supportportal.utility;

import com.supportportal.domain.User;
import com.supportportal.enumeration.Role;
import com.supportportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public void run(String... args) throws Exception {
        createSuperAdminUserIfNotExists();
    }

    private void createSuperAdminUserIfNotExists() {
        // Check if super admin user already exists
        if (userRepository.findUserByUsername("supportPortal") != null) {
            log.info("Super admin user 'supportPortal' already exists. Skipping creation.");
            return;
        }

        log.info("Creating super admin user 'supportPortal'...");

        // Create super admin user
        User superAdmin = new User();
        superAdmin.setUserId(generateUserId());
        superAdmin.setFirstName("Support");
        superAdmin.setLastName("Portal");
        superAdmin.setUsername("supportPortal");
        superAdmin.setEmail("admin@supportportal.com");

        // Set password to "supportPortal"
        String encodedPassword = bCryptPasswordEncoder.encode("supportPortal");
        superAdmin.setPassword(encodedPassword);

        // Set super admin role and authorities
        Role superAdminRole = Role.ROLE_SUPER_ADMIN;
        superAdmin.setRole(superAdminRole.name());
        superAdmin.setAuthorities(superAdminRole.getAuthorities());

        // Set account status
        superAdmin.setActive(true);
        superAdmin.setNotLocked(true);

        // Set profile image to default
        superAdmin.setProfileImageUrl(getTemporaryProfileImageUrl("supportPortal"));
        superAdmin.setJoinDate(new Date());

        // Save the user
        userRepository.save(superAdmin);

        log.info("Super admin user 'supportPortal' created successfully with password 'supportPortal'");
        log.warn("⚠️  IMPORTANT: Change the default password 'supportPortal' after first login!");
    }

    private String generateUserId() {
        return java.util.UUID.randomUUID().toString();
    }

    private String getTemporaryProfileImageUrl(String username) {
        // During startup, we can't use ServletUriComponentsBuilder
        // Use a simple default image URL
        return "https://robohash.org/" + username + "?set=set4&size=200x200";
    }
}
