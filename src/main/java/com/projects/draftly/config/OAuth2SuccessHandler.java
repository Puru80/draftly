package com.projects.draftly.config;

import com.projects.draftly.auth.model.User;
import com.projects.draftly.auth.repository.UserRepository;
import com.projects.draftly.auth.service.EncryptionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();
        String email = oauth2User.getAttribute("email");

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            oauthToken.getAuthorizedClientRegistrationId(),
            oauthToken.getName()
        );

        if (client.getRefreshToken() == null) {
            // If this happens, you already have a refresh token or your prompt=consent configuration is missing!
            System.out.println("Warning: Google did not return a refresh token.");
        } else {
            String rawRefreshToken = client.getRefreshToken().getTokenValue();
            String encryptedToken = encryptionService.encrypt(rawRefreshToken);

            // Upsert user tracking metrics
            User user = userRepository.findByEmail(email)
                .orElseGet(() -> User.builder().email(email).build());

            user.setEncryptedRefreshToken(encryptedToken);
            userRepository.save(user);
        }

        // Redirect the user back to the React Application view
        // In a real application, you might append a short-lived local JWT query parameter here for React auth state management.
        getRedirectStrategy().sendRedirect(request, response, "http://localhost:3000/dashboard?login=success");
    }
}