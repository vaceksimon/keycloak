package org.keycloak.representations.fedcm;

public class WellKnownFileRepresentation {
    private String provider_url;

    public WellKnownFileRepresentation(String provider_url) {
        this.provider_url = provider_url;
    }

    public String getProvider_url() {
        return provider_url;
    }

    public void setProvider_url(String provider_url) {
        this.provider_url = provider_url;
    }

}
