package at.yawk.mdep;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Manifest;
import lombok.SneakyThrows;
import sun.misc.PerfCounter;
import sun.misc.Resource;
import sun.misc.URLClassPath;

/**
 * http://stackoverflow.com/a/6424879/1116343
 */
public class ParentLastURLClassLoader extends URLClassLoader {
    public ParentLastURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * Defines a Class using the class bytes obtained from the specified
     * Resource. The resulting Class must be resolved before it can be
     * used.
     */
    private Class<?> defineClass(String name, Resource res) throws IOException {
        long t0 = System.nanoTime();
        int i = name.lastIndexOf('.');
        URL url = res.getCodeSourceURL();
        if (i != -1) {
            String pkgname = name.substring(0, i);
            // Check if package already loaded.
            Manifest man = res.getManifest();
            if (getPackage(pkgname) == null) {
                try {
                    if (man != null) {
                        definePackage(pkgname, man, url);
                    } else {
                        definePackage(pkgname, null, null, null, null, null, null, null);
                    }
                } catch (IllegalArgumentException iae) {
                    // parallel-capable class loaders: re-verify in case of a
                    // race condition
                    if (getPackage(pkgname) == null) {
                        // Should never happen
                        throw new AssertionError("Cannot find package " +
                                                 pkgname);
                    }
                }
            }
        }
        // Now read the class bytes and define the class
        ByteBuffer bb = res.getByteBuffer();
        if (bb != null) {
            // Use (direct) ByteBuffer:
            CodeSigner[] signers = res.getCodeSigners();
            CodeSource cs = new CodeSource(url, signers);
            PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(t0);
            return defineClass(name, bb, cs);
        } else {
            byte[] b = res.getBytes();
            // must read certificates AFTER reading bytes.
            CodeSigner[] signers = res.getCodeSigners();
            CodeSource cs = new CodeSource(url, signers);
            PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(t0);
            return defineClass(name, b, 0, b.length, cs);
        }
    }

    /**
     * Finds and loads the class with the specified name from the URL search
     * path. Any URLs referring to JAR files are loaded and opened as needed
     * until the class is found.
     *
     * @param name the name of the class
     * @return the resulting class
     * @throws ClassNotFoundException if the class could not be found,
     *                                or if the loader is closed.
     * @throws NullPointerException   if {@code name} is {@code null}.
     */
    @Override
    @SneakyThrows(ReflectiveOperationException.class)
    protected Class<?> findClass(String name)
            throws ClassNotFoundException {
        String path = name.replace('.', '/') + ".class";

        Field ucpField = URLClassLoader.class.getDeclaredField("ucp");
        ucpField.setAccessible(true);
        Resource res = ((URLClassPath) ucpField.get(this)).getResource(path, false);
        if (res != null) {
            try {
                return defineClass(name, res);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        } else {
            throw new ClassNotFoundException(name);
        }
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                // checking local
                c = findClass(name);
            } catch (ClassNotFoundException e) {
                // checking parent
                // This call to loadClass may eventually call findClass again, in case the parent
                // doesn't find anything.
                c = super.loadClass(name, resolve);
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    public URL getResource(String name) {
        URL url = findResource(name);
        if (url == null) {
            // This call to getResource may eventually call findResource again, in case the
            // parent doesn't find anything.
            url = super.getResource(name);
        }
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        /*
         * Similar to super, but local resources are enumerated before parent resources
         */
        Enumeration<URL> localUrls = findResources(name);
        Enumeration<URL> parentUrls = null;
        if (getParent() != null) {
            parentUrls = getParent().getResources(name);
        }
        List<URL> urls = new ArrayList<>();
        while (localUrls.hasMoreElements()) {
            urls.add(localUrls.nextElement());
        }
        if (parentUrls != null) {
            while (parentUrls.hasMoreElements()) {
                urls.add(parentUrls.nextElement());
            }
        }
        return new Enumeration<URL>() {
            Iterator<URL> iter = urls.iterator();

            @Override
            public boolean hasMoreElements() {
                return iter.hasNext();
            }

            @Override
            public URL nextElement() {
                return iter.next();
            }
        };
    }

    @SuppressWarnings("resource")
    @Override
    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        try {
            return url != null ? url.openStream() : null;
        } catch (IOException ignored) {}
        return null;
    }
}
