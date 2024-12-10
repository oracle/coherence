/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.config.TestSerializableHelper;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the {@link SimpleParameterList}.
 *
 * @author pfm 2013.09.24
 */
public class SimpleParameterListTest
    {
    /**
     * Test basic get/set.
     */
    @Test
    public void testGetSet()
        {
        validate(createAndPopulate());
        }

    /**
     * Test POF serialization.
     */
    @Test
    public void testPof()
        {
        validate(TestSerializableHelper.<SimpleParameterList>convertPof(createAndPopulate()));
        }

    /**
     * Test ExternalizableLite serialization.
     */
    @Test
    public void testExternalizableLite()
        {
        validate(TestSerializableHelper.<SimpleParameterList>convertEL(createAndPopulate()));
        }

    // ----- helpers  -------------------------------------------------------

    /*
     * Create and populate the SimpleParameterList.
     *
     * @return the populated SimpleParameterList
     */
    protected SimpleParameterList createAndPopulate()
        {
        SimpleParameterList list1 = new SimpleParameterList();
        list1.add(new Parameter("greeting", String.class, "Gudday Mate"));
        list1.add(new Parameter("farewell", String.class, "See Ya"));

        return list1;
        }

    /*
    * Validate the SimpleParameterList.
    *
    * @param  list the populated SimpleParameterList
    */
    protected void validate(SimpleParameterList list)
        {
        Iterator<Parameter> iter = list.iterator();

        Parameter p1 = iter.next();
        assertEquals(p1.getName(), "greeting");
        assertEquals(p1.evaluate(new NullParameterResolver()).get(), "Gudday Mate");

        Parameter p2 = iter.next();
        assertEquals(p2.getName(), "farewell");
        assertEquals(p2.evaluate(new NullParameterResolver()).get(), "See Ya");
        }
    }
