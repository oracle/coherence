/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.config;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import java.lang.reflect.Modifier;

import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import java.util.jar.JarFile;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests to validate executor POF artifacts.
 *
 * @author  rl 11.29.21
 * @since 21.12
 */
public class PofSanityTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialized the {@link PofContext} for testing.
     *
     * @throws Exception if an error occurs setting up the {@link PofContext}
     */
    @BeforeAll
    protected static void beforeAll()
            throws Exception
        {
        URL urlPofConfig = PofSanityTest.class.getClassLoader().getResource(POF_CONFIG);
        assertThat("Unable to locate POF configuration [" + POF_CONFIG + ']', urlPofConfig, notNullValue());
        s_pofContext = new ConfigurablePofContext(urlPofConfig.toString());

        doSanityChecks();
        }

    /**
     * Reset recording state before each test.
     */
    @BeforeEach
    protected void beforeEach()
        {
        m_colRecordedInvocations.clear();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldHaveValidPofConfiguration()
            throws Exception
        {
        doSanityChecks();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Performs the following checks:
     * <ol>
     *     <li>Will log any classes in package {@value #PORTABLE_TYPE_PACKAGE} (included subpackages) that
     *     implement {@link PortableObject}</li> that are <em>NOT</em> defined in {@value #POF_CONFIG}.</li>
     * </ol>
     *
     * @throws Exception if an unexpected error is raised
     */
    protected static void doSanityChecks()
            throws Exception
        {
        Collection<Class<? extends PortableObject>> colPortableTypes  = getPortableTypes();
        Set<String>                                 setPofConfigTypes = loadPofConfiguration();

        if (colPortableTypes.isEmpty())
            {
            fail("No PortableObjects were found in package " + PORTABLE_TYPE_PACKAGE);
            }

        List<String> listNotFound = new ArrayList<>();
        for (Class<? extends PortableObject> aClass : colPortableTypes)
            {
            String sName = aClass.getName();
            if (!setPofConfigTypes.contains(sName))
                {
                listNotFound.add(sName);
                }
            }

        if (!listNotFound.isEmpty())
            {
            StringBuilder sbMsg = new StringBuilder("The following PortableObjects were found on the classpath,"
                                                    + " but were NOT defined in " + POF_CONFIG + ".\n");
            for (String s : listNotFound)
                {
                sbMsg.append('\t').append(s).append('\n');
                }
            System.err.println(sbMsg);
            fail(sbMsg.toString());
            }
        }

    /**
     * Loads the POF configuration from {@value POF_CONFIG} and returns a {@link Set} of Strings representing
     * the configured POF user types.
     *
     * @return a {@link Set} of Strings representing the configured POF user types
     */
    protected static Set<String> loadPofConfiguration()
        {
        Set<String> setConfigPofTypes = new HashSet<>();
        XmlDocument xmlDoc            = XmlHelper.loadXml(
                PofSanityTest.class.getClassLoader().getResourceAsStream(POF_CONFIG));
        //noinspection rawtypes
        Iterator elements = xmlDoc.getRoot().getElement("user-type-list").getElements("user-type");
        while (elements.hasNext())
            {
            XmlElement element    = (XmlElement) elements.next();
            String     sClassName = element.getElement("class-name").getString();

            setConfigPofTypes.add(sClassName);

            try
                {
                // ensure each referenced class can be loaded
                setConfigPofTypes.add(Class.forName(sClassName, true, PofSanityTest.class.getClassLoader()).getName());
                }
            catch (Exception e)
                {
                throw new IllegalStateException("Class not found for pof configuration: \n" + element);
                }
            }

        return setConfigPofTypes;
        }

    /**
     * Return all types implementing {@link PortableObject} in the package {@value PORTABLE_TYPE_PACKAGE}.
     *
     * @return all types implementing {@link PortableObject} in the package {@value PORTABLE_TYPE_PACKAGE}
     *
     * @throws Exception if an error occurs during classpath scanning
     */
    protected static Collection<Class<? extends PortableObject>> getPortableTypes()
            throws Exception
        {
        ClassLoader loader    = PofSanityTest.class.getClassLoader();
        Class<?>    clzAnchor = Class.forName(PORTABLE_TYPE_PACKAGE + ".function.Predicates", false, loader);
        URL         url       = clzAnchor.getProtectionDomain().getCodeSource().getLocation();

        List<String> listClassNames = getClassNames(url);
        List<Class<? extends PortableObject>> listClasses = new ArrayList<>();

        for (String sClassName : listClassNames)
            {
            Class<?> clz = Class.forName(sClassName, false, loader);

            if (isValidPortableType(clz))
                {
                //noinspection unchecked
                listClasses.add((Class<? extends PortableObject>) clz);
                }
            }

        return listClasses;
        }

    /**
     * Return the class names in {@link #PORTABLE_TYPE_PACKAGE} from a class directory or JAR.
     *
     * @param url  the product artifact location
     *
     * @return the class names in the portable type package
     *
     * @throws Exception if the artifact cannot be scanned
     */
    protected static List<String> getClassNames(URL url)
            throws Exception
        {
        Path         pathArtifact = Paths.get(url.toURI());
        String       sPackagePath = PORTABLE_TYPE_PACKAGE.replace('.', '/');
        List<String> listClasses  = new ArrayList<>();

        if (Files.isDirectory(pathArtifact))
            {
            Path pathPackage = pathArtifact.resolve(sPackagePath);
            try (Stream<Path> stream = Files.walk(pathPackage))
                {
                stream.filter(Files::isRegularFile)
                        .map(pathArtifact::relativize)
                        .map(Path::toString)
                        .filter(sName -> sName.endsWith(CLASS_SUFFIX))
                        .map(PofSanityTest::toClassName)
                        .forEach(listClasses::add);
                }
            }
        else
            {
            try (JarFile fileJar = new JarFile(pathArtifact.toFile()))
                {
                fileJar.stream()
                        .filter(entry -> !entry.isDirectory())
                        .map(entry -> entry.getName())
                        .filter(sName -> sName.startsWith(sPackagePath + '/'))
                        .filter(sName -> sName.endsWith(CLASS_SUFFIX))
                        .map(PofSanityTest::toClassName)
                        .forEach(listClasses::add);
                }
            }

        return listClasses;
        }

    /**
     * Convert a class-file path into its binary class name.
     *
     * @param sPath  the class-file path
     *
     * @return the binary class name
     */
    protected static String toClassName(String sPath)
        {
        return sPath.substring(0, sPath.length() - CLASS_SUFFIX.length())
                .replace('/', '.')
                .replace('\\', '.');
        }

    /**
     * Return {@code true} if the provided class implements {@link PortableObject}, is not abstract,
     * and is not a test artifact.
     *
     * @param clz the class to test
     *
     * @return {@code true} if the provided class implements {@link PortableObject}, is not abstract,
     *         and is not a test artifact
     */
    protected static boolean isValidPortableType(Class<?> clz)
        {
        return !Modifier.isAbstract(clz.getModifiers())
               && !clz.getSimpleName().startsWith("Test")
               && PortableObject.class.isAssignableFrom(clz);
        }

    // ----- constants ------------------------------------------------------

    /**
     * POF configuration under test.
     */
    protected static final String POF_CONFIG = "coherence-concurrent-pof-config.xml";

    /**
     * Package to scan {@link PortableObject} classes.
     */
    protected static final String PORTABLE_TYPE_PACKAGE = "com.oracle.coherence.concurrent.executor";

    /**
     * The suffix used by class files.
     */
    protected static final String CLASS_SUFFIX = ".class";

    // ----- data members ---------------------------------------------------

    /**
     * Collection of recorded events emitted during the ser/deser process.
     */
    protected Collection<String> m_colRecordedInvocations = new ArrayList<>();

    /**
     * POF Context containing the configuration under test.
     */
    protected static ConfigurablePofContext s_pofContext;
    }
