/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.extractor;


import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.extractor.MultiExtractor;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.coherence.reporter.Constants;
import com.tangosol.coherence.reporter.JMXQueryHandler;
import com.tangosol.coherence.reporter.Reporter;

import java.io.IOException;
import java.io.NotSerializableException;

import java.util.Set;
import java.util.Iterator;


/**
* A MultiExtractor extension to implement static and correlated sub queries
* from a MBeanServer source.
*
* @author ew 2008.02.20
* @since Coherence 3.4
*/
public class SubQueryExtractor
        implements ValueExtractor, Constants
    {
    // ----- constructors ----------------------------------------------------

    /**
    * Construct a sub query value extractor.
    *
    * @param aExtractor  ValueExtractor array containing the macro replacement
    *                    extractors for the pattern replacement.
    * @param xmlQuery    the XML definition of the query.
    * @param jmxqOuter   a reference to the outer query.
    * @param sColumnId   the column identifier for the result
    */
    public SubQueryExtractor(ValueExtractor[] aExtractor, XmlElement xmlQuery,
                             JMXQueryHandler jmxqOuter, String sColumnId)
        {
        if (aExtractor.length > 0)
            {
            m_ME = new MultiExtractor(aExtractor);
            }

        m_sPatternTemplate = xmlQuery.getElement(TAG_PATTERN).getString();
        m_xmlQuery         = xmlQuery;
        m_jmxqOuter        = jmxqOuter;
        m_sColumnId        = sColumnId;
        }

    // ----- ValueExtractor interface ----------------------------------------

    /**
    * @inheritDoc
    */
    public Object extract(Object oTarget)
        {
        ImmutableArrayList arResults = (m_ME != null) ?
                (ImmutableArrayList) m_ME.extract(oTarget) : null;
        Set                setMacros = Reporter.getMacros(m_sPatternTemplate);
        String             sPattern  = m_sPatternTemplate;
        int                c         = 0;

        // replace the values in the subquery pattern with the values from the
        // current object
        if (arResults != null)
            {
            for (Iterator iter = setMacros.iterator(); iter.hasNext();)
                {
                String       sId    = (String)iter.next();
                Object       oValue = arResults.get(c);
                if (oValue != null)
                    {
                    sPattern = sPattern.replaceAll(MACRO_START + sId + MACRO_STOP,
                                oValue.toString());
                    }
                c++;
                }
            }


        // update the query xml definition with the new target.
        m_xmlQuery.getElement(TAG_PATTERN).setString(sPattern);

        // create the sub query
        JMXQueryHandler inner = new JMXQueryHandler();

        //  Set the correlated target for the sub query.  This is used to extract
        // correlated values from the outer query
        inner.setCorrelated(oTarget);

        //  Set inner query with the new query and the context (column and
        // filter definitions from the outer context.
        inner.setContext(m_xmlQuery, m_jmxqOuter.getContext());

        // execute the inner query.
        inner.execute();

        //  get the value of the inner query.  null is passed because inner
        // queries can only be aggregates.
        return inner.getValue(null, m_sColumnId);
        }

    // ----- Java custom serialization methods ------------------------------

    /**
     * Disable Java serialization for this {@link ValueExtractor}.
     *
     * @param stream  output stream
     *
     * @throws IOException to indicate Java serialization is not supported
     */
    private void writeObject(java.io.ObjectOutputStream stream)
        throws IOException
        {
        throw new NotSerializableException(SubQueryExtractor.class.getCanonicalName() + " is not serializable");
        }

    /**
     * Disable Java deserialization for this {@link ValueExtractor}.
     *
     * @param stream  input stream
     *
     * @throws IOException to indicate Java deserialization is not supported
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream stream)
        throws IOException, ClassNotFoundException
        {
        throw new NotSerializableException(SubQueryExtractor.class.getCanonicalName() + " is not serializable");
        }

    // ----- data members ----------------------------------------------------

    /*
    * the xml definition of the sub query
    */
    protected XmlElement m_xmlQuery;

    /*
    * the join template for the sub query pattern
    */
    protected String m_sPatternTemplate;

    /*
    * the reference to the outer query.
    */
    protected JMXQueryHandler m_jmxqOuter;

    /*
    * the result column identifier (the aggregate column returned).
    */
    protected String m_sColumnId;

    protected MultiExtractor m_ME;
    }
