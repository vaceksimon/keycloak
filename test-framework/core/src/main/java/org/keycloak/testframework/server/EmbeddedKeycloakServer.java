package org.keycloak.testframework.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.keycloak.Keycloak;
import org.keycloak.common.Version;
import org.keycloak.platform.Platform;
import org.keycloak.testframework.util.JarUtil;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.paths.PathCollection;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class EmbeddedKeycloakServer implements KeycloakServer {

    private Keycloak keycloak;
    private Path homeDir;
    private boolean tlsEnabled = false;

    @Override
    public void start(KeycloakServerConfigBuilder keycloakServerConfigBuilder, boolean tlsEnabled) {
        Keycloak.Builder builder = Keycloak.builder().setVersion(Version.VERSION);
        this.tlsEnabled = tlsEnabled;

        // todo musim vymyslet co s temi zavislostmi. Jak je budu teda uchovavat
        // todo bud to vsude zmenim na tento typ, anebo budou dva seznamy zavislosti, jeden na hot deploy a druhy ne
        deployDependencies(keycloakServerConfigBuilder.toDependencies(), builder);

        Set<Path> configFiles = keycloakServerConfigBuilder.toConfigFiles();
        if (!configFiles.isEmpty()) {
            if (homeDir == null) {
                homeDir = Platform.getPlatform().getTmpDirectory().toPath();
            }

            Path conf = homeDir.resolve("conf");

            if (!conf.toFile().exists()) {
                conf.toFile().mkdirs();
            }

            for (Path configFile : configFiles) {
                try {
                    Files.copy(configFile, conf.resolve(configFile.getFileName()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        builder.setHomeDir(homeDir);
        keycloak = builder.start(keycloakServerConfigBuilder.toArgs());
    }

    private void deployDependencies(Set<KeycloakServerDependency> dependencies, Keycloak.Builder builder) {
        if (!getDependencyHotDeploy()) {
            for(Dependency dependency : dependencies) {
                builder.addDependency(dependency.getGroupId(), dependency.getArtifactId(), "");
            }
        } else {
            dependencies.stream().filter(d -> !d.allowHotDeploy())
                            .forEach(d -> builder.addDependency(d.getGroupId(), d.getArtifactId(), ""));
            hotDeployDependencies(dependencies.stream().filter(KeycloakServerDependency::allowHotDeploy).collect(Collectors.toSet()));
        }
    }

    private void hotDeployDependencies(Set<KeycloakServerDependency> dependencies) {
        if (homeDir == null) {
            homeDir = Platform.getPlatform().getTmpDirectory().toPath();
        }
        Path providersDir = homeDir.resolve("providers");
        if (!providersDir.toFile().mkdirs()) {
            throw new RuntimeException("Failed to create the providers directory " + providersDir.toFile());
        }

        try {
            LocalProject projectRoot = LocalProject.loadWorkspace(Path.of("."));
            while (projectRoot.getLocalParent() != null) {
                projectRoot = projectRoot.getLocalParent();
            }

            for (Dependency dependency : dependencies) {
                LocalProject dependencyModule = projectRoot.getWorkspace().getProject(dependency.getGroupId(), dependency.getArtifactId());
                if (dependencyModule == null) {
                    throw new RuntimeException("No such artifact in the project: " + dependency.getGroupId() + ":" + dependency.getArtifactId());
                }

                String jarName = dependencyModule.getArtifactId() + "-" + dependencyModule.getVersion() + ".jar";
                Path sources = dependencyModule.getSourcesSourcesDir();
                PathCollection resources = dependencyModule.getResourcesSourcesDirs();

                JavaArchive builtDependency = JarUtil.buildJar(jarName, sources, resources);
                builtDependency.as(ZipExporter.class).exportTo(providersDir.resolve(jarName).toFile(), true);
            }
        } catch (BootstrapMavenException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            keycloak.stop();
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBaseUrl() {
        if (tlsEnabled) {
            return "https://localhost:8443";
        } else {
            return "http://localhost:8080";
        }
    }

    @Override
    public String getManagementBaseUrl() {
        if (tlsEnabled) {
            return "https://localhost:9001";
        } else {
            return "http://localhost:9001";
        }
    }
}
