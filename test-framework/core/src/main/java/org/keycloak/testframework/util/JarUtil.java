package org.keycloak.testframework.util;


import io.quarkus.paths.PathCollection;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.keycloak.it.TestProvider;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;


public class JarUtil {

    /**
     *
     * @param jarName the name of the created Java Archive
     * @param sources <code>src/main/java</code> directory of a module
     * @param resourcesCollection all resource dirs of a module (<code>src/main/resources</code>)
     * @return built JAR of the provided sources
     */
    public static JavaArchive buildJar(String jarName, Path sources, PathCollection resourcesCollection) {
        JavaArchive providerJar = ShrinkWrap.create(JavaArchive.class, jarName);

        try (Stream<Path> sourcePathStream = Files.walk(sources)) {
            sourcePathStream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        String classFileName = sources.relativize(p).toString();
                        String fullyQualifiedClassName = classFileName.replace(File.separatorChar, '.')
                                .substring(0, classFileName.lastIndexOf('.'));
                        providerJar.addClass(fullyQualifiedClassName);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Path resources : resourcesCollection) {
            try (Stream<Path> paths = Files.walk(resources)) {
                paths.filter(Files::isRegularFile)
                        .forEach(p -> {
                            File resourceFile = p.toFile();
                            providerJar.addAsResource(resourceFile, resources.relativize(p).toString());
                        });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return providerJar;
    }
}
