package org.keycloak.tests.admin.realm;

import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class KeyConfTest {

    @InjectRealm(config = RealmConf.class)
    ManagedRealm realm;

    @Test
    public void realmNameTest() {
        String realmName = realm.getName();

        assertThat(realm.getBaseUrl(), endsWith("/" + realmName));
        assertEquals("keyconf", realmName);
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
