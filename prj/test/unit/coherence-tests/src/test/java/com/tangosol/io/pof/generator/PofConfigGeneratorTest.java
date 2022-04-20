/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.generator;

import com.tangosol.io.pof.generator.PofConfigGenerator.Dependencies;
import com.tangosol.io.pof.generator.PofConfigGenerator.TypeIdIterator;
import com.tangosol.io.pof.generator.data.Atom;
import com.tangosol.io.pof.generator.data.Electron;
import com.tangosol.io.pof.generator.data.Neutron;
import com.tangosol.io.pof.generator.data.Nucleus;
import com.tangosol.io.pof.generator.data.Proton;

import com.tangosol.run.xml.SimpleDocument;
import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.regex.Pattern;

import static junit.framework.Assert.assertEquals;

/**
 * A test class to exhaust the functionality provided by
 * {@link PofConfigGenerator}.
 *
 * @author hr  2012.07.04
 *
 * @since Coherence 12.1.2
 */
public class PofConfigGeneratorTest
    {

    /**
     * Test the {@link TypeIdIterator} ensuring reserved blocks are respected
     * and allocations occur either side of these reservations.
     */
    @Test
    public void testIterator()
        {
        PofConfigGenerator generator = new PofConfigGenerator(new Dependencies());
        int[][] aanRanges = new int[][]{{1024, 32}, {1280, 32}, {1536, 32}, {2048, 32}};
        Set<Integer> listReserved = new HashSet<Integer>(2048);
        for (int i = 0, j = 0; i <= 2048; i += j < aanRanges.length && i >= aanRanges[j][0] ? aanRanges[j++][1] : 1)
            {
            listReserved.add(i);
            }
        TypeIdIterator iterTypeId = generator.new TypeIdIterator(listReserved);
        for (int i = 0, j = 0, a = 0; i < 128; ++i)
            {
            if (j < aanRanges.length && i % 31 == 0)
                {
                a = aanRanges[j++][0];
                }
            int n = (Integer) iterTypeId.next();
            assertEquals(++a, n);
            }
        }

    /**
     * Test the POF config generation with a previously defined user type of
     * Neutron and against a specified package.
     * <p>
     * The generated XML is checked to ensure the expected number of user
     * types are present and previously defined user types exist with their
     * original type id.
     *
     * @throws IOException
     */
    @Test
    @Ignore
    public void testGeneratePackage() throws IOException
        {
        Class[] aclzAppObjects         = new Class[]
            {
            Neutron.class,
            Proton.class,
            Nucleus.class,
            Electron.class,
            Atom.class
            };
        Map<Integer, String> mapTypes  = createMapTypes(aclzAppObjects[0]);
        XmlElement           xmlResult = runTest(aclzAppObjects, mapTypes, "com.tangosol.io.pof.generator.data");

        assertResultingXml(xmlResult, mapTypes, aclzAppObjects.length);
        }

    /**
     * Test the POF config generation when no package is specified. This
     * generally is a more expensive test requiring the reading of all class
     * files in the class loader determined by {@link PofConfigGenerator}.
     * This generally contains a JAR with the specified classes in this test
     * and the previously defined POF configuration and the location of
     * coherence classes.
     * <p>
     * The generated XML is checked to ensure the expected number of user
     * types are present and previously defined user types exist with their
     * original type id.
     *
      * @throws IOException
     */
    @Test
    @Ignore
    public void testGenerateNoPackage() throws IOException
        {
        Class[] aclzAppObjects    = new Class[]
            {
            Neutron.class,
            Proton.class,
            Nucleus.class,
            Electron.class,
            Atom.class
            };
        Map<Integer, String> mapTypes  = createMapTypes(aclzAppObjects[0]);
        XmlElement           xmlResult = runTest(aclzAppObjects, mapTypes, "");

        assertResultingXml(xmlResult, mapTypes, aclzAppObjects.length);
        }

    /**
     * Test the generation of POF configuration in three generations. The
     * test validates the accommodation for the previous generation.
     * <p>
     * The generated XML is checked to ensure the expected number of user
     * types are present and previously defined user types exist with their
     * original type id.
     *
     * @throws IOException
     */
    @Test
    @Ignore
    public void testGenerations() throws IOException
        {
        Class[] aclzAppObjectsGenOne = new Class[]
            {
            Neutron.class,
            };
        Class[] aclzAppObjectsGenTwo = new Class[]
            {
            Neutron.class,
            Proton.class,
            Nucleus.class,
            };
        Class[] aclzAppObjectsGenThree = new Class[]
            {
            Neutron.class,
            Proton.class,
            Nucleus.class,
            Electron.class,
            Atom.class
            };
        Map<Integer, String> mapTypesGenOne    = createMapTypes(new Class[0]);
        Map<Integer, String> mapTypesGenTwo    = createMapTypes(aclzAppObjectsGenOne);
        Map<Integer, String> mapTypesGenThree  = createMapTypes(aclzAppObjectsGenTwo);
        XmlElement           xmlResultGenOne   = runTest(aclzAppObjectsGenOne, mapTypesGenOne, "com.tangosol.io.pof.generator.data");
        XmlElement           xmlResultGenTwo   = runTest(aclzAppObjectsGenTwo, mapTypesGenTwo, "com.tangosol.io.pof.generator.data");
        XmlElement           xmlResultGenThree = runTest(aclzAppObjectsGenThree, mapTypesGenThree, "com.tangosol.io.pof.generator.data");

        assertResultingXml(xmlResultGenOne, mapTypesGenOne, aclzAppObjectsGenOne.length);
        assertResultingXml(xmlResultGenTwo, mapTypesGenTwo, aclzAppObjectsGenTwo.length);
        assertResultingXml(xmlResultGenThree, mapTypesGenThree, aclzAppObjectsGenThree.length);
        }

    /**
     * Similar to {@link #testGenerations()} except the
     * {@link Dependencies#isInclude()} is set to true resulting in a
     * reference to the previously generated POF configuration and inclusion
     * of newly discovered user types <b>only</b>.
     * <p>
     * The generated XML is checked to ensure the expected number of user
     * types are present and previously defined user types exist with their
     * original type id.
     *
     * @throws IOException
     */
    @Test
    @Ignore
    public void testDeltaGenerations() throws IOException
        {
        Class[] aclzAppObjectsGenOne = new Class[]
            {
            Neutron.class,
            };
        Class[] aclzAppObjectsGenTwo = new Class[]
            {
            Neutron.class,
            Proton.class,
            Nucleus.class,
            };
        Class[] aclzAppObjectsGenThree = new Class[]
            {
            Neutron.class,
            Proton.class,
            Nucleus.class,
            Electron.class,
            Atom.class
            };
        List<String>         listImports       = getAllowedImports();
        Dependencies         deps              = new Dependencies().addPackage("com.tangosol.io.pof.generator.data").setInclude(true);
        Map<Integer, String> mapTypesGenOne    = createMapTypes(new Class[0]);
        Map<Integer, String> mapTypesGenTwo    = createMapTypes(aclzAppObjectsGenOne);
        Map<Integer, String> mapTypesGenThree  = createMapTypes(aclzAppObjectsGenTwo);
        XmlElement           xmlResultGenOne   = runTest(aclzAppObjectsGenOne, mapTypesGenOne, deps);
        XmlElement           xmlResultGenTwo   = runTest(aclzAppObjectsGenTwo, mapTypesGenTwo, deps);
        XmlElement           xmlResultGenThree = runTest(aclzAppObjectsGenThree, mapTypesGenThree, deps);

        assertResultingXml(xmlResultGenOne, mapTypesGenOne, aclzAppObjectsGenOne.length, listImports);
        assertResultingXml(xmlResultGenTwo, mapTypesGenOne, aclzAppObjectsGenTwo.length - aclzAppObjectsGenOne.length, listImports);
        assertResultingXml(xmlResultGenThree, mapTypesGenOne, aclzAppObjectsGenThree.length - aclzAppObjectsGenTwo.length, listImports);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Perform assertions on resulting XML.
     *
     * @param xmlGeneratedPof  the generated xml
     * @param mapTypes         the previously defined user types
     * @param cUserTypes       the total number of eventual user types
     */
    protected void assertResultingXml(XmlElement xmlGeneratedPof, Map<Integer, String> mapTypes, int cUserTypes)
        {
        assertResultingXml(xmlGeneratedPof, mapTypes, cUserTypes, Arrays.asList("coherence-pof-config.xml"));
        }

    /**
     * Perform assertions on resulting XML.
     *
     * @param xmlGeneratedPof  the generated xml
     * @param mapTypes         the previously defined user types
     * @param cUserTypes       the total number of eventual user types
     * @param listImports      the expected imports which can be regular
     *                         expressions
     */
    protected void assertResultingXml(XmlElement xmlGeneratedPof, Map<Integer, String> mapTypes, int cUserTypes, List<String> listImports)
        {
        XmlElement   xmlUserTypes    = xmlGeneratedPof.getElement("user-type-list");

        presentIncludes: for (Iterator<XmlElement> iterXmlIncludes = xmlUserTypes.getElements("include"); iterXmlIncludes.hasNext(); )
            {
            String sInclude = iterXmlIncludes.next().getString();
            for (String sPossible : listImports)
                {
                if (Pattern.matches(sPossible, sInclude))
                    {
                    continue presentIncludes;
                    }
                }
            throw new AssertionError("The following include was not expected: " + sInclude);
            }

        int cActualUserTypes = 0;
        int cMatches         = 0;
        for (Iterator<XmlElement> iterUserTypes = xmlUserTypes.getElements("user-type"); iterUserTypes.hasNext(); ++cActualUserTypes)
            {
            XmlElement xmlUserType     = iterUserTypes.next();
            int        nTypeId         = xmlUserType.getSafeElement("type-id").getInt();
            String     sClassName      = xmlUserType.getSafeElement("class-name").getString();
            String     sPotentialMatch = mapTypes.get(nTypeId);

            if (sPotentialMatch != null && sPotentialMatch.equals(sClassName))
                {
                ++cMatches;
                }
            }
        assertEquals("The pre-defined user type was not propagated to the generated pof configuration",
                mapTypes.size(), cMatches);
        assertEquals("Unexpected number user types", cUserTypes, cActualUserTypes);
        }

    /**
     * Executes a test based on the provided Class[] of objects
     * ({@literal aclzAppObjects}), the map of previously defined user types
     * ({@literal mapTypes}) and a base package to scan for resources
     * ({@literal sBasePackage}).
     *
     * @param aclzAppObjects  the application objects
     * @param mapTypes        previously defined user types
     * @param sBasePackage    base package to start scanning from
     *
     * @return the generated XML pof config
     *
     * @throws IOException
     */
    protected XmlElement runTest(Class[] aclzAppObjects, Map<Integer, String> mapTypes, String sBasePackage) throws IOException
        {
        return runTest(aclzAppObjects, mapTypes, new Dependencies().addPackage(sBasePackage));
        }

    /**
     * Executes a test based on the provided Class[] of objects
     * ({@literal aclzAppObjects}), the map of previously defined user types
     * ({@literal mapTypes}) and a {@link Dependencies} object.
     * <p>
     * The {@link Dependencies} object is merged with the dependencies object
     * that would be used if one was not provided resulting in a new object.
     * Any non null values provided will override other values.
     *
     * @param aclzAppObjects  the application objects
     * @param mapTypes        previously defined user types
     * @param deps            {@link Dependencies} to override the default
     *                        dependencies determined by this test
     *
     * @return the generated XML pof config
     *
     * @throws IOException
     */
    protected XmlElement runTest(Class[] aclzAppObjects, Map<Integer, String> mapTypes, Dependencies deps) throws IOException
        {
        File fileOrigPofConfig = null;
        File fileJar           = null;
        File fileOutput        = null;
        File fileGeneratedPof  = null;
        try
            {
            // create pof configuration file
            XmlDocument xmlConfig         = createPofConfig(mapTypes);
                        fileOrigPofConfig = File.createTempFile("pof-config-generated-", ".xml");
                        fileOutput        = fileOrigPofConfig.getParentFile();
            xmlConfig.writeXml(new PrintWriter(fileOrigPofConfig), true);

            PofConfigGenerator generator = createPofConfigGenerator(merge(deps, new Dependencies()
                    .setPofConfig(fileOrigPofConfig.getName())
                    .setOutputPath(fileOutput.getAbsolutePath())));

            generator.generate();

            String sWrittenPath     = generator.getWrittenPath();
                   fileGeneratedPof =  new File(sWrittenPath);

            return getNewPofConfig(sWrittenPath);
            }
        finally
            {
            if (fileOrigPofConfig != null)
                {
                fileOrigPofConfig.delete();
                }
            if (fileJar != null)
                {
                fileJar.delete();
                }
            if (fileOutput != null)
                {
                fileOutput.delete();
                }
            if (fileGeneratedPof != null)
                {
                fileGeneratedPof.delete();
                }
            }
        }

    /**
     * Merge both left and right {@link Dependencies} resulting in a new
     * {@link Dependencies} object.
     * <p>
     * <b>Note:</b> the {@literal right} parameter can not be null.
     *
     * @param left   lhs dependencies used to override rhs
     * @param right  rhs dependencies which can not be null
     *
     * @return a new merged dependency object
     */
    protected Dependencies merge(Dependencies left, Dependencies right)
        {
        return left == null ? right.clone() :
            new Dependencies().setOutputPath(left.getOutputPath() == null ? right.getOutputPath() : left.getOutputPath())
                .setPackages(left.getPackages().isEmpty() ? right.getPackages() : left.getPackages())
                .setPathRoot(left.getPathRoot() == null || left.getPathRoot().isEmpty() ? right.getPathRoot() : left.getPathRoot())
                .setPofConfig(left.getPofConfig() == null ? right.getPofConfig() : left.getPofConfig())
                .setInclude(left.isInclude() | right.isInclude());
        }

    /**
     * Based on the provided Class[] derive a map of user types starting with
     * a type id of 1000.
     *
     * @param aclzPredefined  the Class[] of predefined types
     *
     * @return map of predefined user types
     */
    protected Map<Integer, String> createMapTypes(Class...aclzPredefined)
        {
        Map<Integer, String> mapTypes = new LinkedHashMap<Integer, String>(aclzPredefined.length);
        for (int of = 1000, i = 0; i < aclzPredefined.length; ++i)
            {
            mapTypes.put(i + of, aclzPredefined[i].getName());
            }
        return mapTypes;
        }

    /**
     * Create a POF configuration based on the provided user types.
     *
     * @param mapTypes  map of user types to create pof config from
     *
     * @return the created pof configuration
     */
    protected XmlDocument createPofConfig(Map<Integer, String> mapTypes)
        {
        XmlDocument xmlConfig    = (XmlDocument) EMPTY_POF_CONFIG.clone();
        XmlElement  xmlUserTypes = xmlConfig.ensureElement("user-type-list");
        XmlElement  xmlInclude   = xmlUserTypes.addElement("include");

        xmlInclude.setString("coherence-pof-config.xml");

        for (Map.Entry<Integer, String> entry : mapTypes.entrySet())
            {
            XmlElement xmlUserType  = xmlUserTypes.addElement("user-type");
            XmlElement xmlTypeId    = xmlUserType.ensureElement("type-id");
            XmlElement xmlClassName = xmlUserType.ensureElement("class-name");

            xmlTypeId.setInt(entry.getKey());
            xmlClassName.setString(entry.getValue());
            }

        return xmlConfig;
        }

    protected PofConfigGenerator createPofConfigGenerator(Dependencies deps)
        {
        return new PofConfigGenerator(deps);
        }

    protected XmlElement getNewPofConfig(String sWrittenPath)
            throws IOException
        {
        return XmlHelper.loadXml(new FileInputStream(new File(sWrittenPath)));
        }

    protected List<String> getAllowedImports()
        {
        return Arrays.asList("pof-config-generated-.*\\.xml");
        }

    // ----- constants ------------------------------------------------------

    /**
     * A template to base pof config creation on.
     */
    private static final XmlDocument EMPTY_POF_CONFIG;

    static
        {
        // create the pof config skeleton
        XmlDocument xmlConfig;
        try
            {
            xmlConfig = new SimpleParser(false).parseXml(
                "<pof-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "            xmlns=\"http://xmlns.oracle.com/coherence/coherence-pof-config\"\n" +
                "            xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-pof-config coherence-pof-config.xsd\">" +
                "</pof-config>");
            }
        catch (IOException e)
            {
            xmlConfig = new SimpleDocument("pof-config");
            }
        EMPTY_POF_CONFIG = xmlConfig;
        }
    }