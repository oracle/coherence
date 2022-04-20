/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.queryplus;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author jk  2014.01.06
 */
public class HelpStatementBuilderTest
    {
    @Test
    public void shouldHaveDescription()
            throws Exception
        {
        assertThat(m_builder.getDescription(), is(notNullValue()));
        }

    @Test
    public void shouldHaveSyntaxText()
            throws Exception
        {
        assertThat(m_builder.getSyntax(), is(notNullValue()));
        }

    @Before
    public void setup()
        {
        m_builder = new HelpStatementBuilder();
        }

    private HelpStatementBuilder m_builder;
    }
