/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;

import java.text.Collator;

import java.util.Enumeration;

import com.tangosol.util.ErrorList;
import com.tangosol.util.StringTable;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.run.xml.XmlElement;


/**
* An interface is a helper class for navigating to associated properties
* and behaviors.  Note that an interface contains immutable information
* about behaviors and properties; all derived/modified interfaces therefore
* can directly copy the data structures from the base interface trait.
*
* @version 1.00, 08/13/98
* @author  Cameron Purdy
*/
public class Interface
        extends    Trait
        implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct an Interface.  This is the "default" constructor.  This
    * constructor is typically not used directly; there are two helper
    * constructors which instantiate "implements", and "dispatches" interfaces.
    *
    * @param cd     the Component with this Interface
    * @param sName  the name of this Interface
    * @param nType  the type of this Interface
    *               (one of IMPLEMENTS, or DISPATCHES)
    * @param enmrBehavior  an enumeration of behavior signatures
    * @param enmrState     an enumeration of property names
    */
    protected Interface(Component cd, String sName, int nType,
            Enumeration enmrBehavior, Enumeration enmrState)
        {
        super(cd, RESOLVED);

        // assertion:  Component required
        if (cd == null)
            {
            throw new IllegalArgumentException(CLASS +
                                               ":  Containing Component required.");
            }

        // assertion:  valid name
        if (sName == null || sName.length() == 0)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Invalid interface name (" + sName + ")");
            }

        // assertion:  valid type
        if (nType != IMPLEMENTS && nType != DISPATCHES)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Invalid interface type (" + nType + ")");
            }

        // store interface name/type
        m_sName = sName;
        m_nType = nType;

        StringTable tbl;
        
        // populate behaviors table if provided
        if (enmrBehavior == null)
            {
            tbl = null;
            }
        else
            {
            tbl = new StringTable();
            while (enmrBehavior.hasMoreElements())
                {
                String sBhvr = (String) enmrBehavior.nextElement();

                // this could be the static initializer from a JCS
                if (!Behavior.isConstructor(sBhvr))
                    {
                    tbl.add(sBhvr);
                    }
                }
            }
        m_tblBehavior = tbl;
        
        // populate properties table if provided
        if (enmrState == null)
            {
            tbl = null;
            }
        else
            {
            tbl = new StringTable();
            while (enmrState.hasMoreElements())
                {
                tbl.add((String) enmrState.nextElement());
                }
            }
        m_tblState = tbl;
        }
    

    /**
    * Construct an implements/dispatches Interface.
    *
    * @param cd         the Component with this Interface
    * @param cdJCS      the Java Class Signature
    * @param fDispatch  true if an event interface (i.e. "dispatches");
    *                   false if a normal Java interface (i.e. "implements")
    */
    protected Interface(Component cd, Component cdJCS, boolean fDispatch)
        {
        this(cd, cdJCS.getName(), (fDispatch ? DISPATCHES : IMPLEMENTS),
                new SimpleEnumerator(cdJCS.getBehavior()),
                new SimpleEnumerator(cdJCS.getProperty()));

        if (!cdJCS.isInterface())
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Interface type required!");
            }

        // event interfaces have implied add/remove methods
        if (fDispatch)
            {
            String sIface = m_sName;
            String sClass = DataType.getClassType(sIface).getTypeString();
            String sSig   = sIface.substring(sIface.lastIndexOf('.') + 1) + '(' + sClass + ')';

            StringTable tbl = m_tblBehavior;
            tbl.add("add"    + sSig);
            tbl.add("remove" + sSig);
            }
        }

    /**
    * Construct an Interface using reflection.
    *
    * @param cd   the Component with this Interface
    * @param clz  the Java Class Signature
    */
    protected Interface(Component cd, Class clz)
        {
        this(cd, clz.getName(), IMPLEMENTS,
                NullImplementation.getEnumeration(), NullImplementation.getEnumeration());

        if (!clz.isInterface())
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Interface type required!");
            }
        }

    /**
    * Construct an Interface for JCS Derivation
    *
    * @param cdJCS      the Java Class Signature with this Interface
    * @param sInterface the name of the interface
    *                   false if a normal Java interface (i.e. "implements")
    */
    protected Interface(Component cdJCS, String sInterface)
        {
        this(cdJCS, sInterface, IMPLEMENTS, null, null);

        if (!cdJCS.isSignature())
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Java Class Signature required!");
            }
        }

    /**
    * Construct a blank Interface Trait.
    *
    * @param base   the base Interface to derive from
    * @param cd     the containing Component
    * @param nMode  one of RESOLVED, DERIVATION, MODIFICATION
    *
    * @see #getBlankDerivedTrait(Trait, int)
    */
    protected Interface(Interface base, Component cd, int nMode)
        {
        super(base, cd, nMode);
        }

    /**
    * Copy constructor.
    *
    * @param cd    the Component containing the new Interface
    * @param that  the Interface to copy from
    */
    protected Interface(Component cd, Interface that)
        {
        super(cd, that);

        this.m_nType       = that.m_nType;
        this.m_sName       = that.m_sName;
        this.m_tblBehavior = that.m_tblBehavior;
        this.m_tblState    = that.m_tblState;
        }

    /**
    * Construct an Interface from a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param cd        the containing Component
    * @param stream    the stream to read this Interface from
    * @param nVersion  version of the data structure in the stream
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            reading the Interface from the stream or if the stream
    *            does not contain a valid Interface
    */
    protected Interface(Component cd, DataInput stream, int nVersion)
            throws IOException
        {
        super(cd, stream, nVersion);

        // name of interface
        m_sName = stream.readUTF();

        // interface type
        m_nType = stream.readInt();

        // details only exist if component was stored resolved
        if (stream.readBoolean())
            {
            // behaviors
            StringTable tbl = new StringTable();
            int c = stream.readInt();
            for (int i = 0; i < c; ++i)
                {
                tbl.add(stream.readUTF());
                }
            m_tblBehavior = tbl;

            // properties
            tbl = new StringTable();
            c   = stream.readInt();
            for (int i = 0; i < c; ++i)
                {
                tbl.add(stream.readUTF());
                }
            m_tblState = tbl;
            }
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
    protected Interface(Trait parent, XmlElement xml, int nVersion)
            throws IOException
        {
        super(parent, xml, nVersion);

        String sName = readString(xml.getElement("name"));
        String sType = readString(xml.getElement("type"));
        if (sName == BLANK || sType == BLANK)
            {
            throw new IOException("name or type is missing");
            }

        m_sName = sName;
        for (int i = /* NOT 0! */ 1, c = DESCRIPTORS.length; i < c; ++i)
            {
            if (DESCRIPTORS[i].equalsIgnoreCase(sType))
                {
                m_nType = i;
                break;
                }
            }
        if (m_nType < 1)
            {
            throw new IOException("illegal type: " + sType);
            }

        m_tblBehavior = readTableKeys(xml, "methods", "method");
        m_tblState    = readTableKeys(xml, "fields", "field");
        if (m_tblBehavior != null || m_tblState != null)
            {
            // binary serialization would have both null, or neither null
            if (m_tblBehavior == null)
                {
                m_tblBehavior = new StringTable();
                }
            if (m_tblState == null)
                {
                m_tblState = new StringTable();
                }
            }
        }


    // ----- persistence ----------------------------------------------------

    /**
    * Save the Interface to a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param stream   the stream to write this Interface to
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            writing the Interface to the stream
    */
    protected synchronized void save(DataOutput stream)
            throws IOException
        {
        super.save(stream);

        // name of interface
        stream.writeUTF(m_sName);

        // interface type
        stream.writeInt(m_nType);

        // only save details if component resolved (i.e. a cached component)
        boolean fCached = (m_tblBehavior != null && m_tblState != null);
        stream.writeBoolean(fCached);
        if (fCached)
            {
            // behaviors
            stream.writeInt(m_tblBehavior.getSize());
            for (Enumeration enmr = m_tblBehavior.keys(); enmr.hasMoreElements(); )
                {
                stream.writeUTF((String) enmr.nextElement());
                }

            // properties
            stream.writeInt(m_tblState.getSize());
            for (Enumeration enmr = m_tblState.keys(); enmr.hasMoreElements(); )
                {
                stream.writeUTF((String) enmr.nextElement());
                }
            }
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
        xml.addElement("type").setString(DESCRIPTORS[m_nType]);

        super.save(xml);

        saveTableKeys(xml, m_tblBehavior, "methods", "method");
        saveTableKeys(xml, m_tblState, "fields", "field");
        }


    // ----- derivation/modification ----------------------------------------

    /**
    * Construct a blank Interface from this base.
    *
    * @param parent  the containing Component
    * @param nMode   RESOLVED, DERIVATION or MODIFICATION
    *
    * @return a new blank derived trait of this trait's class with the
    *         specified parent and mode
    */
    protected Trait getBlankDerivedTrait(Trait parent, int nMode)
        {
        return new Interface(this, (Component) parent, nMode);
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
        Interface base    = this;
        Interface delta   = (Interface) resolveDelta(traitDelta, loader, errlist);
        Interface derived = (Interface) super.resolve(delta, parent, loader, errlist);

        derived.m_nType       = base.m_nType;
        derived.m_sName       = base.m_sName;
        derived.m_tblBehavior = base.m_tblBehavior;
        derived.m_tblState    = base.m_tblState;

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
        Interface base  = (Interface) traitBase;
        Interface delta = (Interface) super.extract(base, parent, loader, errlist);

        // redundant information ... delta interfaces are discarded anyways
        delta.m_nType       = base.m_nType;
        delta.m_sName       = base.m_sName;
        delta.m_tblBehavior = base.m_tblBehavior;
        delta.m_tblState    = base.m_tblState;

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
    protected synchronized void finalizeExtract(Loader loader, ErrorList errlist)
            throws ComponentException
        {
        super.finalizeExtract(loader, errlist);

        // discard behaviors/properties
        m_tblState    = null;
        m_tblBehavior = null;
        }


    // ----- miscellaneous Trait methods ------------------------------------

    /**
    * Reset state, discarding all information.
    */
    protected synchronized void invalidate()
        {
        super.invalidate();

        m_sName       = null;
        m_tblBehavior = null;
        m_tblState    = null;
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


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the Component which contains this Interface.
    *
    * @return the Component trait that contains this Interface
    */
    public Component getComponent()
        {
        return (Component) getParentTrait();
        }

    /**
    * Determine the Interface type.
    *
    * @return one of IMPLEMENTS, or DISPATCHES
    */
    public int getType()
        {
        return m_nType;
        }

    /**
    * Determine the Interface name.
    *
    * @return the Interface name
    */
    public String getName()
        {
        return m_sName;
        }

    /**
    * Determine the Behaviors declared by this Interface.
    *
    * @return an enumeration of Behavior signatures
    */
    public Enumeration getBehaviors()
        {
        return m_tblBehavior.keys();
        }

    /**
    * Determine the Properties declared by this Interface.
    *
    * @return an enumeration of Property names
    */
    public Enumeration getProperties()
        {
        return m_tblState.keys();
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Determine if a signature follows the design pattern for Java Bean
    * event interface registration, and if so, what the action (either
    * registration or unregistration) is.
    *
    * @param sSig  a behavior signature
    *
    * @return the action verb (either "add" or "remove") or null if the
    *         signature is not related to event interface registration
    */
    public static String getDispatchesVerb(String sSig)
        {
        final Collator INSENS  = Constants.INSENS;
        final String   REG   = "add";
        final String   UNREG = "remove";

        // check for registration
        if (INSENS.equals(sSig.substring(0, 3), REG))
            {
            return REG;
            }

        // check for unregistration (note:  minimum signature length
        // is 3 characters, e.g. "f()")
        if (sSig.length() > 6 && INSENS.equals(sSig.substring(0, 6), UNREG))
            {
            return UNREG;
            }

        return null;
        }

    /**
    * Determine if a signature follows the design pattern for Java Bean
    * event interface registration, and if so, what the interface name is.
    *
    * @param sSig  a behavior signature
    *
    * @return the very short (not qualified, without the "Listener"
    *         suffix) interface name implied by the signature, or null
    */
    public static String getDispatchesName(String sSig)
        {
        String sVerb = getDispatchesVerb(sSig);
        if (sVerb == null)
            {
            return null;
            }

        // get the event interface short name
        String sIface = sSig.substring(sVerb.length(), sSig.indexOf('('));
        if (sIface.length() <= 0)
            {
            return null;
            }

        // verify that it ends with Listener
        final String SUFFIX = "Listener";
        int cchName = sIface.length() - SUFFIX.length();
        if (cchName <= 0)
            {
            return null;
            }

        String sEnd = sIface.substring(cchName);
        if (INSENS.equals(sEnd, SUFFIX))
            {
            return sIface.substring(0, cchName);
            }

        return null;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare this Interface to another Object for equality.
    *
    * @param obj  the other Object to compare to this
    *
    * @return true if this Interface equals that Object
    */
    public boolean equals(Object obj)
        {
        if (obj instanceof Interface)
            {
            Interface that = (Interface) obj;
            return this         == that
                || this.m_nType == that.m_nType
                && this.m_sName                                    .equals(that.m_sName      )
                && (this.m_tblBehavior == null ? that.m_tblBehavior == null :
                                                 this.m_tblBehavior.equals(that.m_tblBehavior))
                && (this.m_tblState    == null ? that.m_tblState    == null :
                                                 this.m_tblState   .equals(that.m_tblState   ))
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
        out.println(sIndent + toString());
        super.dump(out, sIndent);

        String sBehavior = (m_tblBehavior == null ? "<null>" : m_tblBehavior.toString());
        String sState    = (m_tblState    == null ? "<null>" : m_tblState   .toString());

        out.println(sIndent + "Behavior:  " + sBehavior);
        out.println(sIndent + "State:  "    + sState);
        }

    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Interface";

    /**
    * Interface type:  Implements.
    */
    public static final int IMPLEMENTS = 1;

    /**
    * Interface type:  Dispatches.
    */
    public static final int DISPATCHES = 2;

    /**
    * The descriptor string for Implements.
    */
    protected static final String DESCRIPTOR_IMPLEMENTS = "Implements";

    /**
    * The descriptor string for Dispatches.
    */
    protected static final String DESCRIPTOR_DISPATCHES = "Dispatches";

    /**
    * The descriptor strings indexed by the interface type.
    */
    private String[] DESCRIPTORS = {"<invalid>",
                                    DESCRIPTOR_IMPLEMENTS,
                                    DESCRIPTOR_DISPATCHES};

    /**
    * The interface type.
    */
    private int m_nType;

    /**
    * The interface name.
    */
    private String m_sName;

    /**
    * The table of behavior names declared by this interface.
    */
    private StringTable m_tblBehavior;
    
    /**
    * The table of property names declared by this interface.
    */
    private StringTable m_tblState;
    }

