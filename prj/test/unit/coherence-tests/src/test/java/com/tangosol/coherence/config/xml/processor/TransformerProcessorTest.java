/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link TransformerProcessor}.
 *
 * @author rlubke
 * @since 12.2.1.4
 */
public class TransformerProcessorTest
        extends AbstractClassSchemeProcessorTest
    {
    // ----- test methods ---------------------------------------------------

    /**
     * Test validates the creation a transformer without any declarative configuration.
     */
    @Test
    public void shouldCreateTransformerNoParams()
        {
        String sXml = "<transformer>\n"
                    + "  <class-scheme>\n"
                    + "    <class-name>com.tangosol.util.extractor.IdentityExtractor</class-name>\n"
                    + "  </class-scheme>\n"
                    + "</transformer>";

        assertThat(createAndInvokeBuilder(sXml, IdentityExtractor.class), is(IdentityExtractor.INSTANCE));
        }

    /**
     * Test validates the creation a transformer with declarative configuration.
     */
    @Test
    public void shouldCreateTransformerWithParams()
        {
        String sXml = "<transformer>\n"
                    + "  <class-scheme>\n"
                    + "    <class-name>com.tangosol.util.extractor.UniversalExtractor</class-name>\n"
                    + "    <init-params>\n"
                    + "      <init-param>\n"
                    + "        <param-type>java.lang.String</param-type>\n"
                    + "        <param-value>from-config</param-value>\n"
                    + "      </init-param>\n"
                    + "    </init-params>\n"
                    + "  </class-scheme>\n"
                    + "</transformer>";

        assertThat(createAndInvokeBuilder(sXml, UniversalExtractor.class),
                   is(new UniversalExtractor<>("from-config")));
        }

    /**
     * Test validates the creation a transformer with via a custom factory.
     */
    @Test
    public void shouldCreateTransformerWithFactory()
        {
        String sXml = "<transformer>\n"
                      + "  <class-scheme>\n"
                      + "    <class-factory-name>" + TransformerFactory.class.getName() + "</class-factory-name>\n"
                      + "    <method-name>create</method-name>\n"
                      + "  </class-scheme>\n"
                      + "</transformer>";

        assertThat(createAndInvokeBuilder(sXml, ValueExtractor.class),
                   is(new UniversalExtractor<>("from-factory")));
        }

    // ----- inner class: TransformerFactory --------------------------------

    /**
     * Simple factory.
     */
    public static final class TransformerFactory
        {
        public static ValueExtractor create()
            {
            return new UniversalExtractor("from-factory");
            }
        }
    }
