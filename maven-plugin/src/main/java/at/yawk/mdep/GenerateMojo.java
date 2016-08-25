/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.mdep;

import at.yawk.mdep.model.Dependency;
import at.yawk.mdep.model.DependencySet;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import lombok.SneakyThrows;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author yawkat
 */
@Mojo(name = "generate",
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
        requiresDependencyCollection = ResolutionScope.COMPILE)
public class GenerateMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;

    @Parameter(name = "outputDirectory", defaultValue = "${project.build.directory}/generated-resources/mdep")
    File outputDirectory;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
    Collection<ArtifactRepository> remoteArtifactRepositories;

    @Parameter(defaultValue = "${project.artifacts}", readonly = true)
    Collection<Artifact> dependencyArtifacts;

    @Parameter(name = "includes")
    @Nullable
    List<String> includes = null;

    @Parameter(name = "excludes")
    List<String> excludes = Collections.emptyList();

    @Parameter(name = "repositories")
    @Nullable
    Set<String> repositories = null;

    // one week caching by default
    @Parameter(name = "cacheHours", defaultValue = "168")
    double cacheHours;

    private Path cacheStore;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (cacheHours > 0) {
            cacheStore = Environment.createCacheStore(new Logger() {
                @Override
                public void info(String msg) {
                    getLog().info(msg);
                }

                @Override
                public void warn(String msg) {
                    getLog().warn(msg);
                }
            }, "mdep-maven-plugin");
        }

        ArtifactMatcher includesMatcher;
        if (includes == null) {
            includesMatcher = ArtifactMatcher.acceptAll();
        } else {
            includesMatcher = ArtifactMatcher.anyMatch(toAntMatchers(includes));
        }
        ArtifactMatcher excludesMatcher = ArtifactMatcher.anyMatch(toAntMatchers(excludes));
        ArtifactMatcher matcher = includesMatcher.and(excludesMatcher.not());

        List<Dependency> dependencies = new ArrayList<>();
        for (Artifact artifact : dependencyArtifacts) {
            // only include compile dependencies
            // todo: configurable
            if (!artifact.getScope().equals(Artifact.SCOPE_COMPILE)) { continue; }

            if (!matcher.matches(artifact)) {
                continue;
            }

            dependencies.add(findArtifact(artifact));
        }

        getLog().info("Saving dependency xml");

        DependencySet dependencySet = new DependencySet();
        dependencySet.setDependencies(dependencies);

        if (!outputDirectory.mkdirs()) {
            throw new MojoExecutionException("Failed to create output directory");
        }
        File outputFile = new File(outputDirectory, "mdep-dependencies.xml");

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(DependencySet.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(dependencySet, outputFile);

        } catch (JAXBException e) {
            throw new MojoExecutionException("Failed to serialize dependency set", e);
        }
        Resource resource = new Resource();
        resource.setDirectory(outputDirectory.toString());
        resource.setFiltering(false);
        project.addResource(resource);
    }

    private Dependency findArtifact(Artifact artifact) throws MojoExecutionException {
        // all are scanned, the first is used to store the dependency
        List<Path> cacheSearchLocations = new ArrayList<>();

        List<String> coordinateComponents = Arrays.asList(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getScope());

        // in 1.0, we used only ':' as the separator. This does not work on windows, and the following code fixes
        // that problem.

        for (String separator : Arrays.asList(":", "/")) {
            try {
                cacheSearchLocations.add(cacheStore.resolve(String.join(separator, coordinateComponents)));
            } catch (InvalidPathException ignored) {}
        }

        // check local cache
        if (cacheHours > 0) {
            for (Path searchLocation : cacheSearchLocations) {
                if (Files.exists(searchLocation)) {
                    Instant cacheDeadline = Instant.now().minusSeconds((long) (60 * 60 * cacheHours));
                    try {
                        if (Files.getLastModifiedTime(searchLocation).toInstant()
                                .isAfter(cacheDeadline)) {

                            try (InputStream in = Files.newInputStream(searchLocation)) {
                                Dependency dependency = (Dependency) JAXBContext.newInstance(Dependency.class)
                                        .createUnmarshaller().unmarshal(in);

                                getLog().info("Checksum was present in local cache: " + artifact);
                                return dependency;
                            }
                        }
                    } catch (IOException | JAXBException e) {
                        throw new MojoExecutionException("Failed to read local cache", e);
                    }
                }
            }
        }

        for (ArtifactRepository repository : remoteArtifactRepositories) {
            // only scan configured repositories
            if (this.repositories != null &&
                !this.repositories.contains(repository.getId())) {
                continue;
            }

            Dependency dependency = findArtifactInRepository(artifact, repository);
            if (dependency != null) {

                if (cacheHours > 0) {
                    try {
                        Path target = cacheSearchLocations.get(0);
                        if (!Files.isDirectory(target.getParent())) {
                            Files.createDirectories(target.getParent());
                        }
                        try (OutputStream out = Files.newOutputStream(target)) {
                            JAXBContext.newInstance(Dependency.class)
                                    .createMarshaller().marshal(dependency, out);
                        }
                    } catch (IOException | JAXBException e) {
                        getLog().warn("Could not save dependency to local cache", e);
                    }
                }
                return dependency;
            }
        }

        throw new MojoExecutionException("Could not find " + artifact + " in configured repositories");
    }

    @Nullable
    @SneakyThrows({ MalformedURLException.class, NoSuchAlgorithmException.class })
    @VisibleForTesting
    Dependency findArtifactInRepository(Artifact artifact, ArtifactRepository repository)
            throws MojoExecutionException {

        String artifactPath = getArtifactPath(artifact, artifact.getVersion());
        if (artifact.isSnapshot()) {
            ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata(artifact) {
                // maven is weird - i have yet to find a better solution.

                @Override
                public boolean storedInArtifactVersionDirectory() {
                    return true;
                }

                @Override
                public String getBaseVersion() {
                    return artifact.getBaseVersion();
                }
            };

            // try to load maven-metadata.xml in case we need to use a different version for snapshots
            URL metaUrl = new URL(repository.getUrl() + '/' + repository.pathOfRemoteRepositoryMetadata(metadata));

            Metadata loadedMetadata;
            try (InputStream input = openStream(metaUrl)) {
                loadedMetadata = new MetadataXpp3Reader().read(input, true);
            } catch (IOException e) {
                // could not find metadata
                loadedMetadata = null;
            } catch (XmlPullParserException e) {
                throw new MojoExecutionException("Failed to parse metadata", e);
            }

            if (loadedMetadata != null) {
                Snapshot snapshot = loadedMetadata.getVersioning().getSnapshot();

                String versionWithoutSuffix = artifact.getVersion()
                        .substring(0, artifact.getBaseVersion().lastIndexOf('-'));
                artifactPath = getArtifactPath(artifact,
                                               versionWithoutSuffix + '-' + snapshot.getTimestamp() + '-' +
                                               snapshot.getBuildNumber());
            }
        }

        URL url = new URL(repository.getUrl() + '/' + artifactPath);
        try (InputStream input = openStream(url)) {
            getLog().info("Getting checksum for " + artifact);

            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] buf = new byte[4096];
            int len;
            while ((len = input.read(buf)) >= 0) {
                digest.update(buf, 0, len);
            }

            Dependency dependency = new Dependency();
            dependency.setUrl(url);
            dependency.setSha512sum(digest.digest());
            return dependency;
        } catch (IOException ignored) {
            // not in this repo
            return null;
        }
    }

    private static String getArtifactPath(Artifact artifact, String version) {
        StringBuilder builder = new StringBuilder()
                .append(artifact.getGroupId().replace('.', '/')).append('/')
                .append(artifact.getArtifactId()).append('/')
                .append(artifact.getBaseVersion()).append('/')
                .append(artifact.getArtifactId()).append('-').append(version);
        if (artifact.getArtifactHandler().getClassifier() != null) {
            builder.append('-').append(artifact.getArtifactHandler().getClassifier());
        }
        String extension = artifact.getArtifactHandler().getExtension();
        if (extension == null) {
            extension = "jar";
        }
        return builder.append('.').append(extension).toString();
    }

    private static InputStream openStream(URL metaUrl) throws IOException {
        URLConnection urlConnection = metaUrl.openConnection();
        // Java user agent is blocked in some cases
        urlConnection.setRequestProperty("User-Agent", "mdep");
        return urlConnection.getInputStream();
    }

    private static List<ArtifactMatcher> toAntMatchers(List<String> antPatterns) {
        return antPatterns.stream()
                .map(AntArtifactMatcher::new)
                .collect(Collectors.toList());
    }
}
