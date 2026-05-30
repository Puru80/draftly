package com.projects.draftly.email.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.services.gmail.Gmail;
import com.projects.draftly.auth.model.User;
import com.projects.draftly.auth.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@RequiredArgsConstructor
public class GmailApiService {

    private final EncryptionService encryptionService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    /**
     * Exchanges the user's stored refresh token for an active Gmail instance
     */
    public Gmail getGmailClient(User user) throws GeneralSecurityException, IOException {
        String decryptedRefreshToken = encryptionService.decrypt(user.getEncryptedRefreshToken());

        // Force a target token refresh grant
        TokenResponse response = new GoogleRefreshTokenRequest(
            new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            decryptedRefreshToken,
            clientId,
            clientSecret
        ).execute();

        // Build credential container
        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
            .setTransport(new NetHttpTransport())
            .setJsonFactory(GsonFactory.getDefaultInstance())
            .build()
            .setAccessToken(response.getAccessToken());

        return new Gmail.Builder(
            new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Draftly").build();
    }
}