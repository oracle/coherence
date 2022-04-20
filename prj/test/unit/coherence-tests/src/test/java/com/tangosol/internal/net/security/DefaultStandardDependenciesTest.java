/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import com.tangosol.util.Resources;
import org.junit.Test;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for DefaultStandardDependencies (security-config element).
 *
 * @author der  2012.1.3
 * @since Coherence 12.1.2
 */
public class DefaultStandardDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default values.
     */
    @Test
    public void testDefaultNoConfig()
        {
        DefaultStandardDependencies deps = new DefaultStandardDependencies();

        deps.validate();
        System.out.println("DefaultStandardDependenciesTest.testDefaultNoConfig:");
        System.out.println(deps.toString());

        // test the default values
        assertDefault(deps);

        // test the clone logic
        DefaultStandardDependencies deps2 = new DefaultStandardDependencies(deps);
        assertCloneEquals(deps, deps2);
        }

    /**
     * Test the (Security) StandardDependencies values set in the default
     * operational configuration file.
     */
    @Test
    public void testStandardDependenciesOperConfigDefaultDisabled()
        {
        String xmlString =
             "<security-config>"
            +  "<enabled system-property=\"tangosol.coherence.security\">false</enabled>"
            +  "<login-module-name>Coherence</login-module-name>"
            +  "<access-controller>"
            +   "<class-name>com.tangosol.net.security.DefaultController</class-name>"
            +    "<init-params>"
            +      "<init-param id=\"1\">"
            +       "<param-type>java.io.File</param-type>"
            +       "<param-value system-property=\"tangosol.coherence.security.keystore\"></param-value>"
            +      "</init-param>"
            +      "<init-param id=\"2\">"
            +       "<param-type>java.io.File</param-type>"
            +       "<param-value system-property=\"tangosol.coherence.security.permissions\"></param-value>"
            +      "</init-param>"
            +    "</init-params>"
            +   "</access-controller>"
            +   "<callback-handler>"
            +    "<class-name/>"
            +   "</callback-handler>"
            + "</security-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        DefaultStandardDependencies deps =
            LegacyXmlStandardHelper.fromXml(xml, new DefaultStandardDependencies());

        assertFalse(deps.isEnabled());
        assertEquals(deps.getLoginModuleName(), "Coherence");
        // not configured if not enabled
        assertNull(deps.getAccessController());
        assertNull(deps.getCallbackHandler());
        assertEquals(deps.isSubjectScoped(), DefaultSecurityDependencies.DEFAULT_SUBJECT_SCOPED);
        assertNull(deps.getIdentityAsserter());
        assertNull(deps.getIdentityTransformer());
        assertEquals(deps.getModel(), DefaultSecurityDependencies.DEFAULT_MODEL);
        }

    /**
     * Test the (Security) StandardDependencies values set in the default
     * operational configuration file when enabled.
     */
    @Test
    public void testStandardDependenciesOperConfigDefaultEnabled() throws Exception
        {
        File   filePermissions = new File(Resources.findFileOrResource("internal/permissions.xml", null).toURI());
        File   fileKeystore    = new File(Resources.findFileOrResource("internal/keystore.jks", null).toURI());
        String xmlString       =
             "<security-config>"
            +  "<enabled system-property=\"tangosol.coherence.security\">true</enabled>"
            +  "<login-module-name>Coherence</login-module-name>"
            +  "<access-controller>"
            +   "<class-name>com.tangosol.net.security.DefaultController</class-name>"
            +    "<init-params>"
            +      "<init-param id=\"1\">"
            +       "<param-type>java.io.File</param-type>"
            +       "<param-value>" + fileKeystore.getAbsolutePath() + "</param-value>"
            +      "</init-param>"
            +      "<init-param id=\"2\">"
            +       "<param-type>java.io.File</param-type>"
            +       "<param-value>" + filePermissions.getAbsolutePath() + "</param-value>"
            +      "</init-param>"
            +    "</init-params>"
            +   "</access-controller>"
            +   "<callback-handler>"
            +    "<class-name/>"
            +   "</callback-handler>"
            + "</security-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        DefaultStandardDependencies deps =
            LegacyXmlStandardHelper.fromXml(xml, new DefaultStandardDependencies());

        assertTrue(deps.isEnabled());
        assertEquals(deps.getLoginModuleName(), "Coherence");
        assertTrue(deps.getAccessController() instanceof com.tangosol.net.security.DefaultController);
        assertNull(deps.getCallbackHandler());
        assertEquals(deps.isSubjectScoped(), DefaultSecurityDependencies.DEFAULT_SUBJECT_SCOPED);
        assertNull(deps.getIdentityAsserter());
        assertNull(deps.getIdentityTransformer());
        assertEquals(deps.getModel(), DefaultSecurityDependencies.DEFAULT_MODEL);
        }

  /**
   * Test the complete (Security) StandardDependencies values being set.
   */
    @Test
    public void testStandardDependenciesComplete() throws Exception
        {
        File   filePermissions = new File(Resources.findFileOrResource("internal/permissions.xml", null).toURI());
        File   fileKeystore    = new File(Resources.findFileOrResource("internal/keystore.jks", null).toURI());
        String xmlString       = "<security-config>"
            + "<enabled>true</enabled>"
            + "<login-module-name>Coherence</login-module-name>"
            + "<access-controller>"
            +   "<class-name>com.tangosol.net.security.DefaultController</class-name>"
            +   "<init-params>"
            +     "<init-param id=\"1\">"
            +        "<param-type>java.io.File</param-type>"
            +       "<param-value>" + fileKeystore.getAbsolutePath() + "</param-value>"
            +     "</init-param>"
            +     "<init-param id=\"2\">"
            +       "<param-type>java.io.File</param-type>"
            +       "<param-value>" + filePermissions.getAbsolutePath() + "</param-value>"
            +     "</init-param>"
            +   "</init-params>"
            + "</access-controller>"
            + "<callback-handler>"
            +   "<class-name>com.tangosol.net.security.SimpleHandler</class-name>"
            +     "<init-params>"
            +       "<init-param id=\"1\">"
            +         "<param-type>java.lang.String</param-type>"
            +         "<param-value>manager</param-value>"
            +       "</init-param>"
            +       "<init-param id=\"2\">"
            +         "<param-type>java.lang.String</param-type>"
            +         "<param-value>password</param-value>"
            +       "</init-param>"
            +       "<init-param id=\"3\">"
            +          "<param-type>java.lang.Boolean</param-type>"
            +          "<param-value>false</param-value>"
            +       "</init-param>"
            +     "</init-params>"
            +  "</callback-handler>"
            +  "<identity-asserter>"
            +     "<class-name>com.tangosol.internal.net.security.TestIdentityAsserter</class-name>"
            +  "</identity-asserter>"
            +  "<identity-transformer>"
            +     "<class-name>com.tangosol.internal.net.security.TestIdentityTransformer</class-name>"
            +  "</identity-transformer>"
            +  "<subject-scope>true</subject-scope>"
            + "</security-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        DefaultStandardDependencies deps = LegacyXmlStandardHelper.fromXml(xml, new DefaultStandardDependencies());

        assertTrue(deps.isEnabled());
        assertEquals(deps.getLoginModuleName(), "Coherence");
        assertTrue(deps.getAccessController() instanceof com.tangosol.net.security.DefaultController);
        assertTrue(deps.getCallbackHandler() instanceof com.tangosol.net.security.SimpleHandler);
        assertTrue(deps.isSubjectScoped());
        assertTrue(deps.getIdentityAsserter() instanceof com.tangosol.internal.net.security.TestIdentityAsserter);
        assertTrue(deps.getIdentityTransformer() instanceof com.tangosol.internal.net.security.TestIdentityTransformer);
        assertEquals(deps.getModel(), DefaultSecurityDependencies.DEFAULT_MODEL);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the default StandardDependencies is set correctly.
     *
     * @param deps  the StandardDependencies object
     */
    protected void assertDefault(StandardDependencies deps)
        {
        DefaultSecurityDependenciesTest.assertDefault(deps);
        assertNull(deps.getAccessController());
        assertNull(deps.getCallbackHandler());
        assertEquals(deps.getLoginModuleName(), DefaultStandardDependencies.DEFAULT_LOGIN_MODULE_NAME);
        }

    /**
     * Assert that the two StandardDependencies are equal.
     *
     * @param deps1  the first StandardDependencies object
     * @param deps2  the second StandardDependencies object
     */
    protected void assertCloneEquals(StandardDependencies deps1, StandardDependencies deps2)
        {
        DefaultSecurityDependenciesTest.assertCloneEquals(deps1, deps2);
        assertEquals(deps1.getAccessController(), deps2.getAccessController());
        assertEquals(deps1.getCallbackHandler(),  deps2.getCallbackHandler());
        assertEquals(deps1.getLoginModuleName(),  deps2.getLoginModuleName());
        }
    }
