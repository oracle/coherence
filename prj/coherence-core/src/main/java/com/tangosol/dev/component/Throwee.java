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

import java.util.Enumeration;

import com.tangosol.util.ErrorList;
import com.tangosol.run.xml.XmlElement;


/**
* A Throwee describes a Java exception which is declared (i.e. potentially
* thrown) by a Behavior.
*
* @version 0.50, 12/16/97
* @version 1.00, 08/13/98
* @author  Cameron Purdy
*/
public class Throwee
        extends Trait
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a Throwee.  This is the "default" constructor.
    *
    * @param behavior  the behavior that declares this exception throwable
    * @param dt        the exception data type
    */
    protected Throwee(Behavior behavior, DataType dt)
        {
        super(behavior, RESOLVED);

        // assertion:  behavior required
        if (behavior == null)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Containing behavior required.");
            }

        // verify that the data type is a Java class; there is no way to
        // verify that it ultimately derives from java.lang.Exception
        if (dt == null || !dt.isClass())
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Illegal Throwee DataType (" + dt + ")");
            }

        m_dt     = dt;
        m_nFlags = EXISTS_INSERT;

        // all exceptions of non-Signature Behaviors require a UID
        if (!behavior.getComponent().isSignature())
            {
            assignUID();
            }
        }

    /**
    * Construct a blank Throwee Trait.
    *
    * @param base      the base Throwee to derive from
    * @param behavior  the containing behavior
    * @param nMode     one of RESOLVED, DERIVATION, MODIFICATION
    *
    * @see #getBlankDerivedTrait(Trait, int)
    */
    protected Throwee(Throwee base, Behavior behavior, int nMode)
        {
        super(base, behavior, nMode);

        this.m_dt     = base.m_dt;
        this.m_nFlags = EXISTS_UPDATE;
        }

    /**
    * Copy constructor.
    *
    * @param behavior  the Behavior containing the new Throwee
    * @param that      the Throwee to copy from
    */
    protected Throwee(Behavior behavior, Throwee that)
        {
        super(behavior, that);

        this.m_dt     = that.m_dt;
        this.m_nFlags = that.m_nFlags;
        }

    /**
    * Construct a Throwee from a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param behavior  the containing behavior
    * @param stream    the stream to read this Behavior Exception from
    * @param nVersion  version of the data structure in the stream
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            reading the Behavior Exception from the stream or if the
    *            stream does not contain a valid Behavior Exception
    */
    protected Throwee(Behavior behavior, DataInput stream, int nVersion)
            throws IOException
        {
        super(behavior, stream, nVersion);

        m_nFlags = stream.readInt();
        m_dt     = DataType.getType(stream.readUTF());
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
    protected Throwee(Trait parent, XmlElement xml, int nVersion)
            throws IOException
        {
        super(parent, xml, nVersion);

        String sType = readString(xml.getElement("type"));
        if (sType == BLANK)
            {
            throw new IOException("type is missing");
            }
        int nFlags = readFlags(xml, "flags", EXISTS_UPDATE);

        m_dt     = DataType.getType(sType);
        m_nFlags = nFlags;
        }


    // ----- persistence ----------------------------------------------------

    /**
    * Save the Behavior Exception to a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param stream   the stream to write this Behavior Exception to
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            writing the Behavior Exception to the stream
    */
    protected synchronized void save(DataOutput stream)
            throws IOException
        {
        super.save(stream);

        stream.writeInt(m_nFlags);
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
        
        saveFlags(xml, "flags", m_nFlags, EXISTS_UPDATE);
        }


    // ----- derivation/modification ----------------------------------------

    /**
    * Construct a blank Throwee from this base.
    *
    * @param parent  the containing behavior
    * @param nMode   RESOLVED, DERIVATION or MODIFICATION
    *
    * @return a new blank derived trait of this trait's class with the
    *         specified parent and mode
    */
    protected Trait getBlankDerivedTrait(Trait parent, int nMode)
        {
        return new Throwee(this, (Behavior) parent, nMode);
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
        Throwee base    = this;
        Throwee delta   = (Throwee) resolveDelta(traitDelta, loader, errlist);
        Throwee derived = (Throwee) super.resolve(delta, parent, loader, errlist);

        // Exists
        int     nBaseFlags     = base .m_nFlags;
        int     nDeltaFlags    = delta.m_nFlags;
        int     nDerivedFlags  = nDeltaFlags;
        if (base.getMode() == RESOLVED)
            {
            // when an exception is declared, it has the exists state of INSERT
            // and subsequent derivation/modification levels are UPDATE
            // when an exception is removed, it has the exists state of DELETE
            // and subsequent derivation/modification levels are NOT
            int nBaseExists = nBaseFlags  & EXISTS_MASK;
            if (nBaseExists == EXISTS_DELETE || nBaseExists == EXISTS_NOT)
                {
                nDerivedFlags = EXISTS_NOT;
                }
            else if ((nDeltaFlags & EXISTS_SPECIFIED) == 0)
                {
                nDerivedFlags = EXISTS_UPDATE;
                }
            }
        else
            {
            // modifying a derivation or modification:  use the base level's
            // exists information if no exists information is present at this
            // level
            if ((nDeltaFlags & EXISTS_SPECIFIED) == 0 &&
                (nBaseFlags  & EXISTS_SPECIFIED) != 0)
                {
                nDerivedFlags = nBaseFlags;
                }
            }
        derived.m_nFlags = nDerivedFlags;

        // check for data type mismatch
        if (delta.m_dt != base.m_dt)
            {
            Object[] aoParam = new Object[]
                {
                base.getBehavior().toString(),
                delta.m_dt.toString(),
                base.m_dt.toString(),
                derived.toPathString()
                };
            logError(RESOLVE_EXCEPTTYPECHANGE, WARNING, aoParam, errlist);
            }

        // the exception type must match the base
        derived.m_dt = base.m_dt;

        return derived;
        }

    /**
    * Finalize the resolve process.  This means that this trait will not
    * be asked to resolve again.  A trait is considered designable after
    * it has finalized the resolve process.
    *
    * @param loader    the Loader object for JCS, CIM, and CD dependencies
    * @param errlist   the error list object to log error information to
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    protected void finalizeResolve(Loader loader, ErrorList errlist)
            throws ComponentException
        {
        super.finalizeResolve(loader, errlist);

        // specified flags are only for deltas
        m_nFlags &= ~EXISTS_SPECIFIED;

        // if this is declared at this level, make sure it is an insert
        if (isDeclaredAtThisLevel() && m_nFlags != EXISTS_DELETE)
            {
            m_nFlags = EXISTS_INSERT;
            }
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
        Throwee derived = this;
        Throwee base    = (Throwee) traitBase;
        Throwee delta   = (Throwee) super.extract(base, parent, loader, errlist);

        // Exists:  if the throwee is removed at this level, then a DELETE is
        // specified in the derivation/modification, otherwise exists is not
        // specified
        if ((derived.m_nFlags & EXISTS_MASK) == EXISTS_DELETE
            && (base.m_nFlags == EXISTS_INSERT || base.m_nFlags == EXISTS_UPDATE)
            && (base.getMode() == RESOLVED || (derived.m_nFlags & EXISTS_SPECIFIED) != 0))
            {
            delta.m_nFlags = EXISTS_SPECIFIED | EXISTS_DELETE;
            }
        else
            {
            delta.m_nFlags = EXISTS_UPDATE;
            }

        // check for data type mismatch
        if (derived.m_dt != base.m_dt)
            {
            Object[] aoParam = new Object[]
                {
                base.getBehavior().toString(),
                derived.m_dt.toString(),
                base.m_dt.toString(),
                derived.toPathString()
                };
            logError(EXTRACT_EXCEPTTYPECHANGE, WARNING, aoParam, errlist);
            }

        // redundantly keep the base level's data type information
        // in order to check for mismatch on resolve
        delta.m_dt = base.m_dt;

        return delta;
        }

    /**
    * Determines if this exception exists for any reason at all.  If an
    * exception exists, for example, as part of an interface, and the
    * interface changes, removing the exception, then the exception will be
    * discarded as part of the Behavior's resolve finalization processing.
    *
    * @return true if the Behavior Exception can be discarded
    */
    protected boolean isDiscardable()
        {
        int nMode = getMode();
        if (nMode == DERIVATION || nMode == MODIFICATION)
            {
            // delta is not discardable if EXISTS is specified
            if ((m_nFlags & EXISTS_SPECIFIED) != 0)
                {
                return false;
                }
            }

        return super.isDiscardable();
        }

    /**
    * Determine whether this trait contains information that would cause
    * generation of a class that would operate differently than the class
    * generated from the information maintained by the super trait.
    *
    * @param base     the base (i.e. the super) Trait to compare to
    *
    * @return true if a delta between this trait and its super trait means
    *         that class generation should generate the class corresponding
    *         to this trait, otherwise false
    */
    protected boolean isClassDiscardable(Trait base)
        {
        Throwee that = (Throwee) base;
        if (that == null || !isFromSuper())
            {
            return false;
            }

        int nExistsThis = (this.m_nFlags & EXISTS_MASK);
        int nExistsThat = (that.m_nFlags & EXISTS_MASK);
        if (nExistsThis != nExistsThat)
            {
            boolean fThisThrowable = (nExistsThis == EXISTS_INSERT || nExistsThis == EXISTS_UPDATE);
            boolean fThatThrowable = (nExistsThat == EXISTS_INSERT || nExistsThat == EXISTS_UPDATE);
            if (fThisThrowable != fThatThrowable)
                {
                return false;
                }
            }
        
        return super.isClassDiscardable(base);
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
        return m_dt.getClassName();
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
    * Determine the behavior which contains this Behavior Exception.
    *
    * @return the behavior trait that contains this Behavior Exception
    */
    public Behavior getBehavior()
        {
        return (Behavior) getParentTrait();
        }


    // ----- exists

    /**
    * Access the Behavior Exception exists flag.  This is only used
    * internally by the Component Definition.
    *
    * @return the enumerated exists flag
    */
    protected int getExists()
        {
        return m_nFlags;
        }

    /**
    * Set the Behavior Exception exists flag.  This is only used internally
    * by the Component Definition.
    *
    * @param nExists  the new exists flag for the Behavior Exception
    */
    protected synchronized void setExists(int nExists)
        {
        m_nFlags = (nExists & EXISTS_MASK);
        }


    // ----- origin:  exception implements interface(s)

    /**
    * Determine if this exception is from an interface.
    *
    * @return true if this exception results from an interface at this level
    */
    public boolean isFromImplements()
        {
        return isFromTraitDescriptor(Interface.DESCRIPTOR_IMPLEMENTS);
        }

    /**
    * Enumerate the interface names that exist at this level which declare
    * this exception.
    *
    * @return an enumeration of interface names
    */
    public Enumeration enumImplements()
        {
        return getOriginTraits(Interface.DESCRIPTOR_IMPLEMENTS);
        }


    // ----- origin:  exception dispatches interface(s)

    /**
    * Determine if this exception is from a dispatched interface.
    *
    * @return true if this exception results from a dispatched interface at
    *         this level
    */
    public boolean isFromDispatches()
        {
        return isFromTraitDescriptor(Interface.DESCRIPTOR_DISPATCHES);
        }

    /**
    * Enumerate the dispatch interface names that exist at this level which
    * declare this exception.
    *
    * @return an enumeration of dispatch interface names
    */
    public Enumeration enumDispatches()
        {
        return getOriginTraits(Interface.DESCRIPTOR_DISPATCHES);
        }


    // ----- origin:  exception integrates

    /**
    * Determine if this exception exists as a result of integration at this
    * level.
    *
    * @return true if this exception results from integration at this level
    */
    public boolean isFromIntegration()
        {
        return isFromTraitDescriptor(Integration.DESCRIPTOR);
        }


    // ----- origin:  exception from a property

    /**
    * Determine if this exception exists due to a property.
    *
    * @return true if this exception results from a property
    */
    public boolean isFromProperty()
        {
        return isFromTraitDescriptor(Property.DESCRIPTOR);
        }


    // ----- origin:  exception declared "manually"

    /**
    * Determine if the manual origin flag can be set.  It cannot be set if
    * the exception's only origin is manual.
    *
    * @return true if the manual origin flag can be set
    */
    public boolean isFromManualSettable()
        {
        return isDeclaredAtThisLevel() && isFromNonManual() && isModifiable();
        }

    /**
    * Set whether or not this exception exists at this level regardless of
    * whether it exists from derivation, implements, dispatches, or
    * integrates.
    *
    * @param fManual
    */
    public void setFromManual(boolean fManual)
            throws PropertyVetoException
        {
        setFromManual(fManual, true);
        }

    /**
    * Set whether or not this exception exists at this level regardless of
    * whether it exists from derivation, implements, dispatches, or
    * integrates.
    *
    * @param fManual
    * @param fVetoable  true if the setter can veto the value
    */
    protected synchronized void setFromManual(boolean fManual, boolean fVetoable)
            throws PropertyVetoException
        {
        boolean fPrev = isFromManual();

        if (fManual == fPrev)
            {
            return;
            }

        Boolean prev  = toBoolean(fPrev);
        Boolean value = toBoolean(fManual);

        if (fVetoable)
            {
            if (!isFromManualSettable())
                {
                readOnlyAttribute(ATTR_FROMMANUAL, prev, value);
                }
        
            fireVetoableChange(ATTR_FROMMANUAL, prev, value);
            }

        if (fManual)
            {
            setFromManual();
            }
        else
            {
            clearFromManual();
            }

        firePropertyChange(ATTR_FROMMANUAL, prev, value);
        }


    // ----- throwable

    /**
    * A Behavior may not be able to throw all of its declared exceptions.
    * Even if an exception cannot be thrown, the details of the exception
    * and its origin are maintained.
    *
    * For example, if a Behavior is declared by two interfaces which each
    * declare a different exception, the Behavior may throw neither.
    *
    * @return true if the exception is throwable
    */
    public boolean isThrowable()
        {
        int nExists = getExists();
        return (nExists == EXISTS_INSERT || nExists == EXISTS_UPDATE);
        }


    // ----- data type

    /**
    * Access the Behavior Exception data type.
    *
    * @return the data type of the Behavior Exception
    */
    public DataType getDataType()
        {
        return m_dt;
        }

    /**
    * Determine if the Behavior Exception data type can be set.  The Behavior Exception
    * data type can only be changed if the behavior is declared at this level
    * and isn't part of an interface or the result of integration.
    *
    * @return true if the data type of the Behavior Exception can be set
    */
    public boolean isDataTypeSettable()
        {
        return isModifiable() && !isFromNonManual();
        }

    /**
    * Determine if the specified data type is a legal data type value for
    * this Behavior Exception.
    *
    * @return true if the specified data type is legal for this Behavior Exception
    */
    public boolean isDataTypeLegal(DataType dt)
        {
        // data type is required and must be a Java class; there is no way
        // at this point to verify if the type is a Java exception class
        if (dt == null || !dt.isClass())
            {
            return false;
            }

        // check for no change
        if (dt == m_dt)
            {
            return true;
            }

        Throwee except = getBehavior().getException(dt.getClassName());
        return except == null || except == this;
        }

    /**
    * Set the Behavior Exception data type.
    *
    * @param dt  the new data type for the Behavior Exception
    */
    public void setDataType(DataType dt)
            throws PropertyVetoException
        {
        setDataType(dt, true);
        }

    /**
    * Set the Behavior Exception data type.
    *
    * @param dt         the new data type for the Behavior Exception
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

        // update the behavior's exception table
        getBehavior().renameException(dtPrev.getClassName(), dt.getClassName());

        firePropertyChange(ATTR_DATATYPE, dtPrev, dt);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare this Throwee to another Object for equality.
    *
    * @param obj  the other Object to compare to this
    *
    * @return true if this Throwee equals that Object
    */
    public boolean equals(Object obj)
        {
        if (obj instanceof Throwee)
            {
            Throwee that = (Throwee) obj;
            return this          ==     that
                || this.m_nFlags ==     that.m_nFlags
                && this.m_dt     ==     that.m_dt
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
        boolean fDerOrMod = (getMode() == MODIFICATION || getMode() == DERIVATION);

        out.println(sIndent + "Exception " + (m_dt == null ? "<null>" : m_dt.toString()));

        out.print  (sIndent + "flags=0x" + Integer.toHexString(m_nFlags));
        out.println(" (" + flagsDescription(m_nFlags, SPECABLE_FLAGS, fDerOrMod) + ')');

        super.dump(out, sIndent);
        }


    // ----- public constants  ----------------------------------------------

    /**
    * The FromManual attribute.
    */
    public static final String ATTR_FROMMANUAL = "FromManual";

    /**
    * The DataType attribute.
    */
    public static final String ATTR_DATATYPE = "DataType";


    // ----- other constants ------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Throwee";

    /**
    * The Throwee's descriptor string.
    */
    protected static final String DESCRIPTOR = "Exception";

    /**
    * The flags that the Throwee Trait can specify.
    */
    private static final int SPECABLE_FLAGS = EXISTS_SPECIFIED;


    // ----- data members ---------------------------------------------------

    /**
    * The Behavior Exception's data type.
    */
    private DataType m_dt;

    /**
    * The Behavior Exception has various attributes stored as bit flags.
    * The attributes are:  Exists.
    */
    private int m_nFlags;
    }
