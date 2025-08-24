package org.keycloak.tests.admin.realm;

import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import org.keycloak.common.Profile;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest(config = KeyConfTest.ServerConf.class)
public class KeyConfTest {

    @InjectRealm(config = RealmConf.class)
    ManagedRealm realm;

    @InjectUser(config = UserConf.class)
    ManagedUser user;

    @InjectClient
    ManagedClient client;

    @InjectRealm(ref = "realm2")
    ManagedRealm realm2;

    @InjectUser(realmRef = "realm2", ref = "user2")
    ManagedUser user2;

    @Test
    public void changeClientId() {
        client.updateWithCleanup(client -> client.clientId("something-else"));

        assertEquals("something-else", client.admin().toRepresentation().getClientId());
    }

    @Test
    public void clientResets() {
        assertNotEquals("something-else", client.admin().toRepresentation().getClientId());
    }

    @Test
    public void realmNameTest() {
        String realmName = realm.getName();

        assertThat(realm.getBaseUrl(), endsWith("/" + realmName));
        assertEquals("keyconf", realmName);
    }

    @Test
    public void userInRealmTest() {
        String userId = user.getId();
        String user2Id = user2.getId();

        assertDoesNotThrow(
                () -> realm.admin().users().get(userId)
        );
        assertDoesNotThrow(
                () -> realm2.admin().users().get(user2Id)
        );

        assertThrows(
                NotFoundException.class,
                () -> realm2.admin().users().get(userId).toRepresentation()
        );
        assertThrows(
                NotFoundException.class,
                () -> realm.admin().users().get(user2Id).toRepresentation()
        );
    }


    private static class UserConf implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user.username("svacek")
                    .name("Simon", "Vacek")
                    .email("svacek@ibm.com")
                    .password("not-telling-you");
        }
    }

    private static class RealmConf implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.name("keyconf")
                    .displayName("KeyConf25")
                    .registrationEmailAsUsername(true);
        }
    }

    public static class ServerConf implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.AUTHORIZATION, Profile.Feature.IMPERSONATION)
                    .option("metrics-enabled", "true");
        }
    }
}
