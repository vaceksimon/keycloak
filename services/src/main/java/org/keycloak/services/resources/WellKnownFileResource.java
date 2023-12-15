package org.keycloak.services.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.models.KeycloakSession;

import java.util.*;

@Path("/.well-known")
public class WellKnownFileResource {

    private final KeycloakSession session;

    public WellKnownFileResource(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Path("web-identity")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getWellKnownFile(@HeaderParam("Sec-Fetch-Dest") String secFetchDest) {
        Map<String, Object> providerUrls = new HashMap<>();
        if (secFetchDest.equals("webidentity")) {
            // correct header parameter, let's give em config file(s)
            providerUrls.put("provider_urls", List.of("http://localhost:8080/realms/fedcm-realm/fedcm/config.json"));
            }
        else
            providerUrls.put("error", "Sec-Fetch-Dest header is not set to webidentity");
        return providerUrls;
    }

}
