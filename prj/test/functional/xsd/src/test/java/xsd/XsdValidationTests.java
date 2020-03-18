/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package xsd;

import static org.junit.Assert.fail;

import org.junit.Test;

/**
* A collection of funtional tests that validate the xml using the
* scheme definition files.
*
* @author der 10/020/2011
*/
public class XsdValidationTests
    {
    // ----- test methods ---------------------------------------------------

   /**
    * Test the system-property attribute for each element with the following files
    * that have been modified to have the system-property added.
    *
    * @throws Exception  if XML does not validate against the XSD
    */
    @Test
    public void testDefaultFiles()
        throws Exception
        {
        XmlValidator.validate("../../../coherence-core/src/main/resources/coherence-cache-config.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/coherence-pof-config.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/management-config.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/tangosol-coherence-override-dev.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/tangosol-coherence-override-eval.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/tangosol-coherence-override-prod.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/tangosol-coherence.xml");
        }

   /**
    * Test the system-property attribute for each element with the following files
    * that have been modified to have the system-property added.
    *
    * @throws Exception  if XML does not validate against the XSD
    */
    @Test
    public void testSystemPropertyAttrib()
        throws Exception
        {
        XmlValidator.validate("system-property-coherence-cache-config.xml");
        XmlValidator.validate("system-property-tangosol-coherence.xml");
        XmlValidator.validate("system-property-tangosol-coherence-override.xml");
        }

    /**
     * Test fix to replicated and optimistic-scheme to enforce no
     * read-write-backing-map-scheme (COH-5916).
     *
     * @throws Exception if XML does not validate against the XSD
     */
    @Test
    public void testCacheConfigCoh5916Valid()
            throws Exception
        {
        XmlValidator.validate("cache-config-coh-5916-1.xml");
        }

    /**
     * Test invalid use of <partition> element in replicated-scheme (COH-5916).
     *
     * @throws Exception if XML does not validate against the XSD
     */
    @Test
    public void testCacheConfigCoh5916ReplicatedInvalidPartition()
            throws Exception
        {
        try
            {
            XmlValidator.validate("cache-config-coh-5916-2.xml");
            fail("Failed to throw expected exception in testCacheConfigCoh5916ReplicatedInvalidPartition");
            }
        catch (java.io.IOException e)
            {
            // expected exception
            }
        }

    /**
     * Test invalid use of RWBM in replicated-scheme (COH-5916).
     *
     * @throws Exception if XML does not validate against the XSD
     */
    @Test
    public void testCacheConfigCoh5916ReplicatedInvalidRWBM()
            throws Exception
        {
        try
            {
            XmlValidator.validate("cache-config-coh-5916-3.xml");
            fail("Failed to throw exception in testCacheConfigCoh5916ReplicatedInvalidRWBM");
            }
        catch (java.io.IOException e)
            {
            // expected exception
            }
        }

    /**
     * Test invalid use of <partition> element in optimistic-scheme (COH-5916).
     *
     * @throws Exception if XML does not validate against the XSD
     */
    @Test
    public void testCacheConfigCoh5916OptimisticInvalidPartition()
            throws Exception
        {
        try
            {
            XmlValidator.validate("cache-config-coh-5916-4.xml");
            fail("Failed to throw expected exception in testCacheConfigCoh5916OptimisticInvalidPartition");
            }
        catch (java.io.IOException e)
            {
            // expected exception
            }
        }

    /**
     * Test invalid use of RWBM in replicated-scheme (COH-5916).
     *
     * @throws Exception if XML does not validate against the XSD
     */
    @Test
    public void testCacheConfigCoh5916OptimisticInvalidRWBM()
            throws Exception
        {
        try
            {
            XmlValidator.validate("cache-config-coh-5916-5.xml");
            fail("Failed to throw expected exception in testCacheConfigCoh5916OptimisticInvalidRWBM");
            }
        catch (java.io.IOException e)
            {
            // expected exception
            }
        }

    /**
     * Test to validate the XML changes done for COH12077 to support password-providers
     *
     * @throws Exception if XML does not validate against the XSD
     */
    @Test
    public void testPasswordProviderXML()
            throws Exception
        {
        XmlValidator.validate("tangosol-coherence-override-password-provider.xml");
        }
    }
