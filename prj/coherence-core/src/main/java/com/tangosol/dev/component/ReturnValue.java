/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import java.beans.PropertyVetoException;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;

import com.tangosol.util.ErrorList;
import com.tangosol.run.xml.XmlElement;


/**
* A ReturnValue represents the set of information that describes a behavior's
* return value.  A ReturnValue has only one additional piece of information:
* the data type of the return value.
*
* @version 1.00, 11/19/97
* @author  Cameron Purdy
*/
public class ReturnValue
        extends Trait
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a ReturnValue.  This is the "default" constructor.
    *
    * @param behavior  the behavior with this ReturnValue
    * @param dt        the ReturnValue data type
    */
    protected ReturnValue(Behavior behavior, DataType dt)
        {
        super(behavior, RESOLVED);

        // assertion:  behavior required
        if (behavior == null)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Containing behavior required.");
            }

        // assertion:  data type required
        if (dt == null)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  DataType required.");
            }

        m_dt = dt;
        }

    /**
    * Construct a blank ReturnValue Trait.
    *
    * @param base      the base ReturnValue to derive from
    * @param behavior  the containing behavior
    * @param nMode     one of RESOLVED, DERIVATION, MODIFICATION
    *
    * @see #getBlankDerivedTrait(Trait, int)
    */
    protected ReturnValue(ReturnValue base, Behavior behavior, int nMode)
        {
        super(base, behavior, nMode);

        this.m_dt = base.m_dt;
        }

    /**
    * Copy constructor.
    *
    * @param behavior  the Behavior containing the new ReturnValue
    * @param that      the ReturnValue to copy from
    */
    protected ReturnValue(Behavior behavior, ReturnValue that)
        {
        super(behavior, that);

        this.m_dt = that.m_dt;
        }

    /**
    * Construct a Return Value from a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param behavior  the containing behavior
    * @param stream    the stream to read this Return Value from
    * @param nVersion  version of the data structure in the stream
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            reading the Return Value from the stream or if the stream
    *            does not contain a valid Return Value
    */
    protected ReturnValue(Behavior behavior, DataInput stream, int nVersion)
            throws IOException
        {
        super(behavior, stream, nVersion);

        m_dt = DataType.getType(stream.readUTF());
        }

    /**
    * Construct a Trait object from persistent data.  All traits implement
    * implement a constructor from persistent data.
    *
    * @param parent    the trait that contains this trait
    * @param xml       the XmlElement to read the persistent information from
    * @param nVersion  version of the data structure in the stream
    *
    * @throws IOException if an exception occurs reading the trait data
    */
    protected ReturnValue(Trait parent, XmlElement xml, int nVersion)
            throws IOException
        {
        super(parent, xml, nVersion);

        String sType = readString(xml.getElement("type"));
        if (sType == BLANK)
            {
            throw new IOException("type is missing");
            }

        m_dt = DataType.getType(sType);
        }


    // ----- persistence ----------------------------------------------------

    /**
    * Save the Return Value to a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param stream   the stream to write this Return Value to
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            writing the Return Value to the stream
    */
    protected synchronized void save(DataOutput stream)
            throws IOException
        {
        super.save(stream);

        stream.writeUTF(m_dt.getTypeString());
        }

    /**
    * Save the Trait information to XML.  If derived traits have any
    * data of their own, then they must implement (i.e. supplement) this
    * method.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param xml  an XmlElement to write the persistent information to
    *
    * @throws IOException if an exception occurs saving the trait data
    */
    protected synchronized void save(XmlElement xml)
            throws IOException
        {
        xml.addElement("type").setString(m_dt.getTypeString());
        super.save(xml);
        }


    // ----- derivation/modification ----------------------------------------

    /**
    * Construct a blank ReturnValue from this base.
    *
    * @param parent  the containing behavior
    * @param nMode   RESOLVED, DERIVATION or MODIFICATION
    *
    * @return a new blank derived trait of this trait's class with the
    *         specified parent and mode
    */
    protected Trait getBlankDerivedTrait(Trait parent, int nMode)
        {
        return new ReturnValue(this, (Behavior) parent, nMode);
        }

    /**
    * Create a derived/modified Trait by combining the information from this
    * Trait with the super or base level's Trait information.  Neither the
    * base ("this") nor the delta may be modified in any way.
    *
    * @param traitDelta  the Trait derivation or modification
    * @param parent      the Trait which will contain the resulting Trait or
    *                    null if the resulting Trait will not be contained
    * @param loader      the Loader object for JCS, CIM, and CD dependencies
    * @param errlist     the error list object to log error information to
    *
    * @return the result of applying the specified delta Trait to this Trait
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    protected Trait resolve(Trait traitDelta, Trait parent, Loader loader, ErrorList errlist)
            throws ComponentException
        {
        ReturnValue base    = this;
        ReturnValue delta   = (ReturnValue) resolveDelta(traitDelta, loader, errlist);
        ReturnValue derived = (ReturnValue) super.resolve(delta, parent, loader, errlist);

        // resolve return types
        if (delta.m_dt != base.m_dt)
            {
            if (getBehavior().getComponent().isSignature())
                {
                // As of Jdk5, covariant return-types are allowed in subclasses;
                // keep the subclass' return type.
                derived.m_dt = delta.m_dt;
                }
            else
                {
                Object[] aoParam = new Object[]
                    {
                    base.getBehavior().toString(),
                    delta.m_dt.toString(),
                    base.m_dt.toString(),
                    derived.toPathString()
                    };
                logError(RESOLVE_RETTYPECHANGE, WARNING, aoParam, errlist);

                // the return type must match the base
                derived.m_dt = base.m_dt;
                }
            }
        return derived;
        }

    /**
    * Create a derivation/modification Trait by determining the differences
    * between the derived Trait ("this") and the passed base Trait.  Neither
    * the derived Trait ("this") nor the base may be modified in any way.
    *
    * @param traitBase  the base Trait to extract
    * @param parent     the Trait which will contain the resulting Trait or
    *                   null if the resulting Trait will not be contained
    * @param loader     the Loader object for JCS, CIM, and CD dependencies
    * @param errlist    the error list object to log error information to
    *
    * @return the delta between this derived Trait and the base Trait
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    protected Trait extract(Trait traitBase, Trait parent, Loader loader, ErrorList errlist)
            throws ComponentException
        {
        ReturnValue derived = this;
        ReturnValue base    = (ReturnValue) traitBase;
        ReturnValue delta   = (ReturnValue) super.extract(base, parent, loader, errlist);

        // reconcile return types
        if (derived.m_dt != base.m_dt)
            {
            if (getBehavior().getComponent().isSignature())
                {
                // As of Jdk5, covariant return-types are allowed in subclasses;
                // keep the derived behavior's return type information.
                delta.m_dt = derived.m_dt;
                }
            else
                {
                Object[] aoParam = new Object[]
                    {
                    base.getBehavior().toString(),
                    derived.m_dt.toString(),
                    base.m_dt.toString(),
                    derived.toPathString()
                    };
                logError(EXTRACT_RETTYPECHANGE, WARNING, aoParam, errlist);
                
                // redundantly keep the base level's data type information
                // in order to check for mismatch on resolve
                delta.m_dt = base.m_dt;
                }
            }
        return delta;
        }


    // ----- miscellaneous Trait methods ------------------------------------

    /**
    * Reset state, discarding all information.
    */
    protected synchronized void invalidate()
        {
        super.invalidate();

        m_dt = null;
        }

    /**
    * Determine the unique name for this trait.
    *
    * A sub-trait that exists within a collection of sub-traits must have
    * two ways of being identified:
    *
    *   1)  A unique name, which is a string identifier
    *   2)  A UID as a secondary identifier (dog tag) 
    *
    * @return the primary string identifier of the trait
    */
    protected String getUniqueName()
        {
        return m_dt.getTypeString();
        }

    /**
    * Determine the unique description for this trait.
    *
    * All traits must be uniquely identifiable by a "description string".
    * This enables the origin implementation built into Trait to use the
    * description string to differentiate between Trait origins.
    *
    * @return the description string for the trait
    */
    protected String getUniqueDescription()
        {
        return DESCRIPTOR + ' ' + getUniqueName();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the behavior which contains this return value.
    *
    * @return the behavior trait that contains this return value
    */
    public Behavior getBehavior()
        {
        return (Behavior) getParentTrait();
        }


    // ----- data type

    /**
    * Access the return value data type.
    *
    * @return the data type of the return value
    */
    public DataType getDataType()
        {
        return m_dt;
        }

    /**
    * Determine if the return value data type can be set.  The return value
    * data type can only be changed if the behavior is declared at this level
    * and isn't part of an interface or the result of integration.
    *
    * @return true if the data type of the return value can be set
    */
    public boolean isDataTypeSettable()
        {
        Behavior behavior = getBehavior();
        return isModifiable() && !behavior.isFromNonManual();
        }

    /**
    * Determine if the specified data type is a legal data type value for
    * this return value.
    *
    * @return true if the specified data type is legal for this return value
    */
    public boolean isDataTypeLegal(DataType dt)
        {
        // data type is required
        return (dt != null);
        }

    /**
    * Set the return value data type.
    *
    * @param dt  the new data type for the return value
    */
    public void setDataType(DataType dt)
            throws PropertyVetoException
        {
        setDataType(dt, true);
        }

    /**
    * Set the return value data type.
    *
    * @param dt         the new data type for the return value
    * @param fVetoable  true if the setter can veto the value
    */
    protected synchronized void setDataType(DataType dt, boolean fVetoable)
            throws PropertyVetoException
        {
        DataType dtPrev = m_dt;

        if (dt == dtPrev)
            {
            return;
            }

        if (fVetoable)
            {
            if (!isDataTypeSettable())
                {
                readOnlyAttribute(ATTR_DATATYPE, dtPrev, dt);
                }
        
            if (!isDataTypeLegal(dt))
                {
                illegalAttributeValue(ATTR_DATATYPE, dtPrev, dt);
                }

            fireVetoableChange(ATTR_DATATYPE, dtPrev, dt);
            }

        m_dt = dt;
        firePropertyChange(ATTR_DATATYPE, dtPrev, dt);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare this ReturnValue to another Object for equality.
    *
    * @param obj  the other Object to compare to this
    *
    * @return true if this ReturnValue equals that Object
    */
    public boolean equals(Object obj)
        {
        if (obj instanceof ReturnValue)
            {
            ReturnValue that = (ReturnValue) obj;
            return this      == that
                || this.m_dt == that.m_dt
                && super.equals(that);
            }
        return false;
        }


    // ----- debugging ------------------------------------------------------

    /**
    * Provide the entire set of trait information in a printed format.
    *
    * @param out      PrintWriter object to dump the information to
    * @param sIndent  a string used to indent each line of dumped information
    */
    public void dump(PrintWriter out, String sIndent)
        {
        out.println(sIndent + DESCRIPTOR + " type=" +
                (m_dt == null ? "<null>" : m_dt.toString()));

        super.dump(out, sIndent);
        }


    // ----- public constants  ----------------------------------------------

    /**
    * The DataType attribute.
    */
    public static final String ATTR_DATATYPE = "DataType";


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "ReturnValue";

    /**
    * The ReturnValue's descriptor string.
    */
    protected static final String DESCRIPTOR = CLASS;

    /**
    * The return value's data type.
    */
    private DataType m_dt;
    }
