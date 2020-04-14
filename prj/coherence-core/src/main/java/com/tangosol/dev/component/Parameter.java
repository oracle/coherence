/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ErrorList;

import java.beans.PropertyVetoException;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;


/**
* A Parameter is an object that describes a Component Definition behavior
* parameter.  A parameter has three specific attributes:  name, type, and
* direction.
*
* @version 1.00, 11/13/97
* @author  Cameron Purdy
*/
public class Parameter
        extends Trait
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a parameter.  This is the "default" constructor.
    *
    * @param behavior  the containing behavior
    * @param dt        the parameter's data type
    * @param sName     the parameter's name
    * @param nDir      the parameter's flags -- currently only direction
    *                  (in, out, in/out)
    */
    protected Parameter(Behavior behavior, DataType dt, String sName, int nDir)
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

        // assertion:  data type cannot be VOID
        if (dt == dt.VOID)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Illegal DataType (" + dt.toString() + ")");
            }

        // TODO disallow a CORBA holder class?
        // if (dt.isClass() && dt.getClassName().endsWith("Holder")) ...

        // assertion:  name required and must be a legal Java identifier
        if (sName == null || sName.length() == 0 ||
                !ClassHelper.isSimpleNameLegal(sName))
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Illegal name (" + sName + ")");
            }

        if (nDir != DIR_IN && nDir != DIR_OUT && nDir != DIR_INOUT)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Illegal direction (" + nDir + ")");
            }

        m_sName  = sName;
        m_dt     = dt;
        m_nFlags = nDir;

        // all parameters of non-Signature Behaviors require a UID
        if (!behavior.getComponent().isSignature())
            {
            assignUID();
            }
        }

    /**
    * Construct a blank Parameter Trait.
    *
    * @param base      the base Parameter to derive from
    * @param behavior  the containing behavior
    * @param nMode     one of RESOLVED, DERIVATION, MODIFICATION
    *
    * @see #getBlankDerivedTrait(Trait, int)
    */
    protected Parameter(Parameter base, Behavior behavior, int nMode)
        {
        super(base, behavior, nMode);

        this.m_sName  = base.m_sName;
        this.m_dt     = base.m_dt;
        this.m_nFlags = base.m_nFlags;
        }

    /**
    * Copy constructor.
    *
    * @param behavior  the Behavior containing the new Parameter
    * @param that      the Parameter to copy from
    */
    protected Parameter(Behavior behavior, Parameter that)
        {
        super(behavior, that);

        this.m_sName  = that.m_sName;
        this.m_dt     = that.m_dt;
        this.m_nFlags = that.m_nFlags;
        }

    /**
    * Construct a Parameter from a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param behavior  the containing behavior
    * @param stream    the stream to read this Parameter from
    * @param nVersion  version of the data structure in the stream
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            reading the Parameter from the stream or if the stream does
    *            not contain a valid Parameter
    */
    protected Parameter(Behavior behavior, DataInput stream, int nVersion)
            throws IOException
        {
        super(behavior, stream, nVersion);

        m_sName  = stream.readUTF();
        m_dt     = DataType.getType(stream.readUTF());
        m_nFlags = stream.readInt();
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
    protected Parameter(Trait parent, XmlElement xml, int nVersion)
            throws IOException
        {
        super(parent, xml, nVersion);

        String sName = readString(xml.getElement("name"));
        String sType = readString(xml.getElement("type"));
        if (sName == BLANK || sType == BLANK)
            {
            throw new IOException("name or type is missing");
            }
        int nFlags = readFlags(xml, "flags", DIR_IN);

        m_sName  = sName;
        m_dt     = DataType.getType(sType);
        m_nFlags = nFlags;
        }


    // ----- persistence ----------------------------------------------------

    /**
    * Save the Parameter to a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param stream   the stream to write this Parameter to
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            writing the Parameter to the stream
    */
    protected synchronized void save(DataOutput stream)
            throws IOException
        {
        super.save(stream);

        stream.writeUTF(m_sName);
        stream.writeUTF(m_dt.getTypeString());
        stream.writeInt(m_nFlags);
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
        xml.addElement("name").setString(m_sName);
        xml.addElement("type").setString(m_dt.getTypeString());

        super.save(xml);

        saveFlags(xml, "flags", m_nFlags, DIR_IN);
        }


    // ----- derivation/modification ----------------------------------------

    /**
    * Construct a blank Parameter from this base.
    *
    * @param parent  the containing behavior
    * @param nMode   RESOLVED, DERIVATION or MODIFICATION
    *
    * @return a new blank derived trait of this trait's class with the
    *         specified parent and mode
    */
    protected Trait getBlankDerivedTrait(Trait parent, int nMode)
        {
        return new Parameter(this, (Behavior) parent, nMode);
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
        Parameter base    = this;
        Parameter delta   = (Parameter) resolveDelta(traitDelta, loader, errlist);
        Parameter derived = (Parameter) super.resolve(delta, parent, loader, errlist);

        delta.verifyMatch(base, true, errlist);

        // the parameter information must match the base
        derived.m_dt     = base.m_dt;
        derived.m_sName  = base.m_sName;
        derived.m_nFlags = base.m_nFlags;

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
        Parameter derived = this;
        Parameter base    = (Parameter) traitBase;
        Parameter delta   = (Parameter) super.extract(base, parent, loader, errlist);

        derived.verifyMatch(base, false, errlist);

        // redundantly keep the base level's parameter information
        // in order to check for mismatch on resolve
        delta.m_dt     = base.m_dt;
        delta.m_sName  = base.m_sName;
        delta.m_nFlags = base.m_nFlags;

        return delta;
        }

    /**
    * Check for illegal mismatches between the base and this parameter.
    *
    * @param base      the super or base level's Parameter object
    * @param fResolve  true if being RESOLVED
    * @param errlist   the error list to log any mismatches to
    *
    * @exception DerivationException
    */
    protected void verifyMatch(Parameter base, boolean fResolve, ErrorList errlist)
            throws DerivationException
        {
        // check for data type mismatch
        if (this.m_dt != base.m_dt)
            {
            String sCode = (fResolve ? RESOLVE_PARAMTYPECHANGE
                                     : EXTRACT_PARAMTYPECHANGE);
            Object[] aoParam = new Object[]
                {
                Integer.toString(base.getBehavior().getParameterPosition(base) + 1),
                base.getBehavior().toString(),
                this.m_dt.toString(),
                base.m_dt.toString(),
                toPathString()
                };
            logError(sCode, WARNING, aoParam, errlist);
            }

        // check for name mismatch
        if (!this.getBehavior().getComponent().isSignature()
            && !this.m_sName.equals(base.m_sName))
            {
            String sCode = (fResolve ? RESOLVE_PARAMNAMECHANGE
                            : EXTRACT_PARAMNAMECHANGE);
            Object[] aoParam = new Object[]
                {
                Integer.toString(base.getBehavior().getParameterPosition(base) + 1),
                base.getBehavior().toString(),
                this.m_sName,
                base.m_sName,
                toPathString()
                };
            logError(sCode, WARNING, aoParam, errlist);
            }

        // check for direction mismatch
        if (this.m_nFlags != base.m_nFlags)
            {
            String sCode = (fResolve ? RESOLVE_PARAMDIRCHANGE
                                     : EXTRACT_PARAMDIRCHANGE);
            Object[] aoParam = new Object[]
                {
                Integer.toString(base.getBehavior().getParameterPosition(base) + 1),
                base.getBehavior().toString(),
                getDirectionString(this.m_nFlags),
                getDirectionString(base.m_nFlags),
                toPathString()
                };
            logError(sCode, WARNING, aoParam, errlist);
            }
        }


    // ----- miscellaneous Trait methods ------------------------------------

    /**
    * Reset state, discarding all information.
    */
    protected synchronized void invalidate()
        {
        super.invalidate();

        m_dt    = null;
        m_sName = null;
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
        // note:  name _can_ change but parameter is not used as a
        // trait origin for other traits
        return m_sName;
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
    * Determine the Behavior which contains this Parameter.
    *
    * @return the Behavior trait that contains this Parameter
    */
    public Behavior getBehavior()
        {
        return (Behavior) getParentTrait();
        }


    // ----- data type

    /**
    * Access the parameter data type.
    *
    * @return the data type of the parameter
    */
    public DataType getDataType()
        {
        return m_dt;
        }

    /**
    * Determine if the parameter data type can be set.  The parameter data
    * type can only be changed if the behavior is declared at this level
    * and isn't part of an interface or the result of integration.
    *
    * @return true if the data type of the parameter can be set
    */
    public boolean isDataTypeSettable()
        {
        return getBehavior().isParameterSettable();
        }

    /**
    * Determine if the specified data type is a legal data type value for
    * this parameter.
    *
    * @return true if the specified data type is legal for this parameter
    */
    public boolean isDataTypeLegal(DataType dt)
        {
        // data type is required
        if (dt == null || dt == dt.VOID)
            {
            return false;
            }

        // TODO disallow a CORBA holder class?
        // if (dt.isClass() && dt.getClassName().endsWith("Holder")) ...

        // check for no change
        if (dt == m_dt)
            {
            return true;
            }

        // build what the behavior signature would be if this type changed
        Behavior behavior = getBehavior();
        DataType[] adt = behavior.getParameterTypes();
        adt[behavior.getParameterPosition(this)] = dt;
        String sSig = Behavior.getSignature(behavior.getName(), adt);

        // verify that the signature is not reserved
        return !behavior.isSignatureReserved(sSig);
        }

    /**
    * Set the parameter data type.
    *
    * @param dt  the new data type for the parameter
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void setDataType(DataType dt)
            throws PropertyVetoException
        {
        setDataType(dt, true);
        }

    /**
    * Set the parameter data type.
    *
    * @param dt         the new data type for the parameter
    * @param fVetoable  true if the setter can veto the value
    *
    * @exception PropertyVetoException if the new attribute value is not
    *            accepted
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
        getBehavior().updateSignature();

        firePropertyChange(ATTR_DATATYPE, dtPrev, dt);
        }


    // ----- signature

    /**
    * Get the signature for the parameter.  The parameter signature is
    * used to build the behavior signature.
    *
    * @return the signature for the parameter
    */
    public String getSignature()
        {
        return m_dt.getTypeString();
        }


    // ----- name

    /**
    * Access the parameter name.
    *
    * @return the name of the parameter
    */
    public String getName()
        {
        return m_sName;
        }

    /**
    * Determine if the parameter name can be set.  The parameter name
    * type can only be set if the behavior is declared at this level,
    * either manually or as a result of implementing an interface
    * or itegration.
    *
    * @return true if the name of the parameter can be set
    */
    public boolean isNameSettable()
        {
        return isModifiable() && getBehavior().isDeclaredAtThisLevel();
        }

    /**
    * Determine if the specified name is a legal parameter name and that
    * it won't conflict with other parameter names.
    *
    * @return true if the specified name is legal for this parameter
    */
    public boolean isNameLegal(String sName)
        {
        // must be a legal Java identifier
        if (sName == null || sName.length() == 0 ||
                !ClassHelper.isSimpleNameLegal(sName))
            {
            return false;
            }

        // check for no change
        if (sName.equals(m_sName))
            {
            return true;
            }

        // check for conflict with other parameter names
        Parameter param = getBehavior().getParameter(sName);
        return param == null || param == this;
        }

    /**
    * Set the parameter name.
    *
    * @param sName the new name for the parameter
    *
    * @exception PropertyVetoException if the new attribute value is not
    *            accepted
    */
    public void setName(String sName)
            throws PropertyVetoException
        {
        setName(sName, true);
        }

    /**
    * Set the parameter name.
    *
    * @param sName      the new name for the parameter
    * @param fVetoable  true if the setter can veto the value
    *
    * @exception PropertyVetoException if the new attribute value is not
    *            accepted
    */
    protected synchronized void setName(String sName, boolean fVetoable)
            throws PropertyVetoException
        {
        String sPrev = m_sName;

        if (sName.equals(sPrev))
            {
            return;
            }

        if (fVetoable)
            {
            if (!isNameSettable())
                {
                readOnlyAttribute(ATTR_NAME, sPrev, sName);
                }

            if (!isNameLegal(sName))
                {
                illegalAttributeValue(ATTR_NAME, sPrev, sName);
                }

            fireVetoableChange(ATTR_NAME, sPrev, sName);
            }

        m_sName = sName;
        firePropertyChange(ATTR_NAME, sPrev, sName);
        }


    // ----- direction

    /**
    * Access the parameter direction.
    *
    * @return the direction of the parameter
    */
    public int getDirection()
        {
        return m_nFlags & DIR_MASK;
        }

    /**
    * Determine if the parameter direction can be set.  The parameter
    * direction can only be changed if the behavior is declared at this level
    * and isn't part of an interface or the result of integration.  The
    * direction attribute is only applicable for distributable Behaviors.
    *
    * @return true if the direction of the parameter can be set
    */
    public boolean isDirectionSettable()
        {
        Behavior behavior = getBehavior();
        return behavior.isParameterSettable() && behavior.isRemote();
        }

    /**
    * Determine if the specified direction is a legal direction value for
    * this parameter.
    *
    * @return true if the specified direction is legal for this parameter
    */
    public boolean isDirectionLegal(int nDir)
        {
        return nDir == DIR_IN || nDir == DIR_OUT || nDir == DIR_INOUT;
        }

    /**
    * Set the parameter direction.
    *
    * @param nDir  the new direction for the parameter
    */
    public void setDirection(int nDir)
            throws PropertyVetoException
        {
        setDirection(nDir, true);
        }

    /**
    * Set the parameter direction.
    *
    * @param nDir       the new direction for the parameter
    * @param fVetoable  true if the setter can veto the value
    */
    protected synchronized void setDirection(int nDir, boolean fVetoable)
            throws PropertyVetoException
        {
        int nPrev = getDirection();

        if (nDir == nPrev)
            {
            return;
            }

        Integer prev  = Integer.valueOf(nPrev);
        Integer value = Integer.valueOf(nDir);

        if (fVetoable)
            {
            if (!isDirectionSettable())
                {
                readOnlyAttribute(ATTR_DIRECTION, prev, value);
                }

            if (!isDirectionLegal(nDir))
                {
                illegalAttributeValue(ATTR_DIRECTION, prev, value);
                }

            fireVetoableChange(ATTR_DIRECTION, prev, value);
            }

        m_nFlags = m_nFlags & ~DIR_MASK | nDir;
        firePropertyChange(ATTR_DIRECTION, prev, value);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare this Parameter to another Object for equality.
    *
    * @param obj  the other Object to compare to this
    *
    * @return true if this Parameter equals that Object
    */
    public boolean equals(Object obj)
        {
        if (obj instanceof Parameter)
            {
            Parameter that = (Parameter) obj;
            return this          == that
                || this.m_nFlags == that.m_nFlags
                && this.m_dt     == that.m_dt
                && this.m_sName.equals(that.m_sName)
                && super.equals(that);
            }
        return false;
        }

    /**
    * Provide a description of the parameter.
    *
    * @return a human-readable description of the parameter
    */
    public String toString()
        {
        StringBuffer sb = new StringBuffer();

        sb.append(m_dt.toString())
          .append(' ')
          .append(m_sName)
          .append(' ')
          .append(getDirectionString(getDirection()));

        return sb.toString();
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
        out.print  (sIndent + "Parameter type=" + m_dt.getTypeString());
        out.print  (" (" + m_dt.toString() + "), name=" + m_sName);
        out.println(", direction=" + getDirectionString(getDirection()));

        super.dump(out, sIndent);
        }

    /**
    * Provide a text description for a direction flag.
    *
    * @param nDir  the direction flag
    *
    * @return a text description for the direction flag
    */
    public static String getDirectionString(int nDir)
        {
        switch (nDir & DIR_MASK)
            {
            case DIR_IN:
                return DIR_IN_TEXT;
            case DIR_OUT:
                return DIR_OUT_TEXT;
            case DIR_INOUT:
                return DIR_INOUT_TEXT;
            default:
                return "<invalid direction>";
            }
        }


    // ----- public constants  ----------------------------------------------

    /**
    * The DataType attribute.
    */
    public static final String ATTR_DATATYPE    = "DataType";

    /**
    * The Name attribute.
    */
    public static final String ATTR_NAME        = "Name";

    /**
    * The Direction attribute.
    */
    public static final String ATTR_DIRECTION   = "Direction";


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Parameter";

    /**
    * The Parameter's descriptor string.
    */
    protected static final String DESCRIPTOR = CLASS;

    /**
    * The parameter's name.
    */
    private String m_sName;

    /**
    * The parameter's data type.
    */
    private DataType m_dt;

    /**
    * The parameter's attribute flags (currently only direction is used).
    */
    private int m_nFlags;
    }
