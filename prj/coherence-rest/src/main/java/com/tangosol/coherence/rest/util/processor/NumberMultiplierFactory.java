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

import com.tangosol.util.processor.NumberMultiplier;

import java.text.NumberFormat;
import java.text.ParseException;

import java.util.Arrays;

/**
 * {@link ProcessorFactory} that can be used to create a
 * {@link NumberMultiplier}.
 *
 * @author vp 2011.07.11
 * @author ic 2011.07.14
 */
public class NumberMultiplierFactory
        implements ProcessorFactory
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct an instance of <tt>NumberMultiplierFactory</tt>.
     *
     * @param fPostFactor  pass true to create <tt>NumberMultiplier</tt>s
     *                     that will return value as it was before it was
     *                     multiplied, or pass false to create
     *                     <tt>NumberMultiplier</tt>s that will return the
     *                     value as it is after it is multiplied
     */
    public NumberMultiplierFactory(boolean fPostFactor)
        {
        m_fPostFactor = fPostFactor;
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
                return new NumberMultiplier((String) null, toNumber(asArgs[0]),
                        m_fPostFactor);

            case 2:
                return new NumberMultiplier(new UniversalManipulator(asArgs[0]),
                        toNumber(asArgs[1]), m_fPostFactor);
                
            default:
                throw new IllegalArgumentException("NumberMultiplierFactory "
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
     * Whether to create NumberMultipliers that will return the value before
     * it was multiplied ("post-factor") or after it is multiplied
     * ("pre-factor").
     */
    private boolean m_fPostFactor;
    }
