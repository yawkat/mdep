package at.yawk.mdep;

import at.yawk.mdep.model.Dependency;
import at.yawk.mdep.model.DependencySet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import lombok.SneakyThrows;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

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

    @Override
    @SneakyThrows({ MalformedURLException.class, NoSuchAlgorithmException.class })
    public void execute() throws MojoExecutionException, MojoFailureException {
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

            if (!matcher.matches(artifact)) { continue; }

            for (ArtifactRepository repository : remoteArtifactRepositories) {
                URL url = new URL(repository.getUrl() + '/' + repository.pathOf(artifact));
                try (InputStream input = url.openStream()) {
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
                    dependencies.add(dependency);

                    // added dependency correctly - don't check the other repos
                    break;
                } catch (IOException ignored) {
                    // skip this repo
                }
            }
            // todo: log if we couldn't find the dependency
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

    private static List<ArtifactMatcher> toAntMatchers(List<String> antPatterns) {
        return antPatterns.stream()
                .map(AntArtifactMatcher::new)
                .collect(Collectors.toList());
    }
}
