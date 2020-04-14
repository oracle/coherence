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

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueManipulator;
import com.tangosol.util.ValueUpdater;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* PropertyProcessor is a base class for EntryProcessor implementations that
* depend on a ValueManipulator.
* <p>
* Any concrete subclass would have to implement the methods of
* {@link ExternalizableLite} interface calling the appropriate super class
* implementations first.
* <p>
* A typical concrete subclass would also implement the {@link #process
* process()} method using the following pattern:
* <pre>
*  public Object process(InvocableMap.Entry entry)
*    {
*    // retrieve an old property value
*    Object oOldValue = get(entry);
*
*    ... // calculate a new value and the process result
*    ... // based on the old value and the processor's attributes
*
*    if (!oNewValue.equals(oOldValue))
*      {
*      // set the new property value
*      set(entry, oNewValue);
*      }
*
*    // return the process result
*    return oResult;
*    }
* </pre>
*
* @author gg 2005.10.31
*/
public abstract class PropertyProcessor<K, V, R>
        extends AbstractProcessor<K, V, R>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public PropertyProcessor()
        {
        }

    /**
    * Construct a PropertyProcessor for the specified property name.
    * <p>
    * This constructor assumes that the corresponding property getter will
    * have a name of ("get" + sName) and the corresponding property setter's
    * name will be ("set + sName).
    *
    * @param sName  a property name
    */
    public PropertyProcessor(String sName)
        {
        this(sName, false);
        }

    /**
    * Construct a PropertyProcessor for the specified property name.
    * <p>
    * This constructor assumes that the corresponding property getter will
    * have a name of either ("get" + sName) or ("is + sName) and the
    * corresponding property setter's name will be ("set + sName).
    *
    * @param sName  a property name
    * @param fUseIs if true, the getter method will be prefixed with "is"
    *                rather than "get"
    */
    public PropertyProcessor(String sName, boolean fUseIs)
        {
        this(sName == null ? null : new PropertyManipulator(sName, fUseIs));
        }

    /**
    * Construct a PropertyProcessor based for the specified
    * ValueManipulator.
    *
    * @param manipulator  a ValueManipulator; could be null
    */
    public PropertyProcessor(ValueManipulator<V, R> manipulator)
        {
        m_manipulator = manipulator;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Get the property value from the passed Entry object.
    *
    * @param entry  the Entry object
    *
    * @return the property value
    *
    * @see ValueExtractor#extract
    */
    protected R get(InvocableMap.Entry<K, V> entry)
        {
        ValueManipulator<V, R> manipulator = m_manipulator;
        if (manipulator != null)
            {
            ValueExtractor<V, R> extractor = manipulator.getExtractor();
            if (extractor == null)
                {
                throw new IllegalStateException("The ValueManipulator ("
                        + manipulator + ") failed to provide a ValueExtractor");
                }
            else
                {
                return entry.extract(extractor);
                }
            }
        //NOTE: This is potentially an unsafe cast from V to T.
        return (R) entry.getValue();
        }

    /**
    * Set the property value into the passed Entry object.
    *
    * @param entry  the Entry object
    * @param oValue a new property value
    *
    * @see ValueUpdater#update
    */
    protected void set(InvocableMap.Entry<K, V> entry, R oValue)
        {
        ValueManipulator<V, R> manipulator = m_manipulator;
        if (m_manipulator != null)
            {
            ValueUpdater<V, R> updater = manipulator.getUpdater();
            if (updater != null)
                {
                entry.update(updater, oValue);
                return;
                }
            }
        entry.setValue((V) oValue, false);
        }

    /**
    * Returns this PropertyProcessor's description.
    *
    * @return  this PropertyProcessor's description
    */
    abstract protected String getDescription();


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the PropertyProcessor with another object to determine
    * equality.
    *
    * @return true iff this PropertyProcessor and the passed object are
    *         equivalent PropertyProcessors
    */
    public boolean equals(Object o)
        {
        if (o instanceof PropertyProcessor)
            {
            PropertyProcessor that = (PropertyProcessor) o;
            return equals(this.m_manipulator, that.m_manipulator);
            }

        return false;
        }

    /**
    * Determine a hash value for the PropertyProcessor object according to
    * the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this PropertyProcessor object
    */
    public int hashCode()
        {
        return m_manipulator.hashCode();
        }

    /**
    * Return a human-readable description for this PropertyProcessor.
    *
    * @return a String description of the PropertyProcessor
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) +
            '(' + m_manipulator + getDescription() + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_manipulator = (ValueManipulator) ExternalizableHelper.readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_manipulator);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_manipulator = (ValueManipulator) in.readObject(0);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_manipulator);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The property value manipulator.
    */
    @JsonbProperty("manipulator")
    protected ValueManipulator<V, R> m_manipulator;
    }