/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import com.tangosol.dev.assembler.ClassFile;
import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Field;
import com.tangosol.dev.assembler.Method;

import com.tangosol.dev.compiler.java.ScriptParser;

import com.tangosol.java.type.ArrayType;
import com.tangosol.java.type.ClassType;
import com.tangosol.java.type.Type;

import com.tangosol.run.xml.SimpleDocument;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlSerializable;

import com.tangosol.util.CacheCollator;
import com.tangosol.util.ChainedEnumerator;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ErrorList;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.util.StringTable;
import com.tangosol.util.UID;

import java.beans.PropertyVetoException;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.text.Collator;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;


/**
* A Component Definition represents the set of information that describes a
* component, a complex property, or a Java Class Signature (JCS).
*
* @version 1.00, 09/01/97
* @author  Cameron Purdy
*/
public class Component
        extends    Trait
        implements Constants, Cloneable, XmlSerializable
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a Component.  This is the "default" constructor.
    *
    * @param parent  the Component or Property that contains this Component
    * @param nType   one of COMPONENT, COMPLEX, or SIGNATURE
    * @param sName   the unqualified name of this Component Definition
    */
    protected Component(Trait parent, int nType, String sName)
        {
        super(parent, RESOLVED);

        // validate type/name and determine read only status

        boolean fReadOnly = false;

        switch (nType)
            {
            case COMPONENT:
                // validate name
                if (sName == null || sName.length() == 0)
                    {
                    throw new IllegalArgumentException(CLASS + ":  "
                            + "Name required for Components!");
                    }
                if (!isSimpleNameLegal(sName, true))
                    {
                    throw new IllegalArgumentException(CLASS + ":  "
                            + "Illegal component name (" + sName + ")");
                    }
                break;

            case COMPLEX:
                // a complex name is the same as the super name, which is
                // the name of the component this is a complex property of
                // validate name
                if (sName == null || sName.length() == 0)
                    {
                    throw new IllegalArgumentException(CLASS + ":  "
                            + "Name required for Complex Components!");
                    }
                break;

            case SIGNATURE:
                // validate name - BLANK names are used to create an empty "root"
                // signature for resolving interfaces against
                if (sName != BLANK)
                    {
                    if (sName == null || sName.length() == 0)
                        {
                        throw new IllegalArgumentException(CLASS + ":  "
                                + "Name required for Signatures!");
                        }
                    if (!ClassHelper.isQualifiedNameLegal(sName))
                        {
                        throw new IllegalArgumentException(CLASS + ":  "
                                + "Illegal signature name (" + sName + ")");
                        }
                    }
                break;

            default:
                throw new IllegalArgumentException(CLASS + ":  "
                        + "Invalid component Type (" + nType + ")");
            }

        m_nType     = nType;
        m_fReadOnly = fReadOnly;
        m_sName     = sName;

        createTables();
        }

    /**
    * Construct a derived Component Definition.  This is not a constructor
    * but since it is public and creates a component it is placed here.
    *
    * @param sName the simple name of the derived Component
    */
    public Component createDerivedComponent(String sName, Loader loader)
        {
        Component cdSuper = this;

        // gg: 2001.5.17 allow JCS creation
        /*
        // validate type of component
        if (!cdSuper.isComponent())
            {
            throw new IllegalArgumentException(CLASS + ".createDerivedComponent:  "
                    + "Can only derive Components!");
            }
        */
        if (cdSuper.getParent() != null)
            {
            throw new IllegalArgumentException(CLASS + ".createDerivedComponent:  "
                    + "Super Component cannot be a child!");
            }

        // validate parameters
        if (sName == null || sName.length() == 0)
            {
            throw new IllegalArgumentException(CLASS + ".createDerivedComponent:  "
                    + "Component Definition name required!");
            }
        else if (cdSuper.isComponent() && !isSimpleNameLegal(sName, true) ||
                 cdSuper.isSignature() && !ClassHelper.isQualifiedNameLegal(sName))
            {
            throw new IllegalArgumentException(CLASS + ".createDerivedComponent:  "
                    + "Illegal Component Definition name! (" + sName + ")");
            }

        Component cdDelta = (Component) cdSuper.getNullDerivedTrait(null, DERIVATION);
        cdDelta.m_sSuper  = getQualifiedName();
        cdDelta.m_sName   = sName;

        try
            {
            ErrorList errlist = new ErrorList();
            Component cdSub   = cdSuper.resolve(cdDelta, loader, errlist);
            cdSub.finalizeResolve(loader, errlist);
            if (errlist.isSevere())
                {
                errlist.print();
                throw new ComponentException(CLASS + ".createDerivedComponent:  "
                        + "Serious errors occurred during resolution!");
                }

            cdSub.validate();
            return cdSub;
            }
        catch (ComponentException e)
            {
            // should not be possible to get a Component exception when
            // resolving a null derivation
            throw new RuntimeException(e.toString());
            }
        }

    /**
    * Construct a complex property using this component as the base.
    * This is not a constructor but since it is public and creates
    * a component it is placed here.
    *
    * @return  the complex property component
    */
    public Component createComplexProperty()
        {
        Component cdBase = this;

        // validate type of component
        if (!cdBase.isComponent())
            {
            throw new IllegalArgumentException(CLASS + ".createComplexProperty:  "
                    + "Can only use Components!");
            }

        if (cdBase.getParent() != null)
            {
            throw new IllegalArgumentException(CLASS + ".createComplexProperty:  "
                    + "Component cannot be a child!");
            }

        // create a child modification from the specified component
        Component cdDelta = (Component) cdBase.getNullDerivedTrait(null, MODIFICATION);
        cdDelta.m_nType   = COMPLEX;
        cdDelta.assignUID();

        // create the new complex property value
        try
            {
            Loader    loader  = new NullStorage();
            ErrorList errlist = new ErrorList();
            Component cdComplex = (Component) cdBase.resolve(cdDelta, null, loader, errlist);
            cdComplex.finalizeResolve(loader, errlist);
            if (errlist.isSevere())
                {
                errlist.print();
                throw new ComponentException(CLASS + ".createComplexProperty:  "
                        + "Serious errors occurred during resolution!");
                }
            cdComplex.validate();
            return cdComplex;
            }
        catch (ComponentException e)
            {
            // should not be possible to get a Component exception when
            // resolving a null derivation
            throw new RuntimeException(e.toString());
            }
        }

    /**
    * Construct a Java Class Signature (JCS) from a Java class using the
    * assembler ClassFile structure.
    *
    * @param clz  the ClassFile to create a signature from
    */
    public Component(ClassFile clz)
            throws ComponentException
        {
        this(clz, null);
        }

    /**
    * Construct a Java Class Signature (JCS) from a Java class using the
    * assembler ClassFile structure.
    *
    * @param clz      the ClassFile to create a signature from
    * @param sScript  the source code that created the ClassFile
    */
    public Component(ClassFile clz, String sScript)
            throws ComponentException
        {
        this(null, SIGNATURE, clz.getName().replace('/', '.'));

        // get attribute information
        int nFlags = EXISTS_INSERT | STG_PERSIST | DIST_LOCAL;
        switch (clz.getAccess())
            {
            case com.tangosol.dev.assembler.Constants.ACC_PUBLIC:
                nFlags |= ACCESS_PUBLIC    | VIS_VISIBLE;
                break;
            case com.tangosol.dev.assembler.Constants.ACC_PROTECTED:
                nFlags |= ACCESS_PROTECTED | VIS_HIDDEN;
                break;
            case com.tangosol.dev.assembler.Constants.ACC_PACKAGE:
                nFlags |= ACCESS_PACKAGE   | VIS_HIDDEN;
                break;
            case com.tangosol.dev.assembler.Constants.ACC_PRIVATE:
                nFlags |= ACCESS_PRIVATE   | VIS_HIDDEN;
                break;
            }
        nFlags |= (clz.isStatic()     ? SCOPE_STATIC     : SCOPE_INSTANCE  );
        nFlags |= (clz.isFinal()      ? DERIVE_FINAL     : DERIVE_DERIVABLE);
        nFlags |= (clz.isAbstract()   ? IMPL_ABSTRACT    : IMPL_CONCRETE   );
        nFlags |= (clz.isDeprecated() ? ANTIQ_DEPRECATED : ANTIQ_CURRENT   );

        // check if the signature represents a throwable class
        if (m_sName.equals("java.lang.Throwable"))
            {
            nFlags |= MISC_ISTHROWABLE;
            }

        // get the super class name
        String sSuper = clz.getSuper();
        if (sSuper == null || sSuper.length() == 0)
            {
            sSuper = BLANK;
            }
        else
            {
            sSuper = sSuper.replace('/', '.');
            }

        // check if the signature is an interface
        if (clz.isInterface())
            {
            nFlags |= MISC_ISINTERFACE;
            // make sure that it's super is java.lang.Object
            if (!sSuper.equals("java.lang.Object"))
                {
                throw new IllegalArgumentException(CLASS + ":  "
                        + "Illegal interface class file (" + m_sName + ") does not have a super java.lang.Object");
                }
            sSuper = BLANK;
            }
        m_sSuper = sSuper;

        // iterate through the class's interfaces
        for (Enumeration enmr = clz.getImplements(); enmr.hasMoreElements(); )
            {
            String sIface = ((String) enmr.nextElement()).replace('/', '.');
            Interface iface = new Interface(this, sIface);
            m_tblImplements.put(sIface, iface);
            }

        // "reflect" the ClassFile fields
        StringTable tblFields = m_tblState;
        for (Enumeration enmr = clz.getFields(); enmr.hasMoreElements(); )
            {
            Field field = (Field) enmr.nextElement();

            if (!field.isSynthetic() && (field.isPublic() || field.isProtected()))
                {
                Property prop = new Property(this, field);
                prop.setExists(EXISTS_INSERT);
                tblFields.put(prop.getUniqueName(), prop);
                }
            }

        // 2002-07-22 cp - parse the parameter names if available
        Map mapParam = new HashMap();
        if (sScript != null && sScript.length() > 0)
            {
            ScriptParser parser = new ScriptParser();
            try
                {
                parser.parse(sScript, mapParam);
                }
            catch (Exception e)
                {
                }
            }

        // "reflect" the ClassFile methods
        StringTable tblMethods = m_tblBehavior;
        for (Enumeration enmr = clz.getMethods(); enmr.hasMoreElements(); )
            {
            Method method = (Method) enmr.nextElement();

            // only show public methods on interfaces
            if (method.isPublic() || (!isInterface() && method.isProtected()))
                {
                Behavior beh = new Behavior(this, method, mapParam);
                String sSig = beh.getUniqueName();

                // we won't know whether this is an insert or update till the
                // resolution time (see Behavior#resolve())
                beh.setExists(EXISTS_INSERT);

                Behavior behPrev = (Behavior) tblMethods.get(sSig);
                if (behPrev == null || behPrev.isSynthetic() && !beh.isSynthetic())
                    {
                    tblMethods.put(sSig, beh);
                    }
                // else; compiler generated methods can be discarded
                }
            }

        // store ClassFile attributes
        m_nFlags = nFlags;

        // adjust our type if we need to be resolved when loaded
        // The only resolved signature is "java.lang.Object"
        if (m_sSuper != BLANK || clz.isInterface())
            {
            setMode(DERIVATION);
            }
        }

    /**
    * Construct a blank Component Trait.
    *
    * @param base    the base Component to derive from
    * @param parent  the containing Component or Property
    * @param nMode   one of RESOLVED, DERIVATION, MODIFICATION
    *
    * @see #getBlankDerivedTrait(Trait, int)
    */
    protected Component(Component base, Trait parent, int nMode)
        {
        super(base, parent, nMode);

        this.m_nType  = base.m_nType;
        this.m_sName  = base.m_sName;
        this.m_sSuper = base.m_sSuper;

        createTables();
        }

    /**
    * Copy constructor.
    *
    * @param parent  the Component or Property that will contain the new
    *                Component
    * @param that    the trait to copy from
    */
    protected Component(Trait parent, Component that)
        {
        super(parent, that);

        // copy intrinsic/immutable attributes
        this.m_nType      = that.m_nType;
        this.m_fReadOnly  = that.m_fReadOnly;
        this.m_sName      = that.m_sName;
        this.m_sSuper     = that.m_sSuper;
        this.m_nFlags     = that.m_nFlags;
        this.m_nPrevFlags = that.m_nPrevFlags;
        this.m_fBaseLevel = that.m_fBaseLevel;

        // integration
        if (that.m_integration != null)
            {
            this.m_integration = new Integration(this, that.m_integration);
            }

        createTables();
        StringTable tblThat;
        StringTable tblThis;

        // implements
        tblThat = that.m_tblImplements;
        if (!tblThat.isEmpty())
            {
            tblThis = this.m_tblImplements;
            for (Enumeration enmr = tblThat.elements(); enmr.hasMoreElements(); )
                {
                Interface iface = (Interface) enmr.nextElement();
                tblThis.put(iface.getUniqueName(), new Interface(this, iface));
                }
            }

        // dispatches
        tblThat = that.m_tblDispatches;
        if (!tblThat.isEmpty())
            {
            tblThis = this.m_tblDispatches;
            for (Enumeration enmr = tblThat.elements(); enmr.hasMoreElements(); )
                {
                Interface iface = (Interface) enmr.nextElement();
                tblThis.put(iface.getUniqueName(), new Interface(this, iface));
                }
            }

        // Properties
        tblThat = that.m_tblState;
        if (!tblThat.isEmpty())
            {
            tblThis = this.m_tblState;
            for (Enumeration enmr = tblThat.elements(); enmr.hasMoreElements(); )
                {
                Property prop = (Property) enmr.nextElement();
                tblThis.put(prop.getUniqueName(), new Property(this, prop));
                }
            }

        // Behaviors
        tblThat = that.m_tblBehavior;
        if (!tblThat.isEmpty())
            {
            tblThis = this.m_tblBehavior;
            for (Enumeration enmr = tblThat.elements(); enmr.hasMoreElements(); )
                {
                Behavior behavior = (Behavior) enmr.nextElement();
                tblThis.put(behavior.getUniqueName(), new Behavior(this, behavior));
                }
            }

        // aggregation categories
        tblThat = that.m_tblCategories;
        if (!tblThat.isEmpty())
            {
            this.m_tblCategories = (StringTable) tblThat.clone();
            }

        // Children
        tblThat = that.m_tblChildren;
        if (!tblThat.isEmpty())
            {
            tblThis = this.m_tblChildren;
            for (Enumeration enmr = tblThat.elements(); enmr.hasMoreElements(); )
                {
                Component cd = (Component) enmr.nextElement();
                tblThis.put(cd.getUniqueName(), new Component(this, cd));
                }
            }
        }

    /**
    * Construct the component from a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param stream   the stream to read this Component Definition from
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            reading the Component Definition from the stream or if the
    *            stream does not contain a valid Component Definition
    */
    public Component(DataInput stream)
            throws IOException
        {
        this(null, stream, getVersion(stream));
        validate();
        }

    /**
    * Construct the component from a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param traitParent the containing Component Definition (if this CD is
    *                    aggregated) or Property (if this Component
    *                    Definition is used to store a complex property)
    * @param stream      the stream to read this Component Definition from
    * @param nVersion    version of the data structure in the stream
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            reading the Component Definition from the stream or if the
    *            stream does not contain a valid Component Definition.
    */
    protected Component(Trait traitParent, DataInput stream, int nVersion)
            throws IOException
        {
        super(traitParent, stream, nVersion);

        // version
        m_nVersion   = nVersion;

        // identity
        m_nType      = stream.readUnsignedByte();
        m_sName      = readString(stream);
        m_sSuper     = readString(stream);
        m_nFlags     = stream.readInt();
        m_nPrevFlags = stream.readInt();
        m_fBaseLevel = stream.readBoolean();

        createTables();

        // integration
        if (stream.readBoolean())
            {
            m_integration = new Integration(this, stream, nVersion);
            }

        // implements
        StringTable tbl    = m_tblImplements;
        int         cItems = stream.readInt();
        for (int i = 0; i < cItems; ++i)
            {
            Interface iface = new Interface(this, stream, nVersion);
            tbl.put(iface.getUniqueName(), iface);
            }

        // dispatches
        tbl    = m_tblDispatches;
        cItems = stream.readInt();
        for (int i = 0; i < cItems; ++i)
            {
            Interface iface = new Interface(this, stream, nVersion);
            tbl.put(iface.getUniqueName(), iface);
            }

        // state
        tbl    = m_tblState;
        cItems = stream.readInt();
        for (int i = 0; i < cItems; ++i)
            {
            Property prop = new Property(this, stream, nVersion);
            tbl.put(prop.getUniqueName(), prop);
            }

        // behavior
        tbl    = m_tblBehavior;
        cItems = stream.readInt();
        for (int i = 0; i < cItems; ++i)
            {
            Behavior behavior = new Behavior(this, stream, nVersion);
            tbl.put(behavior.getUniqueName(), behavior);
            }

        // aggregation
        tbl    = m_tblCategories;
        cItems = stream.readInt();
        for (int i = 0; i < cItems; ++i)
            {
            tbl.put(stream.readUTF(), toBoolean(stream.readBoolean()));
            }

        // children
        tbl    = m_tblChildren;
        cItems = stream.readInt();
        for (int i = 0; i < cItems; ++i)
            {
            Component cd = new Component(this, stream, nVersion);
            tbl.put(cd.getUniqueName(), cd);
            }
        }

    /**
    * Construct the component from an XmlDocument.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param xml  the XmlDocument to read this Component Definition from
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            reading the Component Definition from the XML or if the
    *            XML does not contain a valid Component Definition
    */
    public Component(XmlDocument xml)
            throws IOException
        {
        this(null, xml, getVersion(xml));
        validate();
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
    protected Component(Trait parent, XmlElement xml, int nVersion)
            throws IOException
        {
        super(parent, xml, nVersion);

        // version
        m_nVersion   = nVersion;

        // identity
        String sName = readString(xml.getElement("name"));
        String sType = readString(xml.getElement("type"));
        if (sName == BLANK || sType == BLANK)
            {
            throw new IOException("name or type is missing");
            }

        m_sName = sName;
        m_nType = -1;
        for (int i = 0, c = DESCRIPTORS.length; i < c; ++i)
            {
            if (DESCRIPTORS[i].equalsIgnoreCase(sType))
                {
                m_nType = i;
                break;
                }
            }
        if (m_nType < 0)
            {
            throw new IOException("illegal type: " + sType);
            }

        m_sSuper     = readString(xml.getElement("super"));
        m_nFlags     = readFlags(xml, "flags", 0);
        m_nPrevFlags = readFlags(xml, "prev-flags", 0);
        m_fBaseLevel = readBoolean(xml.getElement("base-level"));

        createTables();

        // integration
        XmlElement xmlIntegration = xml.getElement("integration");
        if (xmlIntegration != null)
            {
            m_integration = new Integration(this, xmlIntegration, nVersion);
            }

        // implements
        XmlElement xmlImplements = xml.getElement("implements");
        if (xmlImplements != null)
            {
            StringTable tbl = m_tblImplements;
            for (Iterator iter = xmlImplements.getElements("interface"); iter.hasNext(); )
                {
                XmlElement xmlIFace = (XmlElement) iter.next();
                Interface  iface    = new Interface(this, xmlIFace, nVersion);
                tbl.put(iface.getUniqueName(), iface);
                }
            }

        // dispatches
        XmlElement xmlDispatches = xml.getElement("dispatches");
        if (xmlDispatches != null)
            {
            StringTable tbl = m_tblDispatches;
            for (Iterator iter = xmlDispatches.getElements("interface"); iter.hasNext(); )
                {
                XmlElement xmlIFace = (XmlElement) iter.next();
                Interface  iface    = new Interface(this, xmlIFace, nVersion);
                tbl.put(iface.getUniqueName(), iface);
                }
            }

        // state
        XmlElement xmlProps = xml.getElement("properties");
        if (xmlProps != null)
            {
            StringTable tbl = m_tblState;
            for (Iterator iter = xmlProps.getElements("property"); iter.hasNext(); )
                {
                XmlElement xmlProp = (XmlElement) iter.next();
                Property   prop    = new Property(this, xmlProp, nVersion);
                tbl.put(prop.getUniqueName(), prop);
                }
            }

        // behavior
        XmlElement xmlBehaviors = xml.getElement("behaviors");
        if (xmlBehaviors != null)
            {
            StringTable tbl = m_tblBehavior;
            for (Iterator iter = xmlBehaviors.getElements("behavior"); iter.hasNext(); )
                {
                XmlElement xmlBehavior = (XmlElement) iter.next();
                Behavior behavior = new Behavior(this, xmlBehavior, nVersion);
                tbl.put(behavior.getUniqueName(), behavior);
                }
            }

        // aggregation
        XmlElement xmlCategories = xml.getElement("categories");
        if (xmlCategories != null)
            {
            StringTable tbl = m_tblCategories;
            for (Iterator iter = xmlCategories.getElements("category"); iter.hasNext(); )
                {
                XmlElement xmlCat   = (XmlElement) iter.next();
                String     sCatName = readString(xmlCat.getElement("name"));
                boolean    fCatDecl = readBoolean(xmlCat.getElement("declared"));
                if (sCatName == BLANK)
                    {
                    throw new IOException("category name is missing");
                    }

                tbl.put(sCatName, toBoolean(fCatDecl));
                }
            }

        // children
        XmlElement xmlChildren = xml.getElement("children");
        if (xmlChildren != null)
            {
            StringTable tbl = m_tblChildren;
            for (Iterator iter = xmlChildren.getElements("child"); iter.hasNext(); )
                {
                XmlElement xmlChild = (XmlElement) iter.next();
                Component  cd       = new Component(this, xmlChild, nVersion);
                tbl.put(cd.getUniqueName(), cd);
                }
            }
        }

    /**
    * Create the tables of sub-trait information based on the type of the
    * Component.
    */
    private void createTables()
        {
        m_tblImplements = new StringTable();
        m_tblDispatches = new StringTable();
        m_tblCategories = new StringTable();

        if (m_nType == SIGNATURE)
            {
            m_tblState      = new StringTable();
            m_tblBehavior   = new StringTable();
            m_tblChildren   = new StringTable();
            }
        else
            {
            m_tblState      = new StringTable(INSENS);
            m_tblBehavior   = new StringTable(INSENS);
            m_tblChildren   = new StringTable(INSENS);
            }
        }


    // ----- persistence ----------------------------------------------------

    /**
    * Save the Component Definition to a stream.  This method is responsible
    * for recursively saving the Component Definition and its aggregated
    * Component Definitions.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    */
    public synchronized void save(DataOutput stream)
            throws IOException
        {
        // write the header
        Trait parent = getParentTrait();
        if (parent == null)
            {
            stream.writeInt(MAGIC);
            stream.writeInt(VERSION);
            }

        // save the trait information
        super.save(stream);

        // write the identity
        stream.writeByte(m_nType);
        stream.writeUTF(m_sName);
        stream.writeUTF(m_sSuper);
        stream.writeInt(m_nFlags);
        stream.writeInt(m_nPrevFlags);
        stream.writeBoolean(m_fBaseLevel);

        // integration
        Integration integration = m_integration;
        boolean     fExists     = (integration != null);
        stream.writeBoolean(fExists);
        if (fExists)
            {
            integration.save(stream);
            }

        // implements
        StringTable tbl = m_tblImplements;
        stream.writeInt(tbl.getSize());
        for (Enumeration enmr = tbl.elements(); enmr.hasMoreElements(); )
            {
            ((Interface) enmr.nextElement()).save(stream);
            }

        // dispatches
        tbl = m_tblDispatches;
        stream.writeInt(tbl.getSize());
        for (Enumeration enmr = tbl.elements(); enmr.hasMoreElements(); )
            {
            ((Interface) enmr.nextElement()).save(stream);
            }

        // state
        tbl = m_tblState;
        stream.writeInt(tbl.getSize());
        for (Enumeration enmr = tbl.elements(); enmr.hasMoreElements(); )
            {
            ((Property) enmr.nextElement()).save(stream);
            }

        // behavior
        tbl = m_tblBehavior;
        stream.writeInt(tbl.getSize());
        for (Enumeration enmr = tbl.elements(); enmr.hasMoreElements(); )
            {
            ((Behavior) enmr.nextElement()).save(stream);
            }

        // aggregation
        tbl = m_tblCategories;
        stream.writeInt(tbl.getSize());
        for (Enumeration enmr = tbl.keys(); enmr.hasMoreElements(); )
            {
            String  sCategory  = (String) enmr.nextElement();
            boolean fThisLevel = ((Boolean) tbl.get(sCategory)).booleanValue();
            stream.writeUTF(sCategory);
            stream.writeBoolean(fThisLevel);
            }

        // children
        tbl = m_tblChildren;
        stream.writeInt(tbl.getSize());
        for (Enumeration enmr = tbl.elements(); enmr.hasMoreElements(); )
            {
            ((Component) enmr.nextElement()).save(stream);
            }
        }

    /**
    * Save the Component Definition to a stream.  This method is responsible
    * for recursively saving the Component Definition and its aggregated
    * Component Definitions.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    */
    public synchronized void save(XmlElement xml)
            throws IOException
        {
        // write the identity
        xml.addElement("type").setString(DESCRIPTORS[m_nType]);
        xml.addElement("name").setString(m_sName);
        xml.addElement("super").setString(m_sSuper);

        // save the trait information
        super.save(xml);

        saveFlags(xml, "flags", m_nFlags, 0);
        saveFlags(xml, "prev-flags", m_nPrevFlags, 0);

        if (isComponent())
            {
            xml.addElement("base-level").setBoolean(m_fBaseLevel);
            }

        // integration
        Integration integration = m_integration;
        if (integration != null)
            {
            integration.save(xml.addElement("integration"));
            }

        // implements
        saveTable(xml, m_tblImplements, "implements", "interface");

        // dispatches
        saveTable(xml, m_tblDispatches, "dispatches", "interface");

        // state
        saveTable(xml, m_tblState, "properties", "property");

        // behavior
        saveTable(xml, m_tblBehavior, "behaviors", "behavior");

        // aggregation
        StringTable tblCats = m_tblCategories;
        if (!tblCats.isEmpty())
            {
            XmlElement xmlCats = xml.addElement("categories");
            for (Enumeration enmr = tblCats.keys(); enmr.hasMoreElements(); )
                {
                String  sCategory  = (String) enmr.nextElement();
                boolean fThisLevel = ((Boolean) tblCats.get(sCategory)).booleanValue();
                XmlElement xmlCat = xmlCats.addElement("category");
                xmlCat.addElement("name").setString(sCategory);
                if (fThisLevel)
                    {
                    xmlCat.addElement("declared").setBoolean(true);
                    }
                }
            }

        // children
        saveTable(xml, m_tblChildren, "children", "child");
        }

    /**
    * Read the header of the Component Definition, extracting the version.
    *
    * @param stream
    *
    * @return version of the Component Definition in the stream
    *
    * @exception IOException if the stream does not contain a Component
    *            Definition, if the Component Definition version is not
    *            supported, or if there is any other error reading from
    *            the stream
    */
    protected static int getVersion(DataInput stream)
            throws IOException
        {
        // read header
        if (stream.readInt() != MAGIC)
            {
            throw new IOException(CLASS + ".getVersion:  "
                    + "Stream does not contain a Component Definition");
            }

        // read version
        int nVersion = stream.readInt();
        switch (nVersion)
            {
            case VERSION:
                break;
            default:
                throw new IOException(CLASS + ".getVersion:  "
                    + "Stream contains an unsupported version of a Component Definition");
            }

        return nVersion;
        }

    /**
    * Get the version of the Component Definition from the passed
    *  XmlDocument.
    *
    * @param xml  the XmlDocument to extract the version from
    *
    * @return version of the Component Definition in the stream
    *
    * @exception IOException if the stream does not contain a Component
    *            Definition, if the Component Definition version is not
    *            supported, or if there is any other error reading from
    *            the stream
    */
    protected static int getVersion(XmlDocument xml)
            throws IOException
        {
        int nVersion = xml.getSafeElement("version").getInt();
        switch (nVersion)
            {
            case 0:
                throw new IOException(CLASS + ".getVersion:  "
                    + "XmlDocument does not contain a valid Component Definition");

            case VERSION:
                break;

            default:
                throw new IOException(CLASS + ".getVersion:  "
                    + "XmlDocument contains an unsupported version of a Component Definition");
            }

        return nVersion;
        }


    // ----- XmlSerializable interface --------------------------------------

    /**
    * Serialize the object into an XmlElement.
    *
    * @return an XmlElement that contains the serialized form of the object
    */
    public XmlElement toXml()
        {
        XmlDocument xml = new SimpleDocument("component");

        // write the header
        Trait parent = getParentTrait();
        if (parent == null)
            {
            xml.addElement("version").setInt(VERSION);
            }

        try
            {
            save(xml);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }

        return xml;
        }

    /**
    * Deserialize the object from an XmlElement.
    *
    * This method can throw one of several RuntimeExceptions.
    *
    * @param xml  an XmlElement that contains the serialized form of the
    *             object
    *
    * @throws UnsupportedOperationException
    * @throws IllegalStateException
    * @throws IllegalArgumentException
    */
    public void fromXml(XmlElement xml)
        {
        throw new UnsupportedOperationException();
        }


    // ----- derivation/modification ----------------------------------------

    /**
    * Get the super of the root.  Assumptions are:
    *   (1) There are methods
    *   (2) There are no fields
    *   (3) There are no interfaces
    *
    * @param loader
    *
    * @return the super of the root component
    */
    public static Component getRootSuper(Loader loader)
            throws ComponentException
        {
        Component cdSuper = new Component(null, COMPONENT, getRootName());
        cdSuper.m_sName     = BLANK;
        cdSuper.m_fReadOnly = true;

        // the root component inherits methods from java/lang/Object;
        Component   cdJCS  = loader.loadSignature(DataType.OBJECT.getClassName());
        StringTable tblJCS = cdJCS  .m_tblBehavior;
        StringTable tblCD  = cdSuper.m_tblBehavior;
        for (Enumeration enmr = tblJCS.keys(); enmr.hasMoreElements(); )
            {
            String sSig = (String) enmr.nextElement();
            if (Behavior.isConstructor(sSig))
                {
                continue;
                }

            Behavior behJCS = (Behavior) tblJCS.get(sSig);
            int nAccess = behJCS.getAccess();
            if (nAccess != ACCESS_PUBLIC && nAccess != ACCESS_PROTECTED)
                {
                continue;
                }

            Behavior behCD = new Behavior(cdSuper, behJCS.getReturnValue().getDataType(),
                    behJCS.getName(), behJCS.isStatic(), nAccess, behJCS.getParameterTypes(),
                    behJCS.getParameterNames(), null);
            try
                {
                behCD.setFinal(behJCS.isFinal(), false);
                behCD.setVisible(VIS_ADVANCED  , false);
                behCD.assignUID();
                }
            catch (PropertyVetoException e)
                {
                throw new IllegalStateException();
                }

            tblCD.put(sSig, behCD);
            }

        cdSuper.validate();
        return cdSuper;
        }

    /**
    * Construct a blank Component from this base.
    *
    * @param parent  the containing Component or Property
    * @param nMode   RESOLVED, DERIVATION or MODIFICATION
    *
    * @return a new blank derived trait of this trait's class with the
    *         specified parent and mode
    */
    protected Trait getBlankDerivedTrait(Trait parent, int nMode)
        {
        return new Component(this, parent, nMode);
        }

    /**
    * Construct a null derivation or modification Trait.  This method is
    * overridden by traits that have a different null derivation/modification
    * from their blank derived trait.  The "this" parameter is the base
    * trait.
    *
    * A null trait derivation or modification is an "r-value" for resolve
    * processing; a null derivation or modification is used when the delta
    * to apply is not present.
    *
    * @param parent  the containing trait or null if the null trait is
    *                not going to be contained by another trait
    * @param nMode   one of DERIVATION or MODIFICATION
    *
    * @return a new null derivation or modification trait of this trait's
    *         class with the specified parent and mode
    */
    protected Trait getNullDerivedTrait(Trait parent, int nMode)
        {
        Component cd = (Component) super.getNullDerivedTrait(parent, nMode);

        // default to update
        cd.setExists(EXISTS_UPDATE);

        // base level is true for derivations, false for modifications
        cd.m_fBaseLevel = (nMode != MODIFICATION);

        return cd;
        }

    /**
    * Apply the Component derivation or modification to this base, resulting
    * in a new Component.
    *
    * @param cdDelta  the Component derivation or modification
    * @param loader   the Loader object for JCS, CIM, and CD dependencies
    * @param errlist  the error list object to log error information to
    *
    * @return the resulting Component
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    public Component resolve(Component cdDelta, Loader loader, ErrorList errlist)
            throws ComponentException
        {
        // validate base and delta component definitions
        if (cdDelta == null)
            {
            throw new IllegalArgumentException(CLASS + ".resolve:  "
                + "Delta Component required!");
            }

        if (m_nType != COMPONENT && m_nType != SIGNATURE)
            {
            throw new IllegalArgumentException(CLASS + ".resolve:  "
                + "Base is not a Component or Java Class Signature!");
            }
        if (this.m_nType != cdDelta.m_nType)
            {
            throw new IllegalArgumentException(CLASS + ".resolve:  "
                + "Base type (" + this.m_nType + ") != Delta type (" + cdDelta.m_nType + ")");
            }

        return (Component) resolve(cdDelta, null, loader, errlist);
        }

    /**
    * Create a derived/modified Trait by combining the information from this
    * Trait with the super or base level's Trait information.  Neither the
    * base ("this") nor the delta may be modified in any way.
    *
    * The delta Component is one of the following:
    * <pre>
    *   1)  Component Derivation - This Component Definition contains the
    *       information to create a new Component from the passed base
    *       Component
    *   2)  Component Modification - This Component Definition contains
    *       information which will alter the passed base Component but
    *       will not result in a new (i.e. different identity) Component
    *   3)  Complex Property Value - This is a stripped-down Component
    *       Modification containing only state (Property) and minimal
    *       identity information
    *
    * The following table illustrates the state changes given the modes of
    * the two inputs (this and base):
    *
    *   base            delta           result
    *   --------------  --------------  -------------
    *   resolved        derivation      resolved
    *   resolved        modification    resolved
    *   derivation      modification    derivation
    *   modification    modification    modification
    *
    * In the above table, any input combination not listed is illegal (and
    * resolved by Trait).  The mode of the base always determines the mode
    * of the result.  This explains why the root Component must be stored
    * in its resolved state, since otherwise it would be impossible to
    * resolve any Component.
    *
    * The resolve method is driven externally by the loader mechanism.
    * It is legal only to resolve a global Component; local (child)
    * Components and complex Property values are automatically resolved
    * recursively by the global Component.
    *
    * There are four fundamental areas of information that are resolved by
    * the Component:
    *
    *   1)  Identity - includes the name of the Component, the interfaces
    *       dispatched and implemented by the Component, the map integrated
    *       by the Component, and other various Component attributes
    *   2)  State - the Properties of the Component
    *   3)  Behavior - the methods and events of the Component; this is the
    *       most dependent section in the Component, since Behaviors can be
    *       the result of integration, implemented/dispatched interfaces,
    *       Property accessors, base/super level Components, and manual
    *       addition
    *   4)  Aggregation - the child Components of this Component
    * </pre>
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
        Component base    = this;
        Component delta   = (Component) resolveDelta(traitDelta, loader, errlist);
        Component derived = (Component) super.resolve(delta, parent, loader, errlist);

        // for general use below
        boolean fComponent = (delta.m_nType == COMPONENT);
        boolean fSignature = (delta.m_nType == SIGNATURE);

        // a complex Property value is implemented using the Component
        // Definition data structure; only some sections of the Component
        // Definition are applicable for a complex Property value
        boolean fComplex     = (delta.m_nType == COMPLEX);
        boolean fBaseComplex = (base .m_nType == COMPLEX);

        // resolve mode
        int     nBaseMode     = base.getMode();
        boolean fBaseResolved = (nBaseMode == RESOLVED);
        int     nDeltaMode    = delta.getMode();
        if (nDeltaMode == RESOLVED)
            {
            // this can happen when the Component is an INSERT with the
            // same name as a Component later added to the base
            nDeltaMode = delta.getExtractMode(base);
            }
        int     nDerivedMode  = derived.getMode();

        // ----- identity

        if (fComplex)
            {
            if (fBaseComplex)
                {
                derived.m_sSuper     = base.m_sSuper;
                derived.m_sName      = base.m_sName;
                derived.m_nFlags     = EXISTS_UPDATE;
                derived.m_nPrevFlags = base.m_nFlags;
                }
            else
                {
                derived.m_nType      = COMPLEX;
                derived.m_sSuper     = base.getQualifiedName();
                derived.m_sName      = derived.m_sSuper;
                derived.m_nFlags     = EXISTS_INSERT;
                }
            }
        else
            {
            // attributes represented as bit flags
            int nBaseFlags    = base.m_nFlags;
            int nDeltaFlags   = delta.m_nFlags;
            int nPrevFlags    = derived.resolveFlags(nBaseFlags, delta.m_nPrevFlags);
            int nDerivedFlags = derived.resolveFlags(nPrevFlags, nDeltaFlags);

            // resolve exists
            int nExists = EXISTS_UPDATE;
            if (nDeltaMode == DERIVATION)
                {
                // an insert component exists when the base is global and the
                // delta is a child
                if (base.getParentTrait() == null && delta.getParentTrait() != null)
                    {
                    nExists = EXISTS_INSERT;
                    }
                else if ((nDeltaFlags & EXISTS_MASK) == EXISTS_DELETE)
                    {
                    nExists = EXISTS_DELETE;
                    }
                }
            else
                {
                // a modification of an insert remains an insert, likewise
                // for deletes
                int nBaseExists = nBaseFlags & EXISTS_MASK;
                if (nBaseExists == EXISTS_INSERT || nBaseExists == EXISTS_DELETE)
                    {
                    nExists = nBaseExists;
                    }
                }
            nDerivedFlags = nDerivedFlags & ~EXISTS_FULLMASK | nExists;

            // resolve identity
            if (nDeltaMode == DERIVATION)
                {
                // super and name are copied from base for child updates
                // otherwise the base is the super for global components and
                // children added at this level
                if (parent != null && nExists == EXISTS_UPDATE)
                    {
                    derived.m_sSuper = base.m_sSuper;
                    derived.m_sName  = base.m_sName;
                    }
                else
                    {
                    derived.m_sSuper = base.getQualifiedName();
                    derived.m_sName  = delta.m_sName;
                    }

                derived.m_fBaseLevel = delta.m_fBaseLevel;
                }
            else
                {
                // modification keeps the Component identity from the base
                derived.m_sSuper = base.m_sSuper;
                derived.m_sName  = base.m_sName;

                // modification results in a Component not at the base level
                derived.m_fBaseLevel = false;
                }

            // store flags
            derived.m_nPrevFlags = nPrevFlags;
            derived.m_nFlags     = nDerivedFlags;

            // null derive all Implements/Dispatches interfaces (any delta
            // interfaces are expanded during behavior resolve processing)
            for (Enumeration enmr = base.m_tblImplements.elements(); enmr.hasMoreElements(); )
                {
                Interface ifaceBase    = (Interface) enmr.nextElement();
                Interface ifaceDelta   = (Interface) ifaceBase.getNullDerivedTrait(delta, nDeltaMode);
                Interface ifaceDerived = (Interface) ifaceBase.resolve(ifaceDelta, derived, loader, errlist);
                derived.m_tblImplements.put(ifaceDerived.getUniqueName(), ifaceDerived);
                }
            for (Enumeration enmr = base.m_tblDispatches.elements(); enmr.hasMoreElements(); )
                {
                Interface ifaceBase    = (Interface) enmr.nextElement();
                Interface ifaceDelta   = (Interface) ifaceBase.getNullDerivedTrait(delta, nDeltaMode);
                Interface ifaceDerived = (Interface) ifaceBase.resolve(ifaceDelta, derived, loader, errlist);
                derived.m_tblDispatches.put(ifaceDerived.getUniqueName(), ifaceDerived);
                }
            }


        // ----- state, behavior

        // The act of resolving a Component derivation/modification is
        // significantly more complex than the extraction of a derivation/
        // modification for the following reasons:
        //
        //  1)  Identity contains interface declarations (integration,
        //      implements, dispatches) which potentially conflict with
        //      Properties and Behaviors present (or reserved) in the base
        //      as well as Behaviors present (or reserved) in this Component
        //  2)  State contains Property declarations (i.e. INSERTs) which
        //      could conflict with the base (either because the Property
        //      already exists or is "reserved")
        //  3)  Behaviors on this could conflict with Behaviors on the base
        //      (either because the Behavior already exists or is "reserved")
        //
        // The manner of resolving interfaces, state, and behavior differs
        // depending on the mode (resolved, derivation, modification) of the
        // base and delta Components.  The most complex scenario is the
        // resolution of a Component Derivation using a resolved base
        // Component Definition (i.e. the super Component).  The other three
        // scenarios (modifications) are implemented as subsets of the first
        // scenario.
        //
        //  1)  The primary rule is that the base is always correct.  The
        //      base is never altered by the resolve of this Component; if
        //      there is a conflict with the base, the information from the
        //      delta Component is discarded or altered.  There are three key
        //      groups of information on the base:
        //      a)  All interfaces (integration, implements, dispatches) from
        //          the base are visible from this Component (see identity
        //          resolution above)
        //      b)  Behaviors on the base fall into one of three categories:
        //          1)  Reserved Behaviors may not be visible from this
        //              Component but cannot be added by (i.e. cannot exist
        //              as INSERTs on) this Component; reserved Behaviors
        //              include case-insensitive conflicts (foo vs. Foo) and
        //              private Behaviors which implement Property accessors
        //              on a super class for Properties with at least one
        //              non-private accessor
        //          2)  In the case of a Component Derivation, Behaviors
        //              which are private on the base Component are not
        //              visible from this Component, but are not by default
        //              reserved so same-named Behaviors can be added to
        //              (i.e. can exist as INSERTs on) this Component
        //          3)  All other Behaviors are modifiable/derivable subject
        //              to the resolve rules implemented by Behavior
        //      c)  Properties are very similar to Behaviors (in terms of the
        //          above categorization) with one notable difference:  a
        //          Property with at least one non-private accessor is
        //          carried forward to derived Components but may not be
        //          "present" (modifiable) there; it serves as a place-holder
        //          to reserve the Property name
        //
        //      The implementation of this rule is that all base Behaviors
        //      and Properties are resolved first, since they must exist
        //      on the resulting derived Component.
        //
        //  2)  The resolve of a derivation includes the expansion of any
        //      interfaces added by the derivation, in the following order:
        //          a)  [integration]
        //          b)  [implements]*
        //          c)  [dispatches]*
        //      Each interface must be able to be expanded fully or the
        //      complete interface is discarded; the act of expanding an
        //      interface is comprised of iterating through the Property/
        //      Behaviors of the interface and creating an INSERT Property/
        //      Behavior if one does not already exist (or one is not already
        //      implied) in the list of Properties/Behaviors generated by
        //      step 2 above (note:  only integration has Properties)
        //
        //  3)  Properties and Behaviors added by the delta Component which
        //      were not already accounted for (used for resolving) by steps
        //      one and two are now processed.  Each is tested for the
        //      ability to apply (i.e. must not be reserved).
        //
        // Component modifications are processed slightly differently:
        //
        //  1)  Behaviors and Properties from the base are always carried to
        //      the derived, regardless of access (e.g. private Behaviors)
        //  2)  No step 2b or 2c (interface expansion)
        //
        // Complex values (components that carry only state) are a special
        // case.  Since Complex values do not have any Behaviors, the
        // Properties on a Complex cannot determine whether the Property
        // values are settable or not.  To simplify this, only (and all)
        // settable Properties are kept on a Complex at the point it
        // resolves against a Component.
        //
        // It is expected that, for the overwhelming majority (perhaps about
        // 99%) of the time, no additional Properties or Behaviors are going
        // to be added or removed, and no additional interfaces (integration,
        // implements, and/or dispatches) are implemented; (in fact, many
        // child Components will probably be null derivations other than at
        // their insert level).  The implementation is optimized based on
        // this assumption.
        //
        // Note:  Behaviors are processed before Properties since the
        // Property implementation uses the existence of accessors to
        // determine which attributes can be set during resolve processing.
        // See the use of Property.isValueSettable() in the implementation
        // of Property.resolve().

        // step 1:  match and derive base items, deferring any unmatched

        StringTable tblDeferBehavior;
        StringTable tblDeferProperty;

        if (fSignature)
            {
            tblDeferBehavior = new StringTable();
            tblDeferProperty = new StringTable();
            }
        else
            {
            tblDeferBehavior = new StringTable(INSENS);
            tblDeferProperty = new StringTable(INSENS);
            }

        // process Behaviors first (see note above)
        if (!fComplex)
            {
            StringTable tblBase    = base.m_tblBehavior;
            StringTable tblDelta   = delta.matchBehaviors(base, true, (nDeltaMode == DERIVATION), errlist);
            StringTable tblDerived = derived.m_tblBehavior; // empty
            StringTable tblDefer   = tblDeferBehavior;
            for (Enumeration enmr = tblDelta.keys(); enmr.hasMoreElements(); )
                {
                String   sSig       = (String) enmr.nextElement();
                Behavior behBase    = (Behavior) tblBase .get(sSig);
                Behavior behDelta   = (Behavior) tblDelta.get(sSig);
                Behavior behDerived = null;

                // a derived component cannot "see" private behaviors on the
                // base component, nor do they inherit constructors of JCS'es
                if (behBase != null)
                    {
                    if (nDeltaMode == DERIVATION
                        && (behBase.getAccess() == ACCESS_PRIVATE || behBase.isConstructor()))
                        {
                        behBase = null;
                        }
                    }

                if (behBase == null)
                    {
                    // no matching behavior on the base; must defer resolve
                    // of the delta behavior until interface have been
                    // applied
                    tblDefer.put(sSig, behDelta);
                    }
                else
                    {
                    if (behDelta != null && behBase.isFinal())
                        {
                        logError(RESOLVE_BEHAVIORFINAL, WARNING, new Object[]
                            {behDelta.getSignature(), behDelta.toPathString()}, errlist);
                        behDelta = null;
                        }

                    // create a null derivation if no delta exists
                    if (behDelta == null)
                        {
                        behDelta = (Behavior) behBase.getNullDerivedTrait(delta, nDeltaMode);

                        // gg: 2003.11.24 Behavior is EXISTS_NOT if the method is not present
                        //                in the class represented by the JCS.
                        if (fSignature && nDeltaMode == DERIVATION)
                            {
                            behDelta.setExists(EXISTS_NOT);
                            }
                        }

                    // apply the delta
                    behDerived = (Behavior) behBase.resolve(behDelta, derived, loader, errlist);
                    tblDerived.put(sSig, behDerived);
                    }
                }
            }

        // process State
        {
        StringTable tblBase    = base.m_tblState;
        StringTable tblDelta   = delta.matchProperties(base, true, (nDeltaMode == DERIVATION), errlist);
        StringTable tblDerived = derived.m_tblState; // empty
        StringTable tblDefer   = tblDeferProperty;
        for (Enumeration enmr = tblDelta.keys(); enmr.hasMoreElements(); )
            {
            String   sProp       = (String) enmr.nextElement();
            Property propBase    = (Property) tblBase .get(sProp);
            Property propDelta   = (Property) tblDelta.get(sProp);
            Property propDerived = null;

            if (propBase != null)
                {
                // only bring across those properties that are
                // considered to be complex property properties
                if (fComplex && !fBaseComplex)
                    {
                    if (!propBase.isComplexPropertyProperty())
                        {
                        propBase = null;
                        }
                    }
                // a derived component cannot "see" private base Properties or
                // base Properties that have only private accessors
                else if (nDeltaMode == DERIVATION && !propBase.isDerivedBySub())
                    {
                    propBase = null;
                    }
                }

            if (propBase == null)
                {
                if (fComplex)
                    {
                    if (propDelta != null)
                        {
                        // complex properties must modify a base
                        logError(RESOLVE_PROPERTYORPHANED, WARNING, new Object[]
                                {propDelta.toString(), delta.toPathString()}, errlist);
                        }
                    }
                else
                    {
                    // no matching Property on the base; must defer resolve
                    // of the delta Property until integration has been applied
                    tblDefer.put(sProp, propDelta);
                    }
                }
            else
                {
                // static base properties are not derivable (but are carried)
                if (propDelta != null && nDeltaMode == DERIVATION && propBase.isStatic())
                    {
                    // in signature's this is okay and the delta carries forward
                    if (!fSignature)
                        {
                        logError(RESOLVE_PROPERTYSTATIC, WARNING, new Object[]
                            {propDelta.getName(), propDelta.toPathString()}, errlist);
                        propDelta = null;
                        }
                    }

                // create a null derivation if no delta exists
                if (propDelta == null)
                    {
                    propDelta = (Property) propBase.getNullDerivedTrait(delta, nDeltaMode);
                    }

                // apply the delta
                propDerived = (Property) propBase.resolve(propDelta, derived, loader, errlist);
                tblDerived.put(sProp, propDerived);
                }
            }
        }

        // step 2a:  expand integration
        // integration, only at the global level
        if (!fComplex && parent == null)
            {
            Integration integrationBase  = base .m_integration;
            Integration integrationDelta = delta.m_integration;

            if (integrationBase != null || integrationDelta != null)
                {
                // Create an empty base Integration for use in resolving
                if (integrationBase == null)
                    {
                    integrationBase = new Integration(base, (String) null);
                    }

                // Create an empty delta Integration for use in resolving
                if (integrationDelta == null)
                    {
                    integrationDelta = (Integration) integrationBase.getNullDerivedTrait(delta, nDeltaMode);
                    }

                // Resolve the Integration map
                Integration integrationDerived = integrationBase.resolve(integrationDelta,
                    derived, derived.m_tblBehavior, derived.m_tblState,
                    tblDeferBehavior, tblDeferProperty, loader, errlist);
                derived.m_integration = integrationDerived;
                }
            }

        // step 2b, 2c:  process added interfaces; expand implements and
        // dispatches only at derivation level
        // 2001.05.08 cp  allow additional interfaces on modifications
        if (!fComplex)
            {
            // implements
            StringTable tblBase    = base   .m_tblImplements;
            StringTable tblDelta   = delta  .m_tblImplements;
            StringTable tblDerived = derived.m_tblImplements;
            for (Enumeration enmr = tblDelta.keys(); enmr.hasMoreElements(); )
                {
                String sIface = (String) enmr.nextElement();
                if (!tblBase.contains(sIface))
                    {
                    Interface ifaceDelta   = (Interface) tblDelta.get(sIface);
                    Interface ifaceDerived = null;

                    if (nDeltaMode == DERIVATION)
                        {
                        // load the JCS for the interface
                        Component cdJCS = loader.loadSignature(sIface);
                        if (cdJCS == null)
                            {
                            // unable to load Java Class Signature for interface
                            logError(RESOLVE_LOADINTERFACE, WARNING, new Object[]
                                {sIface, ATTR_IMPLEMENTS, derived.getQualifiedName()}, errlist);
                            }
                        else
                            {
                            ifaceDerived = new Interface(derived, cdJCS, false);
                            // 2001.05.08 cp  fix origin of interfaces
                            if (ifaceDelta.isFromBase())
                                {
                                ifaceDerived.setFromBase();
                                }

                            if (derived.isImplementsAddable(ifaceDerived, cdJCS, errlist))
                                {
                                // expand the interface
                                try
                                    {
                                    derived.expandInterface(ifaceDerived, cdJCS, tblDeferBehavior, tblDeferProperty, errlist);
                                    }
                                catch (PropertyVetoException e)
                                    {
                                    logError(RESOLVE_EXPANDINTERFACE, ERROR, new Object[] {sIface,
                                        ATTR_IMPLEMENTS, derived.getQualifiedName()}, errlist);
                                    throw new DerivationException(e.toString());
                                    }

                                // add the interface to the derived Component
                                tblDerived.put(sIface, ifaceDerived);

                                // if we are a signature, also add all interfaces
                                // extended by this interface
                                // 2001.05.22 cp  now that JCSs are designable, this
                                //                is no longer the case
                                /*
                                if (fSignature)
                                    {
                                    String [] asExtends = cdJCS.getImplements();
                                    for (int i = 0; i < asExtends.length; ++i)
                                        {
                                        sIface = asExtends[i];
                                        if (!tblDerived.contains(sIface))
                                            {
                                            Interface ifaceJCS = cdJCS.getImplements(sIface);
                                            ifaceDerived       = new Interface(derived, ifaceJCS);
                                            tblDerived.put(sIface, ifaceDerived);
                                            }
                                        }
                                    }
                                */
                                }
                            else
                                {
                                // unable to expand interface
                                logError(RESOLVE_EXPANDINTERFACE, WARNING, new Object[] {sIface,
                                    ATTR_IMPLEMENTS, derived.getQualifiedName()}, errlist);
                                }
                            }
                        }
                    else
                        {
                        ifaceDerived = new Interface(derived, ifaceDelta);
                        tblDerived.put(sIface, ifaceDerived);
                        }
                    }
                }

            // dispatches
            tblBase    = base   .m_tblDispatches;
            tblDelta   = delta  .m_tblDispatches;
            tblDerived = derived.m_tblDispatches;
            for (Enumeration enmr = tblDelta.keys(); enmr.hasMoreElements(); )
                {
                String sIface = (String) enmr.nextElement();
                if (!tblBase.contains(sIface))
                    {
                    Interface ifaceDelta   = (Interface) tblDelta.get(sIface);
                    Interface ifaceDerived = null;

                    if (nDeltaMode == DERIVATION)
                        {
                        // load the JCS for the interface
                        Component cdJCS = loader.loadSignature(sIface);
                        if (cdJCS == null)
                            {
                            // unable to load Java Class Signature for interface
                            logError(RESOLVE_LOADINTERFACE, WARNING, new Object[]
                                {sIface, ATTR_DISPATCHES, derived.getQualifiedName()}, errlist);
                            }
                        else
                            {
                            ifaceDerived = new Interface(derived, cdJCS, true);
                            // 2001.05.08 cp  fix origin of interfaces
                            if (ifaceDelta.isFromBase())
                                {
                                ifaceDerived.setFromBase();
                                }

                            if (derived.isDispatchesAddable(ifaceDerived, cdJCS, errlist))
                                {
                                // expand the interface
                                try
                                    {
                                    derived.expandInterface(ifaceDerived, cdJCS,
                                        tblDeferBehavior, tblDeferProperty, errlist);
                                    }
                                catch (PropertyVetoException e)
                                    {
                                    logError(RESOLVE_EXPANDINTERFACE, ERROR, new Object[] {sIface,
                                        ATTR_DISPATCHES, derived.getQualifiedName()}, errlist);
                                    throw new DerivationException(e.toString());
                                    }

                                // add the interface to the derived Component
                                tblDerived.put(sIface, ifaceDerived);
                                }
                            else
                                {
                                // unable to expand interface
                                logError(RESOLVE_EXPANDINTERFACE, WARNING, new Object[] {sIface,
                                    ATTR_DISPATCHES, derived.getQualifiedName()}, errlist);
                                }
                            }
                        }
                    else
                        {
                        ifaceDerived = new Interface(derived, ifaceDelta);
                        tblDerived.put(sIface, ifaceDerived);
                        }
                    }
                }
            }

        // step 3:  add deferred Behaviors and Properties
        if (!fComplex)
            {
            if (!tblDeferBehavior.isEmpty())
                {
                StringTable tblBase    = base.m_tblBehavior;
                StringTable tblDelta   = tblDeferBehavior;
                StringTable tblDerived = derived.m_tblBehavior;
                for (Enumeration enmr = tblDelta.keys(); enmr.hasMoreElements(); )
                    {
                    String   sSig       = (String) enmr.nextElement();
                    Behavior behBase    = (Behavior) tblBase .get(sSig);
                    Behavior behDelta   = (Behavior) tblDelta.get(sSig);
                    Behavior behDerived = null;

                    // it is an error if the base Behavior exists, because
                    // all matching Behaviors were supposed to be handled
                    // by step 1; however, there are two exceptions.  One when
                    // the base behavior is private and the delta is a derivation,
                    // in which case the base Behavior is not visible to (not
                    // carried to) the derived Component.  The other is when
                    // there is an identical constructor on a JCS, it is not
                    // inherited forward to the derived JCS.
                    if (behBase != null)
                        {
                        if (!(nDeltaMode == DERIVATION
                              && (behBase.getAccess() == ACCESS_PRIVATE || behBase.isConstructor())))
                            {
                            throw new IllegalStateException(CLASS + ".resolve:  "
                                    + "Deferred Behavior has match in base! (" + sSig + ")");
                            }
                        }

                    if (tblDerived.contains(sSig) || derived.isBehaviorReserved(sSig))
                        {
                        // discard the delta
                        logError(RESOLVE_BEHAVIORRESERVED, WARNING, new Object[]
                            {behDelta.getSignature(), behDelta.toPathString()}, errlist);
                        }
                    else
                        {
                        // nothing to apply to, just copy the Behavior
                        // information to the derived Component
                        behDerived = new Behavior(derived, behDelta);
                        tblDerived.put(sSig, behDerived);
                        }
                    }
                }

            if (!tblDeferProperty.isEmpty())
                {
                StringTable tblBase    = base.m_tblState;
                StringTable tblDelta   = tblDeferProperty;
                StringTable tblDerived = derived.m_tblState;
                for (Enumeration enmr = tblDelta.keys(); enmr.hasMoreElements();)
                    {
                    String   sProp       = (String) enmr.nextElement();
                    Property propBase    = (Property) tblBase .get(sProp);
                    Property propDelta   = (Property) tblDelta.get(sProp);
                    Property propDerived = null;

                    // if the delta is a DERIVATION and the base Property
                    // is not carried to derived Components, then it is
                    // as if the base Property does not exist.  Otherwise
                    // it is an error since all carried base Properties
                    // were supposed to be handled by step 1
                    if (propBase != null &&
                            !(nDeltaMode == DERIVATION && !propBase.isDerivedBySub()))
                        {
                        throw new IllegalStateException(CLASS + ".resolve:  "
                                + "Deferred Property has match in base! (" + sProp + ")");
                        }

                    // can only determine real conflicts on a DERIVATION
                    if (nDeltaMode == DERIVATION && propDelta.getExists() != EXISTS_INSERT)
                        {
                        // the derived property is discarded
                        logError(RESOLVE_PROPERTYORPHANED, WARNING, new Object[]
                            {propDelta.toString(), delta.toPathString()}, errlist);
                        }
                    else if (Property.isCreatable(derived, propDelta.getDataType(), sProp,
                            propDelta.getIndexed(), propDelta.isStatic(),
                            propDelta.getDirection(), propDelta.isConstant()))
                        {
                        // nothing to apply to, just copy the Property
                        // information to the derived Component
                        propDerived = new Property(derived, propDelta);
                        tblDerived.put(sProp, propDerived);
                        }
                    else
                        {
                        // discard the delta
                        // TODO if the discarded prop is not "from this"
                        //      then this is an error condition, right?
                        //      it means that something is out of synch
                        //      but not at this modification level so
                        //      there is no way to fix it!  same goes for
                        //      behaviors, integration, and interfaces!
                        logError(RESOLVE_PROPERTYRESERVED, WARNING, new Object[]
                            {propDelta.getName(), propDelta.toPathString()}, errlist);
                        }
                    }
                }
            }

        // ----- aggregation

        if (!fComplex)
            {
            // resolve the aggregate categories
            StringTable tblCategories = derived.m_tblCategories;

            // all categories from the delta are marked as TRUE
            Boolean TRUE = Boolean.TRUE;
            for (Enumeration enmr = delta.m_tblCategories.keys(); enmr.hasMoreElements(); )
                {
                tblCategories.put((String) enmr.nextElement(), TRUE);
                }

            // all categories from the base are marked as FALSE
            // (any categories from the delta that are also in the base are
            // purposefully overwritten, since the delta is a delta)
            Boolean FALSE = Boolean.FALSE;
            for (Enumeration enmr = base.m_tblCategories.keys(); enmr.hasMoreElements(); )
                {
                tblCategories.put((String) enmr.nextElement(), FALSE);
                }

            // resolve the child Component derivations/modifications:
            StringTable tblBase    = base.m_tblChildren;
            StringTable tblDelta   = delta.matchChildren(base, true, errlist);
            StringTable tblDerived = derived.m_tblChildren; // empty
            for (Enumeration enmr = tblDelta.keys(); enmr.hasMoreElements(); )
                {
                String    sChild    = (String) enmr.nextElement();
                Component cdBase    = (Component) tblBase .get(sChild);
                Component cdDelta   = (Component) tblDelta.get(sChild);
                Component cdDerived = null;

                // if the delta is an insert child (i.e. no base child) then
                // resolve it against the global component from which it
                // derives; (note: the check for if the base is resolved is
                // used to defer the resolve of the child from the global
                // until it is the last or "outermost" resolve ... in other
                // words, all components in the parent/child hierarchy
                // become resolved at the same time)
                if (cdBase == null && fBaseResolved && cdDelta.getExists() == EXISTS_INSERT)
                    {
                    // load the global super (which may throw an exception)
                    cdBase = loader.loadComponent(cdDelta.getSuperName(), true, errlist);

                    if (cdBase == null)
                        {
                        // global Component does not exist
                        logError(RESOLVE_CHILDORPHANED, WARNING, new Object[]
                            {cdDelta.toString(), delta.toPathString()}, errlist);
                        }
                    else if (cdBase.isFinal())
                        {
                        // cannot derive from final global Component
                        logError(RESOLVE_GLOBALFINAL, WARNING, new Object[]
                            {cdDelta.getName(), cdBase.getQualifiedName(),
                            cdDelta.toPathString()}, errlist);
                        cdBase = null;
                        }
                    else if (!derived.isChildSuperLegal(cdBase.getQualifiedName()))
                        {
                        // base Component doesn't fit a category
                        // (aka illegal super)
                        logError(RESOLVE_NOCATEGORY, WARNING, new Object[]
                            {cdDelta.toString(), cdBase.toString(), delta.toPathString()},
                            errlist);
                        cdBase = null;
                        }
                    }

                if (cdBase == null)
                    {
                    if (fBaseResolved)
                        {
                        // discard the delta child
                        logError(RESOLVE_CHILDORPHANED, WARNING, new Object[]
                            {cdDelta.toString(), delta.toPathString()}, errlist);
                        }
                    else
                        {
                        // just copy the child derivation/modification to the
                        // derived component; it is expected for modification
                        // levels that most base children will not exist
                        cdDerived = new Component(derived, cdDelta);
                        }
                    }
                else
                    {
                    int nBaseChildFlags = cdBase.m_nFlags;
                    if (cdDelta != null)
                        {
                        // check if base child is final and if so discard delta
                        if ((nBaseChildFlags & DERIVE_MASK) == DERIVE_FINAL &&
                            (cdBase.getMode() == RESOLVED || (nBaseChildFlags & DERIVE_SPECIFIED) != 0))
                            {
                            logError(RESOLVE_LOCALFINAL, WARNING, new Object[]
                                {cdDelta.getName(), cdDelta.toPathString()}, errlist);
                            cdDelta = null;
                            }
                        }

                    // check if the Component should exist at all -
                    // 1) deriving from a DELETE, result is a NOT
                    // 2) deriving from or modifying a NOT, result is a NOT
                    // 3) allow modification of a DELETE (becomes DELETE with
                    //    previous DELETE) in order to carry the DELETE until
                    //    something is found to DELETE
                    if ((nBaseChildFlags & EXISTS_MASK) == EXISTS_NOT ||
                        (nBaseChildFlags & EXISTS_MASK) == EXISTS_DELETE && nDeltaMode == DERIVATION)
                        {
                        // a blank EXISTS_NOT component just reserves the name
                        cdDerived = (Component) cdBase.getBlankDerivedTrait(derived, cdBase.getMode());
                        cdDerived.setExists(EXISTS_NOT);
                        }
                    else
                        {
                        // create a null derivation if no delta exists
                        if (cdDelta == null)
                            {
                            cdDelta = (Component) cdBase.getNullDerivedTrait(delta, nDeltaMode);
                            }

                        // apply the delta
                        cdDerived = (Component) cdBase.resolve(cdDelta, derived, loader, errlist);
                        }
                    }

                if (cdDerived != null)
                    {
                    tblDerived.put(sChild, cdDerived);
                    }
                }
            }

        return derived;
        }

    /**
    * Resolve delta in flags.  Components use the following flags:
    * Exists, Visible, Access (JCS only), Static, Abstract, Final,
    * Deprecation, Storage, and Distribution.
    *
    * implementation note:  a flag value is always meaningful if the
    * Component is resolved; otherwise it is meaningful only if the
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
        boolean fSignature = isSignature();

        // derived flags always start as the base flags; then the delta flags
        // are applied
        int nDerivedFlags = nBaseFlags;

        // optimization: check for nothing specified
        if (nDeltaFlags == 0)
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

        // for signatures the access attribute is one-way:  private to protected to public
        if (fSignature)
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

        // the scope attribute is one-way from instance to static
        if (    (nDeltaFlags & SCOPE_SPECIFIED) != 0
             && (nDeltaFlags & SCOPE_MASK) == SCOPE_STATIC)
            {
            nDerivedFlags = nDerivedFlags & ~SCOPE_FULLMASK | nDeltaFlags & SCOPE_FULLMASK;
            }

        // the abstract attribute is flexible
        if (fSignature || (nDeltaFlags & IMPL_SPECIFIED) != 0)
            {
            nDerivedFlags = nDerivedFlags & ~IMPL_FULLMASK | nDeltaFlags & IMPL_FULLMASK;
            }

        // the final attribute is one-way
        if ((fSignature
             || (nDeltaFlags & DERIVE_SPECIFIED) != 0)
             && (nDeltaFlags & DERIVE_MASK) == DERIVE_FINAL)
            {
            nDerivedFlags = nDerivedFlags & ~DERIVE_FULLMASK | nDeltaFlags & DERIVE_FULLMASK;
            }

        // the antiquity (deprecated) attribute is flexible
        if (fSignature
            || (nDeltaFlags & ANTIQ_SPECIFIED) != 0)
            {
            nDerivedFlags = nDerivedFlags & ~ANTIQ_FULLMASK | nDeltaFlags & ANTIQ_FULLMASK;
            }

        // the storage (persistence) attribute is flexible
        if ((nDeltaFlags & STG_SPECIFIED) != 0)
            {
            nDerivedFlags = nDerivedFlags & ~STG_FULLMASK | nDeltaFlags & STG_FULLMASK;
            }

        // the distribution (remote) attribute is one-way and is specifiable
        // only on global Components
        if (    (nDeltaFlags & DIST_SPECIFIED) != 0
             && (nDeltaFlags & DIST_MASK) == DIST_REMOTE)
            {
            nDerivedFlags = nDerivedFlags & ~DIST_FULLMASK | nDeltaFlags & DIST_FULLMASK;
            }

        if ((nDeltaFlags & MISC_ISINTERFACE) != 0)
            {
            nDerivedFlags |= MISC_ISINTERFACE;
            }

        if ((nDeltaFlags & MISC_ISTHROWABLE) != 0)
            {
            nDerivedFlags |= MISC_ISTHROWABLE;
            }

        return nDerivedFlags;
        }

    /**
    * Complete the resolve processing for this Component and its sub-traits.
    *
    * @param loader    the Loader object for JCS, CIM, and CD dependencies
    * @param errlist   the error list object to log error information to
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    public synchronized void finalizeResolve(Loader loader, ErrorList errlist)
            throws ComponentException
        {
        super.finalizeResolve(loader, errlist);

        // remove all "specified" flags
        m_nFlags     &= ~ALL_SPECIFIED;
        m_nPrevFlags &= ~ALL_SPECIFIED;

        // resolve sub-traits:  Integration
        if (m_integration != null)
            {
            m_integration.finalizeResolve(loader, errlist);
            }

        // resolve sub-traits:  Implements
        for (Enumeration enmr = m_tblImplements.elements(); enmr.hasMoreElements(); )
            {
            Interface iface = (Interface) enmr.nextElement();
            iface.finalizeResolve(loader, errlist);
            }

        // resolve sub-traits:  Dispatches
        for (Enumeration enmr = m_tblDispatches.elements(); enmr.hasMoreElements(); )
            {
            Interface iface = (Interface) enmr.nextElement();
            iface.finalizeResolve(loader, errlist);
            }

        // resolve sub-traits:  Properties
        for (Enumeration enmr = m_tblState.elements(); enmr.hasMoreElements(); )
            {
            Property prop = (Property) enmr.nextElement();
            prop.finalizeResolve(loader, errlist);
            }

        // resolve sub-traits:  Behaviors
        for (Enumeration enmr = m_tblBehavior.elements(); enmr.hasMoreElements(); )
            {
            Behavior behavior = (Behavior) enmr.nextElement();
            behavior.finalizeResolve(loader, errlist);
            }

        // resolve sub-traits:  Children
        for (Enumeration enmr = m_tblChildren.elements(); enmr.hasMoreElements(); )
            {
            Component cd = (Component) enmr.nextElement();
            cd.finalizeResolve(loader, errlist);
            }

        // discard origin-less Properties
        for (Enumeration enmr = m_tblState.elements(); enmr.hasMoreElements(); )
            {
            Property prop = (Property) enmr.nextElement();
            if (prop.isDiscardable())
                {
                m_tblState.remove(prop.getUniqueName());
                prop.invalidate();
                }
            }

        // discard origin-less Behaviors
        for (Enumeration enmr = m_tblBehavior.elements(); enmr.hasMoreElements(); )
            {
            Behavior behavior = (Behavior) enmr.nextElement();
            if (behavior.isDiscardable())
                {
                m_tblBehavior.remove(behavior.getUniqueName());
                behavior.invalidate();
                }
            }

        // discard origin-less Children
        for (Enumeration enmr = m_tblChildren.elements(); enmr.hasMoreElements(); )
            {
            Component cd = (Component) enmr.nextElement();
            if (cd.isDiscardable())
                {
                m_tblChildren.remove(cd.getUniqueName());
                cd.invalidate();
                }
            }

        // gg: 2002.02.13 Clear UID for global components
        if (isGlobal() && getUID() != null)
            {
            logError(RESOLVE_UIDCHANGE, WARNING, new Object[]
                    {getName(), "Global UID must be null"}, errlist);
            clearUID();
            }

        validate();
        }

    /**
    * Determine the mode of extraction assuming the passed base Trait were
    * being exctracted from this derived Trait.
    *
    * This method must be overridden by any Trait which can determine
    * whether the extraction is for a DERIVATION or MODIFICATION.
    * (Component is the only trait which can differentiate.)
    *
    * @param traitBase  the base Trait to extract
    *
    * @return DERIVATION or MODIFICATION
    */
    protected int getExtractMode(Trait traitBase)
        {
        switch (m_nType)
            {
            case COMPONENT:
                {
                Component base = (Component) traitBase;
                if (base.m_sName.length() == 0)
                    {
                    // root component is a derivation if this is not also the root component
                    return (this.m_sName.length() == 0 ? MODIFICATION : DERIVATION);
                    }
                else if (this.getParent() == null)
                    {
                    // global component is a derivation if super is different
                    // 2001.04.27 cp  this appears to have been done in this manner
                    // to support save-as processing;  otherwise, it would have been
                    // done as a comparison of qualified component names
                    return (this.m_sSuper.equals(base.m_sSuper) ? MODIFICATION : DERIVATION);
                    }
                else if (base.getParent() == null)
                    {
                    // child is a derivation if the base is global
                    return DERIVATION;
                    }
                }
                break;

            case SIGNATURE:
                {
                // Signatures are always extracted as DERIVATIONs
                // return DERIVATION;
                Component base = (Component) traitBase;
                return this.m_sName.equals(base.m_sName) ? MODIFICATION : DERIVATION;
                }

            case COMPLEX:
                // complex properties are always extracted as MODIFICATIONs
                return MODIFICATION;
            }

        return super.getExtractMode(traitBase);
        }


    /**
    * Create a derivation/modification Component by determining the delta
    * between the derived Component ("this") and the passed base Component.
    *
    * @param cdBase   the base Component to extract
    * @param loader   the Loader object for JCS, CIM, and CD dependencies
    * @param errlist  the error list object to log error information to
    *
    * @return the delta between this Component and the base Component
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    public Component extract(Component cdBase, Loader loader, ErrorList errlist)
            throws ComponentException
        {
        // validate base component definition
        if (cdBase == null)
            {
            throw new IllegalArgumentException(CLASS + ".extract:  "
                + "Base Component required!");
            }

        if (this.m_nType != COMPONENT && this.m_nType != SIGNATURE)
            {
            throw new IllegalArgumentException(CLASS + ".extract:  "
                + "Derived is not a Component or Java Class Signature!");
            }

        if (this.m_nType != cdBase.m_nType)
            {
            throw new IllegalArgumentException(CLASS + ".extract:  "
                + "Base type (" + cdBase.m_nType + ") != Derived type (" + this.m_nType + ")");
            }
        return (Component) extract(cdBase, null, loader, errlist);
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
        Component derived = this;
        Component base    = (Component) traitBase;
        Component delta   = (Component) super.extract(base, parent, loader, errlist);

        // determine if the base is resolved (which means all base attributes
        // are specified, i.e. have a value)
        boolean fBaseResolved = (base.getMode() == RESOLVED);

        // determine if extracting a DERIVATION or MODIFICATION
        int nDeltaMode = delta.getMode();

        // a complex Property value is implemented using the Component
        // Definition data structure; a complex value is extracted
        // either from another complex value or from a global Component
        boolean fComplex     = (derived.m_nType == COMPLEX);
        boolean fBaseComplex = (base   .m_nType == COMPLEX);

        // ----- identity ----------------------------------------------

        if (fComplex)
            {
            if (fBaseComplex)
                {
                delta.m_nFlags = EXISTS_UPDATE;
                }
            else
                {
                delta.m_nType  = COMPLEX;
                delta.m_nFlags = EXISTS_SPECIFIED | EXISTS_INSERT;
                }
            delta.m_sSuper = derived.m_sSuper;
            delta.m_sName  = derived.m_sName;
            }
        else
            {
            // attributes represented as bit flags; see the length note in the
            // corresponding section of the extract method in Component.java
            int nBaseFlags    = base   .m_nFlags;
            int nDerivedFlags = derived.m_nFlags;
            int nDifFlags     = nBaseFlags ^ nDerivedFlags;
            int nDeltaFlags   = nDerivedFlags & ~ALL_SPECIFIED;

            // extract exists
            int nBaseExists    = nBaseFlags & EXISTS_MASK;
            int nDerivedExists = nDerivedFlags & EXISTS_MASK;
            int nDeltaExists   = EXISTS_UPDATE;
            if (base.getParentTrait() == null && derived.getParentTrait() != null)
                {
                // an insert component exists when the base is global and the
                // derived component is a child
                nDeltaExists = EXISTS_SPECIFIED | EXISTS_INSERT;
                }
            else if (nDerivedExists == EXISTS_DELETE &&
                    (nBaseExists == EXISTS_INSERT || nBaseExists == EXISTS_UPDATE))
                {
                // derived component deletes the base
                nDeltaExists = EXISTS_SPECIFIED | EXISTS_DELETE;
                }
            nDeltaFlags = nDeltaFlags & ~EXISTS_FULLMASK | nDeltaExists;

            // visible
            if ((nDifFlags & VIS_MASK) != 0 &&
                    (fBaseResolved || (nBaseFlags & VIS_SPECIFIED) != 0))
                {
                nDeltaFlags |= VIS_SPECIFIED;
                }

            // scope
            if ((nDifFlags & SCOPE_MASK) != 0 &&
                    (fBaseResolved || (nBaseFlags & SCOPE_SPECIFIED) != 0))
                {
                nDeltaFlags |= SCOPE_SPECIFIED;
                }

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

            // storage
            if ((nDifFlags & STG_MASK) != 0 &&
                    (fBaseResolved || (nBaseFlags & STG_SPECIFIED) != 0))
                {
                nDeltaFlags |= STG_SPECIFIED;
                }

            // distribution
            if ((nDifFlags & DIST_MASK) != 0 &&
                    (fBaseResolved || (nBaseFlags & DIST_SPECIFIED) != 0))
                {
                nDeltaFlags |= DIST_SPECIFIED;
                }

            // extract identity
            if (nDeltaMode == DERIVATION)
                {
                if (parent == null ||
                        nDeltaExists == (EXISTS_SPECIFIED | EXISTS_INSERT))
                    {
                    // derives from a global component
                    delta.m_sSuper = base.getQualifiedName();
                    delta.m_sName  = derived.m_sName;
                    }
                else
                    {
                    // derives from a local component
                    delta.m_sSuper = base.m_sSuper;
                    delta.m_sName  = base.m_sName;
                    }

                // integration only happens at a global derivation level
                Integration integrationBase    = base.m_integration;
                Integration integrationDerived = derived.m_integration;
                Integration integrationDelta   = null;

                // We only have to bother if the derived has integration
                // (since integration can not be removed)
                if (integrationDerived != null)
                    {
                    // Check if this is the first integration applied
                    // to this component tree
                    if (integrationBase == null)
                        {
                        // It is, just copy it across
                        integrationDelta = new Integration(base, integrationDerived);
                        }
                    else
                        {
                        // extract the delta
                        integrationDelta = (Integration) integrationDerived.extract(integrationBase, delta, loader, errlist);
                        }
                    }

                delta.m_integration = integrationDelta;

                delta.m_fBaseLevel = true;
                }
            else
                {
                // modification keeps the Component identity from the base
                delta.m_sSuper = base.m_sSuper;
                delta.m_sName  = base.m_sName;

                // modification means not at the base level
                // (modification cannot integrate)
                delta.m_fBaseLevel = false;
                }

            // store flags
            delta.m_nFlags     = nDeltaFlags;
            delta.m_nPrevFlags = 0;

            // determine the interfaces which are implemented/dispatched by
            // the derived but not the base Component
            // (most common case:  the sets of interfaces are identical
            // or (for modifications) that the derived has no interfaces)
            // extract Implements
            // 2001.05.08 cp  removed "DERIVATION" check to allow
            //                modifications to add interfaces
            StringTable tblBase    = base   .m_tblImplements;
            StringTable tblDerived = derived.m_tblImplements;
            StringTable tblDelta   = delta  .m_tblImplements;
            if (!tblDerived.isEmpty() && !tblDerived.keysEquals(tblBase))
                {
                for (Enumeration enmr = tblDerived.keys(); enmr.hasMoreElements(); )
                    {
                    String sIface = (String) enmr.nextElement();
                    if (!tblBase.contains(sIface))
                        {
                        // get the interface that was added
                        Interface iface = (Interface) tblDerived.get(sIface);
                        // copy it to the delta component
                        tblDelta.put(sIface, new Interface(delta, iface));
                        }
                    }
                }

            // extract Dispatches
            tblBase    = base   .m_tblDispatches;
            tblDerived = derived.m_tblDispatches;
            tblDelta   = delta  .m_tblDispatches;
            if (!tblDerived.isEmpty() && !tblDerived.keysEquals(tblBase))
                {
                for (Enumeration enmr = tblDerived.keys(); enmr.hasMoreElements(); )
                    {
                    String sIface = (String) enmr.nextElement();
                    if (!tblBase.contains(sIface))
                        {
                        // get the interface that was added
                        Interface iface = (Interface) tblDerived.get(sIface);
                        // copy it to the delta component
                        tblDelta.put(sIface, new Interface(delta, iface));
                        }
                    }
                }
            }


        // ----- state

        {
        StringTable tblBase    = base.m_tblState;
        StringTable tblDerived = derived.matchProperties(base, false, (nDeltaMode == DERIVATION), errlist);
        StringTable tblDelta   = delta.m_tblState; // empty

        for (Enumeration enmr = tblDerived.keys(); enmr.hasMoreElements(); )
            {
            String   sProp       = (String) enmr.nextElement();
            Property propBase    = (Property) tblBase   .get(sProp);
            Property propDerived = (Property) tblDerived.get(sProp);
            Property propDelta   = null;

            if (propDerived != null &&
                    (   propDerived.getExists() == EXISTS_NOT
                     || propDerived.getExists() == EXISTS_DELETE))
                {
                continue;
                }

            if (propBase != null)
                {
                // only bring across those properties that are
                // considered to be complex property properties
                if (fComplex && !fBaseComplex)
                    {
                    if (!propBase.isComplexPropertyProperty())
                        {
                        propBase = null;
                        }
                    }
                // a derived component cannot "see" private constants or
                // properties that have all private accessors on the base
                // component
                else if (nDeltaMode == DERIVATION && !propBase.isDerivedBySub())
                    {
                    propBase = null;
                    }
                }

            if (propBase == null)
                {
                if (fComplex)
                    {
                    if (propDelta != null)
                        {
                        // complex properties must modify a base
                        logError(RESOLVE_PROPERTYORPHANED, WARNING, new Object[]
                                {propDelta.toString(), delta.toPathString()}, errlist);
                        }
                    }
                else if (nDeltaMode == DERIVATION && propDerived.getExists() != EXISTS_INSERT)
                    {
                    // the derived property is discarded
                    logError(EXTRACT_PROPERTYORPHANED, WARNING, new Object[]
                            {propDerived.toString(), derived.toPathString()}, errlist);
                    }
                else
                    {
                    // just copy the property derivation/modification to
                    // the delta component
                    propDelta = new Property(delta, propDerived);

                    // allow properties to initialize the extract process, this is
                    // specifically to allow properties to extract base components
                    // from any complex property values stored in the property
                    // note that this must be done with a loader that can load
                    // completely resolved components for the extraction performed
                    // against the complex property values
                    // see Property.java header comments related to complex property
                    // support
                    if (nDeltaMode == DERIVATION)
                        {
                        propDelta.initializeExtract(loader, errlist);
                        }
                    }
                }
            // only extract delta if the super property is not static
            else if (propDerived != null && !(nDeltaMode == DERIVATION && propBase.isStatic()))
                {
                // extract the delta
                propDelta = (Property) propDerived.extract(propBase, delta, loader, errlist);
                }

            if (propDelta != null)
                {
                tblDelta.put(sProp, propDelta);
                }
            }
        }


        // ----- behavior

        if (!fComplex)
            {
            StringTable tblBase    = base.m_tblBehavior;
            StringTable tblDerived = derived.matchBehaviors(base, false, (nDeltaMode == DERIVATION), errlist);
            StringTable tblDelta   = delta.m_tblBehavior; // empty

            for (Enumeration enmr = tblDerived.keys(); enmr.hasMoreElements(); )
                {
                String   sSig       = (String) enmr.nextElement();
                Behavior behBase    = (Behavior) tblBase   .get(sSig);
                Behavior behDerived = (Behavior) tblDerived.get(sSig);
                Behavior behDelta   = null;

                // a derived component cannot "see" private behaviors on the
                // base component, nor constructors on JCS
                if (behBase != null && nDeltaMode == DERIVATION
                        && (behBase.getAccess() == ACCESS_PRIVATE || behBase.isConstructor()))
                    {
                    behBase = null;
                    }

                if (behBase == null)
                    {
                    if (nDeltaMode == DERIVATION && behDerived.getExists() != EXISTS_INSERT)
                        {
                        // the derived behavior is discarded
                        logError(EXTRACT_BEHAVIORORPHANED, WARNING, new Object[]
                                {behDerived.toString(), derived.toPathString()}, errlist);
                        }
                    else
                        {
                        // just copy the behavior derivation/modification to
                        // the delta component
                        behDelta = new Behavior(delta, behDerived);
                        }
                    }
                // only extract delta if the base behavior is not final
                else if (behDerived != null && !behBase.isFinal())
                    {
                    // extract the delta
                    behDelta = (Behavior) behDerived.extract(behBase, delta, loader, errlist);
                    }

                if (behDelta != null)
                    {
                    tblDelta.put(sSig, behDelta);
                    }
                }
            }


        // ----- aggregation

        if (!fComplex)
            {
            // extract Categories
            StringTable tblBase    = base   .m_tblCategories;
            StringTable tblDerived = derived.m_tblCategories;
            StringTable tblDelta   = delta  .m_tblCategories;
            if (!tblDerived.isEmpty() && !tblDerived.keysEquals(tblBase))
                {
                Boolean TRUE = Boolean.TRUE;
                for (Enumeration enmr = tblDerived.keys(); enmr.hasMoreElements(); )
                    {
                    String sCat = (String) enmr.nextElement();
                    if (!tblBase.contains(sCat))
                        {
                        tblDelta.put(sCat, TRUE);
                        }
                    }
                }

            // extract the child Component derivations/modifications;
            // at the derivation level, all children are either extracted as
            // derivations or discarded; at modification levels, matching
            // children between the base and derived components are extracted
            // as modifications, but non matching children are copied,
            // potentially remaining derivations (which implies that they
            // were added at a modification level)
            tblBase    = base.m_tblChildren;
            tblDerived = derived.matchChildren(base, false, errlist);
            tblDelta   = delta.m_tblChildren; // empty
            for (Enumeration enmr = tblDerived.keys(); enmr.hasMoreElements(); )
                {
                String    sChild    = (String) enmr.nextElement();
                Component cdBase    = (Component) tblBase   .get(sChild);
                Component cdDerived = (Component) tblDerived.get(sChild);
                Component cdDelta   = null;

                // if the derived is an insert child (i.e. no base child)
                // then extract the global component from which it derives;
                if (cdBase == null && nDeltaMode == DERIVATION
                        && cdDerived.getExists() == EXISTS_INSERT)
                    {
                    // load the global super (which may throw an exception)
                    cdBase = loader.loadComponent(cdDerived.getSuperName(), true, errlist);

                    if (cdBase == null)
                        {
                        // global Component does not exist
                        logError(EXTRACT_CHILDORPHANED, WARNING, new Object[]
                            {cdDerived.toString(), derived.toPathString()}, errlist);
                        }
                    else if (cdBase.isFinal())
                        {
                        // cannot derive from final global Component
                        logError(EXTRACT_GLOBALFINAL, WARNING, new Object[]
                            {cdBase.toString(), derived.toPathString()}, errlist);
                        cdBase = null;
                        }
                    else if (!derived.isChildSuperLegal(cdBase.getQualifiedName()))
                        {
                        // base Component doesn't fit a category
                        // (aka illegal super)
                        logError(EXTRACT_NOCATEGORY, WARNING, new Object[]
                            {cdDerived.toString(), cdBase.toString(), derived.toPathString()},
                            errlist);
                        cdBase = null;
                        }
                    }

                if (cdBase == null)
                    {
                    if (nDeltaMode == DERIVATION)
                        {
                        // the derived child is discarded
                        logError(EXTRACT_CHILDORPHANED, WARNING, new Object[]
                            {cdDerived.toString(), derived.toPathString()}, errlist);
                        }
                    else
                        {
                        // just copy the child derivation/modification to the
                        // delta component
                        cdDelta = new Component(delta, cdDerived);
                        }
                    }
                else
                    {
                    if (cdDerived != null)
                        {
                        // check if base child is final and if so discard
                        // the derived child
                        int nBaseChildFlags = cdBase.m_nFlags;
                        if ((nBaseChildFlags & DERIVE_MASK) == DERIVE_FINAL &&
                            (cdBase.getMode() == RESOLVED || (nBaseChildFlags & DERIVE_SPECIFIED) != 0))
                            {
                            // this is not an error on extract, although it
                            // is possible that information is being lost;
                            // when a final child exists, it is resolved
                            // with a null derivation, so when it extracts
                            // it will be a null derivation (assuming no
                            // concurrency/transactional issues)
                            cdDerived = null;
                            }
                        // discard all derivations and modifications from
                        // DELETEs and NOTs
                        else if ((nBaseChildFlags & EXISTS_MASK) == EXISTS_NOT ||
                                 (nBaseChildFlags & EXISTS_MASK) == EXISTS_DELETE)
                            {
                            // for the reasons above, this is not an error
                            cdDerived = null;
                            }
                        }

                    if (cdDerived != null)
                        {
                        // extract the delta
                        cdDelta = (Component) cdDerived.extract(cdBase, delta, loader, errlist);
                        }
                    }

                if (cdDelta != null)
                    {
                    tblDelta.put(sChild, cdDelta);
                    }
                }
            }

        return delta;
        }

    /**
    * Finalize the extract process.  This means that this trait will not
    * be asked to extract again.  A trait is considered persistable after
    * it has finalized the extract process.
    *
    * @param loader    the Loader object for JCS, CIM, and CD dependencies
    * @param errlist   the error list object to log error information to
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    public synchronized void finalizeExtract(Loader loader, ErrorList errlist)
            throws ComponentException
        {
        // root component (at base level) does not finalize extraction
        if (getMode() == RESOLVED && m_sSuper == BLANK)
            {
            return;
            }

        // gg: 2002.02.13 Clear UID for global components
        if (isGlobal())
            {
            clearUID();
            }

        super.finalizeExtract(loader, errlist);

        // extract sub-traits:  Integration
        if (m_integration != null)
            {
            m_integration.finalizeExtract(loader, errlist);
            }

        // resolve sub-traits:  Implements
        for (Enumeration enmr = m_tblImplements.elements(); enmr.hasMoreElements(); )
            {
            Interface iface = (Interface) enmr.nextElement();
            iface.finalizeExtract(loader, errlist);
            }

        // resolve sub-traits:  Dispatches
        for (Enumeration enmr = m_tblDispatches.elements(); enmr.hasMoreElements(); )
            {
            Interface iface = (Interface) enmr.nextElement();
            iface.finalizeExtract(loader, errlist);
            }

        // resolve sub-traits:  Properties
        for (Enumeration enmr = m_tblState.elements(); enmr.hasMoreElements(); )
            {
            Property prop = (Property) enmr.nextElement();
            prop.finalizeExtract(loader, errlist);
            }

        // resolve sub-traits:  Behaviors
        for (Enumeration enmr = m_tblBehavior.elements(); enmr.hasMoreElements(); )
            {
            Behavior behavior = (Behavior) enmr.nextElement();
            behavior.finalizeExtract(loader, errlist);
            }

        // resolve sub-traits:  Children
        for (Enumeration enmr = m_tblChildren.elements(); enmr.hasMoreElements(); )
            {
            Component cd = (Component) enmr.nextElement();
            cd.finalizeExtract(loader, errlist);
            }

        // discard blank Implements
        for (Enumeration enmr = m_tblImplements.elements(); enmr.hasMoreElements(); )
            {
            Interface iface = (Interface) enmr.nextElement();
            if (iface.isDiscardable())
                {
                m_tblImplements.remove(iface.getUniqueName());
                iface.invalidate();
                }
            }

        // discard blank Dispatches
        for (Enumeration enmr = m_tblDispatches.elements(); enmr.hasMoreElements(); )
            {
            Interface iface = (Interface) enmr.nextElement();
            if (iface.isDiscardable())
                {
                m_tblDispatches.remove(iface.getUniqueName());
                iface.invalidate();
                }
            }

        // discard blank Properties
        for (Enumeration enmr = m_tblState.elements(); enmr.hasMoreElements(); )
            {
            Property prop = (Property) enmr.nextElement();
            if (prop.isDiscardable())
                {
                m_tblState.remove(prop.getUniqueName());
                prop.invalidate();
                }
            }

        // discard blank Behaviors
        for (Enumeration enmr = m_tblBehavior.elements(); enmr.hasMoreElements(); )
            {
            Behavior behavior = (Behavior) enmr.nextElement();
            if (behavior.isDiscardable())
                {
                m_tblBehavior.remove(behavior.getUniqueName());
                behavior.invalidate();
                }
            }

        // discard blank Children
        for (Enumeration enmr = m_tblChildren.elements(); enmr.hasMoreElements(); )
            {
            Component cd = (Component) enmr.nextElement();
            if (cd.isDiscardable())
                {
                m_tblChildren.remove(cd.getUniqueName());
                cd.invalidate();
                }
            }
        }

    /**
    * Match up the Properties from the base and this Component.  Return a
    * StringTable of Properties to resolve or extract from the base.  The
    * returned StringTable must not be modified.
    *
    * The returned table may have null values which imply null derivations.
    *
    * The returned table may have additional Properties that were added by
    * the delta or derived Component.
    *
    * (Neither the base nor this Component are modified by this method.)
    *
    * @param base      the super or base level Component
    * @param fResolve  true if being RESOLVED
    * @param fDerive   true if extracting/resolving a DERIVATION
    * @param errlist   the error list to log any mismatches to
    *
    * @exception DerivationException
    */
    protected StringTable matchProperties(Component base, boolean fResolve, boolean fDerive, ErrorList errlist)
            throws DerivationException
        {
        StringTable tblThis   = this.m_tblState;
        StringTable tblBase   = base.m_tblState;
        StringTable tblResult = tblThis;

        // most common case for extract:  exact match
        if (tblThis.keysEquals(tblBase))
            {
            return tblResult;
            }

        // create results table (since we cannot change the "this" table);
        tblResult = new StringTable();

        // most common for resolve:  no properties in the delta
        if (tblThis.isEmpty())
            {
            // copy keys from base (but leave Properties null to signify null
            // derivations)
            for (Enumeration enmr = tblBase.elements(); enmr.hasMoreElements(); )
                {
                Property prop = (Property) enmr.nextElement();
                if (!fDerive || prop.isDerivedBySub())
                    {
                    tblResult.put(prop.getUniqueName(), null);
                    }
                }
            return tblResult;
            }

        // build look up tables by UID and unique name
        // this extended processing will only happen at levels which add,
        // remove, or modify Properties (considered to be rare)
        Hashtable   tblByUID  = new Hashtable();
        StringTable tblByName = new StringTable();
        for (Enumeration enmr = tblThis.elements(); enmr.hasMoreElements(); )
            {
            Property prop = (Property) enmr.nextElement();
            UID uid = prop.getUID();
            if (uid != null)
                {
                tblByUID.put(uid, prop);
                }
            tblByName.put(prop.getUniqueName(), prop);
            }

        // look up by UID; remove each one found from the name look up table
        if (!tblByUID.isEmpty())
            {
            for (Enumeration enmr = tblBase.elements(); enmr.hasMoreElements(); )
                {
                Property propBase = (Property) enmr.nextElement();
                UID uid = propBase.getUID();
                if (uid != null)
                    {
                    Property propThis = (Property) tblByUID.get(uid);
                    if (propThis != null && (!fDerive || propBase.isDerivedBySub()))
                        {
                        tblResult.put(propBase.getUniqueName(), propThis);
                        tblByName.remove(propThis.getUniqueName());
                        }
                    }
                }
            }

        // look up by name
        for (Enumeration enmr = tblBase.elements(); enmr.hasMoreElements(); )
            {
            Property propBase = (Property) enmr.nextElement();
            if (!fDerive || propBase.isDerivedBySub())
                {
                String sProp = propBase.getUniqueName();
                if (!tblResult.contains(sProp))
                    {
                    // find the Property on this Component with the same name;
                    // whether or not one exists, reserve the name in the result
                    Property propThis = (Property) tblByName.get(sProp);
                    tblResult.put(sProp, propThis);

                    // only leave unmatched Properties in tblByName
                    // (so they can be added to the results or discarded)
                    if (propThis != null)
                        {
                        tblByName.remove(sProp);
                        }
                    }
                }
            }

        // keep additional Properties (but discard any conflicting ones)
        for (Enumeration enmr = tblByName.elements(); enmr.hasMoreElements(); )
            {
            Property prop  = (Property) enmr.nextElement();
            String   sProp = prop.getUniqueName();

            // check for conflict (this happens when a base Property is
            // renamed which conflicts if another Property was added at
            // this level and coincidentally has the same name)
            if (tblResult.contains(sProp))
                {
                String sCode = (fResolve ? RESOLVE_PROPERTYDISCARDED
                                         : EXTRACT_PROPERTYDISCARDED);
                logError(sCode, WARNING, new Object[] {sProp, toPathString()}, errlist);
                }
            else
                {
                tblResult.put(sProp, prop);
                }
            }

        return tblResult;
        }

    /**
    * Match up the behaviors from the base and this Component.  Return a
    * StringTable of behaviors to resolve or extract from the base.  The
    * returned StringTable must not be modified.
    *
    * The returned table may have null values which imply null derivations.
    *
    * The returned table may have additional behaviors that were added by
    * the delta or derived Component.
    *
    * (Neither the base nor this Component are modified by this method.)
    *
    * @param base      the super or base level Component
    * @param fResolve  true if being RESOLVED
    * @param fDerive   true if extracting/resolving a DERIVATION
    * @param errlist   the error list to log any mismatches to
    *
    * @return a const StringTable mapping ...
    *
    * @exception DerivationException
    */
    protected StringTable matchBehaviors(Component base, boolean fResolve, boolean fDerive, ErrorList errlist)
            throws DerivationException
        {
        StringTable tblThis   = this.m_tblBehavior;
        StringTable tblBase   = base.m_tblBehavior;
        StringTable tblResult = tblThis;

        // most common case for extract:  exact match
        if (tblThis.keysEquals(tblBase))
            {
            return tblResult;
            }

        // create results table (since we cannot change the "this" table);
        tblResult = new StringTable();

        // most common for resolve:  no behaviors in the delta
        if (tblThis.isEmpty())
            {
            // copy keys from base (but leave behaviors null to signify null
            // derivations)
            for (Enumeration enmr = tblBase.elements(); enmr.hasMoreElements(); )
                {
                Behavior behavior = (Behavior) enmr.nextElement();
                if (!(fDerive
                      && (behavior.getAccess() == ACCESS_PRIVATE
                          || behavior.isConstructor())))
                    {
                    tblResult.put(behavior.getUniqueName(), null);
                    }
                }
            return tblResult;
            }

        // build look up tables by UID and unique name (aka signature)
        // this extended processing will only happen at levels which add,
        // remove, or modify behaviors (considered to be rare)
        Hashtable   tblByUID  = new Hashtable();
        StringTable tblByName = new StringTable();

        for (Enumeration enmr = tblThis.elements(); enmr.hasMoreElements(); )
            {
            Behavior behavior = (Behavior) enmr.nextElement();
            UID uid = behavior.getUID();
            if (uid != null)
                {
                tblByUID.put(uid, behavior);
                }
            tblByName.put(behavior.getUniqueName(), behavior);
            }

        // look up by UID; remove each one found from the name look up table
        if (!tblByUID.isEmpty())
            {
            for (Enumeration enmr = tblBase.elements(); enmr.hasMoreElements(); )
                {
                Behavior behBase = (Behavior) enmr.nextElement();
                UID uid = behBase.getUID();
                if (uid != null)
                    {
                    Behavior behThis = (Behavior) tblByUID.get(uid);
                    if (behThis != null
                        && !(fDerive
                             && (behBase.getAccess() == ACCESS_PRIVATE
                                 || behBase.isConstructor())))
                        {
                        tblResult.put(behBase.getUniqueName(), behThis);
                        tblByName.remove(behThis.getUniqueName());
                        }
                    }
                }
            }

        // look up by name
        for (Enumeration enmr = tblBase.elements(); enmr.hasMoreElements(); )
            {
            Behavior behBase = (Behavior) enmr.nextElement();
            if (!(fDerive
                  && (behBase.getAccess() == ACCESS_PRIVATE
                      || behBase.isConstructor())))
                {
                String sSig = behBase.getUniqueName();
                if (!tblResult.contains(sSig))
                    {
                    // find the Behavior on this Component with the same name;
                    // whether or not one exists, reserve the name in the result
                    Behavior behThis = (Behavior) tblByName.get(sSig);
                    tblResult.put(sSig, behThis);

                    // only leave unmatched Behaviors in tblByName
                    // (so they can be added to the results or discarded)
                    if (behThis != null)
                        {
                        tblByName.remove(sSig);
                        }
                    }
                }
            }

        // keep additional Behaviors (but discard any conflicting ones)
        for (Enumeration enmr = tblByName.elements(); enmr.hasMoreElements(); )
            {
            Behavior behavior = (Behavior) enmr.nextElement();
            String   sSig     = behavior.getUniqueName();

            // check for conflict (this happens when a base Behavior is
            // renamed which conflicts if another Behavior was added at
            // this level and coincidentally has the same name)
            if (tblResult.contains(sSig))
                {
                String sCode = (fResolve ? RESOLVE_BEHAVIORDISCARDED
                                         : EXTRACT_BEHAVIORDISCARDED);
                logError(sCode, WARNING, new Object[] {sSig, toPathString()}, errlist);
                }
            else
                {
                tblResult.put(sSig, behavior);
                }
            }

        return tblResult;
        }

    /**
    * Match up the children from the base and this Component.  Return a
    * StringTable of children to resolve or extract from the base.  The
    * returned StringTable must not be modified.
    *
    * The returned table may have null values which imply null derivations.
    *
    * The returned table may have additional children that were added by
    * the delta or derived Component.
    *
    * (Neither the base nor this Component are modified by this method.)
    *
    * @param base      the super or base level Component
    * @param fResolve  true if being RESOLVED
    * @param errlist   the error list to log any mismatches to
    *
    * @exception DerivationException
    */
    protected StringTable matchChildren(Component base, boolean fResolve, ErrorList errlist)
            throws DerivationException
        {
        StringTable tblThis   = this.m_tblChildren;
        StringTable tblBase   = base.m_tblChildren;
        StringTable tblResult = tblThis;

        // most common case overall:  no children (i.e. "leaf nodes")
        // most common case for extract:  exact match
        if (tblThis.keysEquals(tblBase))
            {
            return tblResult;
            }

        // most common for resolve:  no children in the delta
        if (tblThis.isEmpty())
            {
            // copy keys from base (but leave children null to signify null
            // derivations)
            tblResult = (StringTable) tblBase.clone();
            for (Enumeration enmr = tblResult.keys(); enmr.hasMoreElements(); )
                {
                tblResult.put((String) enmr.nextElement(), null);
                }
            return tblResult;
            }

        // create results table (since we cannot change the "this" table);
        // this extended processing will only happen at levels which add,
        // remove, or modify children (considered to be rare)
        tblResult = new StringTable();

        // build look up tables by UID and name
        Hashtable   tblByUID  = new Hashtable();
        StringTable tblByName = new StringTable();
        for (Enumeration enmr = tblThis.elements(); enmr.hasMoreElements(); )
            {
            Component cd = (Component) enmr.nextElement();
            UID uid = cd.getUID();
            if (uid != null)
                {
                tblByUID.put(uid, cd);
                }
            tblByName.put(cd.getUniqueName(), cd);
            }

        // look up by UID; remove each one found from the name look up table
        if (!tblByUID.isEmpty())
            {
            for (Enumeration enmr = tblBase.elements(); enmr.hasMoreElements(); )
                {
                Component cdBase = (Component) enmr.nextElement();
                UID uid = cdBase.getUID();
                if (uid != null)
                    {
                    Component cdThis = (Component) tblByUID.get(uid);
                    if (cdThis != null)
                        {
                        tblResult.put(cdBase.getUniqueName(), cdThis);
                        tblByName.remove(cdThis.getUniqueName());
                        }
                    }
                }
            }

        // look up by name
        for (Enumeration enmr = tblBase.keys(); enmr.hasMoreElements(); )
            {
            String sChild = (String) enmr.nextElement();
            if (!tblResult.contains(sChild))
                {
                // find the Component on this behavior with the same name;
                // whether or not one exists, reserve the name in the result
                Component cd = (Component) tblByName.get(sChild);
                tblResult.put(sChild, cd);

                // only leave unmatched Components in tblByName
                // (so they can be added to the results or discarded)
                if (cd != null)
                    {
                    tblByName.remove(sChild);
                    }
                }
            }

        // keep additional Components (but discard any conflicting ones)
        for (Enumeration enmr = tblByName.elements(); enmr.hasMoreElements(); )
            {
            Component cd = (Component) enmr.nextElement();
            String sChild = cd.getUniqueName();

            // check for conflict (this happens when a base Component is
            // renamed which conflicts if another Component was added at
            // this level and coincidentally has the same name)
            if (tblResult.contains(sChild))
                {
                String sCode = (fResolve ? RESOLVE_CHILDDISCARDED
                                         : EXTRACT_CHILDDISCARDED);
                logError(sCode, WARNING, new Object[] {sChild, toPathString()}, errlist);
                }
            else
                {
                tblResult.put(sChild, cd);
                }
            }

        return tblResult;
        }

    /**
    * Determine if the trait can be discarded.  This has two uses:
    * <ul>
    * <li> Determining if a resolved trait should not exist; for example
    *      a trait without an origin
    * <li> An extracted trait that contains no information (i.e. a "null
    *      derivation") is often discardable
    * </ul>
    * For a resolved trait, this request is only legal after the
    * finalizeResolve call.  Likewise, for a derivation/modification
    * trait, this request is only legal after the finalizeExtract call.
    *
    * @return true if the trait is discardable
    */
    protected boolean isDiscardable()
        {
        int nMode = getMode();
        switch (nMode)
            {
            case RESOLVED:
                // a resolved component is never discardable
                return false;

            case DERIVATION:
            case MODIFICATION:
                // check for delta information
                if (   (m_nFlags & ALL_SPECIFIED) != 0              // any attributes
                    || (!isComplex() && getParent() == null
                        && nMode == DERIVATION)                     // global derivation
                    || (m_integration != null
                        && !m_integration.isDiscardable())          // integration
                    || !m_tblImplements.isEmpty()                   // added interface
                    || !m_tblDispatches.isEmpty() )                 // added event
                    {
                    return false;
                    }
                break;
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
        Component that = (Component) base;
        if (that == null)
            {
            return false;
            }

        if (isGlobal())
            {
            // global components are never optimized out
            return false;
            }
        else if (isStatic())
            {
            // cp 2000.04.03 static children are not discardable if they or a
            // parent of theirs is an insert; this ensures that a static child
            // has its correct "full local identity" available by parsing its
            // getClass().getName(); this is intended for situations in which
            // the child is instantiated by something other than its parent,
            // e.g. JSP tags
            Component cd = getParent();
            while (cd != null)
                {
                if ((cd.m_nFlags & EXISTS_MASK) == EXISTS_INSERT)
                    {
                    return false;
                    }

                cd = cd.getParent();
                }
            }

        // existence
        int nExistsThis = (this.m_nFlags & EXISTS_MASK);
        int nExistsThat = (that.m_nFlags & EXISTS_MASK);
        if (nExistsThis != nExistsThat)
            {
            boolean fThisExists = (nExistsThis == EXISTS_INSERT || nExistsThis == EXISTS_UPDATE);
            boolean fThatExists = (nExistsThat == EXISTS_INSERT || nExistsThat == EXISTS_UPDATE);
            if (fThisExists != fThatExists)
                {
                return false;
                }
            }

        // interfaces
        if ((this.m_integration != null
             && !this.m_integration.isDiscardable())
            || !this.m_tblImplements.keysEquals(that.m_tblImplements)
            || !this.m_tblDispatches.keysEquals(that.m_tblDispatches))
            {
            return false;
            }

        // attributes
        if (((this.m_nFlags ^ that.m_nFlags) & CLASSGEN_FLAGS) != 0)
            {
            return false;
            }

        // properties
        StringTable tblStateThis = this.m_tblState;
        StringTable tblStateThat = that.m_tblState;
        for (Enumeration enmr = tblStateThis.keys(); enmr.hasMoreElements(); )
            {
            String s = (String) enmr.nextElement();
            Property propThis = (Property) tblStateThis.get(s);
            Property propThat = (Property) tblStateThat.get(s);
            if (!propThis.isClassDiscardable(propThat))
                {
                return false;
                }
            }

        // behavior
        StringTable tblBehaviorThis = this.m_tblBehavior;
        StringTable tblBehaviorThat = that.m_tblBehavior;
        for (Enumeration enmr = tblBehaviorThis.keys(); enmr.hasMoreElements(); )
            {
            String s = (String) enmr.nextElement();
            Behavior behThis = (Behavior) tblBehaviorThis.get(s);
            Behavior behThat = (Behavior) tblBehaviorThat.get(s);
            if (!behThis.isClassDiscardable(behThat))
                {
                return false;
                }
            }

        // categories
        if (!this.m_tblCategories.keysEquals(that.m_tblCategories))
            {
            return false;
            }

        // note:  children are not checked; it is implicit that if a child
        // is not discardable, then neither is its parent

        return super.isClassDiscardable(base);
        }


    // ----- miscellaneous Trait methods ------------------------------------

    /**
    * Determine if the component is modifiable.  Components cannot be
    * modified if the super component (for derivation) or base (for
    * modification) is final.  Signatures are only modifiable during
    * resolution.
    *
    * @return true if changes are permitted to the component
    */
    public boolean isModifiable()
        {
        if (m_fReadOnly)
            {
            return false;
            }

        if (m_nType == COMPONENT && (m_nPrevFlags & DERIVE_MASK) != DERIVE_DERIVABLE)
            {
            return false;
            }

        return super.isModifiable();
        }

    /**
    * Mark the component as read-only or modifiable.  Marking the component
    * as modifiable may not make it modifiable, since other reasons may exist
    * that prevent it from being modifiable.
    *
    * @param fModifiable  true to make the component modifiable, false to
    *        make the component read-only
    */
    protected void setModifiable(boolean fModifiable)
        {
        m_fReadOnly = !fModifiable;
        }

    /**
    * Determine all traits contained by this Component.
    *
    * @return the an enumeration of traits contained by this trait
    */
    protected Enumeration getSubTraits()
        {
        ChainedEnumerator enmr = new ChainedEnumerator();

        if (m_integration != null)
            {
            enmr.addEnumeration(new SimpleEnumerator(new Object[] {m_integration}));
            }
        enmr.addEnumeration(m_tblImplements.elements());
        enmr.addEnumeration(m_tblDispatches.elements());
        enmr.addEnumeration(m_tblState     .elements());
        enmr.addEnumeration(m_tblBehavior  .elements());
        enmr.addEnumeration(m_tblChildren  .elements());

        return enmr;
        }

    /**
    * Reset state, discarding all information.
    */
    protected synchronized void invalidate()
        {
        super.invalidate();

        m_sName         = null;
        m_sSuper        = null;
        m_integration   = null;
        m_tblImplements = null;
        m_tblDispatches = null;
        m_tblBehavior   = null;
        m_tblState      = null;
        m_tblCategories = null;
        m_tblChildren   = null;
        }

    /**
    * The Component's name is its unique string identifier (i.e. corresponds
    * to its UID).
    *
    * @return the Component name
    */
    protected String getUniqueName()
        {
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
        return DESCRIPTORS[m_nType] + ' ' + getUniqueName();
        }

    /**
    * Determine whether the trait was added at this level or at a previous
    * level.  This is equivalent to "isNotFromSuperOrBase", whether or not
    * additional origins (manual, traits) exist at this level.
    *
    * @return true if the trait was added at this level
    */
    public boolean isDeclaredAtThisLevel()
        {
        Trait trait = getParentTrait();
        if (trait != null && trait instanceof Component)
            {
            // child of a component is "declared at this level"
            // if it was inserted at this level
            return getExists() == EXISTS_INSERT && m_fBaseLevel;
            }
        else
            {
            return super.isDeclaredAtThisLevel();
            }
        }

    /**
    * Determine if the trait existed at a base level.  This means that the
    * trait is the result of modification (but not the result of derivation).
    *
    * @return true if the trait existed at a base level (but did not exist
    *         at a super level)
    */
    public boolean isFromBase()
        {
        Trait trait = getParentTrait();
        if (trait != null && trait instanceof Component)
            {
            // child of a component is "from base" if it was inserted
            // at a base level
            return getExists() == EXISTS_INSERT && !m_fBaseLevel;
            }
        else
            {
            return super.isFromBase();
            }
        }

    /**
    * Determine if the trait existed at a super level.  This means that
    * the trait is the result of derivation.
    *
    * @return true if the trait existed at a super level
    */
    public boolean isFromSuper()
        {
        Trait trait = getParentTrait();
        if (trait != null && trait instanceof Component)
            {
            // child of a component is "from super" if it is not an insert
            return getExists() != EXISTS_INSERT;
            }
        else
            {
            return super.isFromSuper();
            }
        }


    // ----- component accessors:  identity ---------------------------------

    // ----- categorization

    /**
    * Determine if this is a Component Definition.  This class (Component)
    * is used for Component Definitions, Complex Property values, and Java
    * Class Signatures.
    *
    * @return true if this is a Component Definition
    */
    public boolean isComponent()
        {
        return m_nType == COMPONENT;
        }

    /**
    * Determine if the Component is a global (not contained) Component.
    *
    * @return true if the Component is global
    */
    public boolean isGlobal()
        {
        return getParentTrait() == null;
        }

    /**
    * Get the Component parent of this Component Definition.
    *
    * @return this Component Definition's parent or null if this Component
    *         Definition is not aggregated
    *
    * @exception ClassCastException if the component is a complex property
    */
    public Component getParent()
        {
        return (Component) getParentTrait();
        }

    /**
    * Get the topmost (global) Component parent of this Component Definition.
    *
    * @return the topmost Component Definition's parent
    *
    * @exception ClassCastException if the component is a complex property
    */
    public Component getGlobalParent()
        {
        Component cdParent = this;

        while (!cdParent.isGlobal())
            {
            cdParent = cdParent.getParent();
            }
        return cdParent;
        }

    /**
    * Determine if this is a complex Property value.  This class (Component)
    * is used for Component Definitions, complex Property values, and Java
    * Class Signatures.
    *
    * @return true if this is a Component Definition
    */
    public boolean isComplex()
        {
        return m_nType == COMPLEX;
        }

    /**
    * Get the Property parent of this complex property value.
    *
    * @return the Property which contains this complex property value
    *
    * @exception ClassCastException if the component is a Component Definition
    */
    public Property getComplex()
        {
        return (Property) getParentTrait();
        }

    /**
    * Determine if this is a Java Class Signature (JCS).  This class
    * (Component) is used for Component Definitions, complex Property
    * values, and Java Class Signatures.
    *
    * @return true if this is a JCS
    */
    public boolean isSignature()
        {
        return m_nType == SIGNATURE;
        }

    /**
    * Determine if this is a Java Class Signature (JCS) that represents a
    * Java class.
    *
    * @return true if this is a JCS for a Java class
    */
    public boolean isClass()
        {
        return m_nType == SIGNATURE && (m_nFlags & MISC_ISINTERFACE) == 0;
        }

    /**
    * Determine if this is a Java Class Signature (JCS) that represents a
    * Java interface.
    *
    * @return true if this is a JCS for a Java interface
    */
    public boolean isInterface()
        {
        return m_nType == SIGNATURE && (m_nFlags & MISC_ISINTERFACE) != 0;
        }

    /**
    * Determine if this is a Java Class Signature (JCS) that represents a
    * class deriving from java.lang.Throwable.
    *
    * @return true if this is a throwable JCS
    */
    public boolean isThrowable()
        {
        return m_nType == SIGNATURE && (m_nFlags & MISC_ISTHROWABLE) != 0;
        }


    // ----- name

    /**
    * Get the simple Component Definition name.
    *
    * @return the name (not fully qualified) of the Component Definition
    */
    public String getName()
        {
        return m_sName;
        }

    /**
    * Get fully qualified name.
    *
    * @return fully qualified name of the Component, Complex property,
    *         or Java Class Signature
    */
    public String getQualifiedName()
        {
        String sName = m_sName;
        if (isComponent())
            {
            Component cdParent = getParent();
            if (cdParent != null)
                {
                return new StringBuffer()
                        .append(cdParent.getQualifiedName())
                        .append(LOCAL_ID_DELIM)
                        .append(sName)
                        .toString();
                }

            String sSuper = m_sSuper;
            if (sSuper.length() > 0)
                {
                // JavaC BUG!
                // return sSuper + GLOBAL_ID_DELIM + sName;
                return new StringBuffer()
                        .append(sSuper)
                        .append(GLOBAL_ID_DELIM)
                        .append(sName)
                        .toString();
                }
            }

        return sName;
        }

    /**
    * Get the local portion of this component name.  For example,
    * if the qualified name is X.Y$A$B, the local portion is A$B.
    *
    * @return the local portion of the component name, or null if the
    *         component name is global
    */
    public String getLocalName()
        {
        return getLocalName(getQualifiedName());
        }

    /**
    * Get the name of the super Component of this component
    *
    * @return fully qualified name of the super Component
    */
    public String getSuperName()
        {
        // child components not inserted at this level have a super component
        // which is the same-named child on the parent's super
        if (isComponent() && getParent() != null && getExists() != EXISTS_INSERT)
            {
            return new StringBuffer()
                    .append(getParent().getSuperName())
                    .append(LOCAL_ID_DELIM)
                    .append(m_sName)
                    .toString();
            }

        return m_sSuper;
        }

    /**
    * Get the global super Component Definition name.
    *
    * @return fully qualified name of the global super Component
    */
    public String getGlobalSuperName()
        {
        return m_sSuper;
        }

    /**
    * Checks whether this component or any of its children
    * derive from the global component with a specified name
    *
    * @param sName name of the "impacted by" component
    *
    * @return true if the component itself or one of its children derive
    *         from the specified global component; false otherwise
    */
    public boolean isImpactedBy(String sName)
        {
        if (isDerivedFrom(getGlobalSuperName(), sName))
            {
            return true;
            }

        String[] asChildren = getChildren();
        for (int i = 0; i < asChildren.length; i++)
            {
            Component child = getChild(asChildren[i]);
            if (child != null && child.isImpactedBy(sName))
                {
                return true;
                }
            }

        return false;
        }

    /**
    * Determine if the Component name can be set.  The name cannot be
    * changed if the Component is the result of a modification or if
    * the Component is a child not inserted at this level.
    *
    * @return true if the Component name can be set
    */
    public boolean isNameSettable()
        {
        // component must be modifiable
        if (!isComponent() || !isModifiable())
            {
            return false;
            }

        if (getParent() != null)
            {
            // child must have been added at this level
            return isDeclaredAtThisLevel();
            }

        // for global cd's name to change the component
        // must not be result of modification
        return m_fBaseLevel;
        }

    /**
    * Determine if the specified Component name is legal.
    *
    * @return true if the specified name is legal
    */
    public boolean isNameLegal(String sName)
        {
        if (isComponent())
            {
            Component cdParent = getParent();
            if (cdParent == null)
                {
                return isSimpleNameLegal(sName, true)
                    && m_sSuper != BLANK;              // cannot rename root
                }
            else
                {
                return isSimpleNameLegal(sName, false)
                    && (sName.equalsIgnoreCase(m_sName) // allow case change
                     || !cdParent.isChild(sName));      // check if the name is already taken
                }
            }

        return false;
        }

    /**
    * Set the Component name.
    *
    * @param sName the new name for the Component
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
    * Set the Component name.
    *
    * @param sName      the new name for the Component
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
        if (getParent() != null)
            {
            getParent().renameChild(sPrev, sName);
            }

        firePropertyChange(ATTR_NAME, sPrev, sName);
        }


    // ----- Exists

    /**
    * Get the exists flag for this Component.
    *
    * @return the exists flag for this Component
    */
    protected int getExists()
        {
        return m_nFlags & EXISTS_MASK;
        }

    /**
    * Set the exists flag for this Component.
    *
    * @param nExists  the exists flag for this Component
    */
    protected void setExists(int nExists)
        {
        m_nFlags = m_nFlags & ~EXISTS_MASK | nExists;
        }


    // ----- attribute:  Visibility

    /**
    * Get the component visibility attribute.
    *
    * @return the value of the visbility attribute as defined by
    *         Constants
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
    * Determine whether the Visible attribute of the Component Definition
    * can be set.
    *
    * @return true if the Visible attribute can set with a valid value
    */
    public boolean isVisibleSettable()
        {
        return isComponent() && isModifiable();
        }

    /**
    * Determine whether the Visible attribute of the Component Definition can
    * be set to the specified value.  The Visible attribute of the component
    * is flexible.
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
    * Set the visible attribute of the Component.
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
    * Set the visible attribute of the Component.
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


    // ----- Access

    /**
    * Determine the current accessibility of the component/class signature.
    * Components are always public.
    *
    * @return one of ACCESS_PUBLIC, ACCESS_PROTECTED, ACCESS_PRIVATE, or
    *         ACCESS_PACKAGE
    */
    public int getAccess()
        {
        return m_nType == SIGNATURE ? m_nFlags & ACCESS_MASK : ACCESS_PUBLIC;
        }


    // ----- attribute:  Static

    /**
    * Get the component scope attribute.
    *
    * @return true if the component is static (single instance)
    */
    public boolean isStatic()
        {
        return (m_nFlags & SCOPE_MASK) == SCOPE_STATIC;
        }

    /**
    * Determine whether the scope attribute of the Component Definition can
    * be set.  Since the scope attribute is expressed as a boolean, this
    * method doubles for both the "is settable" and "is legal" questions.
    * For global components, the scope attribute of the component is one-way
    * from insance to static.  A static global component is a singleton,
    * which means that one and only one will exist.  If a global component
    * is marked as static, all components under it will inherit that static
    * attribute, and only one component in that tree will be instantiable
    * at runtime.
    *
    * A local component that is marked as static is not auto-instantiated.
    * That allows a local component to be used for several very convenient
    * types of solutions, such as being designed to be the result of a
    * "factory".  Consider a "tree" data structure that contains a type
    * of "node".  There will be zero or more nodes, but those nodes may
    * only be applicable within the design of the tree.  The solution is
    * that the tree is a global component and the node is a static child
    * thereof.  By implementing a factory method (which will eventually
    * be autogenerated) the tree can virtually instantiate the node
    * component any number of times, and the design of the node is contained
    * inside the design of the tree, helping to keep things neat.
    *
    * @return true if the scope attribute can set
    */
    public boolean isStaticSettable()
        {
        if (isComponent() && isModifiable())
            {
            // cp 2001.03.30 allow child to be changed from instance to
            //               static at the insert level only; do not allow
            //               change from static to instance if the super or
            //               base is static
            if (getParent() == null || isDeclaredAtThisLevel())
                {
                return (m_nPrevFlags & SCOPE_MASK) == SCOPE_INSTANCE;
                }
            }

        return false;
        }

    /**
    * Set the scope attribute of the Component.
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
    * Set the scope attribute of the Component.
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


    // ----- attribute:  Abstract

    /**
    * Get the component implementation attribute.
    *
    * @return true if the component is abstract
    */
    public boolean isAbstract()
        {
        return (m_nFlags & IMPL_MASK) == IMPL_ABSTRACT;
        }

    /**
    * Determine whether the implementation attribute of the Component can
    * be set.  Since the implementation attribute is expressed as a boolean,
    * this method doubles for both the "is settable" and "is legal"
    * questions. The implementation attribute of the component is flexible.
    *
    * @return true if the implementation attribute can set
    */
    public boolean isAbstractSettable()
        {
        return isComponent() && isModifiable();
        }

    /**
    * Set the Component's implementation attribute.
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
    * Set the Component's implementation attribute.
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

    /**
    * Determine if the Component contains any abstract Behaviors.
    *
    * @return true if the Component contains any abstract Behaviors
    */
    public boolean isResultAbstract()
        {
        return isAbstract() || hasAbstractBehavior() || containsAbstractComponent();
        }

    /**
    * Determine if the resulting Java class(es) for the Component are
    * instatiable.  The classes would not be instantiable if the Component
    * were abstract, contains any abstract Behaviors, or .
    *
    * @return true if the Component contains any abstract Behaviors
    */
    public boolean hasAbstractBehavior()
        {
        for (Enumeration enmr = m_tblBehavior.elements(); enmr.hasMoreElements(); )
            {
            if (((Behavior) enmr.nextElement()).isAbstract())
                {
                return true;
                }
            }

        return false;
        }

    /**
    * Determine if the Component contains any abstract child Components.
    *
    * @return true if the Component contains any abstract child Components
    */
    public boolean containsAbstractComponent()
        {
        for (Enumeration enmr = m_tblChildren.elements(); enmr.hasMoreElements(); )
            {
            if (((Component) enmr.nextElement()).isResultAbstract())
                {
                return true;
                }
            }

        return false;
        }


    // ----- attribute:  Final

    /**
    * Get the component derivable attribute.
    *
    * @return the value of the derivable attribute
    */
    public boolean isFinal()
        {
        return (m_nFlags & DERIVE_MASK) == DERIVE_FINAL;
        }

    /**
    * Determine whether the derivable attribute of the Component Definition
    * can be set.  Since the derivable attribute is expressed as a boolean,
    * this method doubles for both the "is settable" and "is legal"
    * questions.  The derivable attribute of the component is one-way from
    * derivable to final.
    *
    * @return true if the derivable attribute can be set
    */
    public boolean isFinalSettable()
        {
        // the implementation of isModifiable verifies the super/base is not
        // already final
        return isComponent() && isModifiable();
        }

    /**
    * Set the finality attribute of the Component.
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
    * Set the finality attribute of the Component.
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


    // ----- attribute:  Deprecated

    /**
    * Get the component antiquity attribute.
    *
    * @return true if the component is deprecated
    */
    public boolean isDeprecated()
        {
        return (m_nFlags & ANTIQ_MASK) == ANTIQ_DEPRECATED;
        }

    /**
    * Determine whether the antiquity attribute of the Component can be set.
    * Since the antiquity attribute is expressed as a boolean,  this method
    * doubles for both the "is settable" and "is legal" questions. The
    * antiquity attribute of the component is flexible.
    *
    * @return true if the antiquity attribute can set
    */
    public boolean isDeprecatedSettable()
        {
        return isComponent() && isModifiable();
        }

    /**
    * Set the antiquity attribute of the Component.
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
    * Set the antiquity attribute of the Component.
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


    // ----- attribute:  Persistent

    /**
    * Get the component persistence attribute.
    *
    * @return true if the component is persistent
    */
    public boolean isPersistent()
        {
        return (m_nFlags & STG_MASK) == STG_PERSIST;
        }

    /**
    * Determine whether the persistence attribute of the Component can be
    * set.  Since the persistence attribute is expressed as a boolean,  this
    * method doubles for both the "is settable" and "is legal" questions. The
    * persistence attribute of the component is flexible.
    *
    * @return true if the persistence attribute can set
    */
    public boolean isPersistentSettable()
        {
        return isComponent() && isModifiable();
        }

    /**
    * Set the persistence attribute of the Component.
    *
    * @param fPersistent the new persistence attribute value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void setPersistent(boolean fPersistent)
            throws PropertyVetoException
        {
        setPersistent(fPersistent, true);
        }

    /**
    * Set the persistence attribute of the Component.
    *
    * @param fPersistent  the new persistence attribute value
    * @param fVetoable    true if the setter can veto the value
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void setPersistent(boolean fPersistent, boolean fVetoable)
            throws PropertyVetoException
        {
        boolean fPrevPersistent = isPersistent();

        if (fPersistent == fPrevPersistent)
            {
            return;
            }

        Boolean prev  = toBoolean(fPrevPersistent);
        Boolean value = toBoolean(fPersistent);

        if (fVetoable)
            {
            if (!isPersistentSettable())
                {
                readOnlyAttribute(ATTR_PERSISTENT, prev, value);
                }

            fireVetoableChange(ATTR_PERSISTENT, prev, value);
            }

        m_nFlags = m_nFlags & ~STG_MASK | (fPersistent ? STG_PERSIST : STG_TRANSIENT);
        firePropertyChange(ATTR_PERSISTENT, prev, value);
        }


    // ----- attribute:  Remote

    /**
    * Get the component distribution attribute.
    *
    * @return true if the component is remotable
    */
    public boolean isRemote()
        {
        return (m_nFlags & DIST_MASK) == DIST_REMOTE;
        }

    /**
    * Determine whether the distribution attribute of the Component can be
    * set.  Since the distribution attribute is expressed as a boolean,  this
    * method doubles for both the "is settable" and "is legal" questions.
    *
    * The distribution attribute of the Component is one-way local to remote.
    * The distribution attribute can only be set for global Components; local
    * Components are remote if and only if they derive from a remote global
    * Component.
    *
    * @return true if the distribution attribute can set
    */
    public boolean isRemoteSettable()
        {
        return isComponent()
            && isGlobal()
            && isModifiable()
            && (m_nPrevFlags & DIST_MASK) != DIST_REMOTE;
        }

    /**
    * Set the Component's distribution attribute.
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
    * Set the Component's distribution attribute.
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
            for(Enumeration enmr = m_tblBehavior.elements(); enmr.hasMoreElements(); )
                {
                ((Behavior) enmr.nextElement()).setRemote(false, false);
                }
            }

        firePropertyChange(ATTR_REMOTE, prev, value);
        }

    // ----- attribute:  Integration

    /**
    * Get the integration trait for this component
    *
    * @return the integration trait
    */
    public Integration getIntegration()
        {
        return m_integration;
        }

    /**
    * Determine whether the Integration attribute of the Component Definition
    * can be set.
    *
    * @return true if the Integration attribute can be set with a valid value
    */
    public boolean isIntegrationSettable()
        {
        return isComponent()
            && getParent() == null
            && m_fBaseLevel
            && isModifiable();
        }

    /**
    * Determine whether the Integration attribute of the Component Definition
    * can be set to the specified Java Class Signature.
    *
    * @param cdJCS  the Java Class Signature to use in Integration
    *
    * @return true if the Integration attribute can be set to the specified
    *         Java Class Signature
    */
    public boolean isIntegrationLegal(Component cdJCS)
        {
        return isIntegrationLegal(cdJCS, null);
        }

    /**
    * Determine whether the Integration attribute of the Component Definition
    * can be set to the specified Java Class Signature.
    *
    * @param cdJCS    the Java Class Signature to use in Integration
    * @param errlist  the ErrorList object to log integration errors to
    *
    * @return true if the Integration attribute can be set to the specified
    *         Java Class Signature.
    */
    public boolean isIntegrationLegal(Component cdJCS, ErrorList errlist)
        {
        Integration map = m_integration;

        // check if integration already exists
        if (map != null)
            {
            if (cdJCS == null)
                {
                // allow integration to be cleared only if this
                // integration level is the start of integration
                // for this Component heirarchy.
                return map.getPreviousSignature().length() == 0;
                }
            return map.isSignatureLegal(cdJCS.getQualifiedName());
            }

        // Otherwise, anything goes...
        return true;
        }

    /**
    * Use the specified Java Class Signature for Integration.
    *
    * @param cdJCS    the Java Class Signature to use with Integration
    * @param errlist  the error list object to log warnings and errors to
    *
    * @exception PropertyVetoException if the new attribute value is not
    *            accepted
    */
    public void setIntegration(Component cdJCS, ErrorList errlist)
            throws PropertyVetoException
        {
        setIntegration(cdJCS, errlist, true);
        }

    /**
    * Use the specified Java Class Signature for Integration.
    *
    * @param cdJCS      the Java Class Signature to use with Integration
    * @param errlist    the error list object to log warnings and errors to
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if the new attribute value is not
    *            accepted
    */
    protected synchronized void setIntegration(Component cdJCS, ErrorList errlist, boolean fVetoable)
            throws PropertyVetoException
        {
        Integration mapPrev = m_integration;
        if (mapPrev != null
            && cdJCS != null)
            {
            mapPrev.setSignature(cdJCS.getQualifiedName(), fVetoable);
            return;
            }

        Integration mapNew  = (cdJCS == null ? null : new Integration(this, cdJCS.getQualifiedName()));

        // assume no changes if integrates names are identical
        // (note:  if assumption is not true, then pass a null map to clear
        // the integrates attribute then call again with the desired map)
        if (mapNew == null ? mapPrev == null
                           : mapNew.equals(mapPrev))
            {
            return;
            }

        if (fVetoable)
            {
            if (!isIntegrationSettable())
                {
                readOnlyAttribute(ATTR_INTEGRATION, mapPrev, mapNew);
                }

            if (!isIntegrationLegal(cdJCS, errlist))
                {
                illegalAttributeValue(ATTR_INTEGRATION, mapPrev, mapNew);
                }

            fireVetoableChange(ATTR_INTEGRATION, mapPrev, mapNew);
            }

        // store new integration
        m_integration = mapNew;

        if (mapNew != null)
            {
            mapNew.validate();
            }

        firePropertyChange(ATTR_INTEGRATION, mapPrev, mapNew);

        if (mapPrev != null)
            {
            mapPrev.invalidate();
            }
        }

    // ----- attribute:  Implements

    /**
    * Get the list of Java interfaces implemented by this component.
    *
    * @return an array of Java interface names
    */
    public String[] getImplements()
        {
        return m_tblImplements.strings();
        }

    /**
    * Get the implemented interface information for the specified name.
    *
    * @param sIface  a Java interface name
    *
    * @return the Interface object for the specified name (or null)
    */
    public Interface getImplements(String sIface)
        {
        return (Interface) m_tblImplements.get(sIface);
        }

    /**
    * Determine whether the specified Java interface can be added to the
    * Implements attribute of the Component Definition.
    *
    * @param cdJCS  the Java Class Signature to implement
    *
    * @return true if the specified JCS can be added to this Component
    *         Definition
    */
    public boolean isImplementsAddable(Component cdJCS)
        {
        return isImplementsAddable(cdJCS, null);
        }

    /**
    * Determine whether the specified Java interface can be added to the
    * Implements attribute of the Component Definition.
    *
    * @param cdJCS    the Java Class Signature to implement
    * @param errlist  the ErrorList to log errors to
    *
    * @return true if the specified JCS can be added to this Component
    *         Definition
    */
    public boolean isImplementsAddable(Component cdJCS, ErrorList errlist)
        {
        Interface iface;
        try
            {
            iface = new Interface(this, cdJCS, false);
            }
        catch (IllegalArgumentException e)
            {
            // cdJCS is not a valid Java interface
            return false;
            }

        return isImplementsAddable(iface, cdJCS, errlist);
        }

    /**
    * Determine whether the specified Java interface can be added to the
    * Implements attribute of the Component Definition.
    *
    * @param iface    the Interface to implement
    * @param cdJCS    the Java Class Signature to implement
    * @param errlist  the ErrorList to log errors to
    *
    * @return true if the specified Interface can be implemented
    */
    protected boolean isImplementsAddable(Interface iface, Component cdJCS, ErrorList errlist)
        {
        // the JCS must be an interface
        if (iface == null)
            {
            return false;
            }

        // this must be modifiable
        if (!isModifiable())
            {
            return false;
            }

        // and it must be either a component or a JCS
        switch (m_nType)
            {
            case COMPONENT:
            case SIGNATURE:
                break;
            default:
                return false;
            }

        // short-cut:  this Component already implements or dispatches the
        // interface
        String sIface = iface.getName();
        if (m_tblImplements.contains(sIface) ||
            m_tblDispatches.contains(sIface))
            {
            return true;
            }

        // short-cut:  tag-only interfaces (e.g. Serializable)
        if (!iface.getBehaviors().hasMoreElements())
            {
            return true;
            }

        return isInterfaceExpandable(iface, cdJCS, errlist);
        }

    /**
    * Add the Java interface to the Component Definition.
    *
    * @param cdJCS  the Java Class Signature to implement
    *
    * @exception PropertyVetoException if the new attribute value is not
    *            accepted
    */
    public void addImplements(Component cdJCS)
            throws PropertyVetoException
        {
        addImplements(cdJCS, true);
        }

    /**
    * Add the Java interface to the Component Definition.
    *
    * @param cdJCS      the Java Class Signature to implement
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if the new attribute value is not
    *            accepted
    */
    protected synchronized void addImplements(Component cdJCS, boolean fVetoable)
            throws PropertyVetoException
        {
        // check for redundant change
        String sIface = cdJCS.getName();
        if (m_tblImplements.contains(sIface))
            {
            return;
            }

        // create the interface object
        Interface iface = null;
        try
            {
            iface = new Interface(this, cdJCS, false);
            }
        catch (IllegalArgumentException e)
            {
            subNotAddable(ATTR_IMPLEMENTS, sIface);
            }

        if (fVetoable)
            {
            // verify the interface can be implemented
            if (!isImplementsAddable(iface, cdJCS, null))
                {
                subNotAddable(ATTR_IMPLEMENTS, sIface);
                }

            // notify veto listeners
            fireVetoableChange(ATTR_IMPLEMENTS, null, iface);
            }

        // add the interface
        m_tblImplements.put(sIface, iface);

        // expand the interface
        expandInterface(iface, cdJCS, null, null, null);

        // notify listeners
        iface.validate();
        firePropertyChange(ATTR_IMPLEMENTS, null, iface);
        }

    /**
    * Determine whether a implemented Java interface can be removed from this
    * Component Definition.
    *
    * @param sIface  the name of the interface
    *
    * @return true if the specified interface can be removed from the
    *         Implements attribute
    */
    public boolean isImplementsRemovable(String sIface)
        {
        // check for redundant change
        Interface iface = (Interface) m_tblImplements.get(sIface);
        return iface != null && isModifiable() && iface.isDeclaredAtThisLevel();
        }

    /**
    * Remove the Java interface from the list of implemented interfaces.
    *
    * @param sIface fully qualified name of the Java interface
    *
    * @exception PropertyVetoException if removal of the interface
    *            fails.
    */
    public void removeImplements(String sIface)
            throws PropertyVetoException
        {
        removeImplements(sIface, true);
        }

    /**
    * Remove the Java interface from the list of implemented interfaces.
    * This will only succeed if the interface is specified at this
    * Component Definition's modification level.
    *
    * @param sIface     fully qualified name of the Java interface
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if removal of the interface
    *            fails.
    */
    protected synchronized void removeImplements(String sIface, boolean fVetoable)
            throws PropertyVetoException
        {
        // check for redundant change
        Interface iface = (Interface) m_tblImplements.get(sIface);
        if (iface == null)
            {
            return;
            }

        if (fVetoable)
            {
            // verify the interface is removable
            if (!isImplementsRemovable(sIface))
                {
                subNotRemovable(ATTR_IMPLEMENTS, iface);
                }

            // notify veto listeners
            fireVetoableChange(ATTR_IMPLEMENTS, iface, null);
            }

        // remove the interface
        m_tblImplements.remove(sIface);

        // remove origin from associated Behaviors and remove any associated
        // Behaviors that are left origin-less
        for (Enumeration enmr = iface.getBehaviors(); enmr.hasMoreElements(); )
            {
            String   sBehavior = (String) enmr.nextElement();
            Behavior behavior  = (Behavior) m_tblBehavior.get(sBehavior);

            // as of Java 8, an interface may contain static "extension methods"
            if (behavior != null)
                {
                behavior.removeOriginTrait(iface);
                if (behavior.isDiscardable())
                    {
                    removeBehavior(behavior, false);
                    }
                }
            }

        // notify listeners
        firePropertyChange(ATTR_IMPLEMENTS, iface, null);
        iface.invalidate();
        }


    // ----- attribute:  Dispatches

    /**
    * Get the list of Java interfaces dispatched by this component.
    *
    * @return an array of Java interface names
    */
    public String[] getDispatches()
        {
        return m_tblDispatches.strings();
        }

    /**
    * Get the dispatched interface information for the specified name.
    *
    * @param sIface  a Java interface name
    *
    * @return the Interface object for the specified name (or null)
    */
    public Interface getDispatches(String sIface)
        {
        return (Interface) m_tblDispatches.get(sIface);
        }

    /**
    * Determine whether the specified Java interface can be added to the
    * Dispatches attribute of the Component Definition.
    *
    * @param cdJCS  the Java Class Signature to dispatch
    *
    * @return true if the specified JCS can be added to this Component
    *         Definition
    */
    public boolean isDispatchesAddable(Component cdJCS)
        {
        return isDispatchesAddable(cdJCS, null);
        }

    /**
    * Determine whether the specified Java interface can be added to the
    * Dispatches attribute of the Component Definition.
    *
    * @param cdJCS    the Java Class Signature to dispatch
    * @param errlist  the ErrorList to log errors to
    *
    * @return true if the specified JCS can be added to this Component
    *         Definition
    */
    public boolean isDispatchesAddable(Component cdJCS, ErrorList errlist)
        {
        Interface iface;
        try
            {
            iface = new Interface(this, cdJCS, true);
            }
        catch (IllegalArgumentException e)
            {
            // cdJCS is not a valid Java interface
            return false;
            }

        return isDispatchesAddable(iface, cdJCS, errlist);
        }

    /**
    * Determine whether the specified Java interface can be added to the
    * Dispatches attribute of the Component Definition.
    *
    * @param iface    the Interface to dispatch
    * @param cdJCS    the Java Class Signature to dispatch
    * @param errlist  the ErrorList to log errors to
    *
    * @return true if the specified Interface can be dispatched
    */
    protected boolean isDispatchesAddable(Interface iface, Component cdJCS, ErrorList errlist)
        {
        // interface required
        if (iface == null)
            {
            return false;
            }

        // this must be modifiable
        if (!isModifiable())
            {
            return false;
            }

        // and it must be either a component or a JCS
        switch (m_nType)
            {
            case COMPONENT:
            case SIGNATURE:
                break;
            default:
                return false;
            }

        // short-cut:  this Component already dispatches the interface
        String sIface = iface.getName();
        if (m_tblDispatches.contains(sIface))
            {
            return true;
            }

        // check interface naming conventions
        if (!sIface.endsWith("Listener"))
            {
            return false;
            }

        // if add/remove listener Behaviors exist, they must be from this
        // level
        String sClass = DataType.getClassType(sIface).getTypeString();
        String sSig   = sIface.substring(sIface.lastIndexOf('.') + 1) + '(' + sClass + ')';
        Behavior behAdd    = (Behavior) m_tblBehavior.get("add"    + sSig);
        Behavior behRemove = (Behavior) m_tblBehavior.get("remove" + sSig);
        if (behAdd    != null && behAdd   .isFromSuper() ||
            behRemove != null && behRemove.isFromSuper()    )
            {
            return false;
            }

        return isInterfaceExpandable(iface, cdJCS, errlist);
        }

    /**
    * Add the Java interface to the Component Definition.
    *
    * @param cdJCS  the Java Class Signature to dispatch
    *
    * @exception PropertyVetoException if the new attribute value is not
    *            accepted
    */
    public void addDispatches(Component cdJCS)
            throws PropertyVetoException
        {
        addDispatches(cdJCS, true);
        }

    /**
    * Add the Java interface to the Component Definition.
    *
    * @param cdJCS      the Java Class Signature to dispatch
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if the new attribute value is not
    *            accepted
    */
    protected synchronized void addDispatches(Component cdJCS, boolean fVetoable)
            throws PropertyVetoException
        {
        // check for redundant change
        String sIface = cdJCS.getName();
        if (m_tblDispatches.contains(sIface))
            {
            return;
            }

        // create the interface object
        Interface iface = null;
        try
            {
            iface = new Interface(this, cdJCS, true);
            }
        catch (IllegalArgumentException e)
            {
            subNotAddable(ATTR_DISPATCHES, sIface);
            }

        if (fVetoable)
            {
            // verify the interface can be dispatched
            if (!isDispatchesAddable(iface, cdJCS, null))
                {
                subNotAddable(ATTR_DISPATCHES, sIface);
                }

            // notify veto listeners
            fireVetoableChange(ATTR_DISPATCHES, null, iface);
            }

        // add the interface
        m_tblDispatches.put(sIface, iface);

        // expand the interface
        expandInterface(iface, cdJCS, null, null, null);

        // notify listeners
        iface.validate();
        firePropertyChange(ATTR_DISPATCHES, null, iface);
        }

    /**
    * Determine whether a dispatched Java interface can be removed from this
    * Component Definition.
    *
    * @param sIface  the name of the interface
    *
    * @return true if the specified interface can be removed from the
    *         Dispatches attribute
    */
    public boolean isDispatchesRemovable(String sIface)
        {
        // check for redundant change
        Interface iface = (Interface) m_tblDispatches.get(sIface);
        return iface != null && isModifiable() && iface.isDeclaredAtThisLevel();
        }

    /**
    * Remove the Java interface from the list of dispatched interfaces.
    *
    * @param sIface fully qualified name of the Java interface
    *
    * @exception PropertyVetoException if removal of the interface
    *            fails.
    */
    public void removeDispatches(String sIface)
            throws PropertyVetoException
        {
        removeDispatches(sIface, true);
        }

    /**
    * Remove the Java interface from the list of dispatched interfaces.
    * This will only succeed if the interface is specified at this
    * Component Definition's modification level.
    *
    * @param sIface     fully qualified name of the Java interface
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if removal of the interface
    *            fails.
    */
    protected synchronized void removeDispatches(String sIface, boolean fVetoable)
            throws PropertyVetoException
        {
        // check for redundant change
        Interface iface = (Interface) m_tblDispatches.get(sIface);
        if (iface == null)
            {
            return;
            }

        if (fVetoable)
            {
            // verify the interface is removable
            if (!isDispatchesRemovable(sIface))
                {
                subNotRemovable(ATTR_DISPATCHES, iface);
                }

            // notify veto listeners
            fireVetoableChange(ATTR_DISPATCHES, iface, null);
            }

        // remove the interface
        m_tblDispatches.remove(sIface);

        // remove origin from associated Behaviors and remove any associated
        // Behaviors that are left origin-less
        for (Enumeration enmr = iface.getBehaviors(); enmr.hasMoreElements(); )
            {
            String   sBehavior = (String) enmr.nextElement();
            Behavior behavior  = (Behavior) m_tblBehavior.get(sBehavior);
            behavior.removeOriginTrait(iface);
            if (behavior.isDiscardable())
                {
                removeBehavior(behavior, false);
                }
            }

        // notify listeners
        firePropertyChange(ATTR_DISPATCHES, iface, null);
        iface.invalidate();
        }


    // ----- helpers for Implements and Dispatches attributes

    /**
    * Determine whether the specified Java interface can be added to this
    * Component Definition (i.e. the Implements or Dispatches attributes).
    *
    * @param iface    the Java Interface to implement/dispatch
    * @param cdJCS    the Java Class Signature for the interface
    * @param errlist  the error list to log errors to
    *
    * @return true if the specified Java interface can be added to this
    *         Component Definition
    */
    protected boolean isInterfaceExpandable(Interface iface, Component cdJCS, ErrorList errlist)
        {
        boolean fExpandable = true;
        String  sAttribute  = (iface.getType() == Interface.DISPATCHES ? ATTR_DISPATCHES
                                                                       : ATTR_IMPLEMENTS);

        try
            {
            StringTable tblMethod   = cdJCS.m_tblBehavior;
            StringTable tblBehavior = this.m_tblBehavior;
            for (Enumeration enmr = iface.getBehaviors(); enmr.hasMoreElements(); )
                {
                String sSignature = (String) enmr.nextElement();
                Behavior method   = (Behavior) tblMethod  .get(sSignature);
                Behavior behavior = (Behavior) tblBehavior.get(sSignature);

                if (behavior == null)
                    {
                    // if no such Behavior signature already exists, make
                    // sure that the signature is not reserved
                    if (isBehaviorReserved(sSignature))
                        {
                        fExpandable = false;
                        logError(IFACE_RESERVEDBEHAVIOR, WARNING, new Object[] {sAttribute,
                            iface.getName(), getQualifiedName(), sSignature}, errlist);
                        }
                    continue;
                    }

                // add/remove listener methods are implied; assume defaults
                DataType dtMethodRet      = DataType.VOID;
                String   sMethodParamDirs = "I";
                if (method != null)
                    {
                    // method exists
                    dtMethodRet      = method.getReturnValue().getDataType();
                    sMethodParamDirs = method.getParameterDirections();
                    }

                // certain behavior attributes that must match cannot be
                // modified (fixed) if the behavior has non-manual origin
                boolean fFixable  = !behavior.isFromNonManual();
                int     nSeverity = (fFixable ? INFO : WARNING);

                // as of Java 8 interface methods could be static
                if (isComponent() && behavior.isStatic())
                    {
                    fExpandable = fExpandable && fFixable;
                    logError(IFACE_SCOPEMISMATCH, nSeverity, new Object[] {sAttribute,
                        iface.getName(), getQualifiedName(), sSignature}, errlist);
                    }

                // verify that the return value type matches
                if (dtMethodRet != behavior.getReturnValue().getDataType())
                    {
                    if (isSignature())
                        {
                        // The return types differ, but since this is a
                        // signature (and thus represents a valid Java class),
                        // this is a co-variant return (the base definition
                        // narrows the return type of the method defined on the
                        // interface).  Treat the behavior as if it were
                        // declared at this level.
                        }
                    else
                        {
                        fExpandable = fExpandable && fFixable;
                        logError(IFACE_RETURNMISMATCH, nSeverity,
                                 new Object[] {sAttribute,
                                               iface.getName(), getQualifiedName(), sSignature,
                                               behavior.getReturnValue().getDataType().toString(),
                                               dtMethodRet.toString()}, errlist);
                        }
                    }

                // verify that the parameter directions match
                if (!sMethodParamDirs.equals(behavior.getParameterDirections()))
                    {
                    fExpandable = fExpandable && fFixable;
                    logError(IFACE_DIRECTIONMISMATCH, nSeverity, new Object[] {sAttribute,
                        iface.getName(), getQualifiedName(), sSignature}, errlist);
                    }

                // as of Java 8 interfaces could have non-public methods
                if (isComponent() && behavior.getAccess() != ACCESS_PUBLIC)
                    {
                    fExpandable = fExpandable && fFixable;
                    logError(IFACE_BEHAVIORACCESS, nSeverity, new Object[] {sAttribute,
                        iface.getName(), getQualifiedName(), sSignature}, errlist);
                    }
                }
            }
        catch (DerivationException e)
            {
            // thrown only if error list fills up
            }

        return fExpandable;
        }

    /**
    * Apply the passed interface to this Component Definition.  There are two
    * possible results:
    * <pre>
    *   1)  The interface conflicts with existing Behaviors of the Component
    *       Definition, in which case the Component Definition is unchanged
    *       and a ComponentException is thrown; there are several scenarios
    *       for each Behavior that could cause a conflict that would prevent
    *       then interface from being applied, specifically if the Behavior
    *       signature already exists and the associated Behavior:
    *       1)  Has a different return value
    *       2)  Is static
    *       3)  Does not match parameter directions (i.e. in/out or out)
    *       4)  Is reserved (e.g. by a Property)
    *       5)  Is not supposed to exist:  if a resolved Component has a
    *           Property derived from a super level and an applicable
    *           accessor does not exist, then the accessor was private at
    *           its declaration level
    *
    *   2)  The interface does not conflict with any existing Behaviors and
    *       is applied fully to this Component Definition; there are two
    *       possible scenarios for each Behavior specified in the interface:
    *       1)  If the Behavior signature does not already exist, it is added
    *           to the Component Definition as a resolved INSERT with the
    *           interface as the origin
    *       2)  If the Behavior signature already exists on the Component
    *           Definition, the interface is added as a Behavior origin;
    *           additionally, the exceptions declared for the interface are
    *           merged with the existing Behavior's exceptions, leaving only
    *           the common subset of exceptions between them
    * </pre>
    * Note:  This Component Definition can be of any mode (RESOLVED,
    *        DERIVATION, MODIFICATION).
    *
    * @param iface             the interface object
    * @param cdJCS             the Java Class Signature for the interface
    * @param tblDeferBehavior  the Behaviors which didn't exist in the
    *                          base but may include interface Behaviors
    * @param tblDeferProperty  the Properties which didn't exist in the
    *                          base but may include interface Properties
    * @param errlist           the error list to log errors to
    *
    * @exception PropertyVetoException  thrown if the interface cannot be
    *            expanded due to conflicts (should never be thrown if
    *            isInterfaceExpandable returned true)
    */
    protected void expandInterface(Interface iface, Component cdJCS, StringTable tblDeferBehavior,
                                   StringTable tblDeferProperty, ErrorList errlist)
            throws PropertyVetoException
        {
        StringTable tblMethod   = cdJCS.m_tblBehavior;
        StringTable tblBehavior = this.m_tblBehavior;

        for (Enumeration enmr = iface.getBehaviors(); enmr.hasMoreElements(); )
            {
            String   sSig     = (String) enmr.nextElement();
            Behavior method   = (Behavior) tblMethod  .get(sSig);
            Behavior behavior = (Behavior) tblBehavior.get(sSig);

            if (method != null && method.isStatic() && isComponent())
                {
                // as of Java 8, an interface may contain static "extension methods"
                // no need to expand them into the component
                continue;
                }

            // search for a Behavior in the "defer" list and use it
            // if possible instead of creating a Behavior from scratch
            if (behavior == null && tblDeferBehavior != null)
                {
                behavior = (Behavior) tblDeferBehavior.get(sSig);
                if (behavior != null)
                    {
                    // un-defer it
                    tblDeferBehavior.remove(sSig);

                    // copy the behavior to this component
                    behavior = new Behavior(this, behavior);

                    // the Behavior is new at this level
                    behavior.setExists(EXISTS_INSERT);

                    // add it to this level
                    tblBehavior.put(sSig, behavior);
                    }
                }

            // if no such Behavior signature already exists, create
            // one based on the method
            if (behavior == null)
                {
                if (method == null)
                    {
                    // implied add/remove listener behavior
                    behavior = new Behavior(this, iface, sSig);
                    }
                else
                    {
                    behavior = new Behavior(this, iface, method);
                    }
                addBehavior(behavior, false);
                }
            else
                {
                // resolve any differences between the existing Behavior
                // and the interface method

                if (method != null &&
                    behavior.getReturnValue().getDataType() != method.getReturnValue().getDataType())
                    {
                    // this is a covariant return (the base definition narrows the
                    // return type of the method defined on the interface).  Treat
                    // the behavior as if it were declared at this level.
                    }
                else
                    {
                    behavior.setDeclaredByInterface(iface, method);
                    }
                }

            // 2001.05.08 cp  fix origin of interfaces
            if (iface.isFromBase() && behavior.isDeclaredAtThisLevel())
                {
                behavior.setFromBase();
                }
            }

        if (isSignature())
            {
            StringTable tblField    = cdJCS.m_tblState;
            StringTable tblProperty =  this.m_tblState;

            for (Enumeration enmr = iface.getProperties(); enmr.hasMoreElements(); )
                {
                String   sProp    = (String) enmr.nextElement();
                Property field    = (Property) tblField   .get(sProp);
                Property property = (Property) tblProperty.get(sProp);

                // search for a Property in the "defer" list and skip
                // it here and instead add it the version from the
                // class itself
                if (property == null && tblDeferProperty != null)
                    {
                    property = (Property) tblDeferProperty.get(sProp);
                    if (property != null)
                        {
                        continue;
                        }
                    }

                // if no such Property name already exists, create
                // one based on the field
                if (property == null)
                    {
                    property = new Property(this, field, iface);
                    addProperty(property, false);
                    }
                else
                    {
                    // check if the current property is also
                    // flagged as being from an interface
                    if (property.getExists() == EXISTS_NOT)
                        {
                        // resolve any differences between the existing Behavior
                        // and the interface method
                        property.mergeJCSFields(field);
                        property.addOriginTrait(iface);
                        }
                    }

                // 2001.05.23 cp  fix origin of interfaces
                if (iface.isFromBase() && property.isDeclaredAtThisLevel())
                    {
                    property.setFromBase();
                    }
                }
            }
        }


    // ----- component accessors:  state ------------------------------------

    /**
    * Get the list of properties within this Component Definition.
    *
    * @return an array of property names
    */
    public String[] getProperty()
        {
        return m_tblState.strings();
        }

    /**
    * Enumerate the properties within this Component Definition.
    *
    * @return an enumeration of properties
    */
    public Enumeration getProperties()
        {
        return m_tblState.elements();
        }

    /**
    * Get a specific property based on its name.
    *
    * @param sProp  the name of the property
    *
    * @return the specified property or null if the property does not exist
    */
    public Property getProperty(String sProp)
        {
        return (Property) m_tblState.get(sProp);
        }

    /**
    * Determine the number of properties within this Component Definition.
    *
    * @return the count of properties
    */
    public int getPropertyCount()
        {
        return m_tblState.getSize();
        }

    /**
    * Determine if the specified Java constant Property can be added to the
    * Component Definition.
    *
    * @param dt        the data type of the Property
    * @param sName     the name of the Property
    * @param nIndexed  specifies whether the Property is indexed, one of
    *                  PROP_SINGLE or PROP_INDEXED
    * @param fStatic   true if the Java constant is static (static final)
    * @param nAccess   accessibility of the Java constant
    *
    * @return true if the specified Property can be added
    */
    public boolean isJavaConstantAddable(DataType dt, String sName, int nIndexed, boolean fStatic, int nAccess)
        {
        return Property.isJavaConstantCreatable(this, dt, sName, nIndexed, fStatic, nAccess);
        }

    /**
    * Create a Java constant Property.  A constant field is generated in the
    * Java class that results from the Component Definition that declares the
    * constant.  The Property is declared as "persistent final out" and the
    * resulting Java field is "final".
    *
    * @param dt        the data type of the Property
    * @param sName     the name of the Property
    * @param nIndexed  specifies whether the Property is indexed, one of
    *                  PROP_SINGLE or PROP_INDEXED
    * @param fStatic   true if the Java constant is static (static final)
    * @param nAccess   accessibility of the Java constant
    *
    * @exception PropertyVetoException if the property cannot be added as
    *            specified or was veto'd by a property listener
    */
    public Property addJavaConstant(DataType dt, String sName, int nIndexed, boolean fStatic, int nAccess)
            throws PropertyVetoException
        {
        if (!isJavaConstantAddable(dt, sName, nIndexed, fStatic, nAccess))
            {
            subNotAddable(ATTR_PROPERTY, sName);
            }

        Property property = Property.createJavaConstant(this, dt, sName,
                nIndexed, fStatic, nAccess);
        property.setFromManual();
        addProperty(property, true);

        return property;
        }

    /**
    * Determine if the specified virtual constant Property can be added to
    * the Component Definition.
    *
    * @param dt        the data type of the Property
    * @param sName     the name of the Property
    * @param nIndexed  specifies whether the Property is indexed, one of
    *                  PROP_SINGLE, PROP_INDEXED, or PROP_INDEXEDONLY
    * @param nAccess   accessibility of the virtual constant
    *
    * @return true if the specified Property can be added
    */
    public boolean isVirtualConstantAddable(DataType dt, String sName, int nIndexed, int nAccess)
        {
        return Property.isVirtualConstantCreatable(this, dt, sName, nIndexed, nAccess);
        }

    /**
    * Create a virtual constant Property,  The property is declared as
    * "persistent instance derivable out" and no Java field results.  No
    * accessor Behavior(s) corresponding to the virtual constant are
    * declared, but they are reserved.
    *
    * @param dt        the data type of the Property
    * @param sName     the name of the Property
    * @param nIndexed  specifies whether the Property is indexed, one of
    *                  PROP_SINGLE, PROP_INDEXED, or PROP_INDEXEDONLY
    * @param nAccess   accessibility of the virtual constant
    *
    * @exception PropertyVetoException if the property cannot be added as
    *            specified or was veto'd by a property listener
    */
    public Property addVirtualConstant(DataType dt, String sName, int nIndexed, int nAccess)
            throws PropertyVetoException
        {
        if (!isVirtualConstantAddable(dt, sName, nIndexed, nAccess))
            {
            subNotAddable(ATTR_PROPERTY, sName);
            }

        Property property = Property.createVirtualConstant(this, dt, sName,
                nIndexed, nAccess);
        property.setFromManual();
        addProperty(property, true);

        return property;
        }

    /**
    * Determine if the specified calculated Property can be added to the
    * Component Definition.
    *
    * @param dt        the data type of the Property
    * @param sName     the name of the Property
    * @param nIndexed  specifies whether the Property is indexed, one of
    *                  PROP_SINGLE, PROP_INDEXED, or PROP_INDEXEDONLY
    * @param fStatic   true if the Java constant is static (static final)
    *
    * @return true if the specified Property can be added
    */
    public boolean isCalculatedPropertyAddable(DataType dt, String sName, int nIndexed, boolean fStatic)
        {
        return Property.isCalculatedPropertyCreatable(this, dt, sName, nIndexed, fStatic);
        }

    /**
    * Create a calculated Property.  A calculated Property is declared as
    * "transient out", which means no corresponding Java field is generated.
    * A calculated Property is implemented by the developer to calculate some
    * value based on, perhaps, other Properties.
    *
    * @param dt        the data type of the Property
    * @param sName     the name of the Property
    * @param nIndexed  specifies whether the Property is indexed, one of
    *                  PROP_SINGLE, PROP_INDEXED, or PROP_INDEXEDONLY
    * @param fStatic   true if the Java constant is static (static final)
    *
    * @exception PropertyVetoException if the property cannot be added as
    *            specified or was veto'd by a property listener
    */
    public Property addCalculatedProperty(DataType dt, String sName, int nIndexed, boolean fStatic)
            throws PropertyVetoException
        {
        if (!isCalculatedPropertyAddable(dt, sName, nIndexed, fStatic))
            {
            subNotAddable(ATTR_PROPERTY, sName);
            }

        Property property = Property.createCalculatedProperty(this, dt,
                sName, nIndexed, fStatic);
        property.setFromManual();
        addProperty(property, true);

        return property;
        }

    /**
    * Determine if the specified functional Property can be added to the
    * Component Definition.
    *
    * @param dt        the data type of the Property
    * @param sName     the name of the Property
    * @param nIndexed  specifies whether the Property is indexed, one of
    *                  PROP_SINGLE, PROP_INDEXED, or PROP_INDEXEDONLY
    * @param fStatic   true if the Java constant is static (static final)
    *
    * @return true if the specified Property can be added
    */
    public boolean isFunctionalPropertyAddable(DataType dt, String sName, int nIndexed, boolean fStatic)
        {
        return Property.isFunctionalPropertyCreatable(this, dt, sName, nIndexed, fStatic);
        }

    /**
    * Create a functional Property.  Properties which are settable-only
    * (declared as "in") are considered to be functional.  A functional
    * Property is implemented by the developer and has no corresponding Java
    * field.  Functional Properties are not gettable, but they do appear in
    * the property sheet at design time, they are persisted in the Component
    * Definition, and they are initialized (which invokes the setter).
    *
    * @param dt        the data type of the Property
    * @param sName     the name of the Property
    * @param nIndexed  specifies whether the Property is indexed, one of
    *                  PROP_SINGLE, PROP_INDEXED, or PROP_INDEXEDONLY
    * @param fStatic   true if the Java constant is static (static final)
    *
    * @exception PropertyVetoException if the property cannot be added as
    *            specified or was veto'd by a property listener
    */
    public Property addFunctionalProperty(DataType dt, String sName, int nIndexed, boolean fStatic)
            throws PropertyVetoException
        {
        if (!isFunctionalPropertyAddable(dt, sName, nIndexed, fStatic))
            {
            subNotAddable(ATTR_PROPERTY, sName);
            }

        Property property = Property.createFunctionalProperty(this, dt,
                sName, nIndexed, fStatic);
        property.setFromManual();
        addProperty(property, true);

        return property;
        }

    /**
    * Determine if the specified standard Property can be added to the
    * Component Definition.
    *
    * @param dt        the data type of the Property
    * @param sName     the name of the Property
    * @param nIndexed  specifies whether the Property is indexed, one of
    *                  PROP_SINGLE, PROP_INDEXED, or PROP_INDEXEDONLY
    * @param fStatic   true if the Java constant is static (static final)
    * @param fPersist  true if the resulting Java field should be serialized
    *                  by the Java serialization mechanism
    *
    * @return true if the specified Property can be added
    */
    public boolean isPropertyAddable(DataType dt, String sName, int nIndexed, boolean fStatic, boolean fPersist)
        {
        return Property.isPropertyCreatable(this, dt, sName, nIndexed, fStatic, fPersist);
        }

    /**
    * Create a standard Property.  Standard Properties are declared as
    * "inout".  A Java field is generated for standard Properties if storage
    * is required.  Java methods are auto-generated for non-private standard
    * Properties if they are declared but not implemented; the value of a
    * private standard Property can be accessed using a get or set field
    * operation if the corresponding accessor is not implemented.
    *
    * @param dt        the data type of the Property
    * @param sName     the name of the Property
    * @param nIndexed  specifies whether the Property is indexed, one of
    *                  PROP_SINGLE, PROP_INDEXED, or PROP_INDEXEDONLY
    * @param fStatic   true if the Java constant is static (static final)
    * @param fPersist  true if the resulting Java field should be serialized
    *                  by the Java serialization mechanism
    *
    * @exception PropertyVetoException if the property cannot be added as
    *            specified or was veto'd by a property listener
    */
    public Property addProperty(DataType dt, String sName, int nIndexed, boolean fStatic, boolean fPersist)
            throws PropertyVetoException
        {
        if (!isPropertyAddable(dt, sName, nIndexed, fStatic, fPersist))
            {
            subNotAddable(ATTR_PROPERTY, sName);
            }

        Property property = Property.createProperty(this, dt, sName,
                nIndexed, fStatic, fPersist);
        property.setFromManual();
        addProperty(property, true);

        return property;
        }

    /**
    * Add the passed Property to the Component Definition.
    *
    * @param property   the property to add
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if the property cannot be added as
    *            specified or was veto'd by a property listener
    */
    protected synchronized void addProperty(Property property, boolean fVetoable)
            throws PropertyVetoException
        {
        try
            {
            if (!isSignature())
                {
                property.assignUID();
                }

            if (fVetoable)
                {
                // notify veto listeners
                fireVetoableSubChange(property, SUB_ADD);
                }

            // add property
            m_tblState.put(property.getName(), property);

            // notify listeners
            property.validate();
            fireSubChange(property, SUB_ADD);
            }
        catch (PropertyVetoException e)
            {
            property.invalidate();
            throw e;
            }
        }

    /**
    * Determine if a property can be removed from the Component Definition.
    *
    * @param sProp  the name of the property
    *
    * @return true if it is possible to remove the specified property
    */
    public boolean isPropertyRemovable(String sProp)
        {
        Property property = getProperty(sProp);

        return property != null && property.isDeclaredAtThisLevel()
                && isModifiable() && !property.isFromNonManual();
        }

    /**
    * Remove the specified property from the Component Definition.
    *
    * @param sProp  the name of the property
    *
    * @exception PropertyVetoException  if removal is illegal or if removal
    *            is vetoed
    */
    public void removeProperty(String sProp)
            throws PropertyVetoException
        {
        // verify that the property can be removed
        if (!isPropertyRemovable(sProp))
            {
            subNotRemovable(ATTR_PROPERTY, sProp);
            }

        removeProperty(getProperty(sProp), true);
        }

    /**
    * Remove the specified property from the Component Definition.
    *
    * @param property   the property to remove
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException  if removal is illegal or if removal
    *            is vetoed
    */
    protected synchronized void removeProperty(Property property, boolean fVetoable)
            throws PropertyVetoException
        {
        if (fVetoable)
            {
            // notify veto listeners
            fireVetoableSubChange(property, SUB_REMOVE);
            }

        // remove related accessor behaviors
        Behavior[] aBehavior = property.getAccessors();
        int        cBehavior = aBehavior.length;
        for (int i = 0; i < cBehavior; ++i)
            {
            Behavior behavior = aBehavior[i];
            if (behavior != null)
                {
                if (property.isAccessorRemovable(i))
                    {
                    property.removeAccessor(i);
                    }
                else
                    {
                    // ! TODO prop should do this on invalidate!
                    //behavior.setFromProperty(false);
                    }
                }
            }

        // remove property
        m_tblState.remove(property.getName());

        // notify listeners
        fireSubChange(property, SUB_REMOVE);

        // discard property
        property.invalidate();
        }

    /**
    * Update the reference to a property when the property name changes.
    * This method is used internally by the Component Definition.
    *
    * @param sOld the old property name
    * @param sNew the new property name
    */
    protected void renameProperty(String sOld, String sNew)
        {
        m_tblState.put(sNew, m_tblState.remove(sOld));
        }


    // ----- component accessors:  behavior ---------------------------------

    /**
    * Get the list of behavior signatures within this Component Definition.
    *
    * @return an array of behavior signatures
    */
    public String[] getBehavior()
        {
        return m_tblBehavior.strings();
        }

    /**
    * Enumerate the behaviors within this Component Definition.
    *
    * @return an enumeration of behaviors
    */
    public Enumeration getBehaviors()
        {
        return m_tblBehavior.elements();
        }

    /**
    * Get a specific behavior based on its signature.
    *
    * @param sSig  the signature of the behavior
    *
    * @return the specified behavior signature or null if the behavior
    *         signature is not within this Component Definition
    */
    public Behavior getBehavior(String sSig)
        {
        return (Behavior) m_tblBehavior.get(sSig);
        }

    /**
    * Get all behaviors with the specified name.
    *
    * @param sName  the name of the behavior
    *
    * @return an enumeration of behaviors that have the specified name
    */
    public Enumeration getBehaviors(String sName)
        {
        StringTable tblBehavior = m_tblBehavior;

        String[] asSig = tblBehavior.stringsStartingWith(sName + '(');
        int      cSigs = asSig.length;
        if (cSigs == 0)
            {
            return NullImplementation.getEnumeration();
            }

        Behavior[] aBeh = new Behavior[cSigs];
        for (int i = 0; i < cSigs; ++i)
            {
            aBeh[i] = (Behavior) tblBehavior.get(asSig[i]);
            }

        return new SimpleEnumerator(aBeh);
        }

    /**
    * Determine the number of behaviors within this Component Definition.
    *
    * @return the count of behaviors
    */
    public int getBehaviorCount()
        {
        return m_tblBehavior.getSize();
        }

    /**
    * Determine if a Behavior can be added to the Component Definition.  The
    * behavior must not conflict with an existing behavior or property.
    *
    * @param sName  the behavior name
    *
    * @return true if it is possible to add the behavior as specified
    */
    public boolean isBehaviorAddable(DataType dtRet, String sName, DataType[] adtParam)
        {
        String sSig = Behavior.getSignature(sName, adtParam);
        return isModifiable() && !m_tblBehavior.contains(sSig) &&
                !isBehaviorReserved(sSig);
        }

    /**
    * Add the specified Behavior to the Component Definition.
    *
    * @param dtRet     the return value type
    * @param sName     the behavior name
    * @param adtParam  the parameter data types
    * @param fStatic    the behavior's scope (static or instance)
    * @param nAccess    the behavior's accessibility
    *
    * @return the new behavior
    *
    * @exception PropertyVetoException if the behavior cannot be added as
    *            specified or was veto'd by a property listener
    */
    public Behavior addBehavior(DataType dtRet, String sName,
            DataType[] adtParam, boolean fStatic, int nAccess)
            throws PropertyVetoException
        {
        if (!isBehaviorAddable(dtRet, sName, adtParam))
            {
            subNotAddable(ATTR_BEHAVIOR, sName);
            }

        // create behavior
        Behavior behavior = new Behavior(this, dtRet, sName, fStatic, nAccess, adtParam, null, null);
        behavior.setFromManual();
        addBehavior(behavior, true);
        return behavior;
        }

    /**
    * Add the passed Behavior to the Component Definition.
    *
    * @param behavior   the behavior to add
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if the behavior cannot be added as
    *            specified or was veto'd by a property listener
    */
    protected synchronized void addBehavior(Behavior behavior, boolean fVetoable)
            throws PropertyVetoException
        {
        try
            {
            if (!isSignature())
                {
                // set UID
                behavior.assignUID();
                }

            if (fVetoable)
                {
                // notify veto listeners
                fireVetoableSubChange(behavior, SUB_ADD);
                }

            // add behavior
            m_tblBehavior.put(behavior.getSignature(), behavior);

            // notify listeners
            behavior.validate();
            fireSubChange(behavior, SUB_ADD);
            }
        catch (PropertyVetoException e)
            {
            behavior.invalidate();
            throw e;
            }
        }

    /**
    * Determine if a behavior can be removed from the Component Definition.
    *
    * @param sSig  the signature of the behavior
    *
    * @return true if it is possible to remove the specified behavior
    */
    public boolean isBehaviorRemovable(String sSig)
        {
        Behavior behavior = getBehavior(sSig);
        return isModifiable() && behavior != null &&
            behavior.isDeclaredAtThisLevel() && !behavior.isFromNonManual();
        }

    /**
    * Remove the specified behavior from the Component Definition.
    *
    * @param sSig  the signature of the behavior
    *
    * @exception PropertyVetoException  if removal is illegal or if removal
    *            is vetoed
    */
    public synchronized void removeBehavior(String sSig)
            throws PropertyVetoException
        {
        if (!isBehaviorRemovable(sSig))
            {
            subNotRemovable(ATTR_BEHAVIOR, sSig);
            }

        removeBehavior(getBehavior(sSig), true);
        }

    /**
    * (Internal) Remove the passed Behavior from the Component Definition.
    *
    * @param behavior   the behavior to add
    * @param fVetoable  pass as false for unconditional addition of behavior
    *
    * @exception PropertyVetoException if the behavior removal is veto'd by
    *            a property listener
    */
    protected synchronized void removeBehavior(Behavior behavior, boolean fVetoable)
            throws PropertyVetoException
        {
        if (fVetoable)
            {
            // notify veto listeners
            fireVetoableSubChange(behavior, SUB_REMOVE);
            }

        // remove behavior
        m_tblBehavior.remove(behavior.getSignature());

        // notify listeners
        fireSubChange(behavior, SUB_REMOVE);

        // discard behavior
        behavior.invalidate();
        }

    /**
    * Update the reference to an behavior when the behavior signature
    * changes.  This method is used internally by the Component Definition.
    *
    * @param sOld the old behavior signature
    * @param sNew the new behavior signature
    */
    protected void renameBehavior(String sOld, String sNew)
        {
        m_tblBehavior.put(sNew, m_tblBehavior.remove(sOld));
        }

    /**
    * Determine if the Behavior signature is reserved.
    *
    * @param sSig the Behavior signature
    *
    * @return true if the signature is reserved
    */
    protected boolean isBehaviorReserved(String sSig)
        {
        return Behavior.isSignatureReserved(this, sSig);
        }


    // ----- component accessors:  aggregation declaration ------------------

    /**
    * Get the list of aggregate categories within this Component Definition.
    *
    * @return an array of category names
    */
    public String[] getCategories()
        {
        return m_tblCategories.strings();
        }

    /**
    * Determine whether the set of aggregate categories contains the
    * specified category.
    *
    * @return true if the set of aggregate categories contains the
    *              specified category
    */
    public boolean isCategory(String sCategory)
        {
        return m_tblCategories.contains(sCategory);
        }

    /**
    * Determine whether the specified category can be added to the set of
    * aggregate categories on this Component Definition.
    *
    * @param sCategory fully qualified Component Definition name for the
    *        aggregate declaration
    *
    * @return true if the specified category can be added to this Component
    *         Definition
    */
    public boolean isCategoryAddable(String sCategory)
        {
        return isModifiable() &&
                sCategory != null &&
                isGlobalNameLegal(sCategory) &&
                !m_tblCategories.contains(sCategory);
        }

    /**
    * Add an aggregation category.  An aggregation category is a Component
    * Definition name which acts as a type declaration, allowing the
    * aggregation of instances of that Component Definition or derived
    * Component Definitions.
    *
    * @param sCategory fully qualified name of the Component Definition
    *
    * @exception PropertyVetoException if the new attribute value is not
    *            accepted
    */
    public void addCategory(String sCategory)
            throws PropertyVetoException
        {
        addCategory(sCategory, true);
        }

    /**
    * Add an aggregation category.  An aggregation category is a Component
    * Definition name which acts as a type declaration, allowing the
    * aggregation of instances of that Component Definition or derived
    * Component Definitions.
    *
    * @param sCategory  fully qualified name of the Component Definition
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if the new attribute value is not
    *            accepted
    */
    protected synchronized void addCategory(String sCategory, boolean fVetoable)
            throws PropertyVetoException
        {
        // check if the category already exists
        if (m_tblCategories.contains(sCategory))
            {
            return;
            }

        if (fVetoable)
            {
            // verify that the category can be added
            if (!isCategoryAddable(sCategory))
                {
                subNotAddable(ATTR_CATEGORY, sCategory);
                }

            // notify veto listeners
            fireVetoableChange(ATTR_CATEGORY, null, sCategory);
            }

        m_tblCategories.put(sCategory, Boolean.TRUE);
        firePropertyChange(ATTR_CATEGORY, null, sCategory);
        }

    /**
    * Determine whether an aggregate category can be removed from this
    * Component Definition.
    *
    * @return true if the specified category can be removed from the
    *         Component Definition
    */
    public boolean isCategoryRemovable(String sCategory)
        {
        return isModifiable() && m_tblCategories.contains(sCategory) &&
                ((Boolean) m_tblCategories.get(sCategory)).booleanValue();
        }

    /**
    * Remove the aggregate category from the list of declared categories.
    * This will only succeed if the category is specified at this
    * Component Definition's modification level.
    *
    * @param sCategory fully qualified name of the aggregate category
    *
    * @exception PropertyVetoException if removal of the category
    *            fails.
    */
    public void removeCategory(String sCategory)
            throws PropertyVetoException
        {
        removeCategory(sCategory, true);
        }

    /**
    * Remove the aggregate category from the list of declared categories.
    * This will only succeed if the category is specified at this
    * Component Definition's modification level.
    *
    * @param sCategory  fully qualified name of the aggregate category
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if removal of the category
    *            fails.
    */
    protected synchronized void removeCategory(String sCategory, boolean fVetoable)
            throws PropertyVetoException
        {
        // check that the category exists
        if (!m_tblCategories.contains(sCategory))
            {
            return;
            }

        if (fVetoable)
            {
            // verify that the category can be removed
            if (!isCategoryRemovable(sCategory))
                {
                subNotRemovable(ATTR_CATEGORY, sCategory);
                }

            // notify veto listeners
            fireVetoableChange(ATTR_CATEGORY, sCategory, null);
            }

        m_tblCategories.remove(sCategory);

        // remove any children that were of that category
NextChild:
        for (Enumeration enmr = m_tblChildren.elements(); enmr.hasMoreElements(); )
            {
            Component cd = (Component) enmr.nextElement();
            if (isDerivedFrom(cd.getSuperName(), sCategory))
                {
                // make sure the child is not legal by any other category
                // (don't lose any children that are still legal)
                for (Enumeration enmrCat = m_tblCategories.keys(); enmrCat.hasMoreElements(); )
                    {
                    if (isDerivedFrom(cd.getSuperName(), (String) enmrCat.nextElement()))
                        {
                        continue NextChild;
                        }
                    }
                // (if it must be removed, then it may be removed, since
                // otherwise the child would not be here)
                if (cd.getExists() == EXISTS_INSERT && cd.m_fBaseLevel)
                    {
                    removeChild(cd, false);
                    }
                }
            }

        // notify listeners
        firePropertyChange(ATTR_CATEGORY, sCategory, null);
        }

    /**
    * Determine what is the aggregatee category on a parent component
    * that this Component Definition belongs to.
    *
    * @return the parent's aggregatee category
    *
    * Note: there could be many categories that the child component
    *       belongs to. In that case we return the first one.
    */
    public String getCategory()
        {
        if (isGlobal())
            {
            throw new IllegalStateException("Component is not child");
            }

        Component cdParent   = getParent();
        String[]  asCategory = cdParent.getCategories();

        for (int i = 0; i < asCategory.length; i++)
            {
            String sCategory = asCategory[i];
            if (this.isDerivedFrom(sCategory))
                {
                return sCategory;
                }
            }

        throw new IllegalStateException("Missing category for " + getQualifiedName());
        }

    // ----- component accessors:  aggregation definition -------------------

    /**
    * Get the list of all aggregated (child) components within this Component
    * Definition.  This list includes children that have been removed.
    *
    * @return an array of child component names
    */
    public String[] getChildren()
        {
        return m_tblChildren.strings();
        }

    /**
    * Determine if the specified child name is taken.
    *
    * @return true if the specified name is taken
    */
    public boolean isChild(String sName)
        {
        return m_tblChildren.contains(sName);
        }

    /**
    * Get a named aggregated component within this Component Definition.
    *
    * @param sChild the unqualified name of the child Component Definition
    *
    * @return the specified child Component Definition or null if the specified
    *         child has been removed or does not exist
    */
    public Component getChild(String sChild)
        {
        Component cd = (Component) m_tblChildren.get(sChild);
        if (cd != null)
            {
            int nState = cd.m_nFlags & EXISTS_MASK;
            if (nState == EXISTS_INSERT || nState == EXISTS_UPDATE)
                {
                return cd;
                }
            }

        return null;
        }

    /**
    * Get a component relative to this Component Definition.
    *
    * @param sLocal  the "local id" that maps from this component to the desired
    *                child component
    *
    * @return the specified child Component Definition or null if the specified
    *         child has been removed or does not exist
    */
    public Component getLocal(String sLocal)
        {
        Component cd = this;
        String[] asSimple = parseDelimitedString(sLocal, LOCAL_ID_DELIM);
        for (int i = 0, c = asSimple.length; i < c; ++i)
            {
            cd = cd.getChild(asSimple[i]);
            if (cd == null)
                {
                break;
                }
            }

        return cd;
        }

    /**
    * Determine whether additional child components can be added to this
    * Component Definition.
    *
    * @param cdSuper  super Component of the proposed child
    * @param sChild   proposed child name (local Component name)
    *
    * @return true if additional child components are allowed
    */
    public boolean isChildAddable(Component cdSuper, String sChild)
        {
        return isModifiable()
                && isSimpleNameLegal(sChild, false)
                && !m_tblChildren.contains(sChild)
                // cp 2001.03.21 ensure that the super is global
                && cdSuper.isGlobal()
                && !cdSuper.isFinal()
                // cp 2000.03.21 disallow addition of child that inherits from
                //               a static global
                // gg 2001.03.30 belay that order (but the child must stay static)
                // && !cdSuper.isStatic()
                && isChildSuperLegal(cdSuper.getQualifiedName());
        }

    /**
    * Determine whether the specified super is in a legal category.
    *
    * @param sSuper  fully qualified global Component Definition identity
    *
    * @return true if super is of a declared category
    */
    public boolean isChildSuperLegal(String sSuper)
        {
        // check if the super component for the proposed child derives
        // from a component specified in the aggregate declaration
        do
            {
            if (m_tblCategories.contains(sSuper))
                {
                return true;
                }

            int ofLastDot = sSuper.lastIndexOf(GLOBAL_ID_DELIM);
            if (ofLastDot < 0)
                {
                return false;
                }

            sSuper = sSuper.substring(0, ofLastDot);
            }
        while (true);
        }

    /**
    * Add a child Component Definition to this Component Definition.
    *
    * @param sChild  the unqualified local name for the child
    * @param cdSuper the Component Definition that the child will derive from
    *
    * @return the new child component
    *
    * @exception ComponentException
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public Component addChild(Component cdSuper, String sChild)
            throws ComponentException, PropertyVetoException
        {
        return addChild(cdSuper, sChild, true);
        }

    /**
    * Add a child Component Definition to this Component Definition.
    *
    * @param sChild     the unqualified local name for the child
    * @param cdSuper    the Component Definition that the child will derive from
    * @param fVetoable  true if the method can veto the change
    *
    * @return the new child component
    *
    * @exception ComponentException
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected Component addChild(Component cdSuper, String sChild, boolean fVetoable)
            throws ComponentException, PropertyVetoException
        {
        // check if a child can be added
        if (fVetoable && (cdSuper == null || cdSuper.getMode() != RESOLVED ||
                !isChildAddable(cdSuper, sChild)))
            {
            subNotAddable(ATTR_CHILD, sChild);
            }

        // create a child derivation from the specified super
        Component cdDelta = (Component) cdSuper.getNullDerivedTrait(this, DERIVATION);
        cdDelta.m_sSuper  = cdSuper.getQualifiedName();
        cdDelta.m_sName   = sChild;
        cdDelta.setExists(EXISTS_INSERT);
        cdDelta.assignUID();

        // create the new child
        Loader    loader  = new NullStorage();
        ErrorList errlist = new ErrorList();
        Component cdChild = (Component) cdSuper.resolve(cdDelta, this, loader, errlist);
        cdChild.finalizeResolve(loader, errlist);
        if (errlist.isSevere())
            {
            errlist.print();
            throw new ComponentException(CLASS + ".addChild:  "
                    + "Serious errors occurred during resolution!");
            }

        // add the child
        return addChild(cdChild, fVetoable);
        }

    /**
    * Add a child Component Definition to this Component Definition.
    *
    * @param cdChild    the child component
    * @param fVetoable  true if the method can veto the change
    *
    * @return the new child component
    *
    * @exception ComponentException
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized Component addChild(Component cdChild, boolean fVetoable)
            throws ComponentException, PropertyVetoException
        {
        try
            {
            cdChild.assignUID();

            if (fVetoable)
                {
                // notify veto listeners
                fireVetoableSubChange(cdChild, SUB_ADD);
                }

            // add the new child
            m_tblChildren.put(cdChild.getName(), cdChild);

            // notify listeners
            cdChild.validate();
            fireSubChange(cdChild, SUB_ADD);
            }
        catch (PropertyVetoException e)
            {
            cdChild.invalidate();
            throw e;
            }

        return cdChild;
        }

    /**
    * Determine whether the specified child component can be removed from
    * this Component Definition.
    *
    * @param sChild  the unqualified local name of the child
    *
    * @return true if the child can be removed
    */
    public boolean isChildRemovable(String sChild)
        {
        // component definition must be modifiable, child must exist
        Component cd = (Component) m_tblChildren.get(sChild);
        if (isModifiable() && cd != null)
            {
            int nState = cd.m_nFlags & EXISTS_MASK;
            if (nState == EXISTS_INSERT || nState == EXISTS_UPDATE)
                {
                return true;
                }
            }

        return false;
        }

    /**
    * Remove the specified child from the Component Definition.
    *
    * @param sChild  the unqualified local name of the child
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public void removeChild(String sChild)
            throws PropertyVetoException
        {
        // verify child can be removed
        if (!isChildRemovable(sChild))
            {
            subNotRemovable(ATTR_CHILD, sChild);
            }

        removeChild((Component) m_tblChildren.get(sChild), true);
        }

    /**
    * Remove the specified child from the Component Definition.
    *
    * @param cd         the child component
    * @param fVetoable  true if the method can veto the change
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized void removeChild(Component cd, boolean fVetoable)
            throws PropertyVetoException
        {
        if (fVetoable)
            {
            // notify veto listeners
            fireVetoableSubChange(cd, SUB_REMOVE);
            }

        // cd.m_fBaseLevel same as:
        //      (cd.m_nPrevFlags & EXISTS_MASK) != EXISTS_INSERT
        // except that root Component is an EXISTS_INSERT so if it
        // were added as a child prev would be EXISTS_INSERT even
        // at the modification level it was inserted
        boolean fDestroy = cd.isDeclaredAtThisLevel();
        if (fDestroy)
            {
            // the child was added at this level so just discard it
            m_tblChildren.remove(cd.getName());
            }
        else
            {
            // the child was added at a previous level so mark it removed
            cd.m_nFlags = (cd.m_nFlags & ~EXISTS_MASK) | EXISTS_DELETE;
            }

        // notify listeners
        fireSubChange(cd, SUB_REMOVE);

        if (fDestroy)
            {
            // discard the component
            cd.invalidate();
            }
        }

    /**
    * Determine if this component and that component are part of the "same"
    * component.
    *
    * @param  that  the other component
    *
    * @return true if this and that are descendents (containment-wise) of
    *         the same component
    */
    public boolean isRelative(Component that)
        {
        return this.getGlobalParent() == that.getGlobalParent();
        }

    /**
    * Determine if the specified component can be copied to this component.
    *
    * @param cdSuper  the super of the child being copied
    * @param sChild   the name to give the child
    * @param cdChild  the child component to copy to this component
    *
    * @return  true if the specified child component can be copied to this
    *          component
    */
    public boolean isChildCopyable(Component cdSuper, String sChild, Component cdChild)
        {
        if (cdSuper == null || sChild == null || cdChild == null)
            {
            return false;
            }

        // child must derive from the provided super
        if (!cdChild.isDerivedFrom(cdSuper.getQualifiedName()))
            {
            return false;
            }

        // child must be addable to its new location
        if (!this.isChildAddable(cdSuper, sChild))
            {
            return false;
            }

        return true;
        }

    /**
    * Create a copy of the specified child under this Component.
    *
    * @param cdSuper    the super of the child being copied
    * @param sChild     the name to give the child
    * @param cdChild    the child component to adopt
    * @param loader     the loader to use if other information is required
    * @param errlist    the error list to log errors to (optional)
    *
    * @exception ComponentException
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public Component copyChild(Component cdSuper, String sChild, Component cdChild, Loader loader, ErrorList errlist)
            throws ComponentException, PropertyVetoException
        {
        return copyChild(cdSuper, sChild, cdChild, loader, errlist, true);
        }

    /**
    * Create a copy of the specified child under this Component.
    *
    * @param cdSuper    the super of the child being copied
    * @param sChild     the name to give the child
    * @param cdChild    the child component to adopt
    * @param loader     the loader to use if other information is required
    * @param errlist    the error list to log errors to (optional)
    * @param fVetoable  true if the method can veto the change
    *
    * @exception ComponentException
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected Component copyChild(Component cdSuper, String sChild, Component cdChild, Loader loader, ErrorList errlist, boolean fVetoable)
            throws ComponentException, PropertyVetoException
        {
        // verify legality of operation
        if (fVetoable && !isChildCopyable(cdSuper, sChild, cdChild))
            {
            subNotAddable(ATTR_CHILD, sChild);
            }

        // for debugging purposes, make sure there is an error list
        if (errlist == null)
            {
            errlist = new ErrorList();
            }

        // extract the child derivation
        Component cdDelta = (Component) cdChild.extract(cdSuper, this, loader, errlist);
        cdDelta.finalizeExtract(loader, errlist);
        if (errlist.isSevere())
            {
            errlist.print();
            throw new ComponentException(CLASS + ".copyChild:  "
                    + "Serious errors occurred during extraction!");
            }

        // it is a copy; nix the old UID
        cdDelta.clearUID();

        // same set-up processing as in addChild()
        cdDelta.m_sSuper  = cdSuper.getQualifiedName();
        cdDelta.m_sName   = sChild;
        cdDelta.setExists(EXISTS_INSERT);
        cdDelta.assignUID();

        // create the copy of the child
        cdChild = (Component) cdSuper.resolve(cdDelta, this, loader, errlist);
        cdChild.finalizeResolve(loader, errlist);
        if (errlist.isSevere())
            {
            errlist.print();
            throw new ComponentException(CLASS + ".copyChild:  "
                    + "Serious errors occurred during resolution!");
            }

        // add the child to this Component
        return addChild(cdChild, fVetoable);
        }

    /**
    * Determine if the specified component can be moved to this component.
    *
    * @param cdSuper  the super of the child being adopted
    * @param sChild   the name to give the child
    * @param cdChild  the child component to move to this component
    *
    * @return  true if specified child component can be moved to this
    *          component
    */
    public boolean isChildMoveable(Component cdSuper, String sChild, Component cdChild)
        {
        // child must be copyable
        if (!isChildCopyable(cdSuper, sChild, cdChild))
            {
            return false;
            }

        // child must be a "relative", since it will be moved from its
        // current location
        if (!isRelative(cdChild))
            {
            return false;
            }

        // child is only movable if it was added at this level
        if (!cdChild.isDeclaredAtThisLevel())
            {
            return false;
            }

        // child must be removable from its current location
        Component cdParent = cdChild.getParent();
        if (cdParent == null || !cdParent.isChildRemovable(cdChild.getName()))
            {
            return false;
            }

        return true;
        }

    /**
    * Move the specified child under this Component.
    *
    * @param cdSuper    the super of the child being moved
    * @param sChild     the name to give the child
    * @param cdChild    the child component to adopt
    * @param loader     the loader to use if other information is required
    * @param errlist    the error list to log errors to (optional)
    *
    * @exception ComponentException
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public Component moveChild(Component cdSuper, String sChild, Component cdChild, Loader loader, ErrorList errlist)
            throws ComponentException, PropertyVetoException
        {
        return moveChild(cdSuper, sChild, cdChild, loader, errlist, true);
        }

    /**
    * Move the specified child under this Component.
    *
    * @param cdSuper    the super of the child being moved
    * @param sChild     the name to give the child
    * @param cdChild    the child component to adopt
    * @param loader     the loader to use if other information is required
    * @param errlist    the error list to log errors to (optional)
    * @param fVetoable  true if the method can veto the change
    *
    * @exception ComponentException
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected Component moveChild(Component cdSuper, String sChild, Component cdChild, Loader loader, ErrorList errlist, boolean fVetoable)
            throws ComponentException, PropertyVetoException
        {
        // verify legality of operation
        if (fVetoable && !isChildMoveable(cdSuper, sChild, cdChild))
            {
            subNotAddable(ATTR_CHILD, sChild);
            }

        // for debugging purposes, make sure there is an error list
        if (errlist == null)
            {
            errlist = new ErrorList();
            }

        // extract the child derivation
        Component cdDelta = (Component) cdChild.extract(cdSuper, this, loader, errlist);
        cdDelta.finalizeExtract(loader, errlist);
        if (errlist.isSevere())
            {
            errlist.print();
            throw new ComponentException(CLASS + ".moveChild:  "
                    + "Serious errors occurred during extraction!");
            }

        // same set-up processing as in addChild() (except do not change UID)
        cdDelta.m_sSuper  = cdSuper.getQualifiedName();
        cdDelta.m_sName   = sChild;
        cdDelta.setExists(EXISTS_INSERT);

        // create the copy of the child
        Component cdNewChild = (Component) cdSuper.resolve(cdDelta, this, loader, errlist);
        cdNewChild.finalizeResolve(loader, errlist);
        if (errlist.isSevere())
            {
            errlist.print();
            throw new ComponentException(CLASS + ".moveChild:  "
                    + "Serious errors occurred during resolution!");
            }

        // remove the child from the other Component
        cdChild.getParent().removeChild(cdChild, fVetoable);

        // add the child to this Component
        return addChild(cdNewChild, fVetoable);
        }

    /**
    * Determine whether the specified child component can be un-removed from
    * this Component Definition.  Note that the use of the term un-removable
    * means that a removal has already occurred and the removal can be
    * undone (i.e. the component can be un-removed); it does not mean that
    * the component cannot be removed (i.e. the component is unremovable).
    *
    * @param sChild  the unqualified local name of the child
    *
    * @return true if the child can be un-removed
    */
    public boolean isChildUnremovable(String sChild)
        {
        // component definition must be modifiable, child must have been
        // removed at this level
        Component cd = (Component) m_tblChildren.get(sChild);
        return isModifiable() && cd != null && (cd.m_nFlags & EXISTS_MASK) == EXISTS_DELETE;
        }


    /**
    * Un-remove the specified child from the Component Definition.
    *
    * @param sChild  the unqualified local name of the child
    *
    * @return the component that was un-removed
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public Component unremoveChild(String sChild)
            throws PropertyVetoException
        {
        // verify child is un-removable
        if (!isChildUnremovable(sChild))
            {
            subNotUnremovable(ATTR_CHILD, sChild);
            }

        return unremoveChild((Component) m_tblChildren.get(sChild), true);
        }

    /**
    * Un-remove the specified child from the Component Definition.
    *
    * @param cd         the child component
    * @param fVetoable  true if the method can veto the change
    *
    * @return the component that was un-removed
    *
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    protected synchronized Component unremoveChild(Component cd, boolean fVetoable)
            throws PropertyVetoException
        {
        // check if there is nothing to do
        if ((cd.m_nFlags & EXISTS_MASK) != EXISTS_DELETE)
            {
            return cd;
            }

        if (fVetoable)
            {
            // notify veto listeners
            fireVetoableSubChange(cd, SUB_UNREMOVE);
            }

        // the child was added at a previous level and removed at this level,
        // so to un-remove it we just mark it as updated at this level
        cd.m_nFlags = (cd.m_nFlags & ~EXISTS_MASK) | EXISTS_UPDATE;

        // notify listeners
        fireSubChange(cd, SUB_UNREMOVE);

        return cd;
        }

    /**
    * Update the reference to a child when the child name changes.
    * This method is used internally by the Component Definition.
    *
    * @param sOld the old child name
    * @param sNew the new child name
    */
    protected void renameChild(String sOld, String sNew)
        {
        m_tblChildren.put(sNew, m_tblChildren.remove(sOld));
        }

    /**
    * Replace the super compoenent of the specified child.
    *
    * @param sChild     the name of the child component
    * @param cdOldSuper the super of the child being modified
    * @param cdNewSuper the new super component for the child component
    * @param loader     the loader to use if other information is required
    * @param errlist    the error list to log errors to (optional)
    *
    * @exception ComponentException
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public Component replaceChildSuper(String sChild, Component cdOldSuper, Component cdNewSuper, Loader loader, ErrorList errlist)
            throws ComponentException, PropertyVetoException
        {
        return replaceChildSuper(sChild, cdOldSuper, cdNewSuper, loader, errlist, true);
        }

    /**
    * Replace the super compoenent of the specified child.
    *
    * @param sChild     the name of the child component
    * @param cdOldSuper the super of the child being modified
    * @param cdNewSuper the new super component for the child component
    * @param loader     the loader to use if other information is required
    * @param errlist    the error list to log errors to (optional)
    *
    * @exception ComponentException
    * @exception PropertyVetoException if the property cannot be set as
    *            specified or was vetoed by a property listener
    */
    public Component replaceChildSuper(String sChild, Component cdOldSuper, Component cdNewSuper, Loader loader, ErrorList errlist, boolean fVetoable)
            throws ComponentException, PropertyVetoException
        {
        Component cdChild = (Component) m_tblChildren.get(sChild);

        // verify legality of operation
        if (fVetoable)
            {
            // child must be removable from its current location
            // child must derive from the provided super
            if (!isChildRemovable(sChild) || !cdChild.isDeclaredAtThisLevel() ||
                    !cdChild.isDerivedFrom(cdOldSuper.getQualifiedName()))
                {
                subNotRemovable(ATTR_CHILD, sChild);
                }

            if (cdOldSuper == null || cdNewSuper == null || cdNewSuper.getMode() != RESOLVED)
                {
                subNotAddable(ATTR_CHILD, sChild);
                }

            // notify veto listeners
            fireVetoableSubChange(cdChild, SUB_CHANGE);
            }

        // for debugging purposes, make sure there is an error list
        if (errlist == null)
            {
            errlist = new ErrorList();
            }

        // extract the child derivation
        Component cdDelta = (Component) cdChild.extract(cdOldSuper, this, loader, errlist);
        cdDelta.finalizeExtract(loader, errlist);
        if (errlist.isSevere())
            {
            errlist.print();
            throw new ComponentException(CLASS + ".replaceChildSuper:  "
                    + "Serious errors occurred during extraction!");
            }

        if (!cdDelta.m_sName.equals(sChild) ||
            !cdDelta.getUID().equals(cdChild.getUID()) ||
            cdDelta.getExists() != EXISTS_INSERT)
            {
            throw new ComponentException(CLASS + ".replaceChildSuper:  "
                    + "Critical attributes have changed during extraction!");
            }

        // replace the super name
        cdDelta.m_sSuper = cdNewSuper.getQualifiedName();

        // resolve against the new super
        cdChild = (Component) cdNewSuper.resolve(cdDelta, this, loader, errlist);
        cdChild.finalizeResolve(loader, errlist);
        if (errlist.isSevere())
            {
            errlist.print();
            throw new ComponentException(CLASS + ".replaceChildSuper:  "
                    + "Serious errors occurred during resolution!");
            }

        // replace the child
        m_tblChildren.put(sChild, cdChild);

        fireSubChange(cdChild, SUB_CHANGE);

        return cdChild;
        }

    // ----- JCS helpers ----------------------------------------------------

    /**
    * Add Java and JASM implementations to the JCS.
    *
    * @param clz    the ClassFile to create a signature from
    * @param sJava  the Java source code
    */
    public void addJcsImplementations(ClassFile clz, String sJava)
        {
        if (!isSignature())
            {
            throw new IllegalStateException();
            }

        Map map = Collections.EMPTY_MAP;
        if (sJava != null && sJava.length() > 0)
            {
            ScriptParser parser = new ScriptParser();
            map = parser.parse(sJava);
            }

        for (Enumeration enmr = m_tblBehavior.elements(); enmr.hasMoreElements(); )
            {
            Behavior beh     = (Behavior) enmr.nextElement();
            String   sLang   = null;
            String   sScript = null;

            if (!map.isEmpty())
                {
                // look up the source code for the behavior
                StringBuffer sb = new StringBuffer(getName());
                sb.append('.')
                  .append(beh.getName())
                  .append('(');
                DataType[] adt = beh.getParameterTypes();
                for (int i = 0, c = adt.length; i < c; ++i)
                    {
                    if (i > 0)
                        {
                        sb.append(',');
                        }
                    sb.append(toSimpleSignature(adt[i].getType()));
                    }
                sb.append(')');
                sLang   = "Java (Original)";
                sScript = (String) map.get(sb.toString());
                }

            if (sScript == null && clz != null)
                {
                // create the JASM for the behavior
                String sSig   = beh.getName() + beh.getJVMSignature();
                Method method = clz.getMethod(sSig);
                if (method != null)
                    {
                    CodeAttribute code = (CodeAttribute) method.getAttribute(
                        com.tangosol.dev.assembler.Constants.ATTR_CODE);
                    if (code != null)
                        {
                        sLang   = "Jasm (Original)";
                        sScript = code.toJasm();

                        // gg 2001.08.24 to improve readability...
                        String sClass = getName();
                        String sSuper = getSuperName();
                        int    ofPkg  = sClass.lastIndexOf('.');

                        sScript = replace(sScript, ' ' + sSuper + '.', " super.");

                        if (ofPkg > 0)
                            {
                            String sPackage = sClass.substring(0, ofPkg + 1);
                            sScript = replace(sScript, ' ' + sPackage, " ");
                            }
                        }
                    }
                }

            if (sScript != null)
                {
                beh.setJcsImplementation(sLang, sScript);
                }
            }
        }

    protected static String toSimpleSignature(Type type)
        {
        if (type instanceof ArrayType)
            {
            return toSimpleSignature(((ArrayType) type).getElementType()) + "[]";
            }
        else if (type instanceof ClassType)
            {
            return ((ClassType) type).getShortName();
            }
        else
            {
            return type.toString();
            }
        }

    /**
    * Undo the effects of addJcsImplementations.
    */
    public void removeJcsImplementations()
        {
        if (!isSignature())
            {
            throw new IllegalStateException();
            }

        for (Enumeration enmr = m_tblBehavior.elements(); enmr.hasMoreElements(); )
            {
            Behavior beh = (Behavior) enmr.nextElement();
            beh.removeJcsImplementation();
            }
        }


    // ----- compile plan ---------------------------------------------------

    /**
    * Determine what child classes must be created and what their supers will
    * be.  This method is only valid against a global (no parent) component.
    *
    * @param loader  the Loader to load super components from
    *
    * @return a CompilePlan
    */
    public CompilePlan getCompilePlan(Loader loader)
            throws ComponentException
        {
        return getCompilePlan(loader, false);
        }

    /**
    * Determine what child classes must be created and what their supers will
    * be.  This method is only valid against a global (no parent) component.
    *
    * @param loader  the Loader to load super components from
    * @param fDebug  true if building a compile plan for a debug compile
    *
    * @return a CompilePlan
    */
    public CompilePlan getCompilePlan(Loader loader, boolean fDebug)
            throws ComponentException
        {
        if (!isGlobal())
            {
            throw new IllegalStateException("Component is not global");
            }

        if (fDebug)
            {
            StringTable tbl = new StringTable();
            buildDebugSuperMap(tbl);
            return new CompilePlan(getQualifiedName(), tbl, tbl);
            }

        // determine what components will be present at this level
        StringTable tblDelta = new StringTable();
        Hashtable   tblCache = new Hashtable();
        buildDeltaMap(tblDelta, loader, tblCache);

        // determine the supers of each local component
        StringTable tblSuper  = new StringTable();
        Hashtable   tblDeltas = new Hashtable();
        resolveSuperMap(tblSuper, loader, tblCache, tblDeltas);

        return new CompilePlan(getQualifiedName(), tblSuper, tblDelta);
        }

    /**
    * Build the default super map.
    */
    private void buildDebugSuperMap(StringTable tbl)
        {
        String[] asChildren = getChildren();
        for (int i = 0; i < asChildren.length; i++)
            {
            Component child = getChild(asChildren[i]);
            if (child != null)
                {
                tbl.put(child.getLocalName(), child.getSuperName());
                child.buildDebugSuperMap(tbl);
                }
            }
        }

    /**
    * Build a table of all child (local) names that have deltas that would
    * require classes to be generated.
    *
    * @param tblDelta  the table into which the local names of delta children
    *                  will be placed
    * @param loader    the loader to use to load components not found in cache
    * @param tblCache  a Hashtable (optional) used to cache loaded components
    *
    * @return true if the component has a delta
    *
    * @throws ComponentException if the requested component cannot be located
    *         and successfully loaded
    */
    private boolean buildDeltaMap(StringTable tblDelta, Loader loader, Hashtable tblCache)
            throws ComponentException
        {
        boolean fDelta = false;

        // determine what children have deltas
        String[] asChildren = getChildren();
        for (int i = 0; i < asChildren.length; i++)
            {
            Component child = (Component) m_tblChildren.get(asChildren[i]);
            if (child != null)
                {
                switch (child.m_nFlags & EXISTS_MASK)
                    {
                    case EXISTS_INSERT:
                    case EXISTS_UPDATE:
                        if (child.buildDeltaMap(tblDelta, loader, tblCache)
                                // cp 2000.03.20 always gen child at insert level
                                || (child.m_nFlags & EXISTS_MASK) == EXISTS_INSERT)
                            {
                            // child has a delta, so add it to the delta table
                            tblDelta.add(child.getLocalName());
                            fDelta = true;
                            }
                        break;

                    case EXISTS_DELETE:
                        // child deleted at this derivation level
                        fDelta = true;
                        break;

                    case EXISTS_NOT:
                        // child does not exist
                        break;

                    default:
                        throw new IllegalStateException();
                    }
                }
            }

        if (!fDelta)
            {
            fDelta = !isClassDiscardable(getComponent(getSuperName(), loader, tblCache));
            }

        return fDelta;
        }

    /**
    * Build a table of all child (local) names and their supers.
    *
    * @param tblSuper   the table into which the local names of children
    *                   will be placed, mapping to their actual supers
    * @param loader     the loader to use to load components not found in cache
    * @param tblCache   a Hashtable used to cache loaded components
    * @param tblDeltas  a Hashtable used to cache delta maps
    *
    * @throws ComponentException if the requested component cannot be located
    *         and successfully loaded
    */
    private void resolveSuperMap(StringTable tblSuper, Loader loader, Hashtable tblCache, Hashtable tblDeltas)
            throws ComponentException
        {
        String[] asChildren = getChildren();
        for (int i = 0; i < asChildren.length; i++)
            {
            Component child = getChild(asChildren[i]);
            if (child != null)
                {
                tblSuper.put(child.getLocalName(), child.resolveSuper(loader, tblCache, tblDeltas));
                child.resolveSuperMap(tblSuper, loader, tblCache, tblDeltas);
                }
            }
        }

    /**
    * Determine the super for the specified component.
    *
    * @param loader     the loader to use to load components not found in cache
    * @param tblCache   a Hashtable used to cache loaded components
    * @param tblDeltas  a Hashtable used to cache component delta tables
    *
    * @return fully qualified name of the super component
    *
    * @throws ComponentException if a necessary component cannot be located and
    *         successfully loaded
    */
    private String resolveSuper(Loader loader, Hashtable tblCache, Hashtable tblDeltas)
            throws ComponentException
        {
        // global supers always exist
        String sSuper = getSuperName();
        if (isGlobal(sSuper))
            {
            return sSuper;
            }

        String    sSuperGlobal  = getGlobalName(sSuper);
        Component cdSuperGlobal = getComponent(sSuperGlobal, loader, tblCache);

        // get the delta map for the super to see if a local super has a delta
        StringTable tblDelta = (StringTable) tblDeltas.get(sSuperGlobal);
        if (tblDelta == null)
            {
            tblDelta = new StringTable();
            cdSuperGlobal.buildDeltaMap(tblDelta, loader, tblCache);
            tblDeltas.put(sSuperGlobal, tblDelta);
            }

        String sSuperLocal = getLocalName(sSuper);
        if (tblDelta.contains(sSuperLocal))
            {
            return sSuper;
            }

        // the expected super did not have a delta, so the super of this component is
        // the super of the previously-expected super
        Component cdSuperLocal = cdSuperGlobal.getLocal(sSuperLocal);
        return cdSuperLocal.resolveSuper(loader, tblCache, tblDeltas);
        }

    /**
    * Load the specified component definition.
    *
    * @param sName     fully qualified name of the component to load
    * @param loader    the loader to use to load components not found in cache
    * @param tblCache  a Hashtable (optional) used to cache loaded components
    *
    * @return the requested component (never null)
    *
    * @throws ComponentException if the requested component cannot be located and
    *         successfully loaded
    */
    private static Component getComponent(String sName, Loader loader, Hashtable tblCache)
            throws ComponentException
        {
        String sGlobal = getGlobalName(sName);
        Component cd = null;
        if (tblCache != null)
            {
            // check the cache
            cd = (Component) tblCache.get(sGlobal);
            }

        if (cd == null)
            {
            ErrorList errlist = new ErrorList();
            try
                {
                // use the loader to find the component
                cd = loader.loadComponent(sGlobal, true, errlist);
                }
            catch (ComponentException e)
                {
                out("Exception loading component " + sGlobal + ":");
                out(e);
                throw e;
                }
            finally
                {
                if (!errlist.isEmpty())
                    {
                    out("Errors encountered loading component " + sGlobal + ":");
                    errlist.print();
                    }
                }

            // update the cache
            if (tblCache != null && cd != null)
                {
                tblCache.put(sGlobal, cd);
                }
            }

        // if local, find the child
        if (!isGlobal(sName))
            {
            cd = cd.getLocal(getLocalName(sName));
            }

        // expected not to return null
        if (cd == null)
            {
            throw new ComponentException("Component " + sName + " does not exist");
            }

        return cd;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compares this Component to another Object for equality.
    *
    * @param obj  the other Object to compare to this
    *
    * @return true if this Component equals that Object
    */
    public boolean equals(Object obj)
        {
        if (obj instanceof Component)
            {
            Component that = (Component) obj;
            return this == that
                || (   this.m_nType             == that.m_nType
                    && this.m_nVersion          == VERSION
                    && this.m_nFlags            == that.m_nFlags
                    && this.m_nPrevFlags        == that.m_nPrevFlags
                    && this.m_fBaseLevel        == that.m_fBaseLevel
                    && this.m_sName        .equals(that.m_sName )
                    && this.m_sSuper       .equals(that.m_sSuper)
                    && (this.m_integration == null ? that.m_integration == null :
                        this.m_integration .equals(that.m_integration) )
                    && this.m_tblImplements.equals(that.m_tblImplements)
                    && this.m_tblDispatches.equals(that.m_tblDispatches)
                    && this.m_tblState     .equals(that.m_tblState     )
                    && this.m_tblBehavior  .equals(that.m_tblBehavior  )
                    && this.m_tblCategories.equals(that.m_tblCategories)
                    && this.m_tblChildren  .equals(that.m_tblChildren  )
                    && super.equals(that)
                    );
            }
        return false;
        }

    /**
    * Creates a cloned copy of this Component, including its sub-traits.
    *
    * @return a cloned copy of this Component
    *
    * @exception  CloneNotSupportedException  if the Component is not global
    */
    public Object clone()
             throws CloneNotSupportedException
        {
        // cannot clone a child component
        if (getParentTrait() != null)
            {
            throw new CloneNotSupportedException();
            }

        // use copy constructor (with no parent)
        Component cd = new Component(null, this);
        if (cd.getMode() == RESOLVED)
            {
            cd.validate();
            }
        return cd;
        }


    // ----- debug methods --------------------------------------------------

    /**
    * Provide the entire set of trait information in a printed format.
    *
    * @param out      PrintWriter object to dump the information to
    * @param sIndent  a string used to indent each line of dumped information
    */
    public void dump(PrintWriter out, String sIndent)
        {
        dump(out, sIndent, true);
        }

    /**
    * Provide the entire set of trait information in a printed format.
    *
    * @param out      PrintWriter object to dump the information to
    * @param sIndent  a string used to indent each line of dumped information
    * @param fIndentFirstLine  false to suppress first line indention
    */
    public void dump(PrintWriter out, String sIndent, boolean fIndentFirstLine)
        {
        toXml().writeXml(out, true);
        /*
        boolean fDerOrMod = (getMode() == MODIFICATION || getMode() == DERIVATION);
        int     nSpecable = EXISTS_SPECIFIED |
                            VIS_SPECIFIED    |
                            SCOPE_SPECIFIED  |
                            IMPL_SPECIFIED   |
                            DERIVE_SPECIFIED |
                            ANTIQ_SPECIFIED  |
                            STG_SPECIFIED    |
                            DIST_SPECIFIED;
        if (isSignature())
            {
            nSpecable |= MISC_ISINTERFACE | MISC_ISTHROWABLE | ACCESS_SPECIFIED;
            }

        if (fIndentFirstLine)
            {
            out.print(sIndent);
            }

        switch (m_nType)
            {
            case COMPLEX:
                out.print("complex property value of type ");
                // no break;
            case COMPONENT:
                out.print("component " + m_sName);
                break;
            case SIGNATURE:
                out.print("Java Class Signature (JCS) " + m_sName);
                break;
            default:
                out.print("<invalid> " + m_sName);
                break;
            }
        out.print  (" derives from " + (m_sSuper == null || m_sSuper.length() == 0 ? "<none>" : m_sSuper));
        out.println(" (base level=" + m_fBaseLevel + ')');
        out.println(sIndent + "qualified=" + getQualifiedName());

        // trait attributes
        super.dump(out, sIndent);

        // integration
        if (m_integration != null)
            {
            out.println(sIndent + "integration " + m_integration.getUniqueName());
            m_integration.dump(out, nextIndent(sIndent));
            }

        // implements
        dump(out, sIndent, m_tblImplements, "interfaces implemented");

        // dispatches
        dump(out, sIndent, m_tblDispatches, "interfaces dispatched");

        // flags
        out.print  (sIndent + "flags=0x" + toHexString(m_nFlags, 8));
        out.println(" (" + flagsDescription(m_nFlags, nSpecable, fDerOrMod) + ')');

        // previous flags
        out.print  (sIndent + "prev flags=0x" + toHexString(m_nPrevFlags, 8));
        out.println(" (" + flagsDescription(m_nPrevFlags, nSpecable, fDerOrMod) + ')');

        // state
        dump(out, sIndent, m_tblState, "properties");

        // behavior
        dump(out, sIndent, m_tblBehavior, "behaviors");

        // aggregate categories
        out.println(sIndent + "aggregates " + arrayDescription(m_tblCategories.strings()));

        // children
        dump(out, sIndent, m_tblChildren, "children");
        */
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Get the name of the root Component Definition.
    *
    * @return the name of the root Component Definition
    */
    public static String getRootName()
        {
        return "Component";
        }

    /**
    * Determine if the passed string is a legal simple (unqualified) name
    * for a Component Definition.
    *
    * According to section 7.1 of the Java Language Specification,
    * Second Edition, it is illegal for a package to contain a class
    * or interface type and a subpackage with the same name.
    * To inforce this rule, all global Component Definitions have names
    * that start with an upper case letter and the corresponding packages
    * just have this first letter lower-cased.
    *
    * @param sName   the string containing the unqualified name
    * param  fGlobal if true, check the global name, otherwise local
    *
    * @return true if the specified name is legal, false otherwise
    *
    * @see com.tangosol.dev.component.ComponentType#getComponentPackage(Component)
    */
    public static boolean isSimpleNameLegal(String sName, boolean fGlobal)
        {
        return sName != null
            && sName.indexOf(GLOBAL_ID_DELIM) < 0
            && sName.indexOf(LOCAL_ID_DELIM)  < 0
            && (!fGlobal || Character.isUpperCase(sName.charAt(0)))
            && ClassHelper.isSimpleNameLegal(sName);
        }

    /**
    * Determine if the passed string is a legal fully qualified Component
    * Definition name.
    *
    * @param sName  the string containing the qualified name
    *
    * @return true if a legal identifier, false otherwise
    */
    public static boolean isQualifiedNameLegal(String sName)
        {
        String sRoot = getRootName();

        // check for obvious problems (blank, doesn't start with root component name)
        if (sName == null || sName.length() == 0 || !sName.startsWith(sRoot))
            {
            return false;
            }

        // check for the name of the root component
        int cchName = sName.length();
        int cchRoot = sRoot.length();
        if (cchName == cchRoot)
            {
            return true;
            }

        try
            {
            // check for delimiter following root name
            char ch = sName.charAt(cchRoot);
            if (ch != GLOBAL_ID_DELIM && ch != LOCAL_ID_DELIM)
                {
                return false;
                }

            // check for illegal identifier or qualification in the name (A.B$c$d)
            int     ofStart = 0;
            boolean fGlobal = true;

            while (true)
                {
                int ofEnd = sName.indexOf(GLOBAL_ID_DELIM, ofStart);
                if (ofEnd > 0)
                    {
                    if (!isSimpleNameLegal(sName.substring(ofStart, ofEnd), fGlobal))
                        {
                        return false;
                        }
                    ofStart = ofEnd + 1;
                    }
                else
                    {
                    break;
                    }
                }

            while (true)
                {
                int ofEnd = sName.indexOf(LOCAL_ID_DELIM, ofStart);
                if (ofEnd > 0)
                    {
                    if (!isSimpleNameLegal(sName.substring(ofStart, ofEnd), fGlobal))
                        {
                        return false;
                        }
                    ofStart = ofEnd + 1;
                    fGlobal = false;
                    }
                else
                    {
                    break;
                    }
                }

            if (!isSimpleNameLegal(sName.substring(ofStart), fGlobal))
                {
                return false;
                }
            }
        catch (StringIndexOutOfBoundsException e)
            {
            return false;
            }


        return true;
        }

    /**
    * Determine if the passed string is a global qualified name (not local).
    *
    * @param sName the string containing the name
    *
    * @return true if a qualified global Component Definition name
    */
    public static boolean isGlobalNameLegal(String sName)
        {
        return isQualifiedNameLegal(sName) && sName.indexOf(LOCAL_ID_DELIM) < 0;
        }

    /**
    * Determine if the fully qualifed component name is a global name.
    *
    * @param sName  fully qualified name
    *
    * @return true if a global name
    */
    public static boolean isGlobal(String sName)
        {
        return sName.indexOf(LOCAL_ID_DELIM) < 0;
        }

    /**
    * Determine the global portion of a fully qualifed component name.
    * For example, if the qualified name is "X.Y$A$B", the global name is "X.Y".
    *
    * @param sName  fully qualified name to get a global name of
    *
    * @return the global name
    */
    public static String getGlobalName(String sName)
        {
        int of = sName.indexOf(LOCAL_ID_DELIM);
        return of < 0 ? sName : sName.substring(0, of);
        }

    /**
    * Determine the simple local portion of a fully qualifed component name.
    * For example, if the qualified name is X.Y$A$B, the simple local name is B.
    *
    * @param sName  fully qualified name to get a simple name of
    *
    * @return the simple local name, or null if the component name
    *         is global
    */
    public static String getSimpleName(String sName)
        {
        int of = sName.lastIndexOf(LOCAL_ID_DELIM);
        return of < 0 ? null : sName.substring(of + 1);
        }

    /**
    * Determine the local portion of a fully qualifed component name.
    * For example, if the qualified name is X.Y$A$B, the local portion is A$B.
    *
    * @param sName  fully qualified name to get a local name of
    *
    * @return the local portion of the component name, or null if the
    *         component name is global
    */
    public static String getLocalName(String sName)
        {
        int of = sName.indexOf(LOCAL_ID_DELIM);
        return of < 0 ? null : sName.substring(of + 1);
        }

    /**
    * Determine if the first qualified global name denotes a Component
    * Definition that derives from the Component Definition denoted by
    * the second qualified global name.
    *
    * @param sSub    the sub in question
    * @param sSuper  the super in question
    *
    * @return true if the sub derives, directly or indirectly, from the super
    */
    public static boolean isDerivedFrom(String sSub, String sSuper)
        {
        return isGlobalNameLegal(sSub) &&
                (sSub.equals(sSuper) || sSub.startsWith(sSuper + '.'));
        }

    /**
    * Determine if this Component derives from a Component with the specified
    * (fully qualified) name, assuming that such a Component does exist.
    *
    * @param sSuperName  the qulified name of the super-component in question
    *
    * @return true if this Component derives from the Component
    */
    public boolean isDerivedFrom(String sSuperName)
        {
        String sThisName = this.getQualifiedName();
        String sThatName = sSuperName;

        if (isGlobal())
            {
            return isDerivedFrom(sThisName, sThatName);
            }

        // could be same component
        if (sThisName.equals(sThatName))
            {
            return true;
            }

        // that could be a global name
        if (isGlobalNameLegal(sThatName))
            {
            return isDerivedFrom(this.getGlobalSuperName(), sThatName);
            }

        // both are children
        int iPos = sThisName.lastIndexOf(LOCAL_ID_DELIM);
        String sThisLocalName  = sThisName.substring(iPos + 1);
        String sThisGlobalName = sThisName.substring(0, iPos);

        iPos = sThatName.lastIndexOf(LOCAL_ID_DELIM);
        String sThatLocalName  = sThatName.substring(iPos + 1);
        String sThatGlobalName = sThatName.substring(0, iPos);

        return sThisLocalName.equals(sThatLocalName) &&
            isDerivedFrom(sThisGlobalName, sThatGlobalName);
        }

    /**
    * Determine if this Component derives from the specified Component.
    *
    * @param cdSuper  the super-component in question
    *
    * @return true if this Component derives from the passed Component
    */
    public boolean isDerivedFrom(Component cdSuper)
        {
        final Component that = cdSuper;

        // could be same component
        String sThisName = this.getQualifiedName();
        String sThatName = that.getQualifiedName();
        if (sThisName.equals(sThatName))
            {
            return true;
            }

        // this could be a child
        Component cdThisParent = this.getParent();
        Component cdThatParent = that.getParent();
        if (cdThisParent != null)
            {
            if (cdThatParent == null)
                {
                // other component is not a child ... so this child's global
                // super must derive from the other component in order for
                // this child to derive from the other component
                sThisName = getGlobalSuperName();
                }
            else
                {
                // both components are children ... this child derives
                // from that child iff this child's name is the same as
                // that child's name and this child's parent derives
                // from that child's parent
                return this.getName().equals(that.getName()) &&
                        cdThisParent.isDerivedFrom(cdThatParent);
                }
            }

        // both sThisName and sThatName are fully qualified global names
        return sThisName.equals(sThatName) || sThisName.startsWith(sThatName + '.');
        }

    /**
    * Get the default collator used for keeping identifiers unique etc.
    *
    * @return a case-insensitive collator based on the English character set
    */
    protected static Collator getDefaultScriptingCollator()
        {
        Collator collator = Collator.getInstance(Locale.ENGLISH);
        // gg 2001.03.23 changed the PRIMARY strength to SECONDARY
        // since the PRIMARY strength collator ignores the white space
        // so strings "abc", "ab c" and "a b    c" are considered equal
        collator.setStrength(Collator.SECONDARY);
        return new CacheCollator(collator);
        }


    // ----- unit test ------------------------------------------------------

    /**
    * Output a given CDB file in binary or XML format to the opposite format.
    *
    * Usage: Component [CBD or XML input file] (output file)
    */
    public static void main(String[] asArg)
        {
        int cArg = asArg.length;
        if (cArg < 1)
            {
            usage();
            return;
            }

        File fileIn = new File(asArg[0]);
        if (!fileIn.isFile())
            {
            usage();
            return;
            }

        InputStream  in  = null;
        OutputStream out = null;
        try
            {
            // determine the input CDB format based upon the file extension
            boolean fBin = fileIn.getName().toLowerCase().endsWith(".cdb");

            // create the input and output streams
            in = new FileInputStream(fileIn);
            if (cArg > 1)
                {
                File fileOut = new File(asArg[1]);
                if (fileOut.exists())
                    {
                    err("Cannot overwrite existing file: " + fileOut);
                    return;
                    }

                out = new FileOutputStream(fileOut);
                }
            else
                {
                out = System.out;
                }

            // create the Component from the input stream
            Component cd = fBin
                    ? new Component(new DataInputStream(in))
                    : new Component(XmlHelper.loadXml(in));

            // output the Component to the oposite format
            if (fBin)
                {
                cd.dump(new PrintWriter(out, true), BLANK);
                }
            else
                {
                cd.save(new DataOutputStream(out));
                }
            }
        catch (Exception e)
            {
            err("Error processing: " + fileIn);
            err(e);
            }
        finally
            {
            if (in != null)
                {
                try
                    {
                    in.close();
                    }
                catch (IOException e) {}
                }
            if (out != null)
                {
                try
                    {
                    out.close();
                    }
                catch (IOException e) {}
                }
            }
        }

    /**
    * Output command line usage.
    */
    public static void usage()
        {
        out("Usage: Component [CBD or XML input file] (output file)");
        }


    // ----- public constants -----------------------------------------------

    /**
    * The Name attribute name.
    */
    public static final String ATTR_NAME        = "Name";

    /**
    * The Visible attribute name.
    */
    public static final String ATTR_VISIBLE     = "Visible";

    /**
    * The Static attribute name.
    */
    public static final String ATTR_STATIC      = "Static";

    /**
    * The Abstract attribute name.
    */
    public static final String ATTR_ABSTRACT    = "Abstract";

    /**
    * The Final attribute name.
    */
    public static final String ATTR_FINAL       = "Final";

    /**
    * The Deprecated attribute name.
    */
    public static final String ATTR_DEPRECATED  = "Deprecated";

    /**
    * The Persistent attribute name.
    */
    public static final String ATTR_PERSISTENT  = "Persistent";

    /**
    * The Remote attribute name.
    */
    public static final String ATTR_REMOTE      = "Remote";

    /**
    * The Integration attribute name.
    */
    public static final String ATTR_INTEGRATION = "Integration";

    /**
    * The Implements interface attribute name.
    */
    public static final String ATTR_IMPLEMENTS  = "Implements";

    /**
    * The Dispatches event interface attribute name.
    */
    public static final String ATTR_DISPATCHES  = "Dispatches";

    /**
    * The Property attribute name.
    */
    public static final String ATTR_PROPERTY    = "Property";

    /**
    * The Behavior attribute name.
    */
    public static final String ATTR_BEHAVIOR    = "Behavior";

    /**
    * The Category attribute name.
    */
    public static final String ATTR_CATEGORY    = "Category";

    /**
    * The Child attribute name.
    */
    public static final String ATTR_CHILD       = "Child";


    // ----- private constants ----------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Component";

    /**
    * The Component's descriptor string.
    */
    protected static final String DESCRIPTOR_COMPONENT = CLASS;

    /**
    * Complex property's descriptor string.
    */
    protected static final String DESCRIPTOR_COMPLEX   = "Complex";

    /**
    * Java Class Signature's descriptor string.
    */
    protected static final String DESCRIPTOR_SIGNATURE = "Signature";

    /**
    * Category:  Component Definition
    */
    protected static final int COMPONENT = 0;

    /**
    * Category:  Complex Property value
    */
    protected static final int COMPLEX   = 1;

    /**
    * Category:  Java Class Signature (JCS)
    */
    protected static final int SIGNATURE = 2;

    /**
    * The descriptor strings indexed by the interface type.
    */
    private String[] DESCRIPTORS = {DESCRIPTOR_COMPONENT,
                                    DESCRIPTOR_COMPLEX,
                                    DESCRIPTOR_SIGNATURE};

    /**
    * The attributes that can affect class generation.
    */
    private static final int CLASSGEN_FLAGS = ACCESS_MASK |
                                              // gg: 2002.09.17 commented out
                                              // VIS_MASK    |
                                              // ANTIQ_MASK  |
                                              SCOPE_MASK  |
                                              IMPL_MASK   |
                                              DERIVE_MASK |
                                              STG_MASK    |
                                              DIST_MASK;

    /**
    * The default attribute values.
    */
    private static final int DEFAULTS = ACCESS_PUBLIC    |
                                        VIS_VISIBLE      |
                                        SCOPE_INSTANCE   |
                                        IMPL_CONCRETE    |
                                        DERIVE_DERIVABLE |
                                        ANTIQ_CURRENT    |
                                        STG_TRANSIENT    |
                                        DIST_LOCAL;

    /**
    * Token representing a reachable base trait.
    * (Temporary used during resolve)
    */
    private static final Object BASE_EXISTS     = new Object();

    /**
    * Token representing an unreachable base trait.
    * (Temporary used during resolve)
    */
    private static final Object BASE_HIDDEN     = new Object();

    /**
    * Token representing a reserved base trait.
    * (Temporary used during resolve)
    */
    private static final Object BASE_RESERVED   = new Object();

    /**
    * Token representing an insert trait at this level.
    * (Temporary used during resolve)
    */
    private static final Object THIS_INSERT     = new Object();

    /**
    * Token representing an update trait at this level.
    * (Temporary used during resolve)
    */
    private static final Object THIS_UPDATE     = new Object();


    // ----- data members ---------------------------------------------------

    /**
    * Components that are loaded from a persistent storage could have versions
    * that are older that the current one. The "equals()" operation always considers
    * the older versions of components as "not equal" to newer ones.
    *
    * Notes:  immutable
    *
    * @see #equals(Object)
    */
    private transient int m_nVersion = VERSION;

    /**
    * The Component class has several purposes:
    *   1.  Component Definition
    *   2.  Complex Property value
    *   3.  Java Class Signature (JCS)
    * This field identifies which this Component instance is.
    *
    * Notes:  immutable
    *
    * @see #COMPONENT
    * @see #COMPLEX
    * @see #SIGNATURE
    */
    private int m_nType;

    /**
    * True if the component is read only.
    */
    private transient boolean m_fReadOnly;

    /**
    * Component Definition/Complex:  The global super Component name.
    *
    * Java Class Signature:  The fully qualified class name which this
    * JCS extends.
    *
    * Notes:  immutable
    */
    private String m_sSuper = BLANK;

    /**
    * Component Definition/Complex:  The Component's name.
    *
    * Java Class Signature:  The fully qualified class name.
    *
    * Notes:  immutable for global components
    */
    private String m_sName;

    /**
    * Component Definition attribute:  If this Component Definition is the
    * result of a modification, then m_fBaseLevel is false.  Integration,
    * interface implementation, and interface dispatch cannot occur except
    * at the base level.
    *
    * Complex/Java Class Signature:  Not used.
    */
    private boolean m_fBaseLevel;

    /**
    * Component Definition attribute:  The Component Definition may integrate
    * an "external" Java class, Javabean, CORBA interface, or other "model".
    * This is the trait which maps the integration to any integrated
    * Properties and Behaviors.
    *
    * Complex/Java Class Signature:  Not used.
    */
    private Integration m_integration;

    /**
    * Component Definition/Java Class Signature:  The component implements
    * Java interfaces.  The key of the string table is the interface name.
    * The corresponding value is the Interface which maps to any Behaviors.
    *
    * Complex:  Not used.
    */
    private StringTable m_tblImplements;

    /**
    * Component Definition attribute:  The component may dispatch various
    * JavaBean events.  The key of the string table is the interface name.
    * The value is the interface which maps to any dispatched Behaviors.
    *
    * Complex/Java Class Signature:  Not used.
    */
    private StringTable m_tblDispatches;

    /**
    * Component Definition/Complex/Java Class Signature:  This Component
    * Definition has various attributes which are represented by bit flags:
    * Exists, Visible, Access (JCS only), Static, Abstract, Final,
    * Deprecation, Storage, and Distribution.
    *
    * @see com.tangosol.dev.component.Constants
    */
    private int m_nFlags;

    /**
    * Component Definition/Complex:  The legal ranges for this Component
    * Definition's attribute flags may be dependent on the super (in the case
    * of derivation) or base (in the case of modification) Component
    * Definition; those values are stored here for reference.
    *
    * Java Class Signature:  Not used.
    */
    private int m_nPrevFlags;

    /**
    * Component Definition/Complex:  The declared and/or defined set of
    * component properties.
    *
    * Java Class Signature:  Class fields (public/protected).
    */
    private StringTable m_tblState;

    /**
    * Component Definition Behavior:  The declared and/or defined set of
    * component behaviors.
    *
    * Complex:  Not used.
    *
    * Java Class Signature:  Class methods (public/protected).
    */
    private StringTable m_tblBehavior;

    /**
    * Component Definition Aggregate Declaration section:  The declared set
    * of aggregate categories.  Each category is named and has an associated
    * Component type which specifies what components can be aggregated.  The
    * associated value is a boolean true if the category was added at this
    * level.
    *
    * Complex/Java Class Signature:  Not used.
    */
    private StringTable m_tblCategories;

    /**
    * Component Definition Aggregate Definition section:  The aggregated
    * Component Definitions, accessed by name.  The associated value is the
    * child Component Definition instance.
    *
    * Complex/Java Class Signature:  Not used.
    */
    private StringTable m_tblChildren;
    }
