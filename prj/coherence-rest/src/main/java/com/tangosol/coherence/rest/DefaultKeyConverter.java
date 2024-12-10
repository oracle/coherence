/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.tangosol.util.Base;
import com.tangosol.util.UID;
import com.tangosol.util.UUID;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

/**
 * Default implementation of {@link KeyConverter} for a given key class.
 * <p>
 * This {@link KeyConverter} implementation uses the following rules to
 * perform key conversion:
 * <ul>
 *   <li><tt>toString</tt>:
 *     <ol>
 *       <li>if the key class is <tt>java.util.Date</tt>, format the argument
 *           as <tt>yyyy-MM-dd</tt>;</li>
 *       <li>if the key class is {@link UUID} or {@link UID}, return a
 *           hexadecimal representation of the argument without the leading
 *           <tt>Ox</tt>;</li>
 *       <li>otherwise, call the <tt>toString</tt> method on the argument</li>
 *     </ol>
 *   </li>
 *   <li><tt>fromString</tt>:
 *     <ol>
 *       <li>if the key class is <tt>java.util.Date</tt>, the given string
 *           needs to be formatted as <tt>yyyy-MM-dd</tt>;</li>
 *       <li>otherwise, look for a constructor or a static <tt>fromString</tt>,
 *           <tt>valueOf</tt> or <tt>parse</tt> method (in that order) on the
 *           key class
 *       </li>
 *     </ol>
 *   </li>
 * </ul>
 *
 * @author as  2011.06.08
 */
public class DefaultKeyConverter
        implements KeyConverter
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultKeyConverter instance.
     *
     * @param clzKey  key class
     */
    public DefaultKeyConverter(Class clzKey)
        {
        m_clzKey = clzKey;

        Constructor ctor = findConstructor();
        m_ctorFromString = ctor;

        Method meth = null;
        if (ctor == null)
            {
            for (String sMethodName : METHODS)
                {
                meth = findMethod(sMethodName);
                if (meth != null)
                    {
                    break;
                    }
                }

            if (meth == null)
                {
                throw new IllegalArgumentException(
                        "DefaultKeyConverter cannot be created for key class "
                        + clzKey.getName() + " because it doesn't have a "
                        + "constructor or a static fromString(), valueOf() "
                        + "or parse method that accepts a single String argument");
                }
            }
        m_methFromString = meth;
        }

    // ----- KeyConverter interface -----------------------------------------

    /**
     * {@inheritDoc}
     */
    public Object fromString(String sKey)
        {
        try
            {
            if (Date.class.equals(m_clzKey))
                {
                return DATE_FORMAT.parse(sKey);
                }
            if (m_ctorFromString == null)
                {
                return m_methFromString.invoke(null, sKey);
                }
            return m_ctorFromString.newInstance(sKey);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * {@inheritDoc}
     */
    public String toString(Object oKey)
        {
        if (Date.class.equals(m_clzKey))
            {
            return DATE_FORMAT.format(oKey);
            }
        if (UUID.class.equals(m_clzKey) || UID.class.equals(m_clzKey))
            {
            // remove leading 'Ox'
            return oKey.toString().substring(2);
            }
        return oKey.toString();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Find a key class constructor that accepts a single String argument.
     *
     * @return constructor if it exists, <tt>null</tt> otherwise
     */
    protected Constructor findConstructor()
        {
        try
            {
            return m_clzKey.getConstructor(String.class);
            }
        catch (NoSuchMethodException e)
            {
            return null;
            }
        }

    /**
     * Find a static method with a specified name that accepts a single
     * String argument.
     *
     * @param sMethodName  the name of the method
     *
     * @return method if it exists, <tt>null</tt> otherwise
     */
    protected Method findMethod(String sMethodName)
        {
        try
            {
            return m_clzKey.getMethod(sMethodName, String.class);
            }
        catch (NoSuchMethodException e)
            {
            return null;
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Key class.
     */
    protected final Class m_clzKey;

    /**
     * Constructor to use for conversion from string.
     */
    protected final Constructor m_ctorFromString;

    /**
     * Static method to use for conversion from string.
     */
    protected final Method m_methFromString;

    // ----- constants ------------------------------------------------------

    /**
     * Method names that should be tried in order when looking for a method
     * that can be used for conversion from string.
     */
    protected static final String[] METHODS = new String[] {"fromString", "valueOf", "parse"};

    /**
     * DateFormat instance that will be used to convert Date keys.
     */
    protected static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    }
