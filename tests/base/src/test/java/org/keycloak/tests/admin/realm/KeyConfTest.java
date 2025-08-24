package org.keycloak.tests.admin.realm;

import org.junit.jupiter.api.Test;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@KeycloakIntegrationTest
public class KeyConfTest {

    @InjectRealm(config = RealmConf.class)
    ManagedRealm realm;

    @InjectUser(config = UserConf.class)
    ManagedUser user;

    @InjectClient
    ManagedClient client;

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

        assertDoesNotThrow(
                () -> realm.admin().users().get(userId)
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
}
