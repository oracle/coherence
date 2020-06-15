/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;

import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.io.pof.reflect.PofNavigator;
import com.tangosol.io.pof.reflect.PofNavigationException;
import com.tangosol.io.pof.reflect.PofValue;
import com.tangosol.io.pof.reflect.PofValueParser;
import com.tangosol.io.pof.reflect.SimplePofPath;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ClassHelper;

import java.io.IOException;
import java.io.NotActiveException;

import java.util.Map;


/**
* POF-based ValueUpdater implementation.
*
* @author as  2009.02.14
* @since Coherence 3.5
*/
public class PofUpdater
        extends    AbstractUpdater
        implements PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the PortableObject interface).
    */
    public PofUpdater()
        {
        }

    /**
    * Constructs a PofUpdater based on a property index.
    * <p>
    * This constructor is equivalent to:
    * <pre>
    *   PofUpdater updater =
    *       new PofUpdater(new SimplePofPath(iProp));
    * </pre>
    *
    * @param iProp  property index
    */
    public PofUpdater(int iProp)
        {
        this(new SimplePofPath(iProp));
        }

    /**
    * Constructs a PofUpdater based on a POF navigator.
    *
    * @param navigator  POF navigator
    */
    public PofUpdater(PofNavigator navigator)
        {
        azzert(navigator != null, "Navigator must not be null.");
        m_navigator = navigator;
        }


    // ----- ValueUpdater interface -----------------------------------------

    /**
    * {@inheritDoc}
    */
    public void update(Object oTarget, Object oValue)
        {
        if (oTarget instanceof Map.Entry)
            {
            // we keep this method only for backward compatibility, since prior
            // to Coherence 3.6 it was possible to write:
            //   new PofUpdater(...).update(entry, oValue);
            updateEntry((Map.Entry) oTarget, oValue);
            }
        else
            {
            super.update(oTarget, oValue);
            }
        }

    // ----- AbstractUpdater methods ----------------------------------------

    /**
    * Update the passed entry using the specified value.
    * <p>
    * It is expected that this updater will only be used against POF-encoded
    * entries implementing {@link BinaryEntry} interface.
    *
    * @param entry   the entry to update
    * @param oValue  the new value to update the target's property with
    *
    * @throws UnsupportedOperationException  if the specified target object
    *         is not a POF-encoded {@link BinaryEntry} or the serializer is not
    *         a PofContext
    * @throws PofNavigationException if the property cannot be located
    *         (navigated to)
    */
    public void updateEntry(Map.Entry entry, Object oValue)
        {
        try
            {
            BinaryEntry binEntry  = (BinaryEntry) entry;
            PofValue    valueRoot = PofValueParser.parse(
                binEntry.getBinaryValue(), (PofContext) binEntry.getSerializer());
            PofValue    valueProp = m_navigator.navigate(valueRoot);
            if (valueProp == null)
                {
                throw new PofNavigationException(
                    "Property is missing: " + m_navigator);
                }
            valueProp.setValue(oValue);
            binEntry.updateBinaryValue(valueRoot.applyChanges());
            }
        catch (ClassCastException cce)
            {
            String sReason = entry instanceof BinaryEntry
                    ? "the configured Serializer is not a PofContext"
                    : "the Map Entry is not a BinaryEntry";
            throw new UnsupportedOperationException(
                    "PofUpdater must be used with POF-encoded Binary entries;"
                    + sReason);
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the PofUpdater with another object to determine
    * equality.
    *
    * @return true iff this PofUpdater and the passed object are
    *         equivalent PofUpdaters
    */
    public boolean equals(Object o)
        {
        if (o instanceof PofUpdater)
            {
            PofUpdater that = (PofUpdater) o;
            return this.m_navigator.equals(that.m_navigator);
            }

        return false;
        }

    /**
    * Determine a hash value for the PofUpdater object according to
    * the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this PofUpdater object
    */
    public int hashCode()
        {
        return m_navigator.hashCode();
        }

    /**
    * Return a human-readable description for this PofUpdater.
    *
    * @return a String description of the PofUpdater
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) +
            "(navigator=" + m_navigator + ')';
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_navigator = (PofNavigator) in.readObject(0);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        PofNavigator navigator = m_navigator;
        if (navigator == null)
            {
            throw new NotActiveException(
                    "PofUpdater was constructed without a navigator");
            }
        out.writeObject(0, navigator);
        }


    // ----- data members ---------------------------------------------------

    /**
    * POF navigator.
    */
    private PofNavigator m_navigator;
    }
