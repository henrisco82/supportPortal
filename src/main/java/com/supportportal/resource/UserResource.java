package com.supportportal.resource;

import com.supportportal.constant.SecurityConstant;
import com.supportportal.domain.HttpResponse;
import com.supportportal.domain.LoginRequest;
import com.supportportal.domain.RegisterRequest;
import com.supportportal.domain.User;
import com.supportportal.domain.UserPrincipal;
import com.supportportal.exception.domain.*;
import com.supportportal.service.UserService;
import com.supportportal.utility.JWTTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.supportportal.constant.FileConstant.*;
import static org.springframework.util.MimeTypeUtils.IMAGE_JPEG_VALUE;

@RestController
@RequestMapping(value="/user")
@Tag(name = "User Management", description = "APIs for managing users, authentication, and user profiles")
@SecurityRequirement(name = "bearerAuth")
public class UserResource extends ExceptionHandling {

    public static final String EMAIL_SENT = " An email with a new password was sent to: ";
    public static final String USER_DELETED_SUCCESSFULLY = "User deleted successfully";
    private UserService userService;
    private AuthenticationManager authenticationManager;
    private JWTTokenProvider jwtTokenProvider;

    @Autowired
    public UserResource(UserService userService, AuthenticationManager authenticationManager, JWTTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping(value = "/login", consumes = "application/json")
    @Operation(summary = "User login", description = "Authenticate user with username and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content)
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "User credentials",
        required = true,
        content = @Content(schema = @Schema(implementation = LoginRequest.class))
    )
    public ResponseEntity<User> login(@RequestBody @Valid LoginRequest loginRequest) {
        authenticate(loginRequest.getUsername(), loginRequest.getPassword());
        User loginUser = userService.findUserByUsername(loginRequest.getUsername());
        UserPrincipal userPrincipal = new UserPrincipal(loginUser);
        HttpHeaders jwtHeader = getJwtHeader(userPrincipal);
        return new ResponseEntity<>(loginUser, jwtHeader, HttpStatus.OK);
    }

    @PostMapping(value = "/register", consumes = "application/json")
    @Operation(summary = "Register new user", description = "Register a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Username or email already exists",
                    content = @Content)
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "User registration data",
        required = true,
        content = @Content(schema = @Schema(implementation = RegisterRequest.class))
    )
    public ResponseEntity<User> register(@RequestBody @Valid RegisterRequest registerRequest) throws UserNotFoundException, UsernameExistException, EmailExistException, MessagingException {
        User newUser = userService.register(registerRequest.getFirstName(), registerRequest.getLastName(),
                                           registerRequest.getUsername(), registerRequest.getEmail());
       return new ResponseEntity<>(newUser, HttpStatus.OK);
    }

    @PostMapping(value = "/add", consumes = "multipart/form-data")
    @Operation(summary = "Add new user", description = "Create a new user account (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Username or email already exists",
                    content = @Content)
    })
    public ResponseEntity<User> addNewUser(
            @Parameter(description = "User's first name") @RequestParam("firstName") String firstName,
            @Parameter(description = "User's last name") @RequestParam("lastName") String lastName,
            @Parameter(description = "Username") @RequestParam("username") String username,
            @Parameter(description = "Email address") @RequestParam("email") String email,
            @Parameter(description = "User role") @RequestParam("role") String role,
            @Parameter(description = "Account active status") @RequestParam("isActive") String isActive,
            @Parameter(description = "Account locked status") @RequestParam("isNonLocked") String isNonLocked,
            @Parameter(description = "Profile image file") @RequestParam(value = "profileImage", required = false) MultipartFile profileImage
    ) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException {
        User newUser = userService.addNewUser(firstName, lastName, username, email, role, Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isActive), profileImage);
        return new ResponseEntity<>(newUser, HttpStatus.OK);
    }

    @PostMapping(value = "/update", consumes = "multipart/form-data")
    @Operation(summary = "Update user", description = "Update an existing user's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Username or email already exists",
                    content = @Content)
    })
    public ResponseEntity<User> updateUser(
            @Parameter(description = "Current username") @RequestParam("currentUsername") String currentUsername,
            @Parameter(description = "Updated first name") @RequestParam("firstName") String firstName,
            @Parameter(description = "Updated last name") @RequestParam("lastName") String lastName,
            @Parameter(description = "Updated username") @RequestParam("username") String username,
            @Parameter(description = "Updated email address") @RequestParam("email") String email,
            @Parameter(description = "Updated user role") @RequestParam("role") String role,
            @Parameter(description = "Updated active status") @RequestParam("isActive") String isActive,
            @Parameter(description = "Updated locked status") @RequestParam("isNonLocked") String isNonLocked,
            @Parameter(description = "Updated profile image") @RequestParam(value = "profileImage", required = false) MultipartFile profileImage
    ) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException {
        User updatedUser = userService.updateUser(currentUsername, firstName, lastName, username, email, role, Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isActive), profileImage);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    @PostMapping(value = "/updateProfileImage", consumes = "multipart/form-data")
    @Operation(summary = "Update profile image", description = "Update user's profile image")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile image updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content)
    })
    public ResponseEntity<User> updateProfileImage(
            @Parameter(description = "Username") @RequestParam("username") String username,
            @Parameter(description = "Profile image file") @RequestParam(value = "profileImage") MultipartFile profileImage
    ) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException {
        User user = userService.updateProfileImage(username, profileImage);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping(path="/image/{username}/{filename}", produces = IMAGE_JPEG_VALUE)
    @Operation(summary = "Get profile image", description = "Retrieve user's profile image by username and filename")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image retrieved successfully",
                    content = @Content(mediaType = "image/jpeg")),
            @ApiResponse(responseCode = "404", description = "Image not found",
                    content = @Content)
    })
    public byte[] getProfileImage(
            @Parameter(description = "Username") @PathVariable("username") String username,
            @Parameter(description = "Image filename") @PathVariable("filename") String filename
    ) throws IOException {
        return Files.readAllBytes(Paths.get(USER_FOLDER + username + FORWARD_SLASH + filename));
    }

    @GetMapping(path="/image/profile/{username}", produces = IMAGE_JPEG_VALUE)
    @Operation(summary = "Get temporary profile image", description = "Retrieve user's temporary profile image")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image retrieved successfully",
                    content = @Content(mediaType = "image/jpeg")),
            @ApiResponse(responseCode = "404", description = "Image not found",
                    content = @Content)
    })
    public byte[] getTempProfileImage(@Parameter(description = "Username") @PathVariable("username") String username) throws IOException {
        URL url = new URL(TEMP_PROFILE_IMAGE_BASE_URL + username);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try(InputStream inputStream = url.openStream()){
            int bytesRead;
            byte[] chunk = new byte[1024];
            while((bytesRead = inputStream.read(chunk)) > 0){
                byteArrayOutputStream.write(chunk, 0, bytesRead);
            }

        }
        return byteArrayOutputStream.toByteArray();
    }

    @GetMapping("/find/{username}")
    @Operation(summary = "Find user by username", description = "Retrieve user information by username")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content)
    })
    public ResponseEntity<User> getUser(@Parameter(description = "Username to search") @PathVariable("username") String username) {
        User user = userService.findUserByUsername(username);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/list")
    @Operation(summary = "List all users", description = "Retrieve a list of all users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class)))
    })
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/resetPassword/{email}")
    @Operation(summary = "Reset password", description = "Send password reset email to user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset email sent successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = HttpResponse.class))),
            @ApiResponse(responseCode = "404", description = "Email not found",
                    content = @Content)
    })
    public ResponseEntity<HttpResponse> resetPassword(@Parameter(description = "User's email address") @PathVariable("email") String email) throws EmailNotFoundException {
        userService.resetPassword(email);
        return response(HttpStatus.OK, EMAIL_SENT + email);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('user:delete')")
    @Operation(summary = "Delete user", description = "Delete a user account (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = HttpResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    public ResponseEntity<HttpResponse> deleteUser(@Parameter(description = "User ID") @PathVariable("id") long id) {
        userService.deleteUser(id);
        return response(HttpStatus.NO_CONTENT, USER_DELETED_SUCCESSFULLY);
    }

    private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {
        return new ResponseEntity<>(new HttpResponse(httpStatus.value(), httpStatus, httpStatus.getReasonPhrase().toUpperCase(), message.toUpperCase()), httpStatus);
    }

    private HttpHeaders getJwtHeader(UserPrincipal userPrincipal) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(SecurityConstant.JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(userPrincipal));
        return headers;
    }

    private void authenticate(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }

}
