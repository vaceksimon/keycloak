package org.keycloak.fedcm;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

            // ExampleRealmResourceProvider
public class WellKnownFileEndpointProvider implements RealmResourceProvider {

    private KeycloakSession session;

    public WellKnownFileEndpointProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new WellKnownFileResource(session);
    }

    @Override
    public void close() {

    }
}
