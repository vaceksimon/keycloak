package org.keycloak.fedcm;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

            // ExampleSpi
public class WellKnownFileSpi implements Spi {
    public static final String NAME = "wellKnownFile";
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return WellKnownFileProvider.class;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return WellKnownFileProviderFactory.class;
    }
}
