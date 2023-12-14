package org.keycloak.fedcm.impl;

import org.keycloak.fedcm.WellKnownFileProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;
import org.keycloak.representations.fedcm.WellKnownFileRepresentation;

import java.util.LinkedList;
import java.util.List;

            // ExampleServiceImpl
public class WellKnownFileProviderImpl implements WellKnownFileProvider {

    private final KeycloakSession session;
    public WellKnownFileProviderImpl(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {

    }

    @Override
    public List<WellKnownFileRepresentation> getWellKnownFile() {
        List<WellKnownFileRepresentation> providers = new LinkedList<>();
        providers.add(new WellKnownFileRepresentation("Test well known file"));
        return providers;
    }
}
