/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.HashHelper;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;
import com.tangosol.util.ValueManipulator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* A ValueUpdater implementation based on an extractor-updater pair that could
* also be used as a ValueManipulator.
*
* @author gg 2005.10.31
* @see ChainedExtractor
*/

/*
* Design note:
*
* The CompositeUpdater does not extend the AbstractUpdater intentionally,
* since it has to assume the entry.getValue() as the CompositeUpdater's
* extractor target. More specifically: if the CompositeUpdater's updater is
* given the Map.Entry to perform the update on, then its extractor becomes
* completely redundant. Therefore, the updater should be given the entry's
* value, so the extractor would take the value as well.
*/
public class CompositeUpdater
        extends Base
        implements ValueUpdater, ValueManipulator,
                   ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public CompositeUpdater()
        {
        }

    /**
    * Construct a CompositeUpdater based on the specified extractor and
    * updater.
    * <p>
    * <b>Note:</b> the extractor and updater here are not symmetrical in
    * nature: the extractor is used to "drill-down" to the target object,
    * while the updater will operate on that extracted object.
    *
    * @param extractor  the ValueExtractor
    * @param updater    the ValueUpdater
    */
    public CompositeUpdater(ValueExtractor extractor, ValueUpdater updater)
        {
        azzert(extractor != null && updater != null);
        m_extractor = extractor;
        m_updater   = updater;
        }

    /**
    * Construct a CompositeUpdater for a specified method name sequence.
    * <p>
    * For example: "getAddress.setZip" method name will indicate that the
    * "getAddress()" method should be used to extract an Address object,
    * which will then be used by the "setZip(String)" call.
    *
    * @param sName  a dot-delimited sequence of N method names which results
    *                 in a CompositeUpdater that is based on an chain of
    *                 (N-1) {@link UniversalExtractor} objects and a single
    *                 {@link UniversalUpdater}.
    */
    public CompositeUpdater(String sName)
        {
        azzert(sName != null && sName.length() > 0);

        int ofLast = sName.lastIndexOf('.');

        m_extractor = ofLast == -1 ?
            (ValueExtractor) IdentityExtractor.INSTANCE :
            new ChainedExtractor(sName.substring(0, ofLast));
        m_updater = new UniversalUpdater(sName.substring(ofLast + 1));
        }


    // ----- ValueUpdater interface -----------------------------------------

    /**
    * {@inheritDoc}
    */
    public void update(Object oTarget, Object oValue)
        {
        getUpdater().update(getExtractor().extract(oTarget), oValue);
        }


    // ----- accessors and helpers ----------------------------------------

    /**
    * Retrieve the ValueExtractor part.
    *
    * @return the ValueExtractor
    */
    public ValueExtractor getExtractor()
        {
        return m_extractor;
        }

    /**
    * Retrieve the ValueUpdater part.
    *
    * @return the ValueUpdater
    */
    public ValueUpdater getUpdater()
        {
        return m_updater;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this CompositeUpdater.
    *
    * @return a String description of the CompositeUpdater
    */
    public String toString()
        {
        return "CompositeUpdater(" + m_extractor + ", " + m_updater +')';
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }

        CompositeUpdater that = (CompositeUpdater) o;

        return equals(m_extractor, that.m_extractor) && equals(m_updater, that.m_updater);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
        {
        int nHash = HashHelper.hash(m_extractor, 31);
        nHash = HashHelper.hash(m_updater, nHash);
        return nHash;
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_extractor = ExternalizableHelper.readObject(in);
        m_updater   = ExternalizableHelper.readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_extractor);
        ExternalizableHelper.writeObject(out, m_updater);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_extractor = in.readObject(0);
        m_updater   = in.readObject(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_extractor);
        out.writeObject(1, m_updater);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The ValueExtractor part.
    */
    @JsonbProperty("extractor")
    protected ValueExtractor m_extractor;

    /**
    * The ValueUpdater part.
    */
    @JsonbProperty("updater")
    protected ValueUpdater m_updater;
    }
