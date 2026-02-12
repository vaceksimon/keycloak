package org.keycloak.testframework.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.it.utils.Maven;
import org.keycloak.testframework.util.Collections;
import org.keycloak.testframework.util.FileUtils;
import org.keycloak.testframework.util.MavenProjectUtil;

import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import org.jboss.logging.Logger;

public final class ProviderDeployer {

    private final Logger log;

    private final File providersDir;
    private final Path providersPath;
    private final boolean hotDeployEnabled;

    private final Set<KeycloakDependency> requestedDependencies;
    private final List<File> existingDependencies;

    public ProviderDeployer(Logger log, File keycloakHomeDir, Set<KeycloakDependency> requestedDependencies) {
        this.log = log;

        this.providersDir = new File(keycloakHomeDir, "providers");
        this.providersPath = providersDir.toPath();

        this.requestedDependencies = requestedDependencies;
        this.existingDependencies = listExistingDependencies();

        this.hotDeployEnabled = KeycloakServer.getDependencyHotDeployEnabled();
    }

    public boolean areDependenciesCompatible() {
        Set<String> requestedDependencies = this.requestedDependencies.stream().map(this::getDependencyJarName).collect(Collectors.toSet());
        Set<String> startedWithDependencies = this.existingDependencies.stream().map(File::getName).collect(Collectors.toSet());
        return Collections.equals(requestedDependencies, startedWithDependencies);
    }

    public void updateDependencies() throws IOException {
        deleteNotRequestedDependencies();

        for (KeycloakDependency d : requestedDependencies) {
            boolean shouldPackageClasses = hotDeployEnabled && d.isHotDeployable();

            String jarName = getDependencyJarName(d);
            Path dependencyPath = getDependencyPath(d);
            File dependencyFile = dependencyPath.toFile();
            Path targetPath = providersPath.resolve(jarName);
            File targetFile = targetPath.toFile();
            File targetLastModified = new File(targetFile.getAbsolutePath() + ".lastModified");
            long lastModified = targetLastModified.isFile() ? FileUtils.readLongFromFile(targetLastModified) : -1;

            if (lastModified != dependencyPath.toFile().lastModified() || !targetFile.isFile()) {
                log.trace("Adding or overwriting existing provider: " + targetPath.toFile().getAbsolutePath());

                if (shouldPackageClasses || d.dependencyCurrentProject()) {
                    MavenProjectUtil.buildJar(jarName, dependencyPath, targetPath);
                } else {
                    Files.copy(dependencyPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                Files.writeString(targetLastModified.toPath(), Long.toString(dependencyFile.lastModified()));
            }
        }
    }

    private List<File> listExistingDependencies() {
        if (providersDir.isDirectory()) {
            File[] files = providersDir.listFiles(n -> n.getName().endsWith(".jar"));
            if (files != null) {
                return Arrays.stream(files).toList();
            }
        }
        return List.of();
    }

    private String getDependencyJarName(KeycloakDependency dependency) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();

        if (dependency.dependencyCurrentProject()) {
            LocalProject project = MavenProjectUtil.getCurrentModule();

            groupId = project.getGroupId();
            artifactId = project.getArtifactId();
        }

        return groupId + "__" + artifactId + ".jar";
    }

    private void deleteNotRequestedDependencies() {
        existingDependencies.stream()
                .filter(f -> {
                    String fileName = f.getName();
                    return requestedDependencies.stream().noneMatch(d -> fileName.equals(getDependencyJarName(d)));
                }).forEach(f -> {
                    log.trace("Deleted non-requested provider: " + f.getAbsolutePath());
                    FileUtils.delete(f);
                    FileUtils.delete(new File(f.getAbsolutePath() + ".lastModified"));
                });
    }

    private Path getDependencyPath(KeycloakDependency d) {
        if (d.dependencyCurrentProject()) {
            return MavenProjectUtil.getCurrentModule().getClassesDir();
        }

        if (d.isHotDeployable() && hotDeployEnabled) {
            return MavenProjectUtil.findLocalModule(d.getGroupId(), d.getArtifactId()).getClassesDir();
        }

        return Maven.resolveArtifact(d.getGroupId(), d.getArtifactId());
    }

}
