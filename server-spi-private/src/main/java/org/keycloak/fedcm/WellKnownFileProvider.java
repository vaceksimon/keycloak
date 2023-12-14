package org.keycloak.fedcm;

import org.keycloak.provider.Provider;
import org.keycloak.representations.fedcm.WellKnownFileRepresentation;

import java.util.List;

                // ExampleService
public interface WellKnownFileProvider extends Provider {
    List<WellKnownFileRepresentation> getWellKnownFile();
}
