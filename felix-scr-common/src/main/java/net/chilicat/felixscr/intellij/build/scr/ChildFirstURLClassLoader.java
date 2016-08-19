package net.chilicat.felixscr.intellij.build.scr;

import java.net.URL;
import java.net.URLClassLoader;

public class ChildFirstURLClassLoader extends URLClassLoader {
    public ChildFirstURLClassLoader(final URL[] urls, final ClassLoader parent) {
        super(urls, parent);
    }

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if ("org.slf4j.impl.StaticLoggerBinder".equals(name)) {
            //hack to fix ClassNotFoundException because org.slf4j.impl.StaticLoggerBinder included in gradle plugin but slf4j not
            //see https://intellij-support.jetbrains.com/hc/en-us/community/posts/208362685-java-lang-NoClassDefFoundError-since-2016-2-update
            throw new ClassNotFoundException(name);
        } else {
            synchronized (getClassLoadingLock(name)) {
                // First, check if the class has already been loaded
                Class<?> c = findLoadedClass(name);

                if (c == null) {
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException e) {
                        // ClassNotFoundException thrown if class not found
                    }
                }

                if (c == null && getParent() != null) {
                    c = getParent().loadClass(name);
                }

                if (resolve) {
                    resolveClass(c);
                }

                return c;
            }
        }
    }
}