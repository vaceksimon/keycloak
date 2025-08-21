package org.keycloak.tests.admin.realm;

import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;

@KeycloakIntegrationTest
public class KeyConfTest {

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void realmNameTest() {
        String realmName = realm.getName();

        assertThat(realm.getBaseUrl(), endsWith("/" + realmName));
    }
}
