/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import com.tangosol.dev.assembler.Method;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ErrorList;
import com.tangosol.util.StringTable;
import com.tangosol.util.UID;
import com.tangosol.util.ChainedEnumerator;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.run.xml.XmlElement;

import java.beans.PropertyVetoException;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;

import java.text.CollationKey;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.List;
import java.util.Iterator;


/**
* A Behavior represents the set of information that describes a component's
* method or event handler declaration and definition.
*
* A Behavior and its contained hierarchy are kept or discarded as a unit.
* This allows any level that has any information specified at that level to
* recover the entire Behavior if the super/base level discards the Behavior.
*
* @version 0.50, 11/11/97
* @version 1.00, 08/17/98
* @author  Cameron Purdy
*/
public class Behavior
        extends Trait
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a behavior.  This is the "default" constructor.
    *
    * @param cd         the containing Component Definition
    * @param dtRet      the return value's data type
    * @param sName      the name of the behavior
    * @param fStatic    the behavior's scope (static or instance)
    * @param nAccess    the behavior's accessibility
    * @param adtParam   the behavior's parameter data types
    * @param asParam    the parameter names (or null for defaults)
    * @param anDir      the parameter directions (or null for defaults)
    */
    protected Behavior(Component cd,
            DataType dtRet, String sName, boolean fStatic, int nAccess,
            DataType[] adtParam, String[] asParam, int[] anDir)
        {
        super(cd, RESOLVED);

        m_nFlags     |= EXISTS_INSERT | nAccess        | (fStatic ? SCOPE_STATIC : SCOPE_INSTANCE);
        m_nPrevFlags |= EXISTS_NOT    | ACCESS_PRIVATE | SCOPE_INSTANCE;

        m_sName  = sName;
        m_retval = new ReturnValue(this, dtRet);

        if (adtParam != null)
            {
            int c = adtParam.length;
            for (int i = 0; i < c; ++i)
                {
                DataType  dt     = adtParam[i];
                String    sParam = (asParam == null ? "Param_" + i : asParam[i]);
                int       nDir   = (anDir   == null ? DIR_IN       : anDir  [i]);
                Parameter param  = new Parameter(this, dt, sParam, nDir);
                m_vectParam.addElement(param);
                }
            }
        updateSignature();

        // all INSERT behaviors for non-Signatures are assigned UID's
        if (!cd.isSignature())
            {
            assignUID();
            }
        }

    /**
    * Construct a Behavior for an integration method.
    *
    * @param cd      containing Component
    * @param map     the Integration trait
    * @param sName   Behavior name that the method mapped to (allowing
    *                name collisions to be resolved)
    * @param method  interface method to integrate
    */
    protected Behavior(Component cd, Integration map, String sName, Behavior method)
        {
        this(cd, method.m_retval.getDataType(), sName, method.isStatic(), method.getAccess(),
                method.getParameterTypes(), method.getParameterNames(), null);
        this.addOriginTrait(map);

        // copy exceptions
        for (Enumeration enmr = method.m_tblExcept.elements(); enmr.hasMoreElements(); )
            {
            Throwee except  = (Throwee) enmr.nextElement();
            Throwee throwee = new Throwee(this, except.getDataType());
            throwee.addOriginTrait(map);
            m_tblExcept.put(throwee.getUniqueName(), throwee);
            }
        }

    /**
    * Construct a Behavior for an interface (implements/dispatches) method.
    *
    * @param cd      containing Component (or JCS)
    * @param iface   declaring interface
    * @param method  interface method
    */
    protected Behavior(Component cd, Interface iface, Behavior method)
        {
        this(cd, method.m_retval.getDataType(), method.m_sName, method.isStatic(), method.getAccess(),
                method.getParameterTypes(), method.getParameterNames(), null);
        this.addOriginTrait(iface);

        // for a JCS of an interface that implements an interface,
        // the resulting methods are abstract unless it's a default method (Java 8)
        if (cd.isInterface() && method.isAbstract())
            {
            m_nFlags = m_nFlags & ~IMPL_MASK | IMPL_ABSTRACT;
            }

        // copy exceptions
        for (Enumeration enmr = method.m_tblExcept.elements(); enmr.hasMoreElements(); )
            {
            Throwee except  = (Throwee) enmr.nextElement();
            Throwee throwee = new Throwee(this, except.getDataType());
            throwee.addOriginTrait(iface);
            m_tblExcept.put(throwee.getUniqueName(), throwee);
            }
        }

    /**
    * Construct a Behavior for event registration.
    *
    * @param cd     containing Component
    * @param iface  event interface
    * @param sSig   behavior signature
    */
    protected Behavior(Component cd, Interface iface, String sSig)
        {
        this(cd, DataType.VOID, Interface.getDispatchesVerb(sSig) +
                Interface.getDispatchesName(sSig) + "Listener", false,
                ACCESS_PUBLIC, new DataType[] {DataType.getClassType(iface.getName())},
                EVENT_REG_PARAM, null);
        addOriginTrait(iface);
        }

    /**
    * Construct a Java Class Signature (JCS) Behavior from a JASM method.
    *
    * @param cdJCS     the containing JCS
    * @param method    the method object
    * @param mapParam  a Map that may contain String[] of param names per signature
    */
    protected Behavior(Component cdJCS, Method method, Map mapParam)
        {
        super(cdJCS, RESOLVED);

        // convert method information to Behavior flags
        int nFlags = EXISTS_INSERT | DIST_LOCAL;

        // access, visible
        if (method.isPublic())
            {
            nFlags |= ACCESS_PUBLIC    | VIS_VISIBLE;
            }
        else if (method.isProtected())
            {
            nFlags |= ACCESS_PROTECTED | VIS_VISIBLE;
            }
        else if (method.isPackage())
            {
            nFlags |= ACCESS_PACKAGE   | VIS_VISIBLE;
            }
        else if (method.isPrivate())
            {
            nFlags |= ACCESS_PRIVATE   | VIS_VISIBLE;
            }

        // mark synthetic methods as "advanced"
        if (method.isSynthetic())
            {
            nFlags |= VIS_ADVANCED;
            }

        nFlags |= (method.isDeprecated()   ? ANTIQ_DEPRECATED : ANTIQ_CURRENT   );
        nFlags |= (method.isStatic()       ? SCOPE_STATIC     : SCOPE_INSTANCE  );
        nFlags |= (method.isSynchronized() ? SYNC_MONITOR     : SYNC_NOMONITOR  );
        nFlags |= (method.isAbstract()     ? IMPL_ABSTRACT    : IMPL_CONCRETE   );
        nFlags |= (method.isFinal()        ? DERIVE_FINAL     : DERIVE_DERIVABLE);

        if (method.isBridge() && method.isSynthetic())
            {
            // use the ISINTERFACE bit for behaviors to indicate the generated
            // nature of the method (generated by a compiler or tool)
            nFlags |= MISC_ISINTERFACE;
            }

        // to allow modification of final JCS methods
        // the last line has to be commented out

        // initialize Behavior info
        m_sName  = method.getName();
        m_nFlags = nFlags;

        String[] asTypes = method.getTypes();
        String[] asNames = method.getNames();

        // Return Value
        m_retval = new ReturnValue(this, DataType.getJVMType(asTypes[0]));

        // Parameters
        int     cParams = asTypes.length - 1;
        Vector  vect    = m_vectParam;
        vect.setSize(cParams);
        for (int i = 1; i <= cParams; ++i)
            {
            DataType dt    = DataType.getJVMType(asTypes[i]);
            String   sName = "Param_" + i;
            if (asNames[i] != null)
                {
                sName = asNames[i];
                }

            vect.setElementAt(new Parameter(this, dt, sName, DIR_IN), i - 1);
            }
        updateSignature();

        // 2002-07-22 cp - fill in parameter names
        if (!mapParam.isEmpty())
            {
            // look up the source code for the behavior
            StringBuffer sb = new StringBuffer(getComponent().getName());
            sb.append('.')
              .append(getName())
              .append('(');
            DataType[] adt = getParameterTypes();
            for (int i = 0, c = adt.length; i < c; ++i)
                {
                if (i > 0)
                    {
                    sb.append(',');
                    }
                sb.append(Component.toSimpleSignature(adt[i].getType()));
                }
            sb.append(')');

            String[] asParam = (String[]) mapParam.get(sb.toString());
            if (asParam != null)
                {
                for (int i = 0, c = Math.min(asParam.length, getParameterCount()); i < c; ++i)
                    {
                    Parameter param    = getParameter(i);
                    String    sOldName = param.getName();
                    String    sNewName = asParam[i];
                    if (sNewName != null && sNewName.length() > 0 && sOldName.startsWith("Param_"))
                        {
                        try
                            {
                            param.setName(sNewName, false);
                            }
                        catch (PropertyVetoException e)
                            {
                            }
                        }
                    }
                }
            }

        // Exceptions
        StringTable tbl = m_tblExcept;
        for (Enumeration enmr = method.getExceptions(); enmr.hasMoreElements(); )
            {
            DataType dt = DataType.getClassType(((String) enmr.nextElement()).replace('/', '.'));
            tbl.put(dt.getClassName(), new Throwee(this, dt));
            }
        }

    /**
    * Construct a blank Behavior Trait.
    *
    * @param base   the base Behavior to derive from
    * @param cd     the containing Component
    * @param nMode  one of RESOLVED, DERIVATION, MODIFICATION
    *
    * @see #getBlankDerivedTrait(Trait, int)
    */
    protected Behavior(Behavior base, Component cd, int nMode)
        {
        super(base, cd, nMode);

        // only temporary; will be immediately resolved or extracted
        // (don't bother creating blank params, return value, or exceptions)
        this.m_sName  = base.m_sName;
        this.m_sSig   = base.m_sSig;
        this.m_nFlags = EXISTS_UPDATE;
        }

    /**
    * Copy constructor.
    *
    * @param cd    the Component containing the new Behavior
    * @param that  the Behavior to copy from
    */
    protected Behavior(Component cd, Behavior that)
        {
        super(cd, that);

        this.m_sName             = that.m_sName;
        this.m_sSig              = that.m_sSig;
        this.m_nFlags            = that.m_nFlags;
        this.m_nPrevFlags        = that.m_nPrevFlags;
        this.m_fOverrideBase     = that.m_fOverrideBase;
        this.m_fPrevOverrideBase = that.m_fPrevOverrideBase;
        this.m_cBaseLevelImpl    = that.m_cBaseLevelImpl;

        this.m_retval = new ReturnValue(this, that.m_retval);

        if (!that.m_vectParam.isEmpty())
            {
            Vector src  = that.m_vectParam;
            Vector dest = this.m_vectParam;
            int c = src.size();
            dest.setSize(c);
            for (int i = 0; i < c; ++i)
                {
                dest.setElementAt(new Parameter(this, (Parameter) src.elementAt(i)), i);
                }
            }

        if (!that.m_vectScript.isEmpty())
            {
            Vector src  = that.m_vectScript;
            Vector dest = this.m_vectScript;
            int c = src.size();
            dest.setSize(c);
            for (int i = 0; i < c; ++i)
                {
                dest.setElementAt(new Implementation(this, (Implementation) src.elementAt(i)), i);
                }
            }

        if (!that.m_tblExcept.isEmpty())
            {
            StringTable tblThat = that.m_tblExcept;
            StringTable tblThis = this.m_tblExcept;
            for (Enumeration enmr = tblThat.keys(); enmr.hasMoreElements(); )
                {
                String  sExcept = (String)  enmr.nextElement();
                Throwee throwee = new Throwee(this, (Throwee) tblThat.get(sExcept));
                if (cd.isSignature())
                    {
                    // TODO: explain why
                    throwee.addOriginTrait(cd);
                    }
                tblThis.put(sExcept, throwee);
                }
            this.m_tblExcept = tblThis;
            }
        }

    /**
    * Construct the behavior from a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param cd        the containing Component Definition
    * @param stream    the stream to read this Behavior from
    * @param nVersion  version of the data structure in the stream
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            reading the Behavior information from the stream
    */
    protected Behavior(Component cd, DataInput stream, int nVersion)
            throws IOException
        {
        super(cd, stream, nVersion);

        // name
        m_sName = stream.readUTF();

        // bit flags (various attributes)
        m_nFlags     = stream.readInt();
        m_nPrevFlags = stream.readInt();

        // return value trait
        m_retval = new ReturnValue(this, stream, nVersion);

        // parameter traits
        int cParam = stream.readInt();
        m_vectParam.setSize(cParam);
        for (int i = 0; i < cParam; ++i)
            {
            Parameter param = new Parameter(this, stream, nVersion);
            m_vectParam.setElementAt(param, i);
            }
        updateSignature();

        // exception traits
        int cExcept = stream.readInt();
        for (int i = 0; i < cExcept; ++i)
            {
            Throwee except = new Throwee(this, stream, nVersion);
            m_tblExcept.put(except.getUniqueName(), except);
            }

        // implementation traits
        int cImpl = stream.readInt();
        m_vectScript.setSize(cImpl);
        for (int i = 0; i < cImpl; ++i)
            {
            Implementation impl = new Implementation(this, stream, nVersion);
            m_vectScript.setElementAt(impl, i);
            }

        // override etc.
        m_fOverrideBase  = stream.readBoolean();
        m_cBaseLevelImpl = stream.readInt();
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
    protected Behavior(Trait parent, XmlElement xml, int nVersion)
            throws IOException
        {
        super(parent, xml, nVersion);

        // name
        String sName = readString(xml.getElement("name"));
        if (sName == BLANK)
            {
            throw new IOException("name is missing");
            }
        m_sName = sName;

        // bit flags (various attributes)
        m_nFlags     = readFlags(xml, "flags", 0);
        m_nPrevFlags = readFlags(xml, "prev-flags", 0);

        // return value trait
        XmlElement xmlRetVal = xml.getElement("return-value");
        if (xmlRetVal == null)
            {
            throw new IOException("return value is missing");
            }
        m_retval = new ReturnValue(this, xmlRetVal, nVersion);

        // parameter traits
        XmlElement xmlParams = xml.getElement("params");
        if (xmlParams != null)
            {
            Vector vect = m_vectParam;
            for (Iterator iter = xmlParams.getElements("param"); iter.hasNext(); )
                {
                XmlElement xmlParam = (XmlElement) iter.next();
                Parameter  param    = new Parameter(this, xmlParam, nVersion);
                vect.add(param);
                }
            }
        updateSignature();

        // exception traits
        XmlElement xmlExcepts = xml.getElement("exceptions");
        if (xmlExcepts != null)
            {
            StringTable tbl = m_tblExcept;
            for (Iterator iter = xmlExcepts.getElements("exception"); iter.hasNext(); )
                {
                XmlElement xmlExcept = (XmlElement) iter.next();
                Throwee    except    = new Throwee(this, xmlExcept, nVersion);
                tbl.put(except.getUniqueName(), except);
                }
            }

        // implementation traits
        Vector vectScript = m_vectScript;
        for (Iterator iter = xml.getElements("implementation"); iter.hasNext(); )
            {
            XmlElement     xmlImpl = (XmlElement) iter.next();
            Implementation impl    = new Implementation(this, xmlImpl, nVersion);
            vectScript.add(impl);
            }

        // override etc.
        m_fOverrideBase  = readBoolean(xml.getElement("override-base"));
        m_cBaseLevelImpl = xml.ensureElement("base-implementations").getInt();
        }


    // ----- persistence ----------------------------------------------------

    /**
    * Save the Behavior to a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param stream   the stream to write this behavior to
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            writing the Behavior contents to the stream
    */
    protected synchronized void save(DataOutput stream)
            throws IOException
        {
        super.save(stream);

        // name
        stream.writeUTF(m_sName);

        // bit flags (various attributes)
        stream.writeInt(m_nFlags);
        stream.writeInt(m_nPrevFlags);

        // return value trait
        m_retval.save(stream);

        // parameter traits
        int cParam = m_vectParam.size();
        stream.writeInt(cParam);
        for (int i = 0; i < cParam; ++i)
            {
            ((Parameter) m_vectParam.elementAt(i)).save(stream);
            }

        // exception traits
        int cExcept = m_tblExcept.getSize();
        stream.writeInt(cExcept);
        Enumeration enmrExcept = m_tblExcept.elements();
        for (int i = 0; i < cExcept; ++i)
            {
            ((Throwee) enmrExcept.nextElement()).save(stream);
            }

        // implementation traits
        int cImpl = m_vectScript.size() - m_cBaseLevelImpl;
        stream.writeInt(cImpl);
        for (int i = 0; i < cImpl; ++i)
            {
            ((Implementation) m_vectScript.elementAt(i)).save(stream);
            }

        // override etc.
        stream.writeBoolean(m_fOverrideBase);
        stream.writeInt(m_cBaseLevelImpl);
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
        // name
        xml.addElement("name").setString(m_sName);

        super.save(xml);

        // bit flags (various attributes)
        saveFlags(xml, "flags", m_nFlags, 0);
        saveFlags(xml, "prev-flags", m_nPrevFlags, 0);

        // REVIEW: jhowes 2007.10.18:
        // Added a human-readable description attribute to the flags elements
        XmlElement xmlFlags = xml.getElement("flags");
        if (xmlFlags != null)
            {
            xmlFlags.addAttribute("desc").setString(flagsDescription(m_nFlags,
                    ACCESS_SPECIFIED | SCOPE_SPECIFIED | SYNC_SPECIFIED, false));
            }
        xmlFlags = xml.getElement("prev-flags");
        if (xmlFlags != null)
            {
            xmlFlags.addAttribute("desc").setString(flagsDescription(m_nPrevFlags,
                    ACCESS_SPECIFIED | SCOPE_SPECIFIED | SYNC_SPECIFIED, false));
            }

        // return value trait
        m_retval.save(xml.addElement("return-value"));

        // parameter traits
        List list = m_vectParam;
        if (list != null && !list.isEmpty())
            {
            XmlElement xmlParams = xml.addElement("params");
            for (Iterator iter = list.iterator(); iter.hasNext(); )
                {
                ((Parameter) iter.next()).save(xmlParams.addElement("param"));
                }
            }

        // exception traits
        saveTable(xml, m_tblExcept, "exceptions", "exception");

        // implementation traits
        int cImpl = m_vectScript.size() - m_cBaseLevelImpl;
        if (cImpl > 0)
            {
            for (int i = 0; i < cImpl; ++i)
                {
                // expect only one, so don't nest them; however, any number
                // is supported
                ((Implementation) m_vectScript.elementAt(i)).save(xml.addElement("implementation"));
                }
            }

        // override etc.
        if (m_fOverrideBase)
            {
            xml.addElement("override-base").setBoolean(true);
            }
        if (m_cBaseLevelImpl != 0)
            {
            xml.addElement("base-implementations").setInt(m_cBaseLevelImpl);
            }
        }


    // ----- derivation/modification ----------------------------------------

    /**
    * Construct a blank Behavior from this base.
    *
    * @param parent  the containing Component
    * @param nMode   RESOLVED, DERIVATION or MODIFICATION
    *
    * @return a new blank derived trait of this trait's class with the
    *         specified parent and mode
    */
    protected Trait getBlankDerivedTrait(Trait parent, int nMode)
        {
        Behavior beh = new Behavior(this, (Component) parent, nMode);

        // for signature, create blank derived traits for exceptions
        // (see special processing in resolve() for exceptions on signature
        // behaviors)
        if (getComponent().isSignature() && !m_tblExcept.isEmpty())
            {
            StringTable tblExcept = (StringTable) m_tblExcept.clone();
            for (Enumeration enmr = tblExcept.keys(); enmr.hasMoreElements(); )
                {
                String  sExcept = (String) enmr.nextElement();
                Throwee throwee = (Throwee) tblExcept.get(sExcept);
                tblExcept.put(sExcept, throwee.getBlankDerivedTrait(this, nMode));
                }
            beh.m_tblExcept = tblExcept;
            }

        return beh;
        }

    /**
    * Create a derived/modified Trait by combining the information from this
    * Trait with the super or base level's Trait information.  Neither the
    * base ("this") nor the delta may be modified in any way.
    *
    * Note:  resolve must be invoked with a null derivation when the base
    *        behavior is final
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
        Behavior base    = this;
        Behavior delta   = (Behavior) resolveDelta(traitDelta, loader, errlist);
        Behavior derived = (Behavior) super.resolve(delta, parent, loader, errlist);

        // for general use below
        boolean fSignature = base.getComponent().isSignature();

        // verify that behavior attributes match
        delta.verifyMatch(base, true, errlist);

        // if the base is resolved, this will be resolved when resolve
        // completes (otherwise it will be a derivation/modification)
        int nBaseMode  = base .getMode();
        boolean fBaseResolved = (nBaseMode == RESOLVED);
        int nDeltaMode = delta.getMode();
        if (nDeltaMode == RESOLVED)
            {
            // this can happen when the Behavior is an INSERT with the
            // same name as a Behavior later added to the base
            nDeltaMode = delta.getExtractMode(base);
            }

        // attributes represented as bit flags
        int nBaseFlags    = base.m_nFlags;
        int nDeltaFlags   = delta.m_nFlags;
        int nPrevFlags    = derived.resolveFlags(nBaseFlags, delta.m_nPrevFlags);
        int nDerivedFlags = derived.resolveFlags(nPrevFlags, nDeltaFlags);

        // resolve exists
        // if delta is a modification of an insert, then keep it as an insert
        int nExists = EXISTS_UPDATE;
        if (nDeltaMode == MODIFICATION && (nBaseFlags & EXISTS_MASK) == EXISTS_INSERT)
            {
            nExists = EXISTS_INSERT;
            }

        // gg: 2003.11.24 Behavior is EXISTS_NOT if the method is not present
        //                in the class represented by the JCS.
        if (fSignature && (nDeltaFlags & EXISTS_MASK) == EXISTS_NOT)
            {
            nExists = EXISTS_NOT;
            }
        nDerivedFlags = nDerivedFlags & ~EXISTS_FULLMASK | nExists;

        // behavior attributes
        derived.m_sName      = base.m_sName;
        derived.m_nFlags     = nDerivedFlags;
        derived.m_nPrevFlags = nPrevFlags;

        // return value
        ReturnValue retBase  = base.m_retval;
        ReturnValue retDelta = delta.m_retval;
        if (retDelta == null)
            {
            retDelta = (ReturnValue) retBase.getNullDerivedTrait(delta, nDeltaMode);
            }
        derived.m_retval = (ReturnValue) retBase.resolve(retDelta, derived, loader, errlist);

        // parameters
        Vector vectBaseParam    = base.m_vectParam;
        Vector vectDeltaParam   = delta.matchParameters(base, true, errlist);
        Vector vectDerivedParam = derived.m_vectParam;
        int cParam = vectBaseParam.size();
        vectDerivedParam.setSize(cParam);
        for (int i = 0; i < cParam; ++i)
            {
            Parameter paramBase  = (Parameter) vectBaseParam .get(i);
            Parameter paramDelta = (Parameter) vectDeltaParam.get(i);
            if (paramDelta == null)
                {
                paramDelta = (Parameter) paramBase.getNullDerivedTrait(delta, nDeltaMode);
                }
            vectDerivedParam.set(i, paramBase.resolve(paramDelta, derived, loader, errlist));
            }
        updateSignature();

        // exceptions (note:  delta exceptions may be a superset of base)

        StringTable tblBaseExcept    = base.m_tblExcept;
        StringTable tblDeltaExcept   = delta.matchExceptions(base, true, errlist);
        StringTable tblDerivedExcept = derived.m_tblExcept; // empty
        for (Enumeration enmr = tblDeltaExcept.keys(); enmr.hasMoreElements(); )
            {
            String  sExcept       = (String) enmr.nextElement();
            Throwee exceptBase    = (Throwee) tblBaseExcept .get(sExcept);
            Throwee exceptDelta   = (Throwee) tblDeltaExcept.get(sExcept);
            Throwee exceptDerived = null;

            if (exceptBase == null)
                {
                // just copy exception information (nothing to apply it to)
                exceptDerived = new Throwee(derived, exceptDelta);

                // if the base is resolved, the derived exception must be
                // marked as deleted (see matchExceptions)
                if (fBaseResolved)
                    {
                    nExists = exceptDerived.getExists();
                    if (nExists == EXISTS_INSERT)
                        {
                        // originated at this level, so mark as removed at this
                        // level
                        exceptDerived.setExists(EXISTS_DELETE);
                        }
                    else if (nExists == EXISTS_UPDATE)
                        {
                        // not originated at this level
                        exceptDerived.setExists(EXISTS_NOT);
                        }
                    }
                }
            else
                {
                if (exceptDelta == null)
                    {
                    if (fSignature && nDeltaMode == DERIVATION)
                        {
                        // the sub-class declared the method without the
                        // exception, implying that the exception has been
                        // removed; this occurs only with signatures because
                        // Java classes do not store exception declarations
                        // as a delta from the super, rather they store the
                        // list in its fully resolved form
                        exceptDerived = new Throwee(derived, exceptBase);
                        exceptDerived.setExists(EXISTS_DELETE);
                        }
                    else
                        {
                        // create a null derivation if no delta exists
                        exceptDelta = (Throwee) exceptBase.getNullDerivedTrait(delta, nDeltaMode);
                        }
                    }

                // apply the delta
                if (exceptDelta != null)
                    {
                    exceptDerived = (Throwee) exceptBase.resolve(exceptDelta, derived, loader, errlist);
                    }
                }

            tblDerivedExcept.put(sExcept, exceptDerived);
            }

        // implementations
        Vector vectBaseScript    = base.m_vectScript;
        Vector vectDeltaScript   = delta.m_vectScript;
        Vector vectDerivedScript = derived.m_vectScript; // empty
        // copy the delta implementations to the derived behavior
        int cScripts = vectDeltaScript.size();
        vectDerivedScript.setSize(cScripts);
        for (int i = 0; i < cScripts; ++i)
            {
            Implementation script = (Implementation) vectDeltaScript.get(i);
            vectDerivedScript.set(i, new Implementation(derived, script));
            }
        // if the delta is a modification, that means that some of the
        // implementations may have come from a base of the delta (i.e. the
        // delta itself may be a result of resolve processing)
        if (nDeltaMode == MODIFICATION && !delta.m_fPrevOverrideBase)
            {
            // determine the number of scripts on the base
            int cBaseImpl = vectBaseScript.size();

            // if the base overrode its base then only keep the reachable
            // base scripts
            if (base.m_fOverrideBase)
                {
                cBaseImpl -= base.m_cBaseLevelImpl;
                }

            // get the base scripts
            for (int i = 0; i < cBaseImpl; ++i)
                {
                Implementation scriptBase    = (Implementation) vectBaseScript.get(i);
                Implementation scriptDelta   = (Implementation) scriptBase.getNullDerivedTrait(delta, MODIFICATION);
                Implementation scriptDerived = (Implementation) scriptBase.resolve(scriptDelta, derived, loader, errlist);
                vectDerivedScript.addElement(scriptDerived);
                }

            derived.m_cBaseLevelImpl    = delta.m_cBaseLevelImpl + cBaseImpl;
            derived.m_fOverrideBase     = delta.m_fOverrideBase;
            derived.m_fPrevOverrideBase = base.m_fOverrideBase || base.m_fPrevOverrideBase;
            }
        else
            {
            derived.m_cBaseLevelImpl    = delta.m_cBaseLevelImpl;
            derived.m_fOverrideBase     = delta.m_fOverrideBase;
            derived.m_fPrevOverrideBase = delta.m_fPrevOverrideBase;
            }

        return derived;
        }

    /**
    * Resolve delta in flags.
    *
    * implementation note:  a flag value is always meaningful if the
    * behavior is resolved; otherwise it is meaningful only if the
    * specific attribute is specified; for example:
    *
    *      (fBaseResolved || (nBaseFlags & <attr>_SPECIFIED) != 0)
    *
    * @param nBaseFlags   the flags to use as the base flags
    * @param nDeltaFlags  the flags to use as the delta flags
    *
    * @return the derived flags
    */
    protected int resolveFlags(int nBaseFlags, int nDeltaFlags)
        {
        // for general use below
        boolean fSignature = getComponent().isSignature();

        // derived flags always start as the base flags; then the delta flags
        // are applied
        int nDerivedFlags = nBaseFlags;

        // optimization: check for nothing specified
        if (!fSignature && nDeltaFlags == 0)
            {
            return nDerivedFlags;
            }

        // flags cannot be changed if the base flags are final
        boolean fBaseResolved = (getMode() == RESOLVED);
        if ((fBaseResolved || (nBaseFlags & DERIVE_SPECIFIED) != 0)
                && (nBaseFlags & DERIVE_MASK) == DERIVE_FINAL)
            {
            return nDerivedFlags;
            }

        // the visible attribute is flexible
        if ((nDeltaFlags & VIS_SPECIFIED) != 0)
            {
            nDerivedFlags = nDerivedFlags & ~VIS_FULLMASK | nDeltaFlags & VIS_FULLMASK;
            }

        // the access attribute is one-way:  private to protected to public
        if (fSignature || (nDeltaFlags & ACCESS_SPECIFIED) != 0)
            {
            int nBaseAccess    = (nBaseFlags  & ACCESS_MASK);
            int nDeltaAccess   = (nDeltaFlags & ACCESS_MASK);
            int nDerivedAccess = nDeltaAccess;

            if ((fBaseResolved || (nBaseFlags & ACCESS_SPECIFIED) != 0)
                    && nDeltaAccess != nBaseAccess)
                {
                // specified on base and in delta; verify delta is legal
                switch (nBaseAccess)
                    {
                    case ACCESS_PRIVATE:
                        // from private to protected or public is allowed for
                        // modification only -- otherwise delta is an error
                        // since private behaviors from the super are dropped
                        // altogether in a derivation
                        break;
                    case ACCESS_PACKAGE:
                        // from package to protected is legal (for JCSs only)
                        if (nDeltaAccess == ACCESS_PROTECTED)
                            {
                            break;
                            }
                        // no break;
                    case ACCESS_PROTECTED:
                        // from protected to public is legal
                        if (nDeltaAccess == ACCESS_PUBLIC)
                            {
                            break;
                            }
                        // no break;
                    case ACCESS_PUBLIC:
                        // from public to protected or private is not legal
                        nDerivedAccess = nBaseAccess;
                        break;
                    }
                }

            nDerivedFlags = nDerivedFlags & ~ACCESS_MASK | ACCESS_SPECIFIED | nDerivedAccess;
            }

        // the synchronized attribute is flexible
        if (fSignature || (nDeltaFlags & SYNC_SPECIFIED) != 0)
            {
            nDerivedFlags = nDerivedFlags & ~SYNC_FULLMASK | nDeltaFlags & SYNC_FULLMASK;
            }

        // the scope attribute is never overridden (a Behavior cannot change
        // from static to instance or vice versa due to a derivation)

        // the abstract attribute is flexible
        if ((nDeltaFlags & IMPL_SPECIFIED) != 0)
            {
            nDerivedFlags = nDerivedFlags & ~IMPL_FULLMASK | nDeltaFlags & IMPL_FULLMASK;
            }

        // the final attribute is one-way
        if ((fSignature || (nDeltaFlags & DERIVE_SPECIFIED) != 0)
                && (nDeltaFlags & DERIVE_MASK) == DERIVE_FINAL)
            {
            nDerivedFlags = nDerivedFlags & ~DERIVE_FULLMASK | nDeltaFlags & DERIVE_FULLMASK;
            }

        // the antiquity (deprecated) attribute is flexible
        if (fSignature || (nDeltaFlags & ANTIQ_SPECIFIED) != 0)
            {
            nDerivedFlags = nDerivedFlags & ~ANTIQ_FULLMASK | nDeltaFlags & ANTIQ_FULLMASK;
            }

        // the distribution (remote) attribute is one-way, and is specifiable
        // only for Behaviors on global remote Components
        if ((nDeltaFlags & DIST_SPECIFIED) != 0
                && (nDeltaFlags & DIST_MASK) == DIST_REMOTE)
            {
            // only process if base is not already remote
            if (!((fBaseResolved || (nBaseFlags & DIST_SPECIFIED) != 0)
                    && (nBaseFlags & DIST_MASK) == DIST_REMOTE))
                {
                // remote behaviors only on global remote Components
                Component cd = getComponent();
                if (cd.isGlobal() && (cd.getMode() != RESOLVED || cd.isRemote()))
                    {
                    // determine accessibility of the method
                    boolean fPublic = (nDerivedFlags & ACCESS_MASK) == ACCESS_PUBLIC;
                    if (!fBaseResolved && (nDerivedFlags & ACCESS_SPECIFIED) == 0)
                        {
                        // if no access information is available, assume public
                        // until a resolve occurs in which information is available
                        fPublic = true;
                        }

                    // 2001.05.08 cp  allow static behaviors to be remote in order to
                    //                support ejb home design using static methods
                    /*
                    // determine scope of the behavior
                    boolean fInstance = (nDerivedFlags & SCOPE_MASK) == SCOPE_INSTANCE;
                    if (!fBaseResolved && (nDerivedFlags & SCOPE_SPECIFIED) == 0)
                        {
                        // if no scope information is available, assume instance
                        // until a resolve occurs in which information is available
                        fInstance = true;
                        }

                    // only allow remote to be set if behavior is non-static and public
                    if (fPublic && fInstance)
                    */
                    if (fPublic)
                        {
                        nDerivedFlags = nDerivedFlags & ~DIST_FULLMASK | nDeltaFlags & DIST_FULLMASK;
                        }
                    }
                }
            }

        return nDerivedFlags;
        }

    /**
    * Complete the resolve processing for this Behavior and its sub-traits,
    * discarding any Behavior Exceptions that have no origin.
    *
    * @param loader    the Loader object for JCS, CIM, and CD dependencies
    * @param errlist   the error list object to log error information to
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    protected synchronized void finalizeResolve(Loader loader, ErrorList errlist)
            throws ComponentException
        {
        super.finalizeResolve(loader, errlist);

        // remove all "specified" flags
        m_nFlags     &= ~ALL_SPECIFIED;
        m_nPrevFlags &= ~ALL_SPECIFIED;

        // discard transient (resolve only) data
        m_fPrevOverrideBase = false;

        // resolve sub-traits:  return value
        m_retval.finalizeResolve(loader, errlist);

        // resolve sub-traits:  parameters
        int c = m_vectParam.size();
        for (int i = 0; i < c; ++i)
            {
            ((Parameter) m_vectParam.elementAt(i)).finalizeResolve(loader, errlist);
            }

        // resolve sub-traits:  exceptions
        if (!m_tblExcept.isEmpty())
            {
            for (Enumeration enmr = m_tblExcept.elements(); enmr.hasMoreElements(); )
                {
                Throwee except = (Throwee) enmr.nextElement();
                except.finalizeResolve(loader, errlist);

                // discard un-needed exceptions
                if (except.isDiscardable())
                    {
                    m_tblExcept.remove(except.getUniqueName());
                    except.invalidate();
                    }
                }
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
        Behavior derived = this;
        Behavior base    = (Behavior) traitBase;
        Behavior delta   = (Behavior) super.extract(base, parent, loader, errlist);

        // for general use below
        boolean fSignature = getComponent().isSignature();

        // verify that behavior attributes match
        derived.verifyMatch(base, false, errlist);

        // determine if the base is resolved (which means all base attributes
        // are specified, i.e. have a value)
        boolean fBaseResolved = (base.getMode() == RESOLVED);

        // determine if extracting a DERIVATION or MODIFICATION
        int nDeltaMode = delta.getMode();

        // attributes represented as bit flags; see the length note in the
        // corresponding section of the extract method in Component.java
        int nBaseFlags    = base   .m_nFlags;
        int nDerivedFlags = derived.m_nFlags;
        int nDifFlags     = nBaseFlags ^ nDerivedFlags;
        int nDeltaFlags   = nDerivedFlags & ~ALL_SPECIFIED;

        // exists (always update for Behavior)
        nDeltaFlags = nDeltaFlags & ~EXISTS_FULLMASK | EXISTS_UPDATE;

        // visible
        if ((nDifFlags & VIS_MASK) != 0 &&
                (fBaseResolved || (nBaseFlags & VIS_SPECIFIED) != 0))
            {
            nDeltaFlags |= VIS_SPECIFIED;
            }

        // access
        if ((nDifFlags & ACCESS_MASK) != 0 &&
                (fBaseResolved || (nBaseFlags & ACCESS_SPECIFIED) != 0))
            {
            nDeltaFlags |= ACCESS_SPECIFIED;
            }

        // synchronized
        if ((nDifFlags & SYNC_MASK) != 0 &&
                (fBaseResolved || (nBaseFlags & SYNC_SPECIFIED) != 0))
            {
            nDeltaFlags |= SYNC_SPECIFIED;
            }

        // scope is not specifiable

        // abstract
        if ((nDifFlags & IMPL_MASK) != 0 &&
                (fBaseResolved || (nBaseFlags & IMPL_SPECIFIED) != 0))
            {
            nDeltaFlags |= IMPL_SPECIFIED;
            }

        // final
        if ((nDifFlags & DERIVE_MASK) != 0 &&
                (fBaseResolved || (nBaseFlags & DERIVE_SPECIFIED) != 0))
            {
            nDeltaFlags |= DERIVE_SPECIFIED;
            }

        // antiquity
        if ((nDifFlags & ANTIQ_MASK) != 0 &&
                (fBaseResolved || (nBaseFlags & ANTIQ_SPECIFIED) != 0))
            {
            nDeltaFlags |= ANTIQ_SPECIFIED;
            }

        // distribution
        if ((nDifFlags & DIST_MASK) != 0 &&
                (fBaseResolved || (nBaseFlags & DIST_SPECIFIED) != 0))
            {
            nDeltaFlags |= DIST_SPECIFIED;
            }

        // behavior attributes
        delta.m_sName      = base.m_sName;
        delta.m_nFlags     = nDeltaFlags;
        delta.m_nPrevFlags = 0;

        // extract sub-trait:  return value
        delta.m_retval = (ReturnValue) derived.m_retval.extract(
                base.m_retval, delta, loader, errlist);

        // extract sub-traits:  parameters
        Vector vectBaseParam    = base.m_vectParam;
        Vector vectDerivedParam = derived.matchParameters(base, false, errlist);
        Vector vectDeltaParam   = delta.m_vectParam;
        int cParam = vectBaseParam.size();
        vectDeltaParam.setSize(cParam);
        for (int i = 0; i < cParam; ++i)
            {
            Parameter paramBase    = (Parameter) vectBaseParam   .get(i);
            Parameter paramDerived = (Parameter) vectDerivedParam.get(i);
            Parameter paramDelta;

            // derived parameter is null to imply a resulting null derivation
            if (paramDerived == null)
                {
                paramDelta = (Parameter) paramBase.getNullDerivedTrait(delta, nDeltaMode);
                }
            else
                {
                paramDelta = (Parameter) paramDerived.extract(
                        paramBase, delta, loader, errlist);
                }

            vectDeltaParam.set(i, paramDelta);
            }
        delta.updateSignature();

        // extract sub-traits:  exceptions
        // (note:  derived exceptions may be a superset of base)
        StringTable tblBaseExcept    = base.m_tblExcept;
        StringTable tblDerivedExcept = derived.matchExceptions(base, false, errlist);
        StringTable tblDeltaExcept   = delta.m_tblExcept; // empty
        for (Enumeration enmr = tblDerivedExcept.keys(); enmr.hasMoreElements(); )
            {
            String  sExcept       = (String) enmr.nextElement();
            Throwee exceptBase    = (Throwee) tblBaseExcept   .get(sExcept);
            Throwee exceptDerived = (Throwee) tblDerivedExcept.get(sExcept);
            Throwee exceptDelta   = null;

            if (exceptBase == null)
                {
                // just copy derived exception information (nothing to extract)
                exceptDelta = new Throwee(delta, exceptDerived);
                }
            else
                {
                if (fSignature && exceptDerived == null)
                    {
                    // the super method has an exception type that is not on
                    // the derived method, meaning it has been deleted, so at
                    // this point reconstruct the "deleted derived" exception
                    exceptDerived = (Throwee) exceptBase.resolve(
                        exceptBase.getNullDerivedTrait(delta, nDeltaMode), derived, loader, errlist);
                    exceptDerived.setExists(EXISTS_DELETE);
                    }

                if (exceptDerived != null)
                    {
                    // extract the delta
                    exceptDelta = (Throwee) exceptDerived.extract(exceptBase, delta, loader, errlist);
                    }
                }

            if (exceptDelta != null)
                {
                tblDeltaExcept.put(sExcept, exceptDelta);
                }
            }

        // extract sub-traits:  implementations
        Vector vectBaseScripts    = base   .m_vectScript;
        Vector vectDerivedScripts = derived.m_vectScript;
        Vector vectDeltaScripts   = delta  .m_vectScript;   // empty

        // delta keeps only reachable implementations from derived
        int cScripts = vectDerivedScripts.size();
        if (cScripts > 0)
            {
            // the JCS implementations are not actually there; they are used only
            // for display in the tools
            if (isJcsImplementation(cScripts - 1))
                {
                cScripts--;
                }

            if (derived.m_fOverrideBase)
                {
                cScripts -= derived.m_cBaseLevelImpl;
                }
            }

        delta.m_fOverrideBase  = derived.m_fOverrideBase;
        delta.m_cBaseLevelImpl = nDeltaMode == DERIVATION ? derived.m_cBaseLevelImpl : 0;

        // deltas are never extracted for implementations; they are either
        // stored resolved (whole) or are discarded
Script: for (int i = 0; i < cScripts; ++i)
            {
            Implementation script = (Implementation) vectDerivedScripts.get(i);

            if (nDeltaMode == MODIFICATION)
                {
                int cBaseScripts = vectBaseScripts.size();
                for (int iBase = 0; iBase < cBaseScripts; ++iBase)
                    {
                    if (script.equalsUID((Implementation)vectBaseScripts.get(iBase)))
                        {
                        // this script belongs to the base; don't copy any
                        // more scripts to the delta
                        break Script;
                        }
                    }
                }

            vectDeltaScripts.add(new Implementation(delta, script));
            }

        return delta;
        }

    /**
    * Complete the extract processing for this Behavior and its sub-traits.
    *
    * @param loader    the Loader object for JCS, CIM, and CD dependencies
    * @param errlist   the error list object to log error information to
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    protected synchronized void finalizeExtract(Loader loader, ErrorList errlist)
            throws ComponentException
        {
        super.finalizeExtract(loader, errlist);

        // for general use below
        boolean fSignature = getComponent().isSignature();

        // finalize sub-trait:  return value
        m_retval.finalizeExtract(loader, errlist);

        // finalize sub-traits:  parameters
        int c = m_vectParam.size();
        for (int i = 0; i < c; ++i)
            {
            ((Parameter) m_vectParam.get(i)).finalizeExtract(loader, errlist);
            }

        // finalize sub-traits:  exceptions
        if (!m_tblExcept.isEmpty())
            {
            for (Enumeration enmr = m_tblExcept.elements(); enmr.hasMoreElements(); )
                {
                Throwee except = (Throwee) enmr.nextElement();
                except.finalizeExtract(loader, errlist);

                // check if the exception is discardable
                // (don't discard exceptions for signature methods:  see the
                // special resolve processing for signatures)
                if (!fSignature && except.isDiscardable())
                    {
                    m_tblExcept.remove(except.getUniqueName());
                    except.invalidate();
                    }
                }
            }

        // 2001.05.23  cp  this will break roll-up but will fix JCSs
        Vector vectScript = m_vectScript;
        int    cScripts   = vectScript.size();
        while (cScripts > 0 &&
               !((Implementation) vectScript.get(cScripts - 1)).isDeclaredAtThisLevel())
            {
            vectScript.remove(--cScripts);
            }
        m_cBaseLevelImpl = 0;
        }

    /**
    * Check for illegal mismatches between the base and this behavior.
    *
    * (Neither the base nor this behavior are modified by this method.)
    *
    * @param base      the super or base level's Behavior object
    * @param fResolve  true if being RESOLVED
    * @param errlist   the error list to log any mismatches to
    *
    * @exception DerivationException
    */
    protected void verifyMatch(Behavior base, boolean fResolve, ErrorList errlist)
            throws DerivationException
        {
        // check for name mismatch
        if (!this.m_sName.equals(base.m_sName))
            {
            String sCode = (fResolve ? RESOLVE_BEHAVIORNAMECHANGE
                                     : EXTRACT_BEHAVIORNAMECHANGE);
            Object[] aoParam = new Object[]
                {
                this.m_sName,
                base.m_sName,
                toPathString()
                };
            logError(sCode, WARNING, aoParam, errlist);
            }
        }

    /**
    * Match up the parameters from the base and this behavior.  Return a
    * Vector of parameters to resolve or extract from the base.  The returned
    * vector must not be modified.
    *
    * The returned vector may have null values which imply null derivations.
    *
    * (Neither the base nor this behavior are modified by this method.)
    *
    * @param base      the super or base level's Behavior object
    * @param fResolve  true if being RESOLVED
    * @param errlist   the error list to log any mismatches to
    *
    * @exception DerivationException
    */
    protected Vector matchParameters(Behavior base, boolean fResolve, ErrorList errlist)
            throws DerivationException
        {
        // get behavior parameters
        Vector  vectThisParam = this.m_vectParam;
        Vector  vectBaseParam = base.m_vectParam;
        int     cThisParam    = vectThisParam.size();
        int     cBaseParam    = vectBaseParam.size();

        if (cThisParam == cBaseParam)
            {
            boolean fMatch = true;
            if (!this.getComponent().isSignature())
                {
                for (int i = 0; i < cBaseParam; ++i)
                    {
                    Parameter pBase = (Parameter) vectBaseParam.get(i);
                    Parameter pThis = (Parameter) vectThisParam.get(i);
                    if (pThis.getUID() == null || !pThis.equalsUID(pBase))
                        {
                        fMatch = false;
                        break;
                        }
                    }
                }

            // most common:  parameters match
            if (fMatch)
                {
                return vectThisParam;
                }
            }

        // if parameters didn't match, create a new vector which does match
        // by adding, removing, and re-arranging parameters; this is assumed
        // to happen rarely
        Vector vectResultParam = new Vector(cBaseParam);
        vectResultParam.setSize(cBaseParam);

        // null derivations have no parameters in the delta to match
        if (cThisParam == 0)
            {
            return vectResultParam;
            }

        // build look up tables by UID and name
        Hashtable   tblParamByUID   = new Hashtable(cBaseParam);
        StringTable tblParamByName  = new StringTable(INSENS);
        for (int i = 0; i < cThisParam; ++i)
            {
            Parameter paramThis = (Parameter) vectThisParam.get(i);
            UID uid = paramThis.getUID();
            if (uid != null)
                {
                tblParamByUID .put(paramThis.getUID       (), paramThis);
                }
            tblParamByName.put(paramThis.getUniqueName(), paramThis);
            }

        // look up parameters by UID; remove each one found from the name
        // look up table
        if (!tblParamByUID.isEmpty())
            {
            for (int i = 0; i < cBaseParam; ++i)
                {
                Parameter paramBase = (Parameter) vectBaseParam.get(i);
                UID uid = paramBase.getUID();
                if (uid != null)
                    {
                    Parameter paramThis = (Parameter) tblParamByUID.get(uid);
                    if (paramThis != null)
                        {
                        vectResultParam.set(i, paramThis);
                        tblParamByName.remove(paramThis.getUniqueName());
                        }
                    }
                }
            }

        // fill in missing parameters by looking up by name; failing that
        // assume "null" derived traits
        for (int i = 0; i < cBaseParam; ++i)
            {
            if (vectResultParam.get(i) == null)
                {
                Parameter paramBase = (Parameter) vectBaseParam.get(i);
                Parameter paramThis = (Parameter) tblParamByName.get(paramBase.getUniqueName());
                if (paramThis != null)
                    {
                    vectResultParam.set(i, paramThis);

                    // only leave unmatched parameters in tblParamByName
                    // (so they can be discarded)
                    tblParamByName.remove(paramThis.getUniqueName());
                    }
                }
            }

        // discard remaining parameters
        for (Enumeration enmr = tblParamByName.elements(); enmr.hasMoreElements(); )
            {
            Parameter param = (Parameter) enmr.nextElement();

            String sCode = (fResolve ? RESOLVE_PARAMETERDISCARDED
                                     : EXTRACT_PARAMETERDISCARDED);
            Object[] aoParam = new Object[]
                {
                param.getName(),
                base.m_sSig,
                toPathString()
                };
            logError(sCode, WARNING, aoParam, errlist);
            }

        return vectResultParam;
        }

    /**
    * Match up the exceptions from the base and this behavior.  Return a
    * StringTable of exceptions to resolve or extract from the base.  The
    * returned StringTable must not be modified.
    *
    * The returned table may have null values which imply null derivations.
    *
    * The returned table may have additional exceptions.  Although the Java
    * Language Specification (8.4.4) states that there cannot be any
    * additional exceptions, it is possible that exception information was
    * added by an interface (for example) although such an exception will be
    * automatically marked as "deleted".
    *
    * (Neither the base nor this behavior are modified by this method.)
    *
    * @param base      the super or base level's Behavior object
    * @param fResolve  true if being RESOLVED
    * @param errlist   the error list to log any mismatches to
    *
    * @exception DerivationException
    */
    protected StringTable matchExceptions(Behavior base, boolean fResolve, ErrorList errlist)
            throws DerivationException
        {
        StringTable tblThis   = this.m_tblExcept;
        StringTable tblBase   = base.m_tblExcept;
        StringTable tblResult = tblThis;

        // most common case overall:  behavior has no declared exceptions
        // most common case for extract:  exact match
        if (tblThis.keysEquals(tblBase))
            {
            return tblResult;
            }

        // most common for resolve:  no exception information in the delta
        if (tblThis.isEmpty())
            {
            // copy keys from base (but leave exception traits null to
            // signify null derivations)
            tblResult = (StringTable) tblBase.clone();
            for (Enumeration enmr = tblResult.keys(); enmr.hasMoreElements(); )
                {
                tblResult.put((String) enmr.nextElement(), null);
                }
            return tblResult;
            }

        // create results table (since we cannot change the "this" table);
        // this extended processing will only happen at levels which add,
        // remove, or modify exceptions (considered to be rare)
        tblResult = new StringTable();

        // build look up tables by UID and name
        Hashtable   tblByUID  = new Hashtable();
        StringTable tblByName = new StringTable();
        for (Enumeration enmr = tblThis.elements(); enmr.hasMoreElements(); )
            {
            Throwee except = (Throwee) enmr.nextElement();
            UID uid = except.getUID();
            if (uid != null)
                {
                tblByUID.put(uid, except);
                }
            tblByName.put(except.getUniqueName(), except);
            }

        // look up by UID; remove each one found from the name look up table
        if (!tblByUID.isEmpty())
            {
            for (Enumeration enmr = tblBase.elements(); enmr.hasMoreElements(); )
                {
                Throwee exceptBase = (Throwee) enmr.nextElement();
                UID uid = exceptBase.getUID();
                if (uid != null)
                    {
                    Throwee exceptThis = (Throwee) tblByUID.get(uid);
                    if (exceptThis != null)
                        {
                        tblResult.put(exceptBase.getUniqueName(), exceptThis);
                        tblByName.remove(exceptThis.getUniqueName());
                        }
                    }
                }
            }

        // look up by name
        for (Enumeration enmr = tblBase.keys(); enmr.hasMoreElements(); )
            {
            String sExcept = (String) enmr.nextElement();
            if (!tblResult.contains(sExcept))
                {
                // find the exception on this behavior with the same name;
                // whether or not one exists, reserve the name in the result
                Throwee except = (Throwee) tblByName.get(sExcept);
                tblResult.put(sExcept, except);

                // only leave unmatched exceptions in tblByName
                // (so they can be added to the results or discarded)
                if (except != null)
                    {
                    tblByName.remove(sExcept);
                    }
                }
            }

        // keep additional exceptions (but discard any conflicting ones)
        for (Enumeration enmr = tblByName.elements(); enmr.hasMoreElements(); )
            {
            Throwee except = (Throwee) enmr.nextElement();
            String sExcept = except.getUniqueName();

            // check for conflict (this happens when a base exception is
            // renamed which conflicts if another exception was added at
            // this level and coincidentally has the same name)
            if (tblResult.contains(sExcept))
                {
                String sCode = (fResolve ? RESOLVE_EXCEPTIONDISCARDED
                                         : EXTRACT_EXCEPTIONDISCARDED);
                Object[] aoParam = new Object[]
                    {
                    except.getUniqueName(),
                    base.m_sSig,
                    toPathString()
                    };
                logError(sCode, WARNING, aoParam, errlist);
                }
            else
                {
                tblResult.put(sExcept, except);
                }
            }

        return tblResult;
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
        Behavior that = (Behavior) base;
        if (that == null || !isFromSuper())
            {
            return false;
            }

        // attributes
        if (((this.m_nFlags ^ that.m_nFlags) & CLASSGEN_FLAGS) != 0)
            {
            return false;
            }

        // return value
        if (!this.m_retval.isClassDiscardable(that.m_retval))
            {
            return false;
            }

        // parameters
        Vector vectParamThis = this.m_vectParam;
        Vector vectParamThat = that.m_vectParam;
        for (int i = 0, c = vectParamThis.size(); i < c; ++i)
            {
            Parameter paramThis = (Parameter) vectParamThis.get(i);
            Parameter paramThat = (Parameter) vectParamThat.get(i);
            if (!paramThis.isClassDiscardable(paramThat))
                {
                return false;
                }
            }

        // exceptions
        if (!isClassDiscardableFromSubtraitTable(this.m_tblExcept, that.m_tblExcept))
            {
            return false;
            }

        // there must be no implementations
        if (this.m_vectScript.size() != (this.m_fOverrideBase ? this.m_cBaseLevelImpl : 0))
            {
            return false;
            }

        return super.isClassDiscardable(base);
        }


    // ----- miscellaneous Trait methods ------------------------------------

    /**
    * Determine if the Behavior is modifiable.  Behaviors that modify or
    * derive from final behaviors are not modifiable.
    *
    * @return true if the Trait is modifiable, false otherwise
    */
    public boolean isModifiable()
        {
        if ((m_nPrevFlags & DERIVE_MASK) == DERIVE_FINAL)
            {
            return false;
            }

        // gg: 2001.7.18 JCS constructors are not designable for now
        if (getComponent().isSignature() && getName().indexOf('<') != -1)
            {
            return false;
            }

        return super.isModifiable();
        }

    /**
    * Determines if this Behavior exists for any reason at all.  If a
    * Behavior exists, for example, as part of an interface, and the
    * interface changes, removing the Behavior, then the Behavior will be
    * discarded as part of the Component's resolve finalization processing.
    *
    * @return true if the Behavior is discardable
    */
    protected boolean isDiscardable()
        {
        switch (getMode())
            {
            case RESOLVED:
                {
                // Don't discard Java Class Signature Behaviors
                if (((Component) getParentTrait()).isSignature())
                    {
                    return false;
                    }
                break;
                }
            case DERIVATION:
            case MODIFICATION:
                {
                // check for delta information
                if ((m_nFlags & ALL_SPECIFIED) != 0 || m_fOverrideBase)
                    {
                    return false;
                    }
                break;
                }
            }

        return super.isDiscardable();
        }

    /**
    * Determine all traits contained by this behavior.
    *
    * @return the an enumeration of traits contained by this trait
    */
    protected Enumeration getSubTraits()
        {
        ChainedEnumerator enmr = new ChainedEnumerator();

        enmr.addEnumeration(new SimpleEnumerator(new Object[] {m_retval}));
        enmr.addEnumeration(m_vectParam.elements());
        enmr.addEnumeration(m_tblExcept.elements());
        enmr.addEnumeration(m_vectScript.elements());

        return enmr;
        }

    /**
    * Reset state, discarding all information.
    */
    protected synchronized void invalidate()
        {
        super.invalidate();

        m_sName      = null;
        m_sSig       = null;
        m_retval     = null;
        m_vectParam  = null;
        m_tblExcept  = null;
        m_vectScript = null;
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
        // note:  unique name _can_ change but not behavior is not used as a
        // trait origin for other traits
        return m_sSig;
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
    * Determine the component which contains this behavior.
    *
    * @return the Component Definition that contains this behavior or null
    *         if this behavior is not part of a component
    */
    public Component getComponent()
        {
        return (Component) getParentTrait();
        }


    // ----- Signature

    /**
    * Build a behavior signature from a behavior name and parameter types.
    *
    * @param sName    the name of the behavior
    * @param adtParam the behavior's parameter data types
    *
    * @return the behavior signature built from the passed information
    */
    public static String getSignature(String sName, DataType[] adtParam)
        {
        return getSignature(sName, adtParam, 0);
        }

    /**
    * Build a behavior signature from a behavior name and parameter types.
    *
    * @param sName    the name of the behavior
    * @param adtParam the behavior's parameter data types
    * @param iFirst   the index of the first parameter in the array
    *
    * @return the behavior signature built from the passed information
    */
    public static String getSignature(String sName, DataType[] adtParam, int iFirst)
        {
        StringBuffer sb = new StringBuffer();

        sb.append(sName)
          .append('(');

        int cParam = (adtParam != null ? adtParam.length : 0);
        for (int i = iFirst; i < cParam; ++i)
            {
            sb.append(adtParam[i].getTypeString());
            }

        sb.append(')');

        return sb.toString();
        }

    /**
    * Get the signature for the behavior.  The signature uniquely identifies
    * the behavior.
    *
    * @return the signature for the behavior
    */
    public String getSignature()
        {
        return m_sSig;
        }

    /**
    * Update the signature for the behavior.  This method is called
    * internally when the behavior or a sub-trait of the behavior is
    * changed in such a way that the behavior signature is changed.
    */
    protected void updateSignature()
        {
        // get old signature (could be null if this has been called to ensure
        // that the signature is cached)
        String sOldSig = m_sSig;

        // get new signature
        StringBuffer sb = new StringBuffer();
        sb.append(m_sName)
          .append('(');
        int c = getParameterCount();
        for (int i = 0; i < c; ++i)
            {
            sb.append(getParameter(i).getSignature());
            }
        sb.append(')');
        String sNewSig = sb.toString();

        if (!sNewSig.equals(sOldSig))
            {
            // store new signature
            m_sSig = sNewSig;

            // update component's list of behaviors
            if (sOldSig != null)
                {
                getComponent().renameBehavior(sOldSig, sNewSig);
                }
            }
        }

    /**
    * Get the JVM signature for the behavior.  The JVM signature for a method
    * is composed of the JVM signatures for each of the parameters (within
    * parenthesis) plus the JVM signature for the method return type.
    *
    * @return the JVM signature for the behavior
    */
    public String getJVMSignature()
        {
        StringBuffer sb = new StringBuffer();

        sb.append('(');

        int c = getParameterCount();
        for (int i = 0; i < c; ++i)
            {
            sb.append(getParameter(i).getDataType().getJVMSignature());
            }

        sb.append(')')
          .append(m_retval.getDataType().getJVMSignature());

        return sb.toString();
        }


    // ----- origin:  helper for interface origins

    /**
    * Resolve any conflicts such that this Behavior matches the passed
    * method declaration for integration.
    *
    * @param map     declaring integration
    * @param method  the Java method information
    */
    protected void setDeclaredByIntegration(Integration map, Behavior method)
        {
        setDeclaredByTrait(map, true, method);
        }

    /**
    * Resolve any conflicts such that this Behavior matches the passed
    * method declaration for an interface.
    *
    * @param iface   declaring interface
    * @param method  the Java method information, or null if the method is
    *                implied (e.g. add/remove listener)
    */
    protected void setDeclaredByInterface(Interface iface, Behavior method)
        {
        setDeclaredByTrait(iface, false, method);
        }

    /**
    * Resolve any conflicts such that this Behavior matches the passed
    * method declaration for an interface or integration.
    *
    * @param trait        declaring interface or integration
    * @param fIntegration true if the integration trait
    * @param method       the Java method information, or null if the method is
    *                     implied (e.g. add/remove listener)
    */
    private void setDeclaredByTrait(Trait trait, boolean fIntegration, Behavior method)
        {
        // add/remove listener methods are implied; assume defaults
        DataType    dtRet      = DataType.VOID;
        String      sParamDirs = "I";
        StringTable tblExcept  = null;
        if (method != null)
            {
            dtRet      = method.m_retval.getDataType();
            sParamDirs = method.getParameterDirections();
            tblExcept  = method.m_tblExcept;
            }

        try
            {
            // update access
            if (fIntegration)
                {
                // integrates requires at least protected access
                if (getAccess() == ACCESS_PRIVATE)
                    {
                    // TODO - uncomment the following when the tools mature
                    // (Gene creates prop first then integrates second to save time)
                    // setAccess(ACCESS_PROTECTED, false);
                    setAccess(ACCESS_PUBLIC, false);
                    }
                }
            else
                {
                // implements/dispatches require public methods
                if (getAccess() != ACCESS_PUBLIC)
                    {
                    setAccess(ACCESS_PUBLIC, false);
                    }
                }

            // cannot be static for Component,
            // but as of Java 8 static extension methods are allowed
            setStatic(method != null && method.isStatic() && getComponent().isSignature(), false);

            // match up return value data type and parameter directions
            m_retval.setDataType(dtRet, false);
            setParameterDirections(sParamDirs, false);

            // the exceptions that currently exist must be merged with the
            // exceptions from the interface/integrates method, and if the
            // Behavior has a non-manual origin, then only the intersection
            // of the exceptions are permitted to remain throwable
            StringTable tblThrowee = m_tblExcept;

            if (getComponent().isSignature())
                {
                // assuming the signature is correct, we can safely
                // ignore any exceptions declared by the interface
                // TODO: the logic below doesn't really do what the comment above
                // claims, but to validate covariant types we need to have a loader
                // (e.g. to figure out that SocketException is an IOException)
                }
            else
                {
                if (tblExcept != null && !tblExcept.isEmpty())
                    {
                    if (getComponent().isSignature())
                        {
                        return;
                        }

                    // copy method exceptions to the Behavior
                    for (Enumeration enmr = tblExcept.keys(); enmr.hasMoreElements(); )
                        {
                        String sExcept = (String) enmr.nextElement();
                        Throwee except = (Throwee) tblExcept.get(sExcept);
                        Throwee throwee = (Throwee) tblThrowee.get(sExcept);

                        // create the exception if is not declared by the Behavior
                        if (throwee == null)
                            {
                            throwee = new Throwee(this, except.getDataType());

                            // if the behavior is declared elsewhere (base or
                            // super level or from an interface), the exception
                            // cannot be thrown
                            if (isFromSuper() || isFromImplements()
                                || isFromDispatches() || isFromIntegration())
                                {
                                throwee.setExists(EXISTS_DELETE);
                                }
                            tblThrowee.put(sExcept, throwee);
                            throwee.validate();
                            }

                        // update exception origin
                        throwee.addOriginTrait(trait);
                        }
                    }

                // remove previously existing declared exceptions that are not
                // declared by the interface/integrates method
                for (Enumeration enmr = tblThrowee.keys(); enmr.hasMoreElements(); )
                    {
                    String sExcept = (String) enmr.nextElement();
                    if (!tblExcept.contains(sExcept))
                        {
                        removeException(sExcept, false);
                        }
                    }
                }

            // update Behavior origin
            addOriginTrait(trait);
            }
        catch (PropertyVetoException e)
            {
            throw new IllegalStateException(e.toString());
            }
        }

    /**
    * Merge the exceptions lists from two behaviors, modifying this behavior.
    * This is used by JCS from ClassFile creation.
    *
    * @param that  the other behavior
    */
    protected void mergeJCSExceptions(Behavior that)
        {
        if (!this.m_tblExcept.equals(that.m_tblExcept))
            {
            this.m_tblExcept.retainAll(that.m_tblExcept);
            }
        }


    // ----- origin:  behavior implements interface(s)

    /**
    * Determine if this behavior is from an interface.
    *
    * @return true if this behavior results from an interface at this level
    */
    public boolean isFromImplements()
        {
        return isFromTraitDescriptor(Interface.DESCRIPTOR_IMPLEMENTS);
        }

    /**
    * Enumerate the interface names that exist at this level which declare
    * this behavior.
    *
    * @return an enumeration of interface names
    */
    public Enumeration enumImplements()
        {
        return getOriginTraits(Interface.DESCRIPTOR_IMPLEMENTS);
        }


    // ----- origin:  behavior dispatches interface(s)

    /**
    * Determine if this behavior is from a dispatched interface.
    *
    * @return true if this behavior results from a dispatched interface at
    *         this level
    */
    public boolean isFromDispatches()
        {
        return isFromTraitDescriptor(Interface.DESCRIPTOR_DISPATCHES);
        }

    /**
    * Enumerate the dispatch interface names that exist at this level which
    * declare this behavior.
    *
    * @return an enumeration of dispatch interface names
    */
    public Enumeration enumDispatches()
        {
        return getOriginTraits(Interface.DESCRIPTOR_DISPATCHES);
        }


    // ----- origin:  behavior integrates

    /**
    * Determine if this behavior exists as a result of integration at this
    * level.
    *
    * @return true if this behavior results from integration at this level
    */
    public boolean isFromIntegration()
        {
        return isFromTraitDescriptor(Integration.DESCRIPTOR);
        }


    // ----- origin:  behavior from a property

    /**
    * Determine if this behavior exists due to a property.
    *
    * @return true if this behavior results from a property
    */
    public boolean isFromProperty()
        {
        return isFromTraitDescriptor(Property.DESCRIPTOR);
        }

    /**
    * Determine the name of the property that this behavior implements.
    *
    * @return the property name
    */
    public String getPropertyName()
        {
        String sProp;
        if (m_sName.startsWith("is"))
            {
            sProp = m_sName.substring("is".length());
            }
        else if (m_sName.startsWith("get"))
            {
            sProp = m_sName.substring("get".length());
            }
        else if (m_sName.startsWith("set"))
            {
            sProp = m_sName.substring("set".length());
            }
        else
            {
            return null;
            }

        Property prop = getComponent().getProperty(sProp);
        return (prop == null ? null : sProp);
        }


    // ----- origin:  behavior declared "manually"

    /**
    * Determine if the manual origin flag can be set.  It cannot be set if
    * the behavior's only origin is manual.
    *
    * @return true if the manual origin flag can be set
    */
    public boolean isFromManualSettable()
        {
        return isDeclaredAtThisLevel() && isFromNonManual() && isModifiable();
        }

    /**
    * Set whether or not this behavior exists at this level regardless of
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
    * Set whether or not this behavior exists at this level regardless of
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


    // ----- Exists

    /**
    * Get the exists flag for this Behavior.
    *
    * For a JCS, a Behavior is EXISTS_NOT if a super or interface declares
    * the method but the method is not present in the class represented by
    * the JCS.
    *
    * @return either EXISTS_INSERT or EXISTS_UPDATE (or EXISTS_NOT if a JCS)
    */
    public int getExists()
        {
        return m_nFlags & EXISTS_MASK;
        }

    /**
    * Set the exists flag for this Behavior.
    *
    * @param nExists  the exists flag for this Behavior
    */
    protected void setExists(int nExists)
        {
        m_nFlags = m_nFlags & ~EXISTS_MASK | nExists;
        }


    // ----- Access

    /**
    * Determine the current accessibility of the Behavior.
    *
    * @return either ACCESS_PUBLIC, ACCESS_PROTECTED, or ACCESS_PRIVATE
    *
    * @see com.tangosol.dev.component.Constants#ACCESS_PUBLIC
    * @see com.tangosol.dev.component.Constants#ACCESS_PROTECTED
    * @see com.tangosol.dev.component.Constants#ACCESS_PRIVATE
    */
    public int getAccess()
        {
        return m_nFlags & ACCESS_MASK;
        }

    /**
    * Determine if the accessibility of the Behavior can be modified.
    *
    * The accessibility cannot be set if the Behavior is declared final at
    * a super level or if the Behavior originates from an interface.
    *
    * @return true if the access attribute of the Behavior can be set
    */
    public boolean isAccessSettable()
        {
        // if Behavior is part of an "interface", it can only be public
        return isModifiable()
            && !isFromImplements()
            && !isFromDispatches();
        }

    /**
    * Determine if the specified value is acceptable for the access
    * attribute.
    *
    * The accessibility can only be increased from the super level; access
    * is a one-way attribute (private -> protected -> public).
    *
    * @return true if the specified value is acceptable for the access
    *         attribute
    *
    * @see com.tangosol.dev.component.Constants#ACCESS_PUBLIC
    * @see com.tangosol.dev.component.Constants#ACCESS_PROTECTED
    * @see com.tangosol.dev.component.Constants#ACCESS_PRIVATE
    */
    public boolean isAccessLegal(int nAccess)
        {
        switch (nAccess)
            {
            case ACCESS_PUBLIC:
            case ACCESS_PROTECTED:
            case ACCESS_PRIVATE:
                break;

            default:
                return false;
            }

        // remote methods must be public
        if (isRemote() && nAccess != ACCESS_PUBLIC)
            {
            return false;
            }

        // flexible at declaration level
        if (isDeclaredAtThisLevel())
            {
            if (isFromImplements() || isFromDispatches())
                {
                // interface Behaviors must be public
                return false;
                }

            if (isFromIntegration())
                {
                // integrated Behaviors must be protected or public in order
                // to be called from the integratee
                return (nAccess != ACCESS_PRIVATE);
                }

            return true;
            }

        // one-way access validation
        switch (m_nPrevFlags & ACCESS_MASK)
            {
            case ACCESS_PUBLIC:
                if (nAccess == ACCESS_PROTECTED)
                    {
                    return false;
                    }
                // no break;
            case ACCESS_PROTECTED:
                if (nAccess == ACCESS_PRIVATE)
                    {
                    return false;
                    }
                // no break;
            case ACCESS_PRIVATE:
            default:
                return true;
            }
        }

    /**
    * Set the accessibility of the Behavior.
    *
    * @param nAccess  ACCESS_PUBLIC, ACCESS_PROTECTED, or ACCESS_PRIVATE
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    *
    * @see com.tangosol.dev.component.Constants#ACCESS_PUBLIC
    * @see com.tangosol.dev.component.Constants#ACCESS_PROTECTED
    * @see com.tangosol.dev.component.Constants#ACCESS_PRIVATE
    */
    public void setAccess(int nAccess)
            throws PropertyVetoException
        {
        setAccess(nAccess, true);
        }

    /**
    * Set the accessibility of the Behavior.
    *
    * @param nAccess    ACCESS_PUBLIC, ACCESS_PROTECTED, or ACCESS_PRIVATE
    * @param fVetoable  true if the setter can veto the value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void setAccess(int nAccess, boolean fVetoable)
            throws PropertyVetoException
        {
        int nPrev = getAccess();

        if (nAccess == nPrev)
            {
            return;
            }

        Integer prev  = Integer.valueOf(nPrev);
        Integer value = Integer.valueOf(nAccess);

        if (fVetoable)
            {
            if (!isAccessSettable())
                {
                readOnlyAttribute(ATTR_ACCESS, prev, value);
                }

            if (!isAccessLegal(nAccess))
                {
                illegalAttributeValue(ATTR_ACCESS, prev, value);
                }

            fireVetoableChange(ATTR_ACCESS, prev, value);
            }

        m_nFlags = m_nFlags & ~ACCESS_MASK | nAccess;
        firePropertyChange(ATTR_ACCESS, prev, value);
        }


    // ----- Visible

    /**
    * Get the visibility attribute of the Behavior.
    *
    * @return the value of the Behavior's visibility attribute
    *
    * @see com.tangosol.dev.component.Constants#VIS_SYSTEM
    * @see com.tangosol.dev.component.Constants#VIS_HIDDEN
    * @see com.tangosol.dev.component.Constants#VIS_ADVANCED
    * @see com.tangosol.dev.component.Constants#VIS_VISIBLE
    */
    public int getVisible()
        {
        return m_nFlags & VIS_MASK;
        }

    /**
    * Determine whether the Visible attribute of the Behavior can be set.
    *
    * @return true if the Behavior attribute can set with a valid value
    */
    public boolean isVisibleSettable()
        {
        return isModifiable();
        }

    /**
    * Determine whether the Visible attribute of the Behavior can be set to
    * the specified value.  The Visible attribute is flexible.
    *
    * @param nVis the new value of the visibility attribute
    *
    * @return true if the specified value is acceptable
    *
    * @see com.tangosol.dev.component.Constants#VIS_SYSTEM
    * @see com.tangosol.dev.component.Constants#VIS_HIDDEN
    * @see com.tangosol.dev.component.Constants#VIS_ADVANCED
    * @see com.tangosol.dev.component.Constants#VIS_VISIBLE
    */
    public boolean isVisibleLegal(int nVis)
        {
        switch (nVis)
            {
            case VIS_SYSTEM:
            case VIS_HIDDEN:
            case VIS_ADVANCED:
            case VIS_VISIBLE:
                return true;
            default:
                return false;
            }
        }

    /**
    * Set the visible attribute of the Behavior.
    *
    * @param nVis the new visibility value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    *
    * @see com.tangosol.dev.component.Constants#VIS_SYSTEM
    * @see com.tangosol.dev.component.Constants#VIS_HIDDEN
    * @see com.tangosol.dev.component.Constants#VIS_ADVANCED
    * @see com.tangosol.dev.component.Constants#VIS_VISIBLE
    */
    public void setVisible(int nVis)
            throws PropertyVetoException
        {
        setVisible(nVis, true);
        }

    /**
    * Set the visible attribute of the Behavior.
    *
    * @param nVis       the new visibility value
    * @param fVetoable  true if the setter can veto the value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void setVisible(int nVis, boolean fVetoable)
            throws PropertyVetoException
        {
        int nPrevVis = getVisible();

        if (nVis == nPrevVis)
            {
            return;
            }

        Integer prev  = Integer.valueOf(nPrevVis);
        Integer value = Integer.valueOf(nVis);

        if (fVetoable)
            {
            if (!isVisibleSettable())
                {
                readOnlyAttribute(ATTR_VISIBLE, prev, value);
                }

            if (!isVisibleLegal(nVis))
                {
                illegalAttributeValue(ATTR_VISIBLE, prev, value);
                }

            fireVetoableChange(ATTR_VISIBLE, prev, value);
            }

        m_nFlags = m_nFlags & ~VIS_MASK | nVis;
        firePropertyChange(ATTR_VISIBLE, prev, value);
        }


    // ----- Synchronized

    /**
    * Get the synchronized attribute of the Behavior.
    *
    * @return true if the Behavior is synchronized
    */
    public boolean isSynchronized()
        {
        return (m_nFlags & SYNC_MASK) == SYNC_MONITOR;
        }

    /**
    * Determine whether the synchronized attribute of the Behavior can be
    * set.  Since the synchronized attribute is expressed as a boolean, this
    * method doubles for both the "is settable" and "is legal" questions. The
    * synchronized attribute of the Behavior is flexible.
    *
    * @return true if the synchronized attribute can set
    */
    public boolean isSynchronizedSettable()
        {
        return isModifiable();
        }

    /**
    * Set the synchronized attribute of the Behavior.
    *
    * @param fSync  the new synchronized attribute value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void setSynchronized(boolean fSync)
            throws PropertyVetoException
        {
        setSynchronized(fSync, true);
        }

    /**
    * Set the synchronized attribute of the Behavior.
    *
    * @param fSync      the new synchronized attribute value
    * @param fVetoable  true if the setter can veto the value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void setSynchronized(boolean fSync, boolean fVetoable)
            throws PropertyVetoException
        {
        boolean fPrevSync = isSynchronized();

        if (fSync == fPrevSync)
            {
            return;
            }

        Boolean prev  = toBoolean(fPrevSync);
        Boolean value = toBoolean(fSync);

        if (fVetoable)
            {
            if (!isSynchronizedSettable())
                {
                readOnlyAttribute(ATTR_SYNCHRONIZED, prev, value);
                }

            fireVetoableChange(ATTR_SYNCHRONIZED, prev, value);
            }

        m_nFlags = m_nFlags & ~SYNC_MASK | (fSync ? SYNC_MONITOR : SYNC_NOMONITOR);
        firePropertyChange(ATTR_SYNCHRONIZED, prev, value);
        }


    // ----- Abstract

    /**
    * Get the Behavior implementation attribute.
    *
    * @return true if the Behavior is abstract
    */
    public boolean isAbstract()
        {
        return (m_nFlags & IMPL_MASK) == IMPL_ABSTRACT;
        }

    /**
    * Determine whether the implementation attribute of the Behavior can
    * be set.  Since the implementation attribute is expressed as a boolean,
    * this method doubles for both the "is settable" and "is legal"
    * questions. The implementation attribute of the Behavior is flexible
    * for instance-scope Behaviors but cannot be set for static-scope.
    *
    * @return true if the implementation attribute can set
    */
    public boolean isAbstractSettable()
        {
        return isModifiable() && !(isStatic() || isFinal());
        }

    /**
    * Set the Behavior's implementation attribute.
    *
    * @param fAbstract the new implementation attribute value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void setAbstract(boolean fAbstract)
            throws PropertyVetoException
        {
        setAbstract(fAbstract, true);
        }

    /**
    * Set the Behavior's implementation attribute.
    *
    * @param fAbstract  the new implementation attribute value
    * @param fVetoable  true if the setter can veto the value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void setAbstract(boolean fAbstract, boolean fVetoable)
            throws PropertyVetoException
        {
        boolean fPrevAbstract = isAbstract();

        if (fAbstract == fPrevAbstract)
            {
            return;
            }

        Boolean prev  = toBoolean(fPrevAbstract);
        Boolean value = toBoolean(fAbstract);

        if (fVetoable)
            {
            if (!isAbstractSettable())
                {
                readOnlyAttribute(ATTR_ABSTRACT, prev, value);
                }

            fireVetoableChange(ATTR_ABSTRACT, prev, value);
            }

        m_nFlags = m_nFlags & ~IMPL_MASK | (fAbstract ? IMPL_ABSTRACT : IMPL_CONCRETE);
        firePropertyChange(ATTR_ABSTRACT, prev, value);
        }


    // ----- Static

    /**
    * Determine the current scope of the Behavior.
    *
    * @return true if the Behavior is static (not instance)
    */
    public boolean isStatic()
        {
        return (m_nFlags & SCOPE_MASK) == SCOPE_STATIC;
        }

    /**
    * Determine if the scope of the Behavior can be modified.
    *
    * Static cannot co-exist with abstract or remote.
    * Static cannot be set if the  Behavior is from an "interface",
    * a Property, or from a super level.
    *
    * @return true if the scope attribute of the Behavior can be set
    */
    public boolean isStaticSettable()
        {
        // 2001.05.08 cp  allow static behaviors to be remote in order to
        //                support ejb home design using static methods
        return isModifiable() && !isFromNonManual() && !isAbstract(); // && !isRemote();
        }

    /**
    * Set the scope attribute of the Behavior.
    *
    * @param fStatic the new scope value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void setStatic(boolean fStatic)
            throws PropertyVetoException
        {
        setStatic(fStatic, true);
        }

    /**
    * Set the scope attribute of the Behavior.
    *
    * @param fStatic    the new scope value
    * @param fVetoable  true if the setter can veto the value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void setStatic(boolean fStatic, boolean fVetoable)
            throws PropertyVetoException
        {
        boolean fPrevStatic = isStatic();

        if (fStatic == fPrevStatic)
            {
            return;
            }

        Boolean prev  = toBoolean(fPrevStatic);
        Boolean value = toBoolean(fStatic);

        if (fVetoable)
            {
            if (!isStaticSettable())
                {
                readOnlyAttribute(ATTR_STATIC, prev, value);
                }

            fireVetoableChange(ATTR_STATIC, prev, value);
            }

        m_nFlags = m_nFlags & ~SCOPE_MASK | (fStatic ? SCOPE_STATIC : SCOPE_INSTANCE);
        firePropertyChange(ATTR_STATIC, prev, value);
        }


    // ----- Final

    /**
    * Determine the current finality of the Behavior.
    *
    * @return true if the Behavior is final
    */
    public boolean isFinal()
        {
        return (m_nFlags & DERIVE_MASK) == DERIVE_FINAL;
        }

    /**
    * Determine if the finality of the Behavior can be modified.
    *
    * @return true if the final attribute of the Behavior can be set
    */
    public boolean isFinalSettable()
        {
        return isModifiable() && !isAbstract();
        }

    /**
    * Set the finality attribute of the Behavior.
    *
    * @param fFinal the new finality value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void setFinal(boolean fFinal)
            throws PropertyVetoException
        {
        setFinal(fFinal, true);
        }

    /**
    * Set the finality attribute of the Behavior.
    *
    * @param fFinal     the new finality value
    * @param fVetoable  true if the setter can veto the value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void setFinal(boolean fFinal, boolean fVetoable)
            throws PropertyVetoException
        {
        boolean fPrevFinal = isFinal();

        if (fFinal == fPrevFinal)
            {
            return;
            }

        Boolean prev  = toBoolean(fPrevFinal);
        Boolean value = toBoolean(fFinal);

        if (fVetoable)
            {
            if (!isFinalSettable())
                {
                readOnlyAttribute(ATTR_FINAL, prev, value);
                }

            fireVetoableChange(ATTR_FINAL, prev, value);
            }

        m_nFlags = m_nFlags & ~DERIVE_MASK | (fFinal ? DERIVE_FINAL : DERIVE_DERIVABLE);
        firePropertyChange(ATTR_FINAL, prev, value);
        }


    // ----- Deprecated

    /**
    * Get the antiquity attribute of the Behavior.
    *
    * @return true if the Behavior is deprecated
    */
    public boolean isDeprecated()
        {
        return (m_nFlags & ANTIQ_MASK) == ANTIQ_DEPRECATED;
        }

    /**
    * Determine whether the antiquity attribute of the Behavior can be set.
    * Since the antiquity attribute is expressed as a boolean,  this method
    * doubles for both the "is settable" and "is legal" questions. The
    * antiquity attribute of the Behavior is flexible.
    *
    * @return true if the antiquity attribute can set
    */
    public boolean isDeprecatedSettable()
        {
        return isModifiable();
        }

    /**
    * Set the antiquity attribute of the Behavior.
    *
    * @param fDeprecated the new antiquity attribute value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void setDeprecated(boolean fDeprecated)
            throws PropertyVetoException
        {
        setDeprecated(fDeprecated, true);
        }

    /**
    * Set the antiquity attribute of the Behavior.
    *
    * @param fDeprecated  the new antiquity attribute value
    * @param fVetoable    true if the setter can veto the value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void setDeprecated(boolean fDeprecated, boolean fVetoable)
            throws PropertyVetoException
        {
        boolean fPrevDeprecated = isDeprecated();

        if (fDeprecated == fPrevDeprecated)
            {
            return;
            }

        Boolean prev  = toBoolean(fPrevDeprecated);
        Boolean value = toBoolean(fDeprecated);

        if (fVetoable)
            {
            if (!isDeprecatedSettable())
                {
                readOnlyAttribute(ATTR_DEPRECATED, prev, value);
                }

            fireVetoableChange(ATTR_DEPRECATED, prev, value);
            }

        m_nFlags = m_nFlags & ~ANTIQ_MASK | (fDeprecated ? ANTIQ_DEPRECATED : ANTIQ_CURRENT);
        firePropertyChange(ATTR_DEPRECATED, prev, value);
        }


    // ----- Remote

    /**
    * Get the Behavior distribution attribute.
    *
    * @return true if the Behavior is remotable
    */
    public boolean isRemote()
        {
        return (m_nFlags & DIST_MASK) == DIST_REMOTE;
        }

    /**
    * Determine whether the distribution attribute of the Behavior can be
    * set.  Since the distribution attribute is expressed as a boolean,  this
    * method doubles for both the "is settable" and "is legal" questions.
    * The distribution is one-way attribute (local -> remote).
    * Note that a behavior cannot be marked as remote if it is static or
    * non-public.
    *
    * @return true if the distribution attribute can set
    */
    public boolean isRemoteSettable()
        {
        Component cd = getComponent();
        // 2001.05.08 cp  allow static behaviors to be remote in order to
        //                support ejb home design using static methods
        // return isModifiable() && !isStatic() && getAccess() == ACCESS_PUBLIC
        //        && cd.isRemote() && cd.isGlobal()
        //        && (m_nPrevFlags & DIST_MASK) == DIST_LOCAL;
        return isModifiable() && getAccess() == ACCESS_PUBLIC
                && cd.isRemote() && cd.isGlobal()
                && (m_nPrevFlags & DIST_MASK) == DIST_LOCAL;
        }

    /**
    * Set the Behavior's distribution attribute.
    *
    * @param fRemote  the new distribution attribute value
    *
    * @exception PropertyVetoException if the new attribute value is not
    *            accepted
    */
    public void setRemote(boolean fRemote)
            throws PropertyVetoException
        {
        setRemote(fRemote, true);
        }

    /**
    * Set the Behavior's distribution attribute.
    *
    * @param fRemote    the new distribution attribute value
    * @param fVetoable  true if the setter can veto the value
    *
    * @exception PropertyVetoException if the new attribute value is not
    *            accepted
    */
    protected synchronized void setRemote(boolean fRemote, boolean fVetoable)
            throws PropertyVetoException
        {
        boolean fPrevRemote = isRemote();

        if (fRemote == fPrevRemote)
            {
            return;
            }

        Boolean prev  = toBoolean(fPrevRemote);
        Boolean value = toBoolean(fRemote);

        if (fVetoable)
            {
            if (!isRemoteSettable())
                {
                readOnlyAttribute(ATTR_REMOTE, prev, value);
                }

            fireVetoableChange(ATTR_REMOTE, prev, value);
            }

        m_nFlags = m_nFlags & ~DIST_MASK | (fRemote ? DIST_REMOTE : DIST_LOCAL);

        if (fPrevRemote)
            {
            // local Behaviors cannot have inout or out Parameters
            int c = getParameterCount();
            for (int i = 0; i < c; ++i)
                {
                Parameter param = getParameter(i);
                if (param.getDirection() != DIR_IN)
                    {
                    param.setDirection(DIR_IN, false);
                    }
                }
            }

        firePropertyChange(ATTR_REMOTE, prev, value);
        }


    // ----- return value

    /**
    * Access the return value sub-trait.  All modifications are done to the
    * sub-trait itself so there is no add/remove/set etc. for the return
    * value here.
    *
    * @return the return value sub-trait
    */
    public ReturnValue getReturnValue()
        {
        return m_retval;
        }


    // ----- name

    /**
    * Determine the name of this behavior.
    *
    * @return the name of this behavior
    */
    public String getName()
        {
        return m_sName;
        }

    /**
    * Determine if this behavior is a constructor,
    * only valid for Java Class Signatures.
    *
    * @return true if it is a constructor
    */
    public boolean isConstructor()
        {
        return isConstructor(m_sName);
        }

    /**
    * Determine if this behavior name or signature is a
    * constructor, only valid for Java Class Signatures.
    *
    * @param  sName the name or signature to check
    * @return true if is is a constructor
    */
    public static boolean isConstructor(String sName)
        {
        return sName.charAt(0) == '<';
        }

    /**
    * Determine if the behavior name can be set.  It cannot be set if
    * the behavior originates from anything except manual addition.
    *
    * @return true if the behavior name can be set
    */
    public boolean isNameSettable()
        {
        return isModifiable() && !isFromNonManual();
        }

    /**
    * Determine if the specified name is a legal behavior name and that
    * it won't conflict with other behavior signatures.
    *
    * @return true if the specified name is legal for this behavior
    */
    public boolean isNameLegal(String sName)
        {
        // must be a legal Java identifier
        if (sName == null || sName.length() == 0 ||
                !ClassHelper.isSimpleNameLegal(sName))
            {
            return false;
            }

        // check if name is unchanged
        if (sName.equals(m_sName))
            {
            return true;
            }

        // build the proposed signature
        String sSig = getSignature(sName, getParameterTypes());

        return !isSignatureReserved(sSig);
        }

    /**
    * Set the behavior name.
    *
    * @param sName the new name for the behavior
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
    * Set the behavior name.
    *
    * @param sName      the new name for the behavior
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
        updateSignature();

        firePropertyChange(ATTR_NAME, sPrev, sName);
        }


    // ----- parameters

    /**
    * Get all behavior parameters.
    *
    * @return an array of behavior parameters
    */
    public Parameter[] getParameter()
        {
        int c = getParameterCount();
        Parameter[] aparam = new Parameter[c];
        for (int i = 0; i < c; ++i)
            {
            aparam[i] = getParameter(i);
            }

        return aparam;
        }

    /**
    * Determine if the parameter array can be set.  The parameter array
    * can only be changed if the behavior is declared at this level
    * and isn't part of an interface or the result of integration.
    *
    * @return true if the array of parameters can be set
    */
    public boolean isParameterSettable()
        {
        return isModifiable() && !isFromNonManual();
        }

    /**
    * Determine if the specified array of parameters is a legal array of
    * parameters for this behavior.
    *
    * @param aparam  an array of behavior parameters; this must be the same
    *                set of parameters as returned from the corresponding
    *                accessor, but can be in a different order
    *
    * @return true if the specified array of parameters is legal
    */
    public boolean isParameterLegal(Parameter[] aparam)
        {
        int c = getParameterCount();
        if (aparam == null || aparam.length != c)
            {
            return false;
            }

        // verify that all parameters are accounted for and then build the
        // proposed signature
        StringBuffer sb = new StringBuffer(m_sName);
        sb.append('(');

        boolean[] afFound = new boolean [c];
        for (int i = 0; i < c; ++i)
            {
            Parameter param = aparam[i];
            int iCurrent = getParameterPosition(param);
            if (iCurrent < 0 || afFound[iCurrent])
                {
                return false;
                }

            afFound[iCurrent] = true;
            sb.append(param.getSignature());
            }

        sb.append(')');
        String sSig = sb.toString();
        return !isSignatureReserved(sSig);
        }

    /**
    * Set all behavior parameters, allowing parameter re-ordering.
    *
    * @param aparam  an array of behavior parameters; this must be the same
    *                set of parameters as returned from the corresponding
    *                accessor, but can be in a different order
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void setParameter(Parameter[] aparam)
            throws PropertyVetoException
        {
        setParameter(aparam, true);
        }

    /**
    * Set all behavior parameters, allowing parameter re-ordering.
    *
    * @param aParam     an array of behavior parameters; this must be the
    *                   same set of parameters as returned from the
    *                   corresponding accessor, but can be in a different
    *                   order
    * @param fVetoable  true if the setter can veto the value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void setParameter(Parameter[] aParam, boolean fVetoable)
            throws PropertyVetoException
        {
        Parameter[] aPrev = getParameter();
        int cParam = (fVetoable ? aPrev : aParam).length;

        // check if the passed set of parameters is in a different order
        try
            {
            boolean fSame = true;
            for (int i = 0; i < cParam; ++i)
                {
                if (aParam[i] != aPrev[i])
                    {
                    fSame = false;
                    break;
                    }
                }
            if (fSame)
                {
                return;
                }
            }
        catch (NullPointerException e)
            {
            // handled by legal check below
            }
        catch (ArrayIndexOutOfBoundsException e)
            {
            // handled by legal check below
            }

        if (fVetoable)
            {
            if (!isParameterSettable())
                {
                readOnlyAttribute(ATTR_PARAMETER, aPrev, aParam);
                }

            if (!isParameterLegal(aParam))
                {
                illegalAttributeValue(ATTR_PARAMETER, aPrev, aParam);
                }

            fireVetoableChange(ATTR_PARAMETER, aPrev, aParam);
            }

        m_vectParam.removeAllElements();
        for (int i = 0; i < cParam; ++i)
            {
            m_vectParam.addElement(aParam[i]);
            }
        updateSignature();

        firePropertyChange(ATTR_PARAMETER, aPrev, aParam);
        }

    /**
    * Helper to modify/add/remove parameters using only names and data types.
    *
    * @param adt     parameter data types
    * @param asName  parameter names
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void setParameter(DataType[] adt, String[] asName)
            throws PropertyVetoException
        {
        // validate passed information is legal
        if (adt == null || asName == null)
            {
            adt    = new DataType[0];
            asName = new String  [0];
            }

        Parameter[] aPrev = getParameter();
        int         cPrev = aPrev.length;
        if (adt.length != asName.length)
            {
            illegalAttributeValue(ATTR_PARAMETER, aPrev, new Object[] {adt, asName});
            }

        // make sure information has been changed
        if (cPrev == adt.length)
            {
            boolean fSame = true;
            for (int i = 0; i < cPrev; ++i)
                {
                Parameter param = aPrev[i];
                if (param.getDataType() != adt[i] || !param.getName().equals(asName[i]))
                    {
                    fSame = false;
                    break;
                    }
                }
            if (fSame)
                {
                return;
                }
            }

        // make sure information can be changed
        if (!(isModifiable() && isDeclaredAtThisLevel()))
            {
            illegalAttributeValue(ATTR_PARAMETER, aPrev, new Object[] {adt, asName});
            }

        // verify that signature is legal
        StringBuffer sb = new StringBuffer(m_sName);
        sb.append('(');
        StringTable tblNames = new StringTable(INSENS);
        int cParam = adt.length;
        for (int i = 0; i < cParam; ++i)
            {
            // validate type and name
            DataType dt    = adt[i];
            String   sName = asName[i];
            if (dt == null || dt == DataType.VOID || sName == null
                    || !ClassHelper.isSimpleNameLegal(sName) || tblNames.contains(sName))
                {
                illegalAttributeValue(ATTR_PARAMETER, aPrev, new Object[] {adt, asName});
                }

            // build signature
            sb.append(dt.getTypeString());

            // keep track of names
            tblNames.put(sName, Integer.valueOf(i));
            }
        sb.append(')');
        String sSig = sb.toString();

        // verify that signature is available
        if (isSignatureReserved(sSig))
            {
            illegalAttributeValue(ATTR_PARAMETER, aPrev, new Object[] {adt, asName});
            }

        // new parameter list (not all the info is available at this time)
        Parameter[] aParam = new Parameter[cParam];

        // check if signature is changed
        boolean fOrderUnchanged = sSig.equals(getSignature());
        if (!fOrderUnchanged)
            {
            // verify that order can be changed
            if (!isParameterSettable())
                {
                readOnlyAttribute(ATTR_PARAMETER, aPrev, aParam);
                }
            }

        // notify veto listeners
        fireVetoableChange(ATTR_PARAMETER, aPrev, aParam);

        if (fOrderUnchanged)
            {
            // only name (if any) changes
            for (int i = 0; i < cParam; ++i)
                {
                aPrev[i].setName(asName[i], false);
                }
            }
        else
            {
            // match up old parameters to new ones
            Vector vectNoMatch = new Vector();
            for (int i = 0; i < cPrev; ++i)
                {
                Parameter prev  = aPrev[i];
                String    sPrev = prev.getName();
                Integer   index = (Integer) tblNames.get(sPrev);
                if (index == null)
                    {
                    vectNoMatch.add(prev);
                    }
                else
                    {
                    // name matched
                    int iParam = index.intValue();
                    aParam[iParam] = prev;

                    // update type and name (in case of a case sensitive change)
                    prev.setDataType(adt   [iParam], false);
                    prev.setName    (asName[iParam], false);
                    }
                }

            // check for unmatched parameters
            boolean fMatch = true;
            for (int i = 0, c = 0; i < cParam; ++i)
                {
                Parameter param = aParam[i];
                if (param == null)
                    {
                    // try to match by data type
                    DataType dt    = adt   [i];
                    String   sName = asName[i];
                    if (fMatch)
                        {
                        if (vectNoMatch.isEmpty())
                            {
                            fMatch = false;
                            }
                        else
                            {
                            Parameter prev = (Parameter) vectNoMatch.get(0);
                            if (dt == prev.getDataType())
                                {
                                prev.setName(sName, false);
                                param = prev;
                                vectNoMatch.remove(0);
                                }
                            else
                                {
                                fMatch = false;
                                }
                            }
                        }

                    // otherwise create a new one
                    if (param == null)
                        {
                        param = new Parameter(this, dt, sName, DIR_IN);
                        param.validate();
                        }

                    aParam[i] = param;
                    }
                }

            // destroy left-overs
            if (!vectNoMatch.isEmpty())
                {
                for (Enumeration enmr = vectNoMatch.elements(); enmr.hasMoreElements(); )
                    {
                    ((Parameter) enmr.nextElement()).invalidate();
                    }
                }

            // store new set of Parameters
            Vector vectParam = new Vector(cParam);
            for (int i = 0; i < cParam; ++i)
                {
                vectParam.add(aParam[i]);
                }
            m_vectParam = vectParam;
            updateSignature();
            }

        // notify
        firePropertyChange(ATTR_PARAMETER, aPrev, aParam);
        }

    /**
    * Determine if a parameter can be added to the behavior.
    *
    * @param iParam  position for the new parameter (-1 to add to end)
    * @param dt      proposed parameter data type
    * @param sName   proposed parameter name
    *
    * @return true if it is possible to add the parameter as specified
    */
    public boolean isParameterAddable(int iParam, DataType dt, String sName)
        {
        // check if the signature is alterable; verify proposed index;
        // parameter name must be a unique legal Java identifier
        int c = getParameterCount();
        if (iParam < 0)
            {
            iParam = c;
            }
        if (!isParameterSettable()
                || iParam > c
                || sName == null
                || sName.length() == 0
                || !ClassHelper.isSimpleNameLegal(sName)
                || getParameter(sName) != null
                || dt == null
                || dt == DataType.VOID)
            {
            return false;
            }

        // build the proposed signature
        StringBuffer sb = new StringBuffer(m_sName);
        sb.append('(');
        for (int i = 0; i < c; ++i)
            {
            if (i == iParam)
                {
                sb.append(dt.getTypeString());
                }
            sb.append(getParameter(i).getSignature());
            }
        if (iParam == c)
            {
            sb.append(dt.getTypeString());
            }
        sb.append(')');
        String sSig = sb.toString();

        return !isSignatureReserved(sSig);
        }

    /**
    * Add the specified parameter to the behavior.
    *
    * @param iParam  position for the new parameter (-1 to add to end)
    * @param dt      proposed parameter data type
    * @param sName   proposed parameter name
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public Parameter addParameter(int iParam, DataType dt, String sName)
            throws PropertyVetoException
        {
        return addParameter(iParam, dt, sName, true);
        }

    /**
    * Add the specified parameter to the behavior.
    *
    * @param iParam     position for the new parameter (-1 to add to end)
    * @param dt         proposed parameter data type
    * @param sName      proposed parameter name
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized Parameter addParameter(int iParam, DataType dt, String sName, boolean fVetoable)
            throws PropertyVetoException
        {
        if (fVetoable && !isParameterAddable(iParam, dt, sName))
            {
            subNotAddable(ATTR_PARAMETER, sName);
            }

        // create parameter
        Parameter param = new Parameter(this, dt, sName, DIR_IN);
        param.setUID(new UID(), fVetoable);

        // notify veto listeners
        if (fVetoable)
            {
            try
                {
                fireVetoableSubChange(param, SUB_ADD);
                }
            catch (PropertyVetoException e)
                {
                param.invalidate();
                throw e;
                }
            }

        // add parameter
        if (iParam < 0)
            {
            m_vectParam.addElement(param);
            }
        else
            {
            m_vectParam.insertElementAt(param, iParam);
            }
        updateSignature();

        // notify listeners
        param.validate();
        fireSubChange(param, SUB_ADD);

        return param;
        }

    /**
    * Determine if a parameter can be removed to the behavior.
    *
    * @param iParam  position of the parameter to remove
    *
    * @return true if it is possible to remove the specified parameter
    */
    public boolean isParameterRemovable(int iParam)
        {
        // check if the signature is alterable; verify index;
        int c = getParameterCount();
        if (!isParameterSettable()
                || iParam < 0
                || iParam >= c)
            {
            return false;
            }

        // build the proposed signature
        StringBuffer sb = new StringBuffer(m_sName);
        sb.append('(');
        for (int i = 0; i < c; ++i)
            {
            if (i != iParam)
                {
                sb.append(getParameter(i).getSignature());
                }
            }
        sb.append(')');
        String sSig = sb.toString();

        return !isSignatureReserved(sSig);
        }

    /**
    * Remove the specified parameter from the behavior.
    *
    * @param iParam  the parameter index to remove
    *
    * @exception PropertyVetoException  if removal is illegal or if removal
    *            is vetoed
    */
    public void removeParameter(int iParam)
            throws PropertyVetoException
        {
        removeParameter(iParam, true);
        }

    /**
    * Remove the specified parameter from the behavior.
    *
    * @param iParam     the parameter index to remove
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException  if removal is illegal or if removal
    *            is vetoed
    */
    protected synchronized void removeParameter(int iParam, boolean fVetoable)
            throws PropertyVetoException
        {
        Parameter param;
        try
            {
            // get the parameter information
            param = getParameter(iParam);
            }
        catch (IndexOutOfBoundsException e)
            {
            if (fVetoable)
                {
                // the caller handed us garbage (so report garbage)
                subNotRemovable(ATTR_PARAMETER, e);
                }
            return;
            }

        if (fVetoable)
            {
            // verify that the parameter can be removed
            if (!isParameterRemovable(iParam))
                {
                subNotRemovable(ATTR_PARAMETER, param);
                }

            // notify veto listeners
            fireVetoableSubChange(param, SUB_REMOVE);
            }

        // remove the parameter
        m_vectParam.removeElementAt(iParam);
        updateSignature();

        // notify listeners
        fireSubChange(param, SUB_REMOVE);

        // discard the parameter
        param.invalidate();
        }

    /**
    * Get the number of behavior parameters.
    *
    * @return the number of behavior parameters
    */
    public int getParameterCount()
        {
        return m_vectParam.size();
        }

    /**
    * Get parameter by position.
    *
    * @param i the parameter number, zero based
    *
    * @return the specified parameter
    */
    public Parameter getParameter(int i)
        {
        return (Parameter) m_vectParam.elementAt(i);
        }

    /**
    * Get position by parameter.
    *
    * @param param  the parameter to search for
    *
    * @return the parameter position, zero based, or -1 if not found
    */
    public int getParameterPosition(Parameter param)
        {
        return m_vectParam.indexOf(param);
        }

    /**
    * Get parameter by name, case insensitive.
    *
    * @param sName the parameter name
    *
    * @return the specified parameter or null if no parameter exists by that
    *         name
    */
    public Parameter getParameter(String sName)
        {
        CollationKey ckName = INSENS.getCollationKey(sName);

        int c = getParameterCount();
        for (int i = 0; i < c; ++i)
            {
            Parameter    param   = getParameter(i);
            CollationKey ckParam = INSENS.getCollationKey(param.getName());
            if (ckName.compareTo(ckParam) == 0)
                {
                return param;
                }
            }

        return null;
        }

    /**
    * Build an array of the Parameter data types.
    *
    * @return an array of the Parameter data types
    */
    public DataType[] getParameterTypes()
        {
        int        cdt = m_vectParam.size();
        DataType[] adt = new DataType[cdt];
        for (int i = 0; i < cdt; ++i)
            {
            adt[i] = ((Parameter) m_vectParam.elementAt(i)).getDataType();
            }
        return adt;
        }

    /**
    * Extract the parameter names from an existing behavior.
    *
    * @return an array of the Parameter names
    */
    public String[] getParameterNames()
        {
        int      cNames = m_vectParam.size();
        String[] asName = new String[cNames];
        for (int i = 0; i < cNames; ++i)
            {
            asName[i] = ((Parameter) m_vectParam.elementAt(i)).getName();
            }
        return asName;
        }

    /**
    * Build a list of default parameter names for the given parameter data
    * types.
    *
    * @param  adtParam  an array of parameter Data Types
    *
    * @return an array of parameter names
    */
    public static String[] getDefaultParameterNames(DataType[] adtParam)
        {
        int      cParams = (adtParam != null ? adtParam.length : 0);
        String[] asParam = new String[cParams];
        for (int i = 0; i < cParams; ++i)
            {
            asParam[i] = "Param_" + (i + 1);
            }
        return asParam;
        }

    /**
    * Build a string representing the Parameter directions.  The string is
    * composed the characters 'I' (in), 'O' (out), and 'B' (both in and out).
    *
    * @return a string of characters representing the Parameter directions
    */
    protected String getParameterDirections()
        {
        int c = m_vectParam.size();
        char[] ach = new char[c];

        for (int i = 0; i < c; ++i)
            {
            switch (((Parameter) m_vectParam.elementAt(i)).getDirection())
                {
                default:
                case DIR_IN:
                    ach[i] = 'I';
                    break;
                case DIR_OUT:
                    ach[i] = 'O';
                    break;
                case DIR_INOUT:
                    ach[i] = 'B';
                    break;
                }
            }

        return new String(ach);
        }

    /**
    * Set the Parameter directions based on a string composed of the
    * characters 'I' (in), 'O' (out), and 'B' (both in and out).
    *
    * @param sDir       a string of characters representing the Parameter
    *                   directions
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected void setParameterDirections(String sDir, boolean fVetoable)
            throws PropertyVetoException
        {
        int c = m_vectParam.size();
        if (c != sDir.length())
            {
            throw new IllegalArgumentException(CLASS + ".setParameterDirections:"
                    + "  Invalid direction string (" + sDir + ")!");
            }

        for (int i = 0; i < c; ++i)
            {
            int nDir;
            switch (sDir.charAt(i))
                {
                case 'I':
                    nDir = DIR_IN;
                    break;
                case 'O':
                    nDir = DIR_OUT;
                    break;
                case 'B':
                    nDir = DIR_INOUT;
                    break;
                default:
                    throw new IllegalArgumentException(CLASS + ".setParameterDirections:"
                            + "  Invalid direction string (" + sDir + ")!");
                }
            ((Parameter) m_vectParam.elementAt(i)).setDirection(nDir, fVetoable);
            }
        }


    // ----- exceptions

    /**
    * Get a list of the names of all declared exceptions.
    *
    * @return an array of the qualified names of the exceptions declared for
    *         this behavior.
    */
    public String[] getExceptionNames()
        {
        return m_tblExcept.strings();
        }

    /**
    * Get an exceptions by its name.  For a list of names, see
    * getExceptionNames.
    *
    * @param sExcept  the qualified name of the exception to get
    *
    * @return the behavior exception corresponding to the passed name or null
    *         if the specified exception is not declared by this behavior
    */
    public Throwee getException(String sExcept)
        {
        Throwee except = (Throwee) m_tblExcept.get(sExcept);
        if (except != null)
            {
            int nExists = except.getExists();
            if (nExists == EXISTS_DELETE || nExists == EXISTS_NOT)
                {
                return null;
                }
            }

        return except;
        }

    /**
    * Determine if the exception array can be set.  The exception array
    * can be set iff at least one of the following is true:
    *
    *   1)  There are exceptions that can be added
    *   2)  There are exceptions that can be removed
    *   3)  There are exceptions that can be unremoved
    *
    * @return true if the array of exceptions can be set
    */
    public boolean isExceptionSettable()
        {
        if (!isModifiable())
            {
            return false;
            }

        // exceptions can be added iff the behavior is not inherited and
        // does not originate from an interface or from integration
        if (!(isFromSuper() || isFromImplements() || isFromDispatches() || isFromIntegration()))
            {
            return true;
            }

        // exceptions can be removed or unremoved iff the behavior says so
        for (Enumeration enmr = m_tblExcept.keys(); enmr.hasMoreElements(); )
            {
            String sExcept = (String) enmr.nextElement();
            if (isExceptionRemovable(sExcept) || isExceptionUnremovable(sExcept))
                {
                return true;
                }
            }

        // nothing can be set
        return false;
        }

    /**
    * Determine if the exception array can be set to the specified value.
    *
    * @param asExcept  an array of behavior exceptions names
    *
    * @return true if the array of exceptions can be set as specified
    */
    public boolean isExceptionLegal(String[] asExcept)
        {
        // get the old list of exceptions
        StringTable tblOld = new StringTable();
        for (Enumeration enmr = m_tblExcept.keys(); enmr.hasMoreElements(); )
            {
            String sExcept = (String) enmr.nextElement();
            if (getException(sExcept) != null)
                {
                tblOld.add(sExcept);
                }
            }

        // get the new list of exceptions
        StringTable tblNew = new StringTable();
        for (int i = asExcept.length - 1; i >= 0; --i)
            {
            tblNew.add(asExcept[i]);
            }

        // check if the set of exceptions is unchanged
        if (tblNew.keysEquals(tblOld))
            {
            return true;
            }

        // determine what exceptions were added (or "unremoved")
        StringTable tblAdded = (StringTable) tblNew.clone();
        tblAdded.removeAll(tblOld);
        for (Enumeration enmr = tblAdded.keys(); enmr.hasMoreElements(); )
            {
            String sExcept = (String) enmr.nextElement();
            if (!isExceptionAddable(sExcept) && !isExceptionUnremovable(sExcept))
                {
                return false;
                }
            }

        // determine what exceptions were removed
        StringTable tblRemoved = tblOld;
        tblRemoved.removeAll(tblNew);
        for (Enumeration enmr = tblRemoved.keys(); enmr.hasMoreElements(); )
            {
            if (!isExceptionRemovable((String) enmr.nextElement()))
                {
                return false;
                }
            }

        return true;
        }

    /**
    * Set all behavior exceptions.
    *
    * @param asExcept  an array of behavior exceptions names
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void setException(String[] asExcept)
            throws PropertyVetoException
        {
        setException(asExcept, true);
        }

    /**
    * Set all behavior exceptions, allowing exception re-ordering.
    *
    * @param asExcept   an array of behavior exceptions names
    * @param fVetoable  true if the setter can veto the value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void setException(String[] asExcept, boolean fVetoable)
            throws PropertyVetoException
        {
        // get the old list of exceptions
        StringTable tblOld = new StringTable();
        for (Enumeration enmr = m_tblExcept.keys(); enmr.hasMoreElements(); )
            {
            String sExcept = (String) enmr.nextElement();
            if (getException(sExcept) != null)
                {
                tblOld.add(sExcept);
                }
            }

        // get the new list of exceptions
        StringTable tblNew = new StringTable();
        for (int i = asExcept.length - 1; i >= 0; --i)
            {
            tblNew.add(asExcept[i]);
            }

        // check if the set of exceptions is unchanged
        if (tblNew.keysEquals(tblOld))
            {
            return;
            }

        // for "previous vs. new" notifications
        String[] asOld = tblOld.strings();
        String[] asNew = tblNew.strings();

        // verify exceptions can be changed
        if (fVetoable)
            {
            if (!isExceptionSettable())
                {
                readOnlyAttribute(ATTR_EXCEPTION, asOld, asNew);
                }

            if (!isExceptionLegal(asExcept))
                {
                illegalAttributeValue(ATTR_EXCEPTION, asOld, asNew);
                }

            fireVetoableChange(ATTR_EXCEPTION, asOld, asNew);
            }

        // determine what exceptions were added (or "unremoved")
        StringTable tblAdded = (StringTable) tblNew.clone();
        tblAdded.removeAll(tblOld);
        for (Enumeration enmr = tblAdded.keys(); enmr.hasMoreElements(); )
            {
            String sExcept = (String) enmr.nextElement();
            if (isExceptionUnremovable(sExcept))
                {
                unremoveException(sExcept, false);
                }
            else
                {
                addException(sExcept, false);
                }
            }

        // determine what exceptions were removed
        StringTable tblRemoved = tblOld;
        tblRemoved.removeAll(tblNew);
        for (Enumeration enmr = tblRemoved.keys(); enmr.hasMoreElements(); )
            {
            removeException((String) enmr.nextElement(), false);
            }

        firePropertyChange(ATTR_EXCEPTION, asOld, asNew);
        }

    /**
    * Determine if the specified exception can be added to this behavior.
    *
    * Unfortunately, there is no way to tell if a name actually references an
    * existing class or if that class is truly an exception (or actually, if
    * that class derives in some way from java.lang.Throwable).
    *
    * From the Java Language Specification (8.4.4):
    * A method that overrides or hides another method (8.4.6), including
    * methods that implement abstract methods defined in interfaces, may not
    * be declared to throw more checked exceptions than the overridden or
    * hidden method.  More precisely, suppose that B is a class or interface,
    * and A is a superclass or super interface of B, and a method declaration
    * n in B overrides or hides a method declaration m in A. If n has a
    * throws clause that mentions any checked exception types, then m must
    * have a throws clause, and for every checked exception type listed in
    * the throws clause of n, that same exception class or one of its super-
    * classes must occur in the throws clause of m; otherwise, a compile-time
    * error occurs.
    *
    * Behaviors resulting from property declarations can have additional
    * exceptions; from the JavaBeans 1.0 API Specification:
    * Both simple and indexed property accessor methods may throw checked
    * exceptions. This allows property setter/getter methods to report
    * exceptional conditions.
    *
    * @param sExcept  the qualified name of the exception to add
    *
    * @return true if it is possible to add the exception as specified
    */
    public boolean isExceptionAddable(String sExcept)
        {
        if (!isModifiable() || isFromSuper() || isFromImplements() || isFromDispatches() || isFromIntegration())
            {
            return false;
            }

        // check if an identically named exception already exists
        if (m_tblExcept.contains(sExcept))
            {
            return false;
            }

        // validate that sExcept is an acceptable class name
        try
            {
            return DataType.getClassType(sExcept) != null;
            }
        catch (IllegalArgumentException e)
            {
            return false;
            }
        }

    /**
    * Add the specified exception to the behavior.
    *
    * @param sExcept  the qualified name of the exception to add
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public Throwee addException(String sExcept)
            throws PropertyVetoException
        {
        return addException(sExcept, true);
        }

    /**
    * Add the specified exception to the behavior.
    *
    * @param sExcept    the qualified name of the exception to add
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized Throwee addException(String sExcept, boolean fVetoable)
            throws PropertyVetoException
        {
        if (fVetoable && !isExceptionAddable(sExcept))
            {
            subNotAddable(ATTR_EXCEPTION, sExcept);
            }

        // create exception
        Throwee except = new Throwee(this, DataType.getClassType(sExcept));
        except.setFromManual(true, false);
        except.setUID(new UID(), fVetoable);

        // notify veto listeners
        if (fVetoable)
            {
            try
                {
                fireVetoableSubChange(except, SUB_ADD);
                }
            catch (PropertyVetoException e)
                {
                except.invalidate();
                throw e;
                }
            }

        // add exception
        m_tblExcept.put(except.getUniqueName(), except);

        // notify listeners
        except.validate();
        fireSubChange(except, SUB_ADD);

        return except;
        }

    /**
    * Determine if the specified exception can be removed from the behavior.
    * Exceptions are basically always removable, since you can throw less
    * (but not more); see the Java Language Specification (8.4.4).
    *
    * @param sExcept  the qualified name of the exception to remove
    *
    * @return true if it is possible to remove the exception as specified
    */
    public boolean isExceptionRemovable(String sExcept)
        {
        if (!isModifiable())
            {
            return false;
            }

        Throwee except = getException(sExcept);
        if (except == null)
            {
            return false;
            }

        int nExists = except.getExists();
        return nExists == EXISTS_INSERT || nExists == EXISTS_UPDATE;
        }

    /**
    * Remove the specified exception from the behavior.
    *
    * @param sExcept  the qualified name of the exception to remove
    *
    * @exception PropertyVetoException  if removal is illegal or if
    *            removal is vetoed
    */
    public void removeException(String sExcept)
            throws PropertyVetoException
        {
        removeException(sExcept, true);
        }

    /**
    * Remove the specified exception from the behavior.
    *
    * @param sExcept    the qualified name of the exception to remove
    * @param fVetoable  if the removal can be vetoed
    *
    * @exception PropertyVetoException  if removal is illegal or if
    *            removal is vetoed
    */
    protected synchronized void removeException(String sExcept, boolean fVetoable)
            throws PropertyVetoException
        {
        // get the exception information
        Throwee except = getException(sExcept);
        if (except == null)
            {
            if (fVetoable)
                {
                subNotRemovable(ATTR_EXCEPTION, sExcept);
                }
            return;
            }

        // check if already removed
        int nExists = except.getExists();
        if (nExists != EXISTS_INSERT && nExists != EXISTS_UPDATE)
            {
            return;
            }

        if (fVetoable)
            {
            // verify the exception can be removed
            if (!isExceptionRemovable(sExcept))
                {
                subNotRemovable(ATTR_EXCEPTION, sExcept);
                }

            // notify veto listeners
            fireVetoableSubChange(except, SUB_REMOVE);
            }

        boolean fDestroy = isDeclaredAtThisLevel() && !except.isFromNonManual();
        if (fDestroy)
            {
            // remove the exception altogether
            m_tblExcept.remove(sExcept);
            }
        else
            {
            // just flag exception as removed
            except.setExists(EXISTS_DELETE);
            }

        // notify listeners
        fireSubChange(except, SUB_REMOVE);

        // if removed altogether, then destroy the exception information
        if (fDestroy)
            {
            except.invalidate();
            }
        }

    /**
    * Determine if the specified exception can be un-removed from the
    * behavior.  Exceptions that have been removed at this level can
    * be un-removed assuming they don't cause a behavior declaration
    * conflict (e.g. two interfaces define same method signature with
    * two different exceptions).
    *
    * @param sExcept  the qualified name of the exception to un-remove
    *
    * @return true if it is possible to un-remove the specified exception
    */
    public boolean isExceptionUnremovable(String sExcept)
        {
        // verify that exception has been removed
        Throwee except = (Throwee) m_tblExcept.get(sExcept);
        if (except == null || except.getExists() != EXISTS_DELETE)
            {
            return false;
            }

        // the non-manual origin of the exception must match the non-manual
        // origin of the behavior; in other words, if two interfaces declare
        // the behavior, then they must both declare the exception
        if (equalsOriginSansManual(except))
            {
            return true;
            }

        // could just be a difference in origins caused by a Property trait
        return this.isFromSuper()       == except.isFromSuper()
            && this.isFromIntegration() == except.isFromIntegration()
            && isEqual(this.enumImplements(), except.enumImplements())
            && isEqual(this.enumDispatches(), except.enumDispatches());
        }

    /**
    * Check for equality of two enumerations in terms of order and
    * contents.  All elements of the first enumeration must be non-null.
    *
    * TODO move to com.tangosol.util.Base
    */
    private static boolean isEqual(Enumeration enmr1, Enumeration enmr2)
        {
        while (enmr1.hasMoreElements() && enmr2.hasMoreElements())
            {
            if (!enmr1.nextElement().equals(enmr2.nextElement()))
                {
                return false;
                }
            }
        return !enmr1.hasMoreElements() && !enmr2.hasMoreElements();
        }

    /**
    * Compare the contents of a vector with the contents of an enumerator.
    *
    * @param vect an instance of java.util.Vector
    * @param enmr an instance of java.util.Enumeration
    *
    * @return true if the contents are identical (irregardless of order)
    */
    private static boolean VectMatchesEnum(Vector vect, Enumeration enmr)
        {
        int cEnum = 0;
        while (enmr.hasMoreElements())
            {
            if (!vect.contains(enmr.nextElement()))
                {
                return false;
                }
            ++cEnum;
            }
        return (cEnum == vect.size());
        }

    /**
    * Un-remove the specified exception from the behavior.
    *
    * @param sExcept  the qualified name of the exception to un-remove
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void unremoveException(String sExcept)
            throws PropertyVetoException
        {
        unremoveException(sExcept, true);
        }

    /**
    * Un-remove the specified exception from the behavior.
    *
    * @param sExcept    the qualified name of the exception to un-remove
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void unremoveException(String sExcept, boolean fVetoable)
            throws PropertyVetoException
        {
        // get the exception information
        Throwee except = (Throwee) m_tblExcept.get(sExcept);
        if (except == null)
            {
            if (fVetoable)
                {
                subNotUnremovable(ATTR_EXCEPTION, sExcept);
                }
            return;
            }

        // check for no change
        int nExists = except.getExists();
        if (nExists == EXISTS_INSERT || nExists == EXISTS_UPDATE)
            {
            return;
            }

        if (fVetoable)
            {
            // verify that the exception can be un-removed
            if (!isExceptionUnremovable(sExcept))
                {
                subNotUnremovable(ATTR_EXCEPTION, sExcept);
                }

            // notify veto listeners
            fireSubChange(except, SUB_UNREMOVE);
            }

        // flag exception as no longer removed
        except.setExists(EXISTS_UPDATE);

        // notify listeners
        fireSubChange(except, SUB_UNREMOVE);
        }

    /**
    * Update the reference to an exception when the exception data type
    * changes.  This method is used internally by the Component Definition.
    *
    * @param sOld the old exception class name
    * @param sNew the new exception class name
    */
    protected void renameException(String sOld, String sNew)
        {
        m_tblExcept.put(sNew, m_tblExcept.remove(sOld));
        }


    // ----- implementations

    /**
    * Get all implementations at this derivation level.
    *
    * @return an array of this behavior's implementations
    */
    public Implementation[] getImplementation()
        {
        Vector           vect  = m_vectScript;
        int              cImpl = vect.size();
        Implementation[] aImpl = new Implementation[cImpl];
        while (cImpl-- > 0)
            {
            aImpl[cImpl] = (Implementation) vect.get(cImpl);
            }

        return aImpl;
        }

    /**
    * Get implementation by position.  The first implementation (index 0) is
    * the first implementation that will be executed at this derivation
    * level.  Each implementation can "super" to the next implementation.  If
    * a modification level of the behavior sets the OverrideBase attribute,
    * then the implementations from the base modification level(s) are not
    * available to "super" to, and thus described as "hidden" or "replaced"
    * or "overridden".  The "super" call from the last implementation in a
    * derivation level executes the first implementation of the behavior in
    * the next derivation level that contains an implementation of the
    * behavior.
    *
    * @param i the implementation number, zero based
    *
    * @return the specified implementation
    */
    public Implementation getImplementation(int i)
        {
        return (Implementation) m_vectScript.elementAt(i);
        }

    /**
    * Get the number of behavior implementations (at this derivation level).
    *
    * @return the number of behavior implementations
    */
    public int getImplementationCount()
        {
        return m_vectScript.size();
        }

    /**
    * Get the number of behavior implementations at this modification level.
    *
    * @return the number of implementations which are at this modification
    *         level
    */
    public int getModifiableImplementationCount()
        {
        return m_vectScript.size() - m_cBaseLevelImpl;
        }

    /**
    * Get the number of behavior implementations at this derivation level,
    * not including this modification level's implementations.
    *
    * Using the indexed accessor method getImplementation(int), the caller
    * can get any base level implementation by adding the return value from
    * getModifiableImplementationCount() to the desired base implementation index.
    *
    * @return the number of base level behavior implementations
    */
    public int getBaseImplementationCount()
        {
        return m_cBaseLevelImpl;
        }

    /**
    * Determine the number of reachable implementations at this derivation
    * level.
    *
    * @return the number of implementations at this modification level plus
    *         (if the base implementations are not overridden) the number
    *         of base implementations
    */
    public int getCallableImplementationCount()
        {
        int cImpl = m_vectScript.size();
        if (m_fOverrideBase)
            {
            cImpl -= m_cBaseLevelImpl;
            }
        return cImpl;
        }

    /**
    * Determine if the implementation array can be set.  The implementation array
    * can be changed if the behavior is modifiable.
    *
    * @return true if the array of implementations can be set
    */
    public boolean isImplementationSettable()
        {
        return isModifiable();
        }

    /**
    * Determine if the specified array of implementations is a legal array of
    * implementations for this behavior.
    *
    * @param aImpl  an array of behavior implementations; this must be the same
    *               set of implementations as returned from the corresponding
    *               accessor, but can be in a different order
    *
    * @return true if the specified array of implementations is legal
    */
    public boolean isImplementationLegal(Implementation[] aImpl)
        {
        Implementation[] aPrev = getImplementation();
        int              cImpl = aPrev.length;
        if (aImpl == null || aImpl.length != cImpl)
            {
            return false;
            }

        // verify that non-moveable implementations did not move
        int cMove = getModifiableImplementationCount();
        for (int i = cMove; i < cImpl; ++i)
            {
            if (aImpl[i] != aPrev[i])
                {
                return false;
                }
            }

        // verify that all moveable implementations are accounted for
        boolean[] afFound = new boolean [cMove];
        for (int i = 0; i < cMove; ++i)
            {
            Implementation impl = aImpl[i];
            if (impl == null)
                {
                return false;
                }

            int iCurrent = getImplementationPosition(impl);
            if (iCurrent < 0 || iCurrent >= cMove || afFound[iCurrent])
                {
                return false;
                }

            afFound[iCurrent] = true;
            }

        return true;
        }

    /**
    * Set all behavior implementations, allowing implementation re-ordering.
    *
    * @param aImpl  an array of behavior implementations; this must be the same
    *               set of implementations as returned from the corresponding
    *               accessor, but can be in a different order
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void setImplementation(Implementation[] aImpl)
            throws PropertyVetoException
        {
        setImplementation(aImpl, true);
        }

    /**
    * Set all behavior implementations, allowing implementation re-ordering.
    *
    * @param aImpl      an array of behavior implementations; this must be
    *                   the same set of implementations as returned from the
    *                   corresponding accessor, but can be in a different
    *                   order
    * @param fVetoable  true if the setter can veto the value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void setImplementation(Implementation[] aImpl, boolean fVetoable)
            throws PropertyVetoException
        {
        Implementation[] aPrev = getImplementation();
        int cImpl = (fVetoable ? aPrev : aImpl).length;

        // check if the passed set of Implementations is in a different order
        try
            {
            boolean fSame = true;
            for (int i = 0; i < cImpl; ++i)
                {
                if (aImpl[i] != aPrev[i])
                    {
                    fSame = false;
                    break;
                    }
                }
            if (fSame)
                {
                return;
                }
            }
        catch (NullPointerException e)
            {
            // handled by legal check below
            }
        catch (ArrayIndexOutOfBoundsException e)
            {
            // handled by legal check below
            }

        if (fVetoable)
            {
            if (!isImplementationSettable())
                {
                readOnlyAttribute(ATTR_IMPLEMENTATION, aPrev, aImpl);
                }

            if (!isImplementationLegal(aImpl))
                {
                illegalAttributeValue(ATTR_IMPLEMENTATION, aPrev, aImpl);
                }

            fireVetoableChange(ATTR_IMPLEMENTATION, aPrev, aImpl);
            }

        // store new set of implementations
        Vector vectScript = new Vector();
        for (int i = 0; i < cImpl; ++i)
            {
            vectScript.add(aImpl[i]);
            }
        m_vectScript = vectScript;

        firePropertyChange(ATTR_IMPLEMENTATION, aPrev, aImpl);
        }


    /**
    * Determine if a implementation can be added to the behavior.
    *
    * @param iImpl    position for the new implementation (0 to insert at the
    *                 beginning or -1 to add to end of the execution "chain")
    * @param sLang    proposed implementation language
    * @param sScript  proposed implementation script
    *
    * @return true if it is possible to add the implementation as specified
    */
    public boolean isImplementationAddable(int iImpl, String sLang, String sScript)
        {
        return isImplementationSettable()
                && iImpl <= getModifiableImplementationCount()
                && Implementation.isLanguageLegal(sLang)
                && Implementation.isScriptLegal(sScript);
        }

    /**
    * Add the specified implementation to the behavior.
    *
    * @param iImpl    position for the new implementation (0 to insert at the
    *                 beginning or -1 to add to end of the execution "chain")
    * @param sLang    implementation language
    * @param sScript  implementation script
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public Implementation addImplementation(int iImpl, String sLang, String sScript)
            throws PropertyVetoException
        {
        return addImplementation(iImpl, sLang, sScript, true);
        }

    /**
    * Add the specified implementation to the behavior.
    *
    * @param iImpl      position for the new implementation (0 to insert at
    *                   the beginning or -1 to add to end of the execution
    *                   "chain")
    * @param sLang      implementation language
    * @param sScript    implementation script
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized Implementation addImplementation(int iImpl, String sLang, String sScript, boolean fVetoable)
            throws PropertyVetoException
        {
        // verify implementation can be added
        if (fVetoable && !isImplementationAddable(iImpl, sLang, sScript))
            {
            subNotAddable(ATTR_IMPLEMENTATION, sScript);
            }

        // create implementation
        Implementation impl = new Implementation(this, sLang, sScript);
        impl.assignUID();

        // notify veto listeners
        if (fVetoable)
            {
            try
                {
                fireVetoableSubChange(impl, SUB_ADD);
                }
            catch (PropertyVetoException e)
                {
                impl.invalidate();
                throw e;
                }
            }

        // add implementation
        if (iImpl < 0)
            {
            iImpl = getModifiableImplementationCount();
            }

        if (iImpl >= m_vectScript.size())
            {
            m_vectScript.addElement(impl);
            }
        else
            {
            m_vectScript.insertElementAt(impl, iImpl);
            }

        // check if abstract should be toggled to concrete automatically
        if ((m_nFlags     & IMPL_MASK) == IMPL_ABSTRACT &&
            (m_nPrevFlags & IMPL_MASK) == IMPL_ABSTRACT &&
            isFromSuper())
            {
            setAbstract(false, false);
            }

        // notify listeners
        fireSubChange(impl, SUB_ADD);

        return impl;
        }

    /**
    * Store the specified implementation on the JCS behavior.
    *
    * @param sLang      implementation language
    * @param sScript    implementation script
    */
    void setJcsImplementation(String sLang, String sScript)
        {
        azzert(sLang.indexOf('(') != -1);

        // create implementation
        Implementation impl = new Implementation(this, sLang, sScript);
        impl.assignUID();

        removeJcsImplementation();      // provides idempotency
        m_vectScript.addElement(impl);
        ++m_cBaseLevelImpl;             // assumed to be at base level
        }

    /**
    * Remove the JCS implementation.
    */
    void removeJcsImplementation()
        {
        Vector vectScript = m_vectScript;
        if (vectScript.isEmpty())
            {
            return;
            }

        int iScript = vectScript.size() - 1;
        if (isJcsImplementation(iScript))
            {
            vectScript.remove(iScript);
            if (m_cBaseLevelImpl > 0)
                {
                --m_cBaseLevelImpl;
                }
            }
        }

    /**
    * Check whether the implementation at the specified index
    * is an added "synthetic" implementation
    *
    * @return true if the implementation at the specified index
    *              is an added implementation; false otherwise
    *
    * @see #setJcsImplementation(String, String)
    */
    private boolean isJcsImplementation(int iImpl)
        {
        Implementation impl = (Implementation) m_vectScript.get(iImpl);

        return impl.getLanguage().indexOf('(') != -1;
        }


    /**
    * Determine if a implementation can be removed from the behavior.
    *
    * @param iImpl  position of the implementation to remove
    *
    * @return true if it is possible to remove the specified implementation
    */
    public boolean isImplementationRemovable(int iImpl)
        {
        return isImplementationSettable()
                && iImpl >= 0
                && iImpl < getModifiableImplementationCount();
        }

    /**
    * Remove the specified implementation from the behavior.
    *
    * @param iImpl  the implementation index to remove
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void removeImplementation(int iImpl)
            throws PropertyVetoException
        {
        removeImplementation(iImpl, true);
        }

    /**
    * Remove the specified implementation from the behavior.
    *
    * @param iImpl      the implementation index to remove
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void removeImplementation(int iImpl, boolean fVetoable)
            throws PropertyVetoException
        {
        // get the implementation
        Implementation impl = null;
        try
            {
            impl = getImplementation(iImpl);
            }
        catch (IndexOutOfBoundsException e)
            {
            if (fVetoable)
                {
                // the caller handed us garbage (so report garbage)
                subNotRemovable(ATTR_PARAMETER, e);
                }
            return;
            }

        if (fVetoable)
            {
            // verify the implementation can be removed
            if (!isImplementationRemovable(iImpl))
                {
                subNotRemovable(ATTR_IMPLEMENTATION, getImplementation(iImpl));
                }

            // notify veto listeners
            fireVetoableSubChange(impl, SUB_REMOVE);
            }

        // remove the implementation
        m_vectScript.removeElementAt(iImpl);

        // check if abstract should be set automatically
        if ((m_nFlags     & IMPL_MASK) == IMPL_CONCRETE &&
            (m_nPrevFlags & IMPL_MASK) == IMPL_ABSTRACT &&
            isFromSuper())
            {
            setAbstract(true, false);
            }

        // notify listeners
        fireSubChange(impl, SUB_REMOVE);

        // discard the implementation
        impl.invalidate();
        }

    /**
    * Get position by implementation.
    *
    * @param impl  the implementation to search for
    *
    * @return the implementation position, zero based, or -1 if not found
    */
    public int getImplementationPosition(Implementation impl)
        {
        return m_vectScript.indexOf(impl);
        }


    // ----- override base

    /**
    * Determine if this modification level's implementations override (do not
    * super to) the base level's implementations.
    *
    * @return true if the implementations at this modification level override
    *         (i.e. hide) the base implementations, false if this level's
    *         implementations supplement (super to) the base implementations
    */
    public boolean isOverrideBase()
        {
        return m_fOverrideBase;
        }

    /**
    * Specify implementation override to either override (do not super to)
    * or supplement (allow super to) the base level's implementations.
    *
    * @param fOverride  true if the implementations at this modification
    *                   level should override (i.e. hide) the base
    *                   implementations, false if this level's implement-
    *                   ations supplement (super to) the base implementations
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void setOverrideBase(boolean fOverride)
            throws PropertyVetoException
        {
        setOverrideBase(fOverride, true);
        }

    /**
    * Specify implementation override to either override (do not super to)
    * or supplement (allow super to) the base level's implementations.
    *
    * @param fOverride  true if the implementations at this modification
    *                   level should override (i.e. hide) the base
    *                   implementations, false if this level's implement-
    *                   ations supplement (super to) the base implementations
    * @param fVetoable  true if the setter can veto the value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void setOverrideBase(boolean fOverride, boolean fVetoable)
            throws PropertyVetoException
        {
        boolean fPrev = isOverrideBase();

        if (fOverride == fPrev)
            {
            return;
            }

        Boolean prev  = toBoolean(fPrev);
        Boolean value = toBoolean(fOverride);

        if (fVetoable)
            {
            if (!isModifiable())
                {
                readOnlyAttribute(ATTR_OVERRIDEBASE, prev, value);
                }

            fireVetoableChange(ATTR_OVERRIDEBASE, prev, value);
            }

        m_fOverrideBase = fOverride;
        firePropertyChange(ATTR_OVERRIDEBASE, prev, value);
        }


    // ----- synthetic

    /**
    * Determine whether this behavior was based on a compiler generated method
    * (({@link Method#isBridge()} && {@link Method#isSynthetic()}).
    *
    * @return whether this behavior was based on a compiler generated method
    */
    public boolean isSynthetic()
        {
        return (m_nFlags & MISC_ISINTERFACE) != 0;
        }


    // ----- helpers

    /**
    * Determine if the signature is reserved and cannot be used by this
    * Behavior.
    *
    * @param sSig  the proposed signature
    *
    * @return true if the signature cannot be used by this Behavior
    */
    protected boolean isSignatureReserved(String sSig)
        {
        // check for conflict with other behavior names
        Behavior behavior = getComponent().getBehavior(sSig);
        if (behavior != null && behavior != this)
            {
            return true;
            }

        // verify that the signature is not reserved
        return isSignatureReserved(getComponent(), sSig);
        }

    /**
    * Determine if the specified signature is reserved within the
    * specified Component.
    *
    * @param cd    the Component
    * @param sSig  the proposed signature
    *
    * @return true if the signature cannot be used within the specified Component
    */
    protected static boolean isSignatureReserved(Component cd, String sSig)
        {
        // gg 2001.5.3 -- no restrictions for a JCS
        if (cd.isSignature())
            {
            return false;
            }

        // compiler reserves double-underscore names
        if (sSig.startsWith("__"))
            {
            return true;
            }

        // check Property accessors
        String sProp = Property.getPropertyName(sSig);
        if (sProp != null)
            {
            Property prop = cd.getProperty(sProp);
            if (prop != null)
                {
                if (prop.isReservedBehaviorSignature(sSig))
                    {
                    if (!sProp.equals(prop.getName()))
                        {
                        // case sensitivity conflict
                        return false;
                        }

                    // accessors from super levels could be private ... in
                    // which case the signature cannot be created at this
                    // level
                    if (prop.isFromSuper())
                        {
                        return false;
                        }

                    // the Behavior cannot be created if the accessor is not
                    // applicable
                    for (int i = Property.PA_FIRST; i <= Property.PA_LAST; ++i)
                        {
                        if (sSig.equals(prop.getAccessorSignature(i))
                            && !prop.isAccessorApplicable(i))
                            {
                            return true;
                            }
                        }
                    }
                }
            }

        return false;
        }

    /**
    * Parse the behavior signature into discrete return and parameter data
    * types.
    *
    * @param sSig the behavior signature
    *
    * @return an array of DataType instances, where [0] is the return
    *         type and [1]..[c] are the parameter types.
    */
    public static DataType[] getParameterTypes(String sSig)
        {
        return DataType.parseSignature(sSig);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare this Behavior to another Object for equality.
    *
    * @param obj  the other Object to compare to this
    *
    * @return true if this Behavior equals that Object
    */
    public boolean equals(Object obj)
        {
        if (obj instanceof Behavior)
            {
            Behavior that = (Behavior) obj;
            return this                     == that
                || this.m_nFlags            == that.m_nFlags
                && this.m_nPrevFlags        == that.m_nPrevFlags
                && this.m_fOverrideBase     == that.m_fOverrideBase
                && this.m_cBaseLevelImpl    == that.m_cBaseLevelImpl
                && this.m_sName     .equals(that.m_sName     )
                && this.m_sSig      .equals(that.m_sSig      )
                && this.m_retval    .equals(that.m_retval    )
                && this.m_tblExcept .equals(that.m_tblExcept )
                && this.m_vectParam .equals(that.m_vectParam )
                && this.m_vectScript.equals(that.m_vectScript)
                && super.equals(that);

            // PJM Removed the following because they are not persistent fields
            //&& this.m_fPrevOverrideBase == that.m_fPrevOverrideBase
            }
        return false;
        }

    /**
    * Provide a human-readable description of the behavior.
    *
    * @return a string describing the behavior
    */
    public String toString()
        {
        StringBuffer sb = new StringBuffer();

        if (m_retval != null && !isConstructor())
            {
            sb.append(m_retval.getDataType().toString())
              .append(' ');
            }

        sb.append(m_sName)
          .append('(');

        int c = getParameterCount();
        for (int i = 0; i < c; ++i)
            {
            if (i > 0)
                {
                sb.append(", ");
                }
            Parameter parameter = getParameter(i);
            sb.append(parameter.getDataType().toString());
            sb.append(" " + parameter.getName());
            }

        sb.append(')');

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
        boolean fDerOrMod = (getMode() == MODIFICATION || getMode() == DERIVATION);

        out.println(sIndent + "Behavior " + toString());
        out.println(sIndent + "Signature=\"" + m_sSig + '\"');

        super.dump(out, sIndent);

        // Exists, Access, Visible, Synchronized, Abstract, Static, Final,
        // Deprecated, Remotable
        out.print  (sIndent + "flags=0x" + Integer.toHexString(m_nFlags));
        out.println(" (" + flagsDescription(m_nFlags, SPECABLE_FLAGS, fDerOrMod) + ')');
        out.print  (sIndent + "previous flags=0x" + Integer.toHexString(m_nPrevFlags));
        out.println(" (" + flagsDescription(m_nPrevFlags, SPECABLE_FLAGS, fDerOrMod) + ')');

        // return value
        out.print(sIndent + "Return Value:");
        if (m_retval == null)
            {
            out.println("  <null>");
            }
        else
            {
            out.println();
            m_retval.dump(out, nextIndent(sIndent));
            }

        // parameters
        out.println(sIndent + m_vectParam.size() + " parameter(s)");
        int i = 0;
        for (Enumeration enmr = m_vectParam.elements(); enmr.hasMoreElements(); )
            {
            Parameter param = (Parameter) enmr.nextElement();

            out.print(sIndent + "Parameter " + (++i) + ":");
            if (param == null)
                {
                out.println("  <null>");
                }
            else
                {
                out.println();
                param.dump(out, nextIndent(sIndent));
                }
            }

        // exceptions
        out.println(sIndent + m_tblExcept.getSize() + " exception(s)");
        for (Enumeration enmr = m_tblExcept.keys(); enmr.hasMoreElements(); )
            {
            String sExcept = (String) enmr.nextElement();
            Throwee except = (Throwee) m_tblExcept.get(sExcept);

            out.print(sIndent + "Exception \"" + sExcept + "\":");
            if (except == null)
                {
                out.println("  <null>");
                }
            else
                {
                out.println();
                except.dump(out, nextIndent(sIndent));
                }
            }

        // implementations
        out.print  (sIndent + m_vectScript.size() + " implementation(s), ");
        out.print  (m_cBaseLevelImpl + " at the base level (");
        out.println((m_fOverrideBase ? "overridden" : "supplemented") + ')');

        i = 0;
        for (Enumeration enmr = m_vectScript.elements(); enmr.hasMoreElements(); )
            {
            Implementation impl = (Implementation) enmr.nextElement();

            out.print(sIndent + "Implementation " + (++i) + ":");
            if (impl == null)
                {
                out.println("  <null>");
                }
            else
                {
                out.println();
                impl.dump(out, nextIndent(sIndent));
                }
            }
        }


    // ----- public constants  ----------------------------------------------

    /**
    * The FromManual attribute.
    */
    public static final String ATTR_FROMMANUAL      = "FromManual";

    /**
    * The Accessibility attribute.
    */
    public static final String ATTR_ACCESS          = "Access";

    /**
    * The Visible attribute.
    */
    public static final String ATTR_VISIBLE         = "Visible";

    /**
    * The Synchronized attribute.
    */
    public static final String ATTR_SYNCHRONIZED    = "Synchronized";

    /**
    * The Static attribute.
    */
    public static final String ATTR_STATIC          = "Static";

    /**
    * The Abstract attribute.
    */
    public static final String ATTR_ABSTRACT        = "Abstract";

    /**
    * The Final attribute.
    */
    public static final String ATTR_FINAL           = "Final";

    /**
    * The Deprecated attribute name.
    */
    public static final String ATTR_DEPRECATED      = "Deprecated";

    /**
    * The Remote attribute name.
    */
    public static final String ATTR_REMOTE          = "Remote";

    /**
    * The Name attribute.
    */
    public static final String ATTR_NAME            = "Name";

    /**
    * The Parameter attribute.
    */
    public static final String ATTR_PARAMETER       = "Parameter";

    /**
    * The Exception attribute.
    */
    public static final String ATTR_EXCEPTION       = "Exception";

    /**
    * The Implementation attribute.
    */
    public static final String ATTR_IMPLEMENTATION  = "Implementation";

    /**
    * The OverrideBase attribute.
    */
    public static final String ATTR_OVERRIDEBASE    = "OverrideBase";


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Behavior";

    /**
    * The Behavior's descriptor string.
    */
    protected static final String DESCRIPTOR = CLASS;

    /**
    * The default flags for the Behavior Trait.
    */
    private static final int DEFAULT_FLAGS  = VIS_VISIBLE      |
                                              SYNC_NOMONITOR   |
                                              IMPL_CONCRETE    |
                                              DERIVE_DERIVABLE |
                                              ANTIQ_CURRENT    |
                                              DIST_LOCAL;

    /**
    * The flags that the Behavior Trait can specify.
    */
    private static final int SPECABLE_FLAGS = EXISTS_SPECIFIED |
                                              ACCESS_SPECIFIED |
                                              VIS_SPECIFIED    |
                                              SYNC_SPECIFIED   |
                                              IMPL_SPECIFIED   |
                                              SCOPE_SPECIFIED  |
                                              DERIVE_SPECIFIED |
                                              ANTIQ_SPECIFIED  |
                                              DIST_SPECIFIED;

    /**
    * Flags that will cause a Behavior Trait to force compilation if modified.
    */
    private static final int CLASSGEN_FLAGS = ACCESS_MASK |
                                              VIS_MASK    |
                                              SYNC_MASK   |
                                              IMPL_MASK   |
                                              SCOPE_MASK  |
                                              DERIVE_MASK |
                                              ANTIQ_MASK  |
                                              DIST_MASK;

    /**
    * Parameter name for event listeners.
    */
    private static final String[] EVENT_REG_PARAM = new String[] {"l"};


    // ----- attributes

    /**
    * The behavior's name.
    */
    private String m_sName;

    /**
    * The behavior's signature (cached).
    */
    private String m_sSig;

    /**
    * Behavior attributes which are represented by bit flags, specifically
    * the Visible, Access, Synchronized, Static, Abstract, Final, Deprecated,
    * and Remotable attributes.
    *
    * @see com.tangosol.dev.component.Constants
    */
    private int m_nFlags = DEFAULT_FLAGS;

    /**
    * The legal ranges for this Behavior's attribute flags may be dependent
    * on the super (in the case of derivation) or base (in the case of
    * modification); those values are stored here for reference.
    */
    private int m_nPrevFlags = DEFAULT_FLAGS;

    /**
    * The behavior's return value.
    */
    private ReturnValue m_retval;

    /**
    * The behavior's parameters.
    */
    private Vector m_vectParam = new Vector();

    /**
    * The behavior's declared exceptions.
    */
    private StringTable m_tblExcept = new StringTable();

    /**
    * The behavior's implementation (scripts).  The order of the vector
    * reflects the order of "virtual method" execution in Java.  In other
    * words, the 0 element is executed, and if it super's, the 1 element
    * is executed, and so on.  The vector contains scripts only from this
    * derivation level (i.e. from this behavior and any modified base, but
    * not from a derived base aka "super").  This reflects .class generation
    * where a .class corresponds to a component, so all implementations at
    * a derivation level are incorporated into the .class.
    */
    private Vector m_vectScript = new Vector();

    /**
    * The number of implementations that do not belong to this behavior's
    * modification level.  These are the implementations which have null
    * modifications applied when the behavior is resolved.  They are
    * immutable at this level.  They can be "hidden" by setting the
    * override-base attribute of the behavior.
    */
    private int m_cBaseLevelImpl;

    /**
    * Does this behavior's modification level override (i.e. replace instead
    * of supplement) base implementations.  If true, the implementations from
    * modified base behaviors are not "reachable".
    */
    private boolean m_fOverrideBase;

    /**
    * The previous override of the implementations; this value is used only
    * during resolve processing.
    */
    private transient boolean m_fPrevOverrideBase;
    }
