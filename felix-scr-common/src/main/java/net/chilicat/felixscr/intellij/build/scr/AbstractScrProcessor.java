package net.chilicat.felixscr.intellij.build.scr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import net.chilicat.felixscr.intellij.settings.ScrSettings;
import org.apache.felix.scrplugin.bnd.SCRDescriptorBndPlugin;

public abstract class AbstractScrProcessor {

    public static final String OSGI_INF = "OSGI-INF";

    private ScrSettings settings;

    private ScrLogger logger;


    private static class ReportingBuilder extends Builder {

        private final ScrLogger logger;

        private ReportingBuilder(final ScrLogger logger) {
            super();

            this.logger = logger;
        }

        @Override
        public SetLocation error(final String string, final Object... args) {
            final SetLocation setLocation = super.error(string, args);

            final Location location = setLocation.location();

            logger.error(location.message, location.file, location.line);

            return setLocation;
        }

        @Override
        public SetLocation warning(final String string, final Object... args) {
            final SetLocation setLocation = super.warning(string, args);

            final Location location = setLocation.location();

            logger.warn(location.message, location.file, location.line);

            return setLocation;
        }
    }

    public AbstractScrProcessor() {
    }

    public void setLogger(ScrLogger logger) {
        this.logger = logger;
    }

    public ScrLogger getLogger() {
        return logger;
    }

    public void setSettings(ScrSettings settings) {
        this.settings = settings;
    }

    public boolean execute() {
        final File classDir = this.getClassOutDir();

        if (classDir == null) {
            getLogger().error("Compiler Output path must be set for: " + getModuleName(), null, -1, -1);

            return false;
        }

        try (final Builder builder = new ReportingBuilder(logger)) {
            builder.setTrace(logger.isDebugEnabled());

            logger.debug("Class dir: " + classDir.getPath());

            deleteServiceComponentXMLFiles(classDir, logger);

            builder.setBase(classDir);
            builder.setJar(classDir);
            builder.setProperties(buildProprties(classDir));
            builder.setClasspath(buildClasspath(classDir));

            try (final Jar jar = builder.build()) {
                writeGeneratedResources(jar, classDir);

                updateManifest(jar);

                logger.debug(String.format("Built: %s", jar.getName()));
            }

            return !logger.isErrorPrinted();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

        }

        return false;
    }

    private void writeGeneratedResources(final Jar jar, final File classDir) {
        for (Map.Entry<String, Resource> entry : jar.getResources().entrySet()) {
            final String jarFilePath = entry.getKey();

            if (jarFilePath.startsWith(OSGI_INF)) {
                File outputFile = new File(classDir, jarFilePath);

                try (final FileOutputStream out = new FileOutputStream(outputFile)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("Writing: %s", outputFile.getCanonicalPath()));
                    }
                    entry.getValue().write(out);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private Properties buildProprties(final File classDir) throws IOException {
        Properties properties = new Properties();

        properties.put(Analyzer.BUNDLE_SYMBOLICNAME, getModuleName());
        properties.put(Analyzer.DSANNOTATIONS, "*");
        properties.put(Analyzer.METATYPE_ANNOTATIONS, "*");
        properties.put(Analyzer.IMPORT_PACKAGE, "*");


        final Map<String, String> felixScrPluginOptions = new LinkedHashMap<>();

        felixScrPluginOptions.put("strictMode", Boolean.toString(settings.isStrictMode()));
        felixScrPluginOptions.put("generateAccessors", Boolean.toString(settings.isGenerateAccessors()));
        felixScrPluginOptions.put("specVersion", settings.getSpec());
        felixScrPluginOptions.put("log", settings.isDebugLogging() ? "Debug" : "Warn");
        felixScrPluginOptions.put("destdir", classDir.getCanonicalPath());

        header(
            properties,
            Analyzer.PLUGIN,
            String.format("%s;%s", SCRDescriptorBndPlugin.class.getName(), pluginOptions(felixScrPluginOptions))
        );

        return properties;
    }

    private List<Jar> buildClasspath(final File classDir) throws IOException {
        List<Jar> classpath = new ArrayList<>();

        if (classDir.isDirectory()) {
            classpath.add(new Jar(getModuleName(), classDir));
        }

        final Collection<String> projectClassPath = new LinkedHashSet<String>();

        collectClasspath(projectClassPath);

        for (String path : projectClassPath) {
            File cpe = new File(path);

            if (cpe.exists()) {
                classpath.add(new Jar(cpe));
            } else {
                logger.warn(String.format("Path %s does not exist", cpe.getCanonicalPath()));
            }
        }

        return classpath;
    }

    private static void header(Properties properties, String key, Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
            return;
        }

        properties.put(key, value.toString().replaceAll("[\r\n]", ""));
    }

    private static String pluginOptions(Map<String, String> options) {
        return options.entrySet()
            .stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(";"));
    }

    private void deleteServiceComponentXMLFiles(File classDir, ScrLogger logger) {
        final Set<String> nonDelete = collectNonDeletes();

        logger.debug("Preserve files: " + Arrays.toString(nonDelete.toArray()));

        File xmlDir = new File(classDir, OSGI_INF);

        logger.debug("OSGI-INF exists: " + xmlDir.exists() + " Is dir: " + xmlDir.isDirectory());

        if (xmlDir.exists() && xmlDir.isDirectory()) {
            File[] files = xmlDir.listFiles();

            logger.debug("OSGI-INF has files: " + (files != null));

            if (files != null) {
                for (File file : files) {
                    if (!nonDelete.contains(file.getName()) && file.getName().endsWith(".xml")) {
                        logger.debug("Delete service xml: " + file.getAbsolutePath());
                        if (!file.delete()) {
                            logger.warn("Cannot delete service xml: " + file.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    /**
     * Collects a list of file which are not allowed to be deleted.
     *
     * @return a set of files.
     */
    private Set<String> collectNonDeletes() {

        final File[] sourceRoots = getModuleSourceRoots();
        final Set<String> nonDelete = new HashSet<String>();

        for (File sourceRoot : sourceRoots) {
            File file = new File(sourceRoot, "OSGI-INF");
            if (file.exists()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File a : files) {
                        nonDelete.add(a.getName());
                    }

                }
            }
        }

        return nonDelete;
    }

    private void updateManifest(final Jar jar) {
        File manifest = new File(this.getClassOutDir(), "/META-INF/MANIFEST.MF");

        boolean hasScrFiles = jar.getResources(s -> s.matches(OSGI_INF + "/.*\\.xml")).findAny().isPresent();

        logger.debug("Update Manifest, Has manifest: " + manifest.exists() + ", SCR Comps: " + hasScrFiles);

        if (manifest.exists() && hasScrFiles) {
            final String componentLine = OSGI_INF + "/*.xml";

            try {
                FileInputStream in = new FileInputStream(manifest);
                Manifest m = null;
                try {
                    m = new Manifest(in);
                    logger.debug("Overwrite Manifest policy");
                    m.getMainAttributes().putValue("Service-Component", componentLine);
                } finally {
                    in.close();
                }

                FileOutputStream out = new FileOutputStream(manifest);
                try {
                    m.write(out);
                } finally {
                    out.close();
                }

            } catch (IOException e) {
                logger.error(e);
            }
        } else {
            logger.info("Module '" + getModuleName() + "' has no manifest. Couldn't add component descriptor");
        }
    }

    protected abstract File[] getModuleSourceRoots();

    protected abstract File getClassOutDir();

    protected abstract String getModuleName();

    protected abstract void collectClasspath(Collection<String> classPath);
}
