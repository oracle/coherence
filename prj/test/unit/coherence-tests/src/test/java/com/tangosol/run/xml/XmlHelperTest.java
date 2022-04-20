/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * XmlHelper unit test.
 */
public class XmlHelperTest
    {
    /**
     * Ensure that encodings in Xml occur correctly.
     */
    @Test
    public void testEncodeAttribute()
        {
        assertEquals("&#x1F;", XmlHelper.encodeAttribute(String.valueOf((char) 0x1F), '"'));
        }

    /**
     * A helper method to assert that {@link XmlHelper#overrideElement(XmlElement, XmlElement)}
     * works as expected.
     *
     * @param sBase      the base xml {@link String}
     * @param sOverride  the override xml {@link String}
     * @param sExpected  the expected result of the override.
     */
    protected void assertOverrideAsExpected(String sBase, String sOverride, String sExpected)
        {
        XmlElement xmlBase     = XmlHelper.loadXml(sBase);
        XmlElement xmlOverride = XmlHelper.loadXml(sOverride);
        XmlElement xmlExpected = XmlHelper.loadXml(sExpected);

        XmlHelper.overrideElement(xmlBase, xmlOverride);

        assertEquals(xmlExpected, xmlBase);
        }

    /**
     * Ensure that elements containing a single value, without children,
     * are overridden correctly with elements containing children.
     */
    @Test
    public void testOverrideElementCOH5655()
        {
        String sBase =
            "<service-guardian><service-failure-policy>exit-cluster</service-failure-policy></service-guardian>";

        String sOverride =
            "<service-guardian><service-failure-policy>"
            + "<instance><class-name>com.ml.cad.coherence.admin.guardian.CadCustomServiceGuardianPolicy</class-name></instance>"
            + "</service-failure-policy></service-guardian>";

        String sExpected = sOverride;

        assertOverrideAsExpected(sBase, sOverride, sExpected);
        }

    /**
     * Ensure that a base element is left unchanged when there is nothing
     * to override.
     */
    @Test
    public void testOverrideElementNonOverlapping()
        {
        String sBase     = "<a>a1</a>";
        String sOverride = "<b>b1</b>";
        String sExpected = sBase;

        assertOverrideAsExpected(sBase, sOverride, sExpected);
        }

    /**
     * Ensure that non-overlapping child elements are merged
     */
    @Test
    public void testMergingNonOverlappingElements()
        {
        String sBase     = "<root><a>a1</a></root>";
        String sOverride = "<root><b>b1</b></root>";
        String sExpected = "<root><a>a1</a><b>b1</b></root>";

        assertOverrideAsExpected(sBase, sOverride, sExpected);
        }

    /**
     * Ensure that a nested child element overrides another
     */
    @Test
    public void testOverrideNestedChild()
        {
        String sBase = "<root><a><b><c>c1</c><d>d1</d></b><e>e1</e></a><f>f1</f></root>";

        // changes d1 to d2
        String sOverride = "<root><a><b><d>d2</d></b></a></root>";
        String sExpected = "<root><a><b><c>c1</c><d>d2</d></b><e>e1</e></a><f>f1</f></root>";

        assertOverrideAsExpected(sBase, sOverride, sExpected);
        }

    /**
     * Ensure that multiple nested children overrides others
     */
    @Test
    public void testOverrideNestedChildren()
        {
        String sBase = "<root><a><b><c>c1</c><d>d1</d></b><e>e1</e></a><f>f1</f></root>";

        // changes d1 to d2 and f1 to f2
        String sOverride = "<root><a><b><d>d2</d></b></a><f>f2</f></root>";
        String sExpected = "<root><a><b><c>c1</c><d>d2</d></b><e>e1</e></a><f>f2</f></root>";

        assertOverrideAsExpected(sBase, sOverride, sExpected);
        }

    /**
     * Ensure that an empty element content is replaced with override child content
     */
    @Test
    public void testOverrideElementEmptyBaseContent()
        {
        String sBase     = "<root><a></a></root>";
        String sOverride = "<root><a>b1</a></root>";
        String sExpected = sOverride;

        assertOverrideAsExpected(sBase, sOverride, sExpected);
        }

    /**
     * Ensure that child element content is replaced with override child content
     */
    @Test
    public void testOverrideElementBaseContent()
        {
        String sBase     = "<root><a>a1</a></root>";
        String sOverride = "<root><a>b1</a></root>";
        String sExpected = sOverride;

        assertOverrideAsExpected(sBase, sOverride, sExpected);
        }

    /**
     * Ensure that an override child element containing only content will replace
     * a child element with children.
     */
    @Test
    public void testOverrideElementChildStructureWithChildContent()
        {
        String sBase     = "<root><a><b>b1</b><b>b2</b><c>c1</c></a></root>";
        String sOverride = "<root><a>a</a></root>";
        String sExpected = sOverride;

        assertOverrideAsExpected(sBase, sOverride, sExpected);
        }

    /**
     * Ensure that an override child with children replaces a child with only
     * content.
     */
    @Test
    public void testOverrideElementChildContentWithChildStructure()
        {
        String sBase     = "<root><a>a</a></root>";
        String sOverride = "<root><a><b>b1</b><c>c1</c></a></root>";
        String sExpected = sOverride;

        assertOverrideAsExpected(sBase, sOverride, sExpected);
        }

    /**
     * Ensure that an override child with id'd children replaces a child with
     * only content.
     */
    @Test
    public void testOverrideElementChildContentWithIdsInChildStructure()
        {
        String sBase     = "<root><a>a</a></root>";
        String sOverride = "<root><a><b id=\"b1\">b1</b><b id=\"b2\">b2</b><c>c1</c></a></root>";
        String sExpected = sOverride;

        assertOverrideAsExpected(sBase, sOverride, sExpected);
        }
    }
