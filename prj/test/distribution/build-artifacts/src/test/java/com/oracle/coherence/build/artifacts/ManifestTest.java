/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.build.artifacts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import static org.junit.Assert.assertTrue;


public class ManifestTest
    {

    // ----- test lifecycle ---------------------------------------------

    @Before
    public void setup()
        {
        System.setProperty("coherence.xml.validation.disable", "true");
        }

    @After
    public void teardown()
        {
        System.clearProperty("coherence.xml.validation.disable");
        }

    // ----- test methods -----------------------------------------------

    /**
     * Verify that the required attributes are present in in the manifest.
     **/
    @Test
    public void testRequiredAttributes()
            throws IOException 
        {
        String sCoherenceBOM = TEST_MVN_REPO + File.separator + ARTIFACT_GROUP.replace(".", File.separator) + File.separator +
                COHERENCE_BOM + File.separator + ARTIFACT_VERSION + File.separator + COHERENCE_BOM + "-" + ARTIFACT_VERSION + ".pom";

        XmlElement dependencies = null;

        try (FileInputStream fis = new FileInputStream(sCoherenceBOM))
            {
            XmlDocument xmlDoc = XmlHelper.loadXml(fis);

            for (Object obj : xmlDoc.getElementList())
                {
                XmlElement projectElem = (XmlElement) obj;
                
                if (("/project/dependencyManagement").equals(projectElem.getAbsolutePath()))
                    {
                    dependencies = XmlHelper.findElement(projectElem, "dependencies");
                    }
                }
            }

        assertTrue(dependencies != null);

        List<String> sBomDependencies = new ArrayList<>();

        for (Object obj : dependencies.getElementList())
            {
            XmlElement dependency = (XmlElement) obj;

            XmlElement artifactId = XmlHelper.findElement(dependency, "artifactId");

            sBomDependencies.add(artifactId.getString());
            }

        for (String s : sBomDependencies)
            {
            String jarPath =  TEST_MVN_REPO + File.separator + ARTIFACT_GROUP.replace(".", File.separator) + File.separator +
                    s + File.separator + ARTIFACT_VERSION + File.separator + s + "-" + ARTIFACT_VERSION + ".jar";

            try (JarFile jar = new JarFile(jarPath))
                {
                System.out.println("Verifying Manifest of a jar: " + jarPath);

                Manifest manifest = jar.getManifest();
                Attributes attributes = manifest.getMainAttributes();

                Attributes.Name[] attrNames = attributes.keySet().toArray(new Attributes.Name[attributes.size()]);
                List<String>       attrList = new ArrayList<>();

                for (Attributes.Name name : attrNames)
                    {
                    attrList.add(name.toString());
                    }

                for (String attr : Arrays.asList(REQUIRED_ATTRS))
                    {
                    assertTrue(jarPath + " missing required attribute: " + attr, attrList.contains(attr));
                    }
                }
            }
        }

    // ----- data members ---------------------------------------------

    private static final String COHERENCE_BOM = "coherence-bom";

    private static final String TEST_MVN_REPO = System.getProperty("test.maven.repository");

    private static final String ARTIFACT_GROUP = System.getProperty("coherence.groupId");
    
    private static final String ARTIFACT_VERSION = System.getProperty("project.version");

    private static final String[] REQUIRED_ATTRS =
            new String[] {"Implementation-Build", "Implementation-Title", "Implementation-Vendor", "Implementation-Version"};
    }
