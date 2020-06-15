/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.psr;


import com.tangosol.coherence.component.net.Member;

import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PortableException;
import com.tangosol.io.pof.PortableObjectSerializer;
import com.tangosol.io.pof.SimplePofContext;
import com.tangosol.io.pof.ThrowablePofSerializer;

import com.tangosol.util.UUID;

import com.tangosol.util.extractor.ReflectionExtractor;


/**
* PofContext used to serialize and deserialize all PortableObject classes
* transported by the various Message classes in this package.
*
* @author jh  2007.02.14
*/
public class PofContext
        extends SimplePofContext
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public PofContext()
        {
        super();

        registerUserType(1, UUID.class,                new PortableObjectSerializer(1));
        registerUserType(2, Member.class,              new PortableObjectSerializer(2));
        registerUserType(3, TestResult.class, new PortableObjectSerializer(3));
        registerUserType(4, Histogram.class, new PortableObjectSerializer(4));
        registerUserType(5, ScaledHistogram.class, new PortableObjectSerializer(5));
        registerUserType(6, ReflectionExtractor.class, new PortableObjectSerializer(6));
        }


    // ----- PofContext interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public PofSerializer getPofSerializer(int i)
        {
        if (i == 0)
            {
            return THROWABLE_SERIALIZER;
            }
        return super.getPofSerializer(i);
        }

    /**
    * {@inheritDoc}
    */
    public int getUserTypeIdentifier(Class aClass)
        {
        if (aClass != null && Throwable.class.isAssignableFrom(aClass))
            {
            return 0;
            }
        return super.getUserTypeIdentifier(aClass);
        }

    /**
    * {@inheritDoc}
    */
    public Class getClass(int i)
        {
        if (i == 0)
            {
            return PortableException.class;
            }
        return super.getClass(i);
        }

    /**
    * {@inheritDoc}
    */
    public boolean isUserType(Class aClass)
        {
        if (aClass != null && Throwable.class.isAssignableFrom(aClass))
            {
            return true;
            }
        return super.isUserType(aClass);
        }


    // ----- SimplePofContext methods ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void registerUserType(int i, Class aClass, PofSerializer serializer)
        {
        if (i == 0)
            {
            throw new IllegalArgumentException("0 is a reserved type");
            }
        super.registerUserType(i, aClass, serializer);
        }

    /**
    * {@inheritDoc}
    */
    public void unregisterUserType(int i)
        {
        if (i == 0)
            {
            throw new IllegalArgumentException("0 is a reserved type");
            }
        super.unregisterUserType(i);
        }


    // ----- contants -------------------------------------------------------

    /**
    * PofSerializer for all Throwables.
    */
    private static final PofSerializer THROWABLE_SERIALIZER = new ThrowablePofSerializer();
    }
