package org.keycloak.testframework.server;

import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyBuilder;

public class KeycloakServerDependency extends ArtifactDependency {

    private final boolean allowHotDeploy;

    private KeycloakServerDependency(KeycloakServerDependencyBuilder dependencyBuilder) {
        super(dependencyBuilder);
        this.allowHotDeploy = dependencyBuilder.allowHotDeploy();
    }

    public boolean allowHotDeploy() {
        return this.allowHotDeploy;
    }

    public static class KeycloakServerDependencyBuilder extends DependencyBuilder {

        private boolean allowHotDeploy;

        public KeycloakServerDependencyBuilder allowHotDeploy(boolean allowHotDeploy) {
            this.allowHotDeploy = allowHotDeploy;
            return this;
        }

        public boolean allowHotDeploy() {
            return this.allowHotDeploy;
        }

        @Override
        public KeycloakServerDependencyBuilder setGroupId(String groupId) {
            super.setGroupId(groupId);
            return this;
        }

        @Override
        public KeycloakServerDependencyBuilder setArtifactId(String artifactId) {
            super.setArtifactId(artifactId);
            return this;
        }

        @Override
        public KeycloakServerDependency build() {
            return new KeycloakServerDependency(this);
        }

    }
}
