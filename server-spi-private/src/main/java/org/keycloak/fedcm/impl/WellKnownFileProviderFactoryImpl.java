package org.keycloak.fedcm.impl;

import org.keycloak.Config.Scope;
import org.keycloak.fedcm.WellKnownFileProvider;
import org.keycloak.fedcm.WellKnownFileProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

            // ExampleServiceProviderFactoryImpl
public class WellKnownFileProviderFactoryImpl implements WellKnownFileProviderFactory {

    public static final String ID = "wellKnownFile";

    @Override
    public WellKnownFileProvider create(KeycloakSession session) {
        return new WellKnownFileProviderImpl(session);
    }

    @Override
    public void init(Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return ID;
    }
}
