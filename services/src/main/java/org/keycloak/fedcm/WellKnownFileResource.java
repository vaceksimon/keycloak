package org.keycloak.fedcm;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.fedcm.WellKnownFileRepresentation;

import java.util.LinkedList;
import java.util.List;

public class WellKnownFileResource {
    private KeycloakSession session;
    public WellKnownFileResource(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Path("/.well-known/web-identity")
    @Produces(MediaType.APPLICATION_JSON)
    public List<WellKnownFileRepresentation> listProviders() {
        return session.getProvider(WellKnownFileProvider.class).getWellKnownFile();
    }
}
