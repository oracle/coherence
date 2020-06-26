/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.reporter.extractor;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.reporter.Constants;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.ValueExtractor;

import java.lang.reflect.Array;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * ValueExtractor implementation to extract value from an
 * MBean operation invocation.
 *
 * @author sr 2017.09.20
 * @since Coherence 12.2.1
 */
public class OperationExtractor
        implements ValueExtractor, Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a OperationExtractor based on an Operation name.
     *
     * @param sMethodName           the name of the operation to be invoked
     * @param chDelim               the delimiter to use for merging the individual array elements, if the output is an
     *                              array
     * @param aoMethodParamsParams  the input parameters array
     * @param asParamTypes          an array containing the input parameter types of the operation
     */
    public OperationExtractor(String sMethodName, char chDelim, Object[] aoMethodParamsParams, String[] asParamTypes)
        {
        this(sMethodName, chDelim, aoMethodParamsParams, asParamTypes, MBeanHelper.findMBeanServer());
        }

    /**
     * Construct a OperationExtractor based on an Operation name.
     *
     * @param sMethodName           the name of the operation to be invoked
     * @param chDelim               the delimiter to use for merging the individual array elements, if the output is an
     *                              array
     * @param aoMethodParamsParams  the input parameters array
     * @param asParamTypes          an array containing the input parameter types of the operation
     * @param server                the {@link MBeanServer} to query against
     */
    public OperationExtractor(String sMethodName, char chDelim, Object[] aoMethodParamsParams, String[] asParamTypes, MBeanServer server)
        {
        m_sMethodName          = sMethodName;
        m_chDelim              = chDelim;
        m_aoMethodParamsParams = aoMethodParamsParams;
        m_asParamTypes         = asParamTypes;
        f_mbs                  = server;
        }

    // ----- ValueExtractor interface ---------------------------------------

    @Override
    public Object extract(Object oTarget)
        {
        try
            {
            ObjectName objectName   = (ObjectName) oTarget;
            Object[]   aoParams     = m_aoMethodParamsParams;
            String[]   asParamTypes = m_asParamTypes;

            Object oResult = f_mbs.invoke(objectName, m_sMethodName, aoParams, asParamTypes);
            return getValueForReportColumn(oResult);
            }
        catch (Exception e)
            {
            Logger.err("OperationExtractor.extract: handled exception while invoking an MBean operation", e);
            // reporter extractors does not seem to throw the exception further,
            // rather they are just returning default values
            }
        return DEFAULT_VALUE;
        }

    // ----- OperationExtractor methods -------------------------------------

    /**
     * Process the input object to return a value which is amenable to be added as a report column value.
     *
     * @param oResult  the object to be processed
     *
     * @return the object to be added in a report column
     */
    protected Object getValueForReportColumn(Object oResult)
        {
        // if the result is an array, append the elements with the delimiter in between
        char   chSubDelim = m_chDelim;
        Class  clzResult  = oResult.getClass();
        String sPrefix    = "";
        if (clzResult.isArray())
            {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, c = Array.getLength(oResult); i < c; i++)
                {
                sb.append(sPrefix).append(Array.get(oResult, i));
                sPrefix = String.valueOf(chSubDelim);
                }
            return sb.toString();
            }
        return oResult;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link MBeanServer} to query against.
     */
    protected final MBeanServer f_mbs;

    /**
     * The name of the method to be invoked.
     */
    protected String m_sMethodName;

    /**
     * The column delimiter for string values.
     */
    protected char m_chDelim;

    /**
     * The array of Object parameters.
     */
    protected Object[] m_aoMethodParamsParams;

    /**
     * The method parameter type array.
     */
    protected String[] m_asParamTypes;
    }
