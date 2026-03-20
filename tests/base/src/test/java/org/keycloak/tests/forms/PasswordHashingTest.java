/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.tests.forms;

import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.BadRequestException;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProviderFactory;
import org.keycloak.credential.hash.Pbkdf2Sha256PasswordHashProviderFactory;
import org.keycloak.credential.hash.Pbkdf2Sha512PasswordHashProviderFactory;
import org.keycloak.crypto.hash.Argon2Parameters;
import org.keycloak.crypto.hash.Argon2PasswordHashProviderFactory;
import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectCryptoHelper;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.crypto.CryptoHelper;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class PasswordHashingTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauthClient;

    @InjectPage
    LoginPage loginPage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectCryptoHelper
    CryptoHelper cryptoHelper;

    @Test
    void testSetInvalidProvider() {
        try {
            setPasswordPolicy("hashAlgorithm(nosuch)");
            fail("Expected error");
        } catch (BadRequestException e) {
            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Invalid config for hashAlgorithm: Password hashing provider not found", error.getErrorMessage());
        }
    }

    @Test
    void testPasswordRehashedOnAlgorithmChanged() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha256PasswordHashProviderFactory.ID + ") and hashIterations(1)");

        String username = "testPasswordRehashedOnAlgorithmChanged";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(Pbkdf2Sha256PasswordHashProviderFactory.ID, credential.getPasswordCredentialData().getAlgorithm());

        assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA256", 1);

        setPasswordPolicy("hashAlgorithm(" + Pbkdf2PasswordHashProviderFactory.ID + ") and hashIterations(1)");

        oauthClient.doLogin(username, password);

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(Pbkdf2PasswordHashProviderFactory.ID, credential.getPasswordCredentialData().getAlgorithm());
        assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA1", 1);
    }

    @Test
    void testPasswordRehashedOnAlgorithmChangedWithMigratedSalt() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha256PasswordHashProviderFactory.ID + ") and hashIterations(1)");

        String username = "testPasswordRehashedOnAlgorithmChangedWithMigratedSalt";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(Pbkdf2Sha256PasswordHashProviderFactory.ID, credential.getPasswordCredentialData().getAlgorithm());

        assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA256", 1);

        setPasswordPolicy("hashAlgorithm(" + Pbkdf2PasswordHashProviderFactory.ID + ") and hashIterations(1)");

        String credentialId = credential.getId();
        runOnServer.run(session -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            CredentialEntity credentialEntity = em.find(CredentialEntity.class, credentialId);
            // adding a dummy value to the salt column to trigger migration in JpaUserCredentialStore#toModel on next fetch of the credential
            credentialEntity.setSalt("dummy".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // Clearing the user cache as we updated the database directly
            session.getProvider(UserCache.class).clear();
        });

        oauthClient.doLogin(username, password);

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(Pbkdf2PasswordHashProviderFactory.ID, credential.getPasswordCredentialData().getAlgorithm());
        assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA1", 1);
    }

    @Test
    void testPasswordRehashedToDefaultProviderIfHashAlgorithmRemoved() {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha256PasswordHashProviderFactory.ID + ")");

        String username = "testPasswordRehashedToDefaultProviderIfHashAlgorithmRemoved";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(Pbkdf2Sha256PasswordHashProviderFactory.ID, credential.getPasswordCredentialData().getAlgorithm());

        setPasswordPolicy("");

        oauthClient.doLogin(username, password);

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        String expectedAlgorithm = notFips() ? Argon2PasswordHashProviderFactory.ID : Pbkdf2Sha512PasswordHashProviderFactory.ID;
        assertEquals(expectedAlgorithm, credential.getPasswordCredentialData().getAlgorithm());
    }

    @Test
    void testPasswordRehashedOnIterationsChanged() throws Exception {
        setPasswordPolicy("hashIterations(1)");

        String username = "testPasswordRehashedOnIterationsChanged";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(1, credential.getPasswordCredentialData().getHashIterations());

        setPasswordPolicy("hashIterations(2)");

        oauthClient.doLogin(username, password);

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(2, credential.getPasswordCredentialData().getHashIterations());

        if (notFips()) {
            assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "Argon2id", 2);
        } else {
            assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA512", 2);
        }
    }

    // KEYCLOAK-5282
    @Test
    void testPasswordNotRehasedUnchangedIterations() {
        setPasswordPolicy("");

        String username = "testPasswordNotRehasedUnchangedIterations";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        String credentialId = credential.getId();
        byte[] salt = credential.getPasswordSecretData().getSalt();

        setPasswordPolicy("hashIterations");

        oauthClient.doLogin(username, password);

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(credentialId, credential.getId());
        assertArrayEquals(salt, credential.getPasswordSecretData().getSalt());

        setPasswordPolicy("hashIterations(" + Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS + ")");

        AccountHelper.logout(realm.admin(), username);

        oauthClient.doLogin(username, password);

        credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        assertEquals(credentialId, credential.getId());
        assertArrayEquals(salt, credential.getPasswordSecretData().getSalt());
    }

    @Test
    void testPasswordRehashedWhenCredentialImportedWithDifferentKeySize() {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha512PasswordHashProviderFactory.ID + ") and hashIterations(" + Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS + ")");

        String username = "testPasswordRehashedWhenCredentialImportedWithDifferentKeySize";
        String password = generatePassword();

        // Encode with a specific key size (256 instead of default: 512)
        Pbkdf2PasswordHashProvider specificKeySizeHashProvider = new Pbkdf2PasswordHashProvider(Pbkdf2Sha512PasswordHashProviderFactory.ID,
                Pbkdf2Sha512PasswordHashProviderFactory.PBKDF2_ALGORITHM,
                Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS,
                0,
                256);
        PasswordCredentialModel passwordCredentialModel = specificKeySizeHashProvider.encodedCredential(password, -1);

        // Create a user with the encoded password, simulating a user import from a different system using a specific key size
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        user.setEmail(username + "@localhost");
        user.setFirstName(username);
        user.setLastName("Test");
        user.setCredentials(List.of(ExportUtils.exportCredential(passwordCredentialModel)));
        AdminApiUtil.createUserWithAdminClient(realm.admin(), user);

        oauthClient.doLogin(username, password);

        PasswordCredentialModel postLoginCredentials = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        // Check that the password was rehashed and the secret string is now twice the size as before
        assertEquals(passwordCredentialModel.getPasswordSecretData().getValue().length() * 2, postLoginCredentials.getPasswordSecretData().getValue().length());
    }

    @Test
    void testPbkdf2Sha1() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2PasswordHashProviderFactory.ID + ")");
        String username = "testPbkdf2Sha1";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA1", Pbkdf2PasswordHashProviderFactory.DEFAULT_ITERATIONS);
    }

    @Test
    void testArgon2() {
        Assumptions.assumeTrue(notFips(), "Argon2 tests skipped in FIPS mode");

        setPasswordPolicy("hashAlgorithm(" + Argon2PasswordHashProviderFactory.ID + ")");
        String username = "testArgon2";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        PasswordCredentialData data = credential.getPasswordCredentialData();

        assertEquals("argon2", data.getAlgorithm());
        assertEquals(5, data.getHashIterations());
        assertEquals("1.3", data.getAdditionalParameters().getFirst("version"));
        assertEquals("id", data.getAdditionalParameters().getFirst("type"));
        assertEquals("32", data.getAdditionalParameters().getFirst("hashLength"));
        assertEquals("7168", data.getAdditionalParameters().getFirst("memory"));
        assertEquals("1", data.getAdditionalParameters().getFirst("parallelism"));

        oauthClient.openLoginForm();
        loginPage.fillLogin("testArgon2", "invalid");
        loginPage.submit();
        loginPage.assertCurrent();
        assertEquals("Invalid username or password.", loginPage.getUsernameInputError());

        loginPage.fillLogin("testArgon2", password);
        loginPage.submit();
        oauthClient.parseLoginResponse();
    }

    private boolean notFips() {
        return !cryptoHelper.isFips();
    }

    @Test
    void testDefault() throws Exception {
        setPasswordPolicy("");
        String username = "testDefault";
        final String password = createUser(username);
        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));

        if (notFips()) {
            assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "Argon2id", Argon2Parameters.DEFAULT_ITERATIONS);
        } else {
            assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA512", Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS);
        }
    }

    @Test
    void testPbkdf2Sha256() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha256PasswordHashProviderFactory.ID + ")");
        String username = "testPbkdf2Sha256";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA256", Pbkdf2Sha256PasswordHashProviderFactory.DEFAULT_ITERATIONS);
    }

    @Test
    void testPbkdf2Sha512() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha512PasswordHashProviderFactory.ID + ")");
        String username = "testPbkdf2Sha512";
        final String password = createUser(username);

        PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username));
        assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA512", Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS);
    }

    @Test
    void testPbkdf2Sha256WithPadding() throws Exception {
        setPasswordPolicy("hashAlgorithm(" + Pbkdf2Sha256PasswordHashProviderFactory.ID + ")");

        int originalPaddingLength = configurePaddingForKeycloak(14);
        try {
            // Assert password created with padding enabled can be verified
            String username1 = "test1-Pbkdf2Sha2562";
            final String password = createUser(username1);

            PasswordCredentialModel credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username1));
            assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA256", Pbkdf2Sha256PasswordHashProviderFactory.DEFAULT_ITERATIONS);

            // Now configure padding to bigger than 64. The verification without padding would fail as for longer padding than 64 characters, the hashes of the padded password and unpadded password would be different
            configurePaddingForKeycloak(65);
            String username2 = "test2-Pbkdf2Sha2562";
            createUser(username2);

            credential = PasswordCredentialModel.createFromCredentialModel(fetchCredentials(username2));
            assertEncoded(credential, password, credential.getPasswordSecretData().getSalt(), "PBKDF2WithHmacSHA256", Pbkdf2Sha256PasswordHashProviderFactory.DEFAULT_ITERATIONS, false);

        } finally {
            configurePaddingForKeycloak(originalPaddingLength);
        }
    }

    private String createUser(String username) {
        final String password = generatePassword();
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        user.setEmail(username + "@localhost");
        user.setFirstName(username);
        user.setLastName("Test");
        AdminApiUtil.createUserAndResetPasswordWithAdminClient(realm.admin(), user, password);
        return password;
    }

    private static String generatePassword() {
        return SecretGenerator.getInstance().randomString(64);
    }

    private void setPasswordPolicy(String policy) {
        RealmRepresentation realmRep = realm.admin().toRepresentation();
        realmRep.setPasswordPolicy(policy);
        realm.admin().update(realmRep);
    }

    private CredentialModel fetchCredentials(String username) {
        return runOnServer.fetch(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realmModel, username);
            return user.credentialManager().getStoredCredentialsByTypeStream(CredentialRepresentation.PASSWORD)
                    .findFirst().orElse(null);
        }, CredentialModel.class);
    }

    private void assertEncoded(PasswordCredentialModel credential, String password, byte[] salt, String algorithm, int iterations) throws Exception {
        assertEncoded(credential, password, salt, algorithm, iterations, true);
    }

    private void assertEncoded(PasswordCredentialModel credential, String password, byte[] salt, String algorithm, int iterations, boolean expectedSuccess) throws Exception {
        if (algorithm.startsWith("PBKDF2")) {
            int keyLength = 512;

            if (Pbkdf2Sha256PasswordHashProviderFactory.ID.equals(credential.getPasswordCredentialData().getAlgorithm())) {
                keyLength = 256;
            }

            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
            byte[] key = SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded();
            if (expectedSuccess) {
                assertEquals(Base64.getEncoder().encodeToString(key), credential.getPasswordSecretData().getValue());
            } else {
                assertNotEquals(Base64.getEncoder().encodeToString(key), credential.getPasswordSecretData().getValue());
            }
        } else if (algorithm.equals("Argon2id")) {
            org.bouncycastle.crypto.params.Argon2Parameters parameters = new org.bouncycastle.crypto.params.Argon2Parameters.Builder(org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_id)
                    .withVersion(org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_VERSION_13)
                    .withSalt(salt)
                    .withParallelism(1)
                    .withMemoryAsKB(7168)
                    .withIterations(iterations).build();

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(parameters);

            byte[] result = new byte[32];
            generator.generateBytes(password.toCharArray(), result);
            assertEquals(Base64.getEncoder().encodeToString(result), credential.getPasswordSecretData().getValue());
        }
    }

    private int configurePaddingForKeycloak(int paddingLength) {
        return runOnServer.fetch(session -> {
            Pbkdf2Sha256PasswordHashProviderFactory factory = (Pbkdf2Sha256PasswordHashProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(PasswordHashProvider.class, Pbkdf2Sha256PasswordHashProviderFactory.ID);
            int origPaddingLength = factory.getMaxPaddingLength();
            factory.setMaxPaddingLength(paddingLength);
            return origPaddingLength;
        }, Integer.class);
    }
}
