package org.keycloak.services.resource;

import org.keycloak.provider.Provider;

public interface RootResourceProvider extends Provider {
    Object getResource();
}
