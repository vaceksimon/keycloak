package org.keycloak.fedcm;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

            // ExampleRealmResourceProviderFactory
public class WellKnownFileEndpointProviderFactory implements RealmResourceProviderFactory {

    public static final String ID = "wellKnownFile";

    @Override
    public String getId() {
                    return ID;
                }

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new WellKnownFileEndpointProvider(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

}
