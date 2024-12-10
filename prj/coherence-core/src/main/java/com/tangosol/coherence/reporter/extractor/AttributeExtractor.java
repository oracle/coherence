/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.extractor;


import com.tangosol.coherence.reporter.Constants;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.Base;
import com.tangosol.util.ValueExtractor;

import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.AttributeNotFoundException;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;


/**
* MBean Attribute ValueExtractor implementation.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class AttributeExtractor
        implements ValueExtractor, Constants
    {

    // ----- constructors ----------------------------------------------------

    /**
    * Construct a AttributeExtractor based on a Attribute name.
    *
    * @param sAttribute  the name of the attribute to extract
    * @param cDelim      the delimiter for array attributes
    * @param fReturnNeg  a flag that allows for the return of negative values
    *                    By default negative numbers are returned as zero
    */
    public AttributeExtractor(String sAttribute, char cDelim, boolean fReturnNeg)
        {
        this(sAttribute, cDelim, fReturnNeg, MBeanHelper.findMBeanServer());
        }

    /**
    * Construct a AttributeExtractor based on a Attribute name.
    *
    * @param sAttribute  the name of the attribute to extract
    * @param cDelim      the delimiter for array attributes
    * @param fReturnNeg  a flag that allows for the return of negative values
    *                    By default negative numbers are returned as zero
    * @param server      the {@link MBeanServer} to query against
    */
    public AttributeExtractor(String sAttribute, char cDelim, boolean fReturnNeg, MBeanServer server)
        {
        super();
        m_sAttribute = sAttribute;
        m_cDelim     = cDelim;
        m_fReturnNeg = fReturnNeg;
        f_mbs        = server;
        }

    // ----- ValueExtractor interface ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Object extract(Object oTarget)
        {
        try
            {
            ObjectName oJoinName = (ObjectName) oTarget;

            String sAttribName = m_sAttribute;
            String sCurrent    = currentKey(sAttribName);
            Object oData       = f_mbs.getAttribute(oJoinName, sCurrent);
            Object oReturn;

            if (oData == null)
                {
                return DEFAULT_VALUE;
                }

            if (!(sCurrent.equals(sAttribName)))
                {
                oData = getValue(nextKey(sAttribName), oData);
                }

            if (oData == null)
                {
                oReturn = DEFAULT_VALUE;
                }
            else
                {
                oReturn = getValue(sAttribName, oData);
                if (m_fReturnNeg)
                    {
                    return oReturn;
                    }
                else
                    {
                    if (oReturn instanceof Number &&
                            ((Number) oReturn).doubleValue() < 0)
                        {
                        if (oReturn instanceof Long ||
                            oReturn instanceof Integer)
                            {
                            return 0L;
                            }
                        else
                            {
                            return 0.0;
                            }
                        }
                    }
                }

            return oReturn;
            }
        catch (InstanceNotFoundException e)
            {
            // Exceptions will occur when nodes are removed from the grid after
            // query and before the data is extracted. The Default 'n/a'
            // will be returned when this occurs.
            // Return Default
            }
        catch (MBeanException e)
            {
            // Return Default
            }
        catch (AttributeNotFoundException e)
            {
            // Return Default
            }
        catch (ReflectionException e)
            {
            // Return Default
            }
        return DEFAULT_VALUE;
        }

    // ----- helpers ---------------------------------------------------------

    /**
    * Recursively inspects JMC composite data and extract the value in the sKey
    * path.
    *
    * @param sKey      the key path to the data.  sKey is a '\' delimited
    *                  string of Composite data names.
    * @param oData     the composite data attribute to extract the value
    *
    * @return the information located in the sKey path
    */
    protected Object getValue(String sKey, Object oData)
        {
        String sName     = currentKey(sKey);
        char   cSubDelim = m_cDelim;

        if (sName.length() == 0) return oData;

        if (oData instanceof CompositeData)
            {
            CompositeData oCompData = (CompositeData) oData;
            Object        oValue    = oCompData.get(sName);
            return getValue(nextKey(sKey), oValue);
            }

        else if (oData instanceof Attribute)
            {
            return getValue(nextKey(sName), ((Attribute) oData).getValue());
            }

        else if (oData instanceof TabularData)
            {
            TabularDataSupport oTabData = (TabularDataSupport) oData;
            String[]           asKey    = {sName};
            Object             cdData   = oTabData.get(asKey);
            return getValue(nextKey(sKey), cdData);
            }

        else if (oData instanceof Object[])
            {
            Object[] aoData = (Object[]) oData;
            for (int c = 0; c < aoData.length; c++)
                {
                if (aoData[c] instanceof Attribute)
                    {
                    Attribute a           = (Attribute) aoData[c];
                    String    sAttribName = a.getName();
                    if (sAttribName.equals(sName))
                        {
                        Object oValue = a.getValue();
                        return getValue(nextKey(sKey), oValue);
                        }
                    }
                }
            }

        else if (oData instanceof Map)
            {
            Map    mapData = (Map) oData;
            Object oValue  = mapData.get(sName);
            return getValue(nextKey(sKey), oValue);
            }

        else if (oData instanceof long[])
            {
            long[]       anData = (long[]) oData;
            int          nSize  = anData.length;
            StringBuffer sb     = new StringBuffer();
            for (int i = 0; i < nSize; i++)
                {
                if (i != 0)
                    {
                    sb.append(cSubDelim);
                    }
                sb.append(anData[i]);
                }
            return sb.toString();
            }

        else if (oData instanceof int[])
            {
            int[]        anData = (int[]) oData;
            int          nSize  = anData.length;
            StringBuffer sb     = new StringBuffer();
            for (int i = 0; i < nSize; i++)
                {
                if (i != 0)
                    {
                    sb.append(cSubDelim);
                    }
                sb.append(anData[i]);
                }
            return sb.toString();
            }

        else if (oData instanceof double[])
            {
            double[]     anData = (double[]) oData;
            int          nSize  = anData.length;
            StringBuffer sb     = new StringBuffer();
            for (int i = 0; i < nSize; i++)
                {
                if (i != 0)
                    {
                    sb.append(cSubDelim);
                    }
                sb.append(anData[i]);
                }
            return sb.toString();
            }

        else
            {
            if (oData == null)
                {
                oData = "";
                }
            }

        return oData;
        }

    /**
    * return the first part of a '/' delimited string
    *
    * @param  sKey  a '/' delimited path string for composite data.
    *
    * @return the first part of the path.
    */
    public static String currentKey(String sKey)
        {
        if (sKey.length() == 0) return "";
        return Base.parseDelimitedString(sKey, '/')[0];
        }

    /**
    * Remove the first part of the key path and return the rest of the string.
    *
    * @param  sKey  a '/' delimited key path string to access composite data
    *
    * @return sKey after removing the first part of the key
    */
    public static String nextKey(String sKey)
        {
        String[] asKey = Base.parseDelimitedString(sKey, '/');
        String   sRet  = "";
        long     lLen  = asKey.length;
        for (int c = 1; c < lLen; c++)
            {
            sRet += asKey[c];
            if (c != lLen - 1)
                {
                sRet += "/";
                }
            }
        return sRet;
        }

    //----- data members ----------------------------------------------------

    /**
    * The {@link MBeanServer} to query against.
    */
    protected final MBeanServer f_mbs;

    /**
    * A JMX ObjectName string containing value replacement macros  The macros will
    * be replaced with data from column information during runtime.
    */
    protected String m_sJoinTemplate;

    /**
    * The Attribute name string to be extracted.
    */
    protected String m_sAttribute;

    /**
    * The column delimiter for string values.
    */
    protected char   m_cDelim;

    /**
    * flag allow the return of a negative number
    */
    protected boolean m_fReturnNeg = false;
    }
