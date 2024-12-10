/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.processor;

import com.tangosol.util.UniversalManipulator;

import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.processor.NumberIncrementor;

import java.text.NumberFormat;
import java.text.ParseException;

import java.util.Arrays;

/**
 * {@link ProcessorFactory} that can be used to create a
 * {@link NumberIncrementor}.
 *
 * @author vp 2011.07.11
 * @author ic 2011.07.14
 */
public class NumberIncrementorFactory
        implements ProcessorFactory
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct an instance of <tt>NumberIncrementorFactory</tt>.
     *
     * @param fPostIncrement  pass true to create <tt>NumberIncrementor</tt>s
     *                        that will return value as it was before it was
     *                        incremented, or pass false to create
     *                        <tt>NumberIncrementor</tt>s that will return
     *                        the value as it is after it is incremented
     */
    public NumberIncrementorFactory(boolean fPostIncrement)
        {
        m_fPostIncrement = fPostIncrement;
        }

    //----- ProcessorFactory implementation ---------------------------------

    /**
     * {@inheritDoc}
     */
    public InvocableMap.EntryProcessor getProcessor(String... asArgs)
        {
        switch(asArgs.length)
            {
            case 1:
                return new NumberIncrementor((String) null, toNumber(asArgs[0]),
                        m_fPostIncrement);

            case 2:
                return new NumberIncrementor(new UniversalManipulator(asArgs[0]),
                        toNumber(asArgs[1]), m_fPostIncrement);
                
            default:
                throw new IllegalArgumentException("NumberIncrementorFactory "
                        + "cannot be constructed with given arguments: "
                        + Arrays.toString(asArgs));
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Convert the given string into a number.
     *
     * @param sNumber  number as a string
     *
     * @return numeric representation of the string
     */
    protected static Number toNumber(String sNumber)
        {
        try
            {
            return NumberFormat.getInstance().parse(sNumber);
            }
        catch (ParseException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Whether to create NumberIncrementors that will return the value before
     * it was incremented ("post-increment") or after it is incremented
     * ("pre-increment").
     */
    private boolean m_fPostIncrement;
    }