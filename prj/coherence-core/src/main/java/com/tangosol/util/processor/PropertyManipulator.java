/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.processor;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueManipulator;
import com.tangosol.util.ValueUpdater;

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.CompositeUpdater;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.extractor.ReflectionUpdater;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* PropertyManipulator is a reflection based ValueManipulator implementation
* based on the JavaBean property name conventions.
*
* @param <V>  the type of value manipulated
*
* @author gg 2005.10.31
*/
public class PropertyManipulator<V, R>
        extends Base
        implements ValueManipulator<V, R>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public PropertyManipulator()
        {
        }

    /**
    * Construct a PropertyManipulator for the specified property name.
    * <p>
    * This constructor assumes that the corresponding property getter will
    * have a name of ("get" + sName) and the corresponding property setter's
    * name will be ("set + sName).
    *
    * @param sName  a property name
    */
    public PropertyManipulator(String sName)
        {
        this(sName, false);
        }

    /**
    * Construct a PropertyManipulator for the specified property name.
    * <p>
    * This constructor assumes that the corresponding property getter will
    * have a name of either ("get" + sName) or ("is + sName) and the
    * corresponding property setter's name will be ("set + sName).
    *
    * @param sName  a property name
    * @param fUseIs if true, the getter method will be prefixed with "is"
    *                rather than "get"
    */
    public PropertyManipulator(String sName, boolean fUseIs)
        {
        azzert(sName != null && sName.length() > 0);

        // composite ('.'-delimited) names are supported, but not documented
        m_sName  = sName;
        m_fUseIs = fUseIs;
        }


    // ----- ValueManipulator interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public ValueExtractor<V, R> getExtractor()
        {
        ValueExtractor<V, R> extractor = m_extractor;
        if (extractor == null)
            {
            init();
            extractor = m_extractor;
            }
        return extractor;
        }

    /**
    * {@inheritDoc}
    */
    public ValueUpdater<V, R> getUpdater()
        {
        ValueUpdater<V, R> updater = m_updater;
        if (updater == null)
            {
            init();
            updater = m_updater;
            }

        return updater;
        }


    // ----- internal -------------------------------------------------------

    /**
    * Parse the property name and initialize necessary extractor and updator.
    */
    protected void init()
        {
        // allow composite property name (not documented)
        String sName  = m_sName;
        int    ofLast = sName.lastIndexOf('.');
        String sProp  = sName.substring(ofLast + 1);

        ValueExtractor extractor = new ReflectionExtractor(
            (m_fUseIs ? "is" : "get") + sProp);
        ValueUpdater updater = new ReflectionUpdater("set" + sProp);

        if (ofLast > 0)
            {
            String[] asProp = parseDelimitedString(sName.substring(0, ofLast), '.');
            int      cParts = asProp.length;

            ValueExtractor[] veGet = new ValueExtractor[cParts + 1];
            ValueExtractor[] vePut = new ValueExtractor[cParts];
            for (int i = 0; i < cParts; i++)
                {
                veGet[i] = vePut[i] =
                    new ReflectionExtractor("get" + asProp[i]);
                }
            veGet[cParts] = extractor;

            extractor = new ChainedExtractor(veGet);
            updater   = new CompositeUpdater(
                cParts == 1 ? vePut[0] : new ChainedExtractor(vePut),
                updater);
            }
        m_extractor = extractor;
        m_updater   = updater;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the PropertyManipulator with another object to determine
    * equality.
    *
    * @return true iff this PropertyManipulator and the passed object are
    *         equivalent PropertyManipulators
    */
    public boolean equals(Object o)
        {
        if (o instanceof PropertyManipulator)
            {
            PropertyManipulator that = (PropertyManipulator) o;
            return this.m_sName.equals(that.m_sName)
                && this.m_fUseIs  ==   that.m_fUseIs;
            }

        return false;
        }

    /**
    * Determine a hash value for the PropertyManipulator object according to
    * the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this PropertyManipulator object
    */
    public int hashCode()
        {
        return m_sName.hashCode();
        }

    /**
    * Return a human-readable description for this PropertyManipulator.
    *
    * @return a String description of the PropertyManipulator
    */
    public String toString()
        {
        return "PropertyManipulator(" + m_sName + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_sName  = in.readUTF();
        m_fUseIs = in.readBoolean();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        out.writeUTF(m_sName);
        out.writeBoolean(m_fUseIs);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_sName  = in.readString(0);
        m_fUseIs = in.readBoolean(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeString(0, m_sName);
        out.writeBoolean(1, m_fUseIs);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The property name, never null.
    */
    @JsonbProperty("name")
    protected String m_sName;

    /**
    * The getter prefix flag.
    */
    @JsonbProperty("useIsPrefix")
    protected boolean m_fUseIs;

    /**
    * The underlying ValueExtractor.
    */
    transient protected ValueExtractor<V, R> m_extractor;

    /**
    * The underlying ValueUpdater.
    */
    transient protected ValueUpdater<V, R> m_updater;
    }