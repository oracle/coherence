/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.extractor;

import com.tangosol.coherence.reporter.Constants;
import com.tangosol.coherence.reporter.Reporter;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.ValueExtractor;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;


/**
* MultiExtractor implementation to return the results from a related object
* instead of the target extraction.
*
* @author ew 2008.02.20
* @since Coherence 3.4
*/
public class JoinExtractor
    extends MultiExtractor
    implements Constants
    {
    // ----- constructors ----------------------------------------------------
    /**
    * Construct a JoinExtractor
    *
    * @param aExtractors    an array of value extractors used to map the
    *                       JoinTemplate to the new target.  The extractors
    *                       will replace the the JoinTemplate {macros} in order
    * @param sJoinTemplate  a "macro-ized" string that represents the new target
    *                       key
    * @param veSource       the new source ValueExtractor
    */
    public JoinExtractor(ValueExtractor[] aExtractors, String sJoinTemplate,
            ValueExtractor veSource)
        {
        this(aExtractors, sJoinTemplate, veSource, MBeanHelper.findMBeanServer());
        }

    /**
    * Construct a JoinExtractor
    *
    * @param aExtractors    an array of value extractors used to map the
    *                       JoinTemplate to the new target.  The extractors
    *                       will replace the the JoinTemplate {macros} in order
    * @param sJoinTemplate  a "macro-ized" string that represents the new target
    *                       key
    * @param veSource       the new source ValueExtractor
    * @param server         the {@link MBeanServer} to query
    */
    public JoinExtractor(ValueExtractor[] aExtractors, String sJoinTemplate,
                         ValueExtractor veSource, MBeanServer server)
        {
        super(aExtractors);

        m_sJoinTemplate = sJoinTemplate;
        m_veSource      = veSource;
        f_mbs           = server;
        }

    // ----- ValueExtractor interface ----------------------------------------

    /**
    * @inheritDoc
    */
    public Object extract(Object oTarget)
        {
        List   listResult  = (List) super.extract(oTarget);
        Set    set         = Reporter.getMacros(m_sJoinTemplate);
        String sJoinTarget = m_sJoinTemplate;
        int    index       = 0;

        for (Iterator iter = set.iterator(); iter.hasNext();)
            {
            String sId    = (String)iter.next();

            // by convention the results and the macro count are equal.
            // this is done in the AttributeLocator
            Object oValue = listResult.get(index);

            if (oValue != null)
                {
                // The replaceAll is to work around a Bug with replaceAll/regex
                // There is a Matcher method quoteReplacement in 1.5+
                // for 1.4 compatiblity the replace all escape sequences the $
                sJoinTarget = sJoinTarget.replaceAll(MACRO_START + sId + MACRO_STOP,
                            oValue.toString().replaceAll("\\$", "\\\\\\$"));
                }
            index++;
            }

        try
            {
            ObjectName onameTarget = new ObjectName(sJoinTarget);
            if (sJoinTarget.contains("*"))
                {
                // Wild card in ObjectName. Need to resolve the actual ObjectName.
                Set<ObjectName> setNames = f_mbs.queryNames(onameTarget, null);
                // We only support join with a single target. Else we pass the unresolved object name
                // and let the value extractor handle it.
                onameTarget = setNames.size() == 1 ? setNames.iterator().next() : onameTarget;
                }
            return m_veSource.extract(onameTarget);
            }
        catch (MalformedObjectNameException e)
            {
            // the join definition failed to create a valid object.
            }
        return null;
        }

    // ----- data members ----------------------------------------------------

    /**
    * The {@link MBeanServer} this ValueExtractor operates against.
    */
    protected final MBeanServer f_mbs;

    /*
    * a macro-ized string used to map the related object.
    */
    protected String m_sJoinTemplate;

    /*
    * the "wrapped" ValueExtractor.
    */
    protected ValueExtractor m_veSource;
    }
