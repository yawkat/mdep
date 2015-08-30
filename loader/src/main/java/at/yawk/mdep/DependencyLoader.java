package at.yawk.mdep;

import at.yawk.mdep.model.Dependency;
import at.yawk.mdep.model.DependencySet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import lombok.SneakyThrows;

/**
 * @author yawkat
 */
@NotThreadSafe
public class DependencyLoader {
    private final Logger logger;
    private final List<URL> urls = new ArrayList<>();

    // visible for testing
    DependencyStore dependencyStore;

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public DependencyLoader() {
        this(System.err);
    }

    public DependencyLoader(PrintStream streamLogger) {
        this(new StreamLogger(streamLogger));
    }

    public DependencyLoader(java.util.logging.Logger julLogger) {
        this(new JulLogger(julLogger));
    }

    private DependencyLoader(Logger logger) {
        this.logger = logger;
    }

    private void initDependencyStore() {
        if (dependencyStore == null) {
            dependencyStore = Environment.createDependencyStore(logger);
        }
    }

    public DependencyLoader addUrls(URL... urls) {
        Collections.addAll(this.urls, urls);
        return this;
    }

    @SneakyThrows(NoSuchAlgorithmException.class)
    public DependencyLoader downloadDependencies(DependencySet dependencySet) throws IOException {
        initDependencyStore();
        for (Dependency dependency : dependencySet.getDependencies()) {
            URL cached = dependencyStore.getCachedDependency(dependency);
            if (cached == null) {
                logger.info("Downloading " + dependency.getUrl());
                MessageDigest digest = MessageDigest.getInstance("SHA-512");
                @SuppressWarnings("resource")
                ByteArrayOutputStream memoryStream = new ByteArrayOutputStream();
                try (InputStream stream = dependency.getUrl().openStream()) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = stream.read(buf)) >= 0) {
                        digest.update(buf, 0, len);
                        memoryStream.write(buf, 0, len);
                    }
                }
                byte[] hash = digest.digest();
                if (!Arrays.equals(hash, dependency.getSha512sum())) {
                    throw new IOException("Mismatched hash on " + dependency.getUrl() + ": expected " +
                                          DatatypeConverter.printHexBinary(dependency.getSha512sum()) + " but got " +
                                          DatatypeConverter.printHexBinary(hash));
                }
                cached = dependencyStore.saveDependency(dependency, memoryStream.toByteArray());
            }
            urls.add(cached);
        }
        logger.info("Added " + dependencySet.getDependencies().size() + " dependencies.");
        return this;
    }

    public DependencyLoader downloadDependencies() throws IOException {
        DependencySet dependencies;
        try {
            dependencies = (DependencySet) JAXBContext.newInstance(DependencySet.class)
                    .createUnmarshaller().unmarshal(DependencyLoader.class.getResource("/mdep-dependencies.xml"));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        return downloadDependencies(dependencies);
    }

    @SneakyThrows(ReflectiveOperationException.class)
    public void appendToClassLoader(URLClassLoader classLoader) {
        Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addUrlMethod.setAccessible(true);
        for (URL url : urls) {
            addUrlMethod.invoke(classLoader, url);
        }
    }

    public ClassLoader toParentFirstClassLoader() {
        return toParentFirstClassLoader(DependencyLoader.class.getClassLoader());
    }

    public ClassLoader toParentFirstClassLoader(ClassLoader parent) {
        return new URLClassLoader(urls.toArray(new URL[urls.size()]), parent);
    }

    public ClassLoader toParentLastClassLoader() {
        return toParentLastClassLoader(DependencyLoader.class.getClassLoader());
    }

    public ClassLoader toParentLastClassLoader(ClassLoader parent) {
        return new ParentLastURLClassLoader(urls.toArray(new URL[urls.size()]), parent);
    }
}
