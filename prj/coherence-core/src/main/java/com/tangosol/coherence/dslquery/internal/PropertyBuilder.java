/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.internal;

import com.tangosol.util.Extractors;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.CompositeUpdater;
import com.tangosol.util.extractor.UniversalExtractor;
import com.tangosol.util.extractor.UniversalUpdater;

import java.util.ArrayList;

/**
 * PropertyBuilder is a utility class that turns property Strings into
 * proper getter and setter names.
 *
 * @author djl  2009.08.31
 */
public class PropertyBuilder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new PropertyBuilder.
     */
    public PropertyBuilder()
        {
        }

    /**
     * Construct a new PropertyBuilder that will suppress the given prefix
     * String.
     *
     * @param sPrefix  the String to suppress
     */
    public PropertyBuilder(String sPrefix)
        {
        m_sPrefixToSuppress = sPrefix;
        }

    // ----- PropertyBuilder API --------------------------------------------

    /**
     * Make a ValueExtractor for the given String.  Allow '.' and make
     * a ChainedValueExtractor.
     *
     * @param sName  the String used to make ValueExtractor
     *
     * @return the constructed ValueExtractor
     */
    public ValueExtractor extractorFor(String sName)
        {
        return extractorFor(splitString(sName, '.'));
        }

    /**
     * Make a getter String suitable for use in a ValueExtractor.
     *
     * @param sName  the property String used to make getter String
     *
     * @return the constructed String
     */
    public String extractorStringFor(String sName)
        {
        return uniformStringFor(uniformArrayFor(splitString(sName, '.'), "get"));
        }

    /**
     * Make a setter String suitable for use in a ValueUpdater.  If String
     * already starts with "set" then leave it alone.
     *
     * @param sName  the property String used to make setter String
     *
     * @return the constructed String
     */
    public String updaterStringFor(String sName)
        {
        return uniformStringFor(uniformArrayFor(splitString(sName, '.'), "set"));
        }

    /**
     * Make a getter String suitable for use in a ValueUpdater.
     *
     * @param sName  the property String used to make getter String
     *
     * @return the constructed String
     */
    public String propertyStringFor(String sName)
        {
        return uniformStringFor(uniformArrayFor(splitString(sName, '.'), ""));
        }

    /**
     * Make a ValueUpdater for the given String.  Allow '.' for chaining.
     *
     * @param sName  the String used to make ValueExtractor
     *
     * @return the constructed ValueUpdater
     */
    public ValueUpdater updaterFor(String sName)
        {
        return updaterFor(splitString(sName, '.'));
        }

    /**
     * Make a ValueExtractor for the given array of property Strings.
     *
     * @param asProps  the String[] used to make ValueExtractor
     *
     * @return the constructed ValueExtractor
     */
    public ValueExtractor extractorFor(String[] asProps)
        {
        String[] asMethodNames = uniformArrayFor(asProps, "get");

        if (asMethodNames.length == 1)
            {
            return Extractors.extract(asMethodNames[0]);
            }

        ValueExtractor[] aExtractors = new ValueExtractor[asMethodNames.length];

        for (int i = 0; i < aExtractors.length; i++)
            {
            aExtractors[i] = Extractors.extract(asMethodNames[i]);
            }

        return new ChainedExtractor(aExtractors);
        }

    /**
     * Make a ValueUpdater for the given array of property Strings.
     *
     * @param asProps  the String[] used to make ValueUpdater
     *
     * @return the constructed ValueUpdater
     */
    public ValueUpdater updaterFor(String[] asProps)
        {
        int          nStart     = 0;
        int          nPartCount = asProps.length;
        String       sProp      = asProps[nPartCount - 1];
        ValueUpdater updater    = new UniversalUpdater(makeSimpleName("set", sProp));

        if (m_sPrefixToSuppress != null && asProps[0].equals(m_sPrefixToSuppress))
            {
            nStart = 1;
            }

        if (nPartCount - nStart > 1)
            {
            ValueExtractor[] extractors = new ValueExtractor[nPartCount - nStart - 1];

            for (int i = 0; i < extractors.length; i++)
                {
                extractors[i] = Extractors.extract(makeSimpleName("get", asProps[i + nStart]));
                }

            updater = new CompositeUpdater(new ChainedExtractor(extractors), updater);
            }

        return updater;
        }

    /**
     * Make a camel case String by prefixing a given String with a given
     * prefix.  If name already starts with prefix and the following char is
     * uppercase then just name was already a proper camel case getter or
     * setter and return the given String.
     *
     * @param sPrefix  the String to prefix, typically get or set
     * @param sName    the property String being transformed
     *
     * @return the constructed String
     */
    public String makeSimpleName(String sPrefix, String sName)
        {
        if (sName.startsWith(sPrefix))
            {
            String sn = sName.substring(sPrefix.length());

            if (sn.length() >= 1 && Character.isUpperCase(sn.charAt(0)))
                {
                return sName + UniversalExtractor.METHOD_SUFFIX;
                }
            else if (sn.length() == 0)
                {
                return sName;
                }
            else if (sPrefix.isEmpty())
                {
                return sName;
                }
            else
                {
                return sPrefix + Character.toUpperCase(sn.charAt(0)) + sn.substring(1) + UniversalExtractor.METHOD_SUFFIX;
                }
            }

        return sPrefix + sName.substring(0, 1).toUpperCase() + sName.substring(1) + UniversalExtractor.METHOD_SUFFIX;
        }

    /**
     * Returns the specified method name with any "set" or
     * "get" prefix removed and the first letter of the
     * remaining String converted to lowercase.
     *
     * @param sName  the method name to convert
     *
     * @return the converted method name
     */
    public String plainName(String sName)
        {
        if (sName == null || sName.isEmpty())
            {
            return sName;
            }

        if (sName.startsWith("set") || sName.startsWith("get"))
            {
            sName = sName.substring(3);
            }

        StringBuilder sb = new StringBuilder();

        sb.append(Character.toLowerCase(sName.charAt(0)));

        if (sName.length() > 1)
            {
            sb.append(sName.substring(1));
            }

        return sb.toString();
        }

    // ----- helper methods  ------------------------------------------------

    /**
     * Make a getter or setter String array from the given array of property
     * Strings using the given prefix string.
     *
     * @param asProps  the String[] used to make getters or setters
     * @param sPrefix  the String to be prepended.
     *
     * @return the constructed String array
     */
    protected String[] uniformArrayFor(String[] asProps, String sPrefix)
        {
        int nStart     = 0;
        int nPartCount = asProps.length;

        if (m_sPrefixToSuppress != null && asProps[0].equals(m_sPrefixToSuppress))
            {
            nStart = 1;
            }

        String[] aExtractors = new String[nPartCount - nStart];

        for (int i = 0; i < aExtractors.length; i++)
            {
            aExtractors[i] = makeSimpleName(sPrefix, asProps[i + nStart]);
            }

        return aExtractors;
        }

    /**
     * Make a String from the given array of Strings parts.
     *
     * @param asParts  the String[] used to make one string separated by '.'
     *
     * @return the constructed String
     */
    protected String uniformStringFor(String[] asParts)
        {
        if (asParts == null || asParts.length == 0)
            {
            return "";
            }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < asParts.length; ++i)
            {
            sb.append('.').append(asParts[i]);
            }

        return sb.substring(1);
        }

    /**
     * Take a given String and bust apart into a String array by splitting at
     * the given delimiter character.
     *
     * @param sName  the String to prefix, typically get or set
     * @param delim  the delimiter character
     *
     * @return   the constructed String
     */
    public String[] splitString(String sName, char delim)
        {
        if (sName == null)
            {
            return null;
            }

        ArrayList list  = new ArrayList();
        int       nPrev = -1;

        while (true)
            {
            int nNext = sName.indexOf(delim, nPrev + 1);

            if (nNext < 0)
                {
                list.add(sName.substring(nPrev + 1));
                break;
                }
            else
                {
                list.add(sName.substring(nPrev + 1, nNext));
                }

            nPrev = nNext;
            }

        return (String[]) list.toArray(new String[list.size()]);
        }

    // ----- data members ---------------------------------------------------

    /**
     * Prefix string that should be suppressed.
     */
    protected String m_sPrefixToSuppress = null;
    }
