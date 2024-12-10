/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import com.tangosol.coherence.config.Config;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;

import com.tangosol.util.Base;
import com.tangosol.util.ErrorList;
import com.tangosol.util.Listeners;
import com.tangosol.util.StringTable;
import com.tangosol.util.UID;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.util.StringMap;
import com.tangosol.util.IllegalStringException;

import java.beans.PropertyVetoException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.VetoableChangeListener;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Enumeration;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.List;
import java.util.Iterator;


/**
* A trait is an abstract class representing a collection of attributes and
* a recursive container of traits.
*
* It's easy to imagine a component ... something with properties, methods,
* events, etc.  It's easy to imagine an attribute ... one specific piece of
* information, like "this property is of type int" or "this behavior is
* final".  What is difficult is imagining the assembly of attributes into
* a component such that specific questions can be asked of any piece of the
* component including the component itself.  To simplify this, everything
* above the level of an attribute is represented as a Trait.
*
* A component, for example, is a trait which contains property traits,
* behavior traits, child component traits, implements/dispatches/integrates
* interface traits, and has attributes such as name, static, final, etc.
*
* - a Component is a trait which contains Component traits, Behavior traits,
*   Property traits, and Interface traits
* - a Property is a trait of a Component
* - a Behavior is a trait of a Component which contains a ReturnValue trait
*   and zero or more Parameter traits
* - a ReturnValue is a trait of a Behavior
* - a Parameter is a trait of a Behavior
* - an exception (Throwee) is a trait of a Behavior
*
* Each attribute of the trait is required to be a vetoable bean property,
* which means that:
*
* - for a given trait, any object can request to be a listener such that
*   a vetoable PropertyChangeEvent is delivered before the change; in
*   response to the PropertyChangeEvent, the listener can veto the change
*   by throwing a PropertyVetoException
* - for a given trait, any object can request to be a listener such that
*   a non-vetoable PropertyChangeEvent is delivered after the change is
*   made
*
* Additionally, in order to simplify tool development, an listener interface
* exists that reports the addition, removal, and modification of sub-traits.
* Again, there is a vetoable (before) and non-vetoable (after) option for
* receiving these notifications.
*
* The trait itself incorporates several attributes:
*
*   1.  A description, providing documentation for each component trait.
*       Since component traits are derived and modified, the description can
*       be derived and modified.  This takes the form of providing additional
*       documentation, replacing the documentation that already exists, or
*       not changing the documentation at all.
*
*   2.  A tip, which is a "short description" intended for user interface
*       assistance, such as a "tool tip".  This is considered to be part of
*       the documentation.
*
*   3.  A secondary unique identifier, sometimes referred to as a "doggy
*       tag", which provides a way of repairing links between derived or
*       modified traits when the primary identifier, such as a name, changes
*       at the base level.
*
*   4.  An origin, which describes whether the trait was added at this level,
*       a base level (for modification), or a super level (for derivation).
*       Additionally, if the trait was added at this level, the origin gives
*       information as to the reason why it was added at this level.  These
*       reasons include:
*
*       1.  The trait was added "manually"
*       2.  The trait resulted automatically from another trait, for example:
*           1.  implements
*           2.  dispatches
*           3.  integrates
*           4.  property
*
* @version 0.50, 11/16/97
* @version 1.00, 08/07/98
* @author  Cameron Purdy
*/
public abstract class Trait
        extends    Base
        implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a Trait.  This is the "default" constructor which is used by
    * deriving classes.  Additionally, this constructor is delegated to by
    * all constructors of this class.
    *
    * @param parent  the containing trait or null if this trait is not
    *                contained by another trait
    * @param nMode   one of RESOLVED, DERIVATION, MODIFICATION
    */
    protected Trait(Trait parent, int nMode)
        {
        // assertion:  (remove after testing) mode is valid
        if (nMode != RESOLVED && nMode != DERIVATION && nMode != MODIFICATION)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Invalid mode for Trait construction (" + nMode + ")");
            }

        m_parent = parent;
        m_nMode  = nMode;
        }

    /**
    * Construct a blank Trait.  All derivable traits blank trait constructor.
    * The result is one of:
    * <ol>
    * <li> A blank resolved trait
    * <li> A blank derivation
    * <li> A blank modification
    * </ol>
    * This is the constructor used by implementations of the
    * getBlankDerivedTrait() method.
    *
    * @param base    the super or base Trait to derive from
    * @param parent  the containing trait or null if this trait is not
    *                contained by another trait
    * @param nMode   one of RESOLVED, DERIVATION, MODIFICATION
    *
    * @see #getBlankDerivedTrait(Trait, int)
    */
    protected Trait(Trait base, Trait parent, int nMode)
        {
        this(parent, nMode);

        // assertion:  (remove after testing) base is present and valid
        if (base == null || base.getClass() != this.getClass())
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Invalid base for blank Trait construction (" + base + ")");
            }

        m_uid = base.m_uid;
        }

    /**
    * Copy constructor.  This constructor is used by the copy constructor
    * of the deriving trait.  All traits implement a copy constructor.
    *
    * Note:  Event listeners are not copied.
    *
    * @param parent  the trait that will contain the new copy of this trait
    * @param that    the trait to copy from
    */
    protected Trait(Trait parent, Trait that)
        {
        this(parent, that.m_nMode);

        StringTable tbl = that.m_tblOrigin;
        if (tbl != null)
            {
            tbl = (StringTable) tbl.clone();
            }

        this.m_uid               = that.m_uid;
        this.m_nOrigin           = that.m_nOrigin;
        this.m_tblOrigin         = tbl;
        this.m_sTip              = that.m_sTip;
        this.m_sPrevTip          = that.m_sPrevTip;
        this.m_sDesc             = that.m_sDesc;
        this.m_sPrevDesc         = that.m_sPrevDesc;
        this.m_fReplaceDesc      = that.m_fReplaceDesc;
        this.m_fPrevReplaceDesc  = that.m_fPrevReplaceDesc;
        this.m_nState            = that.m_nState;
        }

    /**
    * Construct a Trait object from persistent data.  All traits implement
    * implement a constructor from persistent data.
    *
    * @param parent    the trait that contains this trait
    * @param stream    the object to read the persistent information from
    * @param nVersion  version of the data structure in the stream
    *
    * @throws IOException if an exception occurs reading the trait data
    */
    protected Trait(Trait parent, DataInput stream, int nVersion)
            throws IOException
        {
        this(parent, /* mode= */ stream.readUnsignedByte());

        m_sTip         = readString(stream);
        m_sDesc        = readString(stream);
        m_fReplaceDesc = stream.readBoolean();

        // TODO 1999.11.15  cp   potentially remove; this is a fix for
        // blank descriptions that were saved before the description
        // delta was fixed
        if (m_fReplaceDesc && m_sDesc == BLANK)
            {
            m_fReplaceDesc = false;
            }

        // there may or may not be a UID in the stream
        if (stream.readBoolean())
            {
            m_uid = new UID(stream);
            }

        // origin
        m_nOrigin = stream.readUnsignedByte();

        // origin traits
        int cTraits = stream.readUnsignedByte();
        if (cTraits > 0)
            {
            StringTable tbl = new StringTable();
            for (int i = 0; i < cTraits; ++i)
                {
                tbl.add(stream.readUTF());
                }
            m_tblOrigin = tbl;
            }

        // set the trait's processing state to resolving
        m_nState = STATE_RESOLVING;
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
    protected Trait(Trait parent, XmlElement xml, int nVersion)
            throws IOException
        {
        this(parent, /* mode= */ parseMode(xml.getSafeElement("mode").getString()));

        XmlElement xmlDesc = xml.getElement("description");
        if (xmlDesc != null)
            {
            m_sTip         = readString(xmlDesc.getElement("tip"));
            m_sDesc        = readString(xmlDesc.getElement("text"));
            m_fReplaceDesc = readBoolean(xmlDesc.getElement("replace"));
            }

        // there may or may not be a UID in the stream
        XmlElement xmlUid = xml.getElement("uid");
        if (xmlUid != null)
            {
            m_uid = new UID(xmlUid.getString());
            }

        // origin
        int         nOrigin   = ORIGIN_SUPER;
        StringTable tblTraits = null;
        XmlElement  xmlOrigin = xml.getElement("origin");
        if (xmlOrigin != null)
            {
            String sLevel = readString(xmlOrigin.getElement("level"));
            if (sLevel.length() > 0)
                {
                switch (sLevel.charAt(0))
                    {
                    case 'T': case 't':
                        nOrigin = ORIGIN_THIS;
                        break;

                    case 'B': case 'b':
                        nOrigin = ORIGIN_BASE;
                        break;

                    case 'S': case 's':
                        nOrigin = ORIGIN_SUPER;
                        break;

                    default:
                        throw new IOException("invalid origin level: " + sLevel);
                    }
                }

            if (readBoolean(xmlOrigin.getElement("manual")))
                {
                nOrigin |= ORIGIN_MANUAL;
                }

            XmlElement xmlTraits = xmlOrigin.getElement("traits");
            if (xmlTraits != null)
                {
                List listTraits = xmlTraits.getElementList();
                for (Iterator iter = listTraits.iterator(); iter.hasNext(); )
                    {
                    XmlElement xmlTrait = (XmlElement) iter.next();
                    if (xmlTrait.getName().equals("trait"))
                        {
                        String sTrait = readString(xmlTrait);
                        if (sTrait.length() > 0)
                            {
                            if (tblTraits == null)
                                {
                                tblTraits  = new StringTable();
                                nOrigin   |= ORIGIN_TRAIT;
                                }
                            tblTraits.add(sTrait);
                            }
                        }
                    }
                }
            }
        m_nOrigin   = nOrigin;
        m_tblOrigin = tblTraits;

        // set the trait's processing state to resolving
        m_nState = STATE_RESOLVING;
        }


    // ----- persistence ----------------------------------------------------

    /**
    * Save the Trait information to a stream.  If derived traits have any
    * data of their own, then they must implement (i.e. supplement) this
    * method.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param stream  the object to write the persistent information to
    *
    * @throws IOException if an exception occurs reading the trait data
    */
    protected synchronized void save(DataOutput stream)
            throws IOException
        {
        if (!(m_nMode == RESOLVED || m_nMode == DERIVATION || m_nMode == MODIFICATION))
            {
            throw new IOException(CLASS + ".save:  Trait contains invalid mode (" + toString() + ", " + m_nMode + ")");
            }

        stream.writeByte(m_nMode);
        stream.writeUTF(m_sTip);
        stream.writeUTF(m_sDesc);
        stream.writeBoolean(m_fReplaceDesc);

        // store the UID (if any)
        boolean fHasUID = (m_uid != null);
        stream.writeBoolean(fHasUID);
        if (fHasUID)
            {
            m_uid.save(stream);
            }

        // origin
        stream.writeByte(m_nOrigin);

        // origin traits
        StringTable tbl = m_tblOrigin;
        int cTraits = (tbl == null ? 0 : tbl.getSize());
        stream.writeByte(cTraits);
        if (cTraits > 0)
            {
            for (Enumeration enmr = tbl.keys(); enmr.hasMoreElements(); )
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
        // store mode
        String sMode;
        switch (m_nMode)
            {
            case RESOLVED:
                sMode = "resolved";
                break;

            case DERIVATION:
                sMode = "derivation";
                break;

            case MODIFICATION:
                sMode = "modification";
                break;

            default:
                throw new IOException(CLASS + ".save:  Trait contains invalid mode ("
                                      + toString() + ", " + m_nMode + ")");
            }
        xml.addElement("mode").setString(sMode);

        // store description info
        String  sTip     = m_sTip;
        String  sText    = m_sDesc;
        boolean fReplace = m_fReplaceDesc;
        if (sTip != BLANK || sText != BLANK || fReplace)
            {
            XmlElement xmlDesc = xml.addElement("description");
            if (sTip != BLANK)
                {
                xmlDesc.addElement("tip").setString(m_sTip);
                }
            if (sText != BLANK || fReplace)
                {
                xmlDesc.addElement("text").setString(sText);
                }
            if (fReplace)
                {
                xmlDesc.addElement("replace").setBoolean(true);
                }
            }

        // store the UID (if any)
        if (m_uid != null)
            {
            xml.addElement("uid").setString(m_uid.toString());
            }

        // origin
        int         nOrigin   = m_nOrigin;
        StringTable tblOrigin = m_tblOrigin;
        if (nOrigin != ORIGIN_SUPER || (tblOrigin != null && !tblOrigin.isEmpty()))
            {
            XmlElement xmlOrigin = xml.addElement("origin");

            String sLevel;
            switch (nOrigin & ORIGIN_LEVEL)
                {
                case ORIGIN_THIS:
                    sLevel = "this";
                    break;
                case ORIGIN_BASE:
                    sLevel = "base";
                    break;
                case ORIGIN_SUPER:
                    sLevel = "super";
                    break;
                default:
                    sLevel = "invalid";
                    break;
                }
            xmlOrigin.addElement("level").setString(sLevel);

            if ((nOrigin & ORIGIN_MANUAL) != 0)
                {
                xmlOrigin.addElement("manual").setBoolean(true);
                }

            if (tblOrigin != null && !tblOrigin.isEmpty())
                {
                XmlElement xmlTraits = xmlOrigin.addElement("traits");
                for (Enumeration enmr = tblOrigin.keys(); enmr.hasMoreElements(); )
                    {
                    xmlTraits.addElement("trait").setString((String) enmr.nextElement());
                    }
                }
            }
        }

    /**
    * Helper for re-constituting strings.  Guarantees that all 0-length
    * strings are BLANK.
    *
    * @param stream  the object to read the persistent information from
    *
    * @return the string that is read from the stream or BLANK if a 0-length
    *         string was read
    */
    protected static String readString(DataInput stream)
            throws IOException
        {
        String s = stream.readUTF();
        return (s.length() > 0 ? s : BLANK);
        }

    /**
    * Determine the component mode from a mode string, as would be encoded
    * in an XML serialized format.
    *
    * @param s  the mode string
    *
    * @return the component mode enumeration value
    */
    private static int parseMode(String s)
        {
        int nMode = RESOLVED;
        if (s != null && s.length() > 1)
            {
            switch (s.charAt(0))
                {
                case 'D': case 'd':
                    nMode = DERIVATION;
                    break;

                case 'M': case 'm':
                    nMode = MODIFICATION;
                    break;
                }
            }
        return nMode;
        }

    /**
    * Helper for re-constituting strings.  Guarantees that all 0-length
    * strings are BLANK.
    *
    * @param xml  the XmlValue to get a String value from
    *
    * @return the string from the XmlValue or BLANK if the string is 0-length
    */
    protected static String readString(XmlValue xml)
        {
        String s = null;

        if (xml != null)
            {
            s = xml.getString();
            }

        if (s == null || s.length() == 0)
            {
            s = BLANK;
            }

        return s;
        }

    /**
    * Helper for reading booleans from XML.  Assumes "false" for missing
    * data.
    *
    * @param xml  the XmlValue to get a boolean value from
    *
    * @return the boolean from the XmlValue or false if the value is missing
    */
    protected static boolean readBoolean(XmlValue xml)
        {
        boolean f = false;

        if (xml != null)
            {
            f = xml.getBoolean();
            }

        return f;
        }

    /**
    * Helper for reading flags from XML.
    *
    * @param xml       the XmlElement for the trait to get the flags from
    * @param sName     the name of the sub-element that stores the flags
    * @param nDefault  the default flags value
    *
    * @return the flags value
    */
    protected static int readFlags(XmlElement xml, String sName, int nDefault)
        {
        int nFlags = nDefault;

        xml = xml.getElement(sName);
        if (xml != null)
            {
            String sFlags = xml.getString();
            if (sFlags != null && sFlags.length() > 0)
                {
                if (sFlags.length() > 2 &&     sFlags.charAt(0) == '0'
                                        && (   sFlags.charAt(1) == 'x'
                                            || sFlags.charAt(1) == 'X'))
                    {
                    nFlags = Integer.parseInt(sFlags.substring(2), 16);
                    }
                else
                    {
                    nFlags = Integer.parseInt(sFlags);
                    }
                }
            }

        return nFlags;
        }

    /**
    * Helper for writing flags to XML.
    *
    * @param xml       the XmlElement for the trait to store the flags into
    * @param sName     the name of the sub-element that stores the flags
    * @param nFlags    the flags value
    * @param nDefault  the default flags value
    */
    protected static void saveFlags(XmlElement xml, String sName, int nFlags, int nDefault)
        {
        if (nFlags != nDefault)
            {
            xml.addElement(sName).setString("0x" + toHexString(nFlags, 8));
            }
        }

    /**
    * Save a StringTable of traits to XML form.
    *
    * @param xml     the XML element to save the sub-traits into
    * @param tbl     the StringTable of named sub-traits
    * @param sTable  the XML element name to use to enclose the table of
    *                sub-traits
    * @param sEntry  the XML element name to use for each sub-trait
    */
    protected static void saveTable(XmlElement xml, StringTable tbl, String sTable, String sEntry)
            throws IOException
        {
        if (tbl != null && !tbl.isEmpty())
            {
            xml = xml.addElement(sTable);
            for (Enumeration enmr = tbl.elements(); enmr.hasMoreElements(); )
                {
                Trait trait = (Trait) enmr.nextElement();
                trait.save(xml.addElement(sEntry));
                }
            }
        }

    /**
    * Save a StringTable of keys to XML form.
    *
    * @param xml     the XML element to save the String keys into
    * @param tbl     the StringTable of keys
    * @param sTable  the XML element name to use to enclose the keys
    * @param sEntry  the XML element name to use for each key
    *
    * @throws IOException  if a problem occursing saving the keys to XML
    */
    protected static void saveTableKeys(XmlElement xml, StringTable tbl, String sTable, String sEntry)
            throws IOException
        {
        if (tbl != null && !tbl.isEmpty())
            {
            xml = xml.addElement(sTable);
            for (Enumeration enmr = tbl.keys(); enmr.hasMoreElements(); )
                {
                xml.addElement(sEntry).setString((String) enmr.nextElement());
                }
            }
        }

    /**
    * Read a StringTable of keys from XML.
    *
    * @param xml     the XML element that contains the table of keys
    * @param sTable  the XML element name that encloses the keys
    * @param sEntry  the XML element name for each key
    *
    * @return the StringTable of keys, or null if there are no keys
    *
    * @throws IOException  if a problem occursing reading the keys from XML
    */
    protected static StringTable readTableKeys(XmlElement xml, String sTable, String sEntry)
            throws IOException
        {
        StringTable tbl = null;

        xml = xml.getElement(sTable);
        if (xml != null)
            {
            List listEntry = xml.getElementList();
            for (Iterator iter = listEntry.iterator(); iter.hasNext(); )
                {
                XmlElement xmlEntry = (XmlElement) iter.next();
                if (xmlEntry.getName().equals(sEntry))
                    {
                    if (tbl == null)
                        {
                        tbl = new StringTable();
                        }
                    tbl.add(xmlEntry.getString());
                    }
                }
            }

        return tbl;
        }

    /**
    * Write a StringMap of data to XML.
    *
    * @param xml     the XML to write the StringMap to
    * @param sMap    the name for the set of key/key mappings in the XML
    * @param sEntry  the name for each key/key mapping in the XML
    * @param map     the StringMap to write
    *
    * @throws IOException  if there were any problems writing the StringMap
    */
    protected static void saveStringMap(XmlElement xml, String sMap, String sEntry, StringMap map)
            throws IOException
        {
        if (!map.isEmpty())
            {
            xml = xml.addElement(sMap);
            Enumeration enmr = map.primaryStrings();
            while (enmr.hasMoreElements())
                {
                String sKey   = (String) enmr.nextElement();
                String sValue = map.get(sKey);
                XmlElement xmlEntry = xml.addElement(sEntry);
                xmlEntry.addElement("map-from").setString(sKey);
                xmlEntry.addElement("map-to").setString(sValue);
                }
            }
        }

    /**
    * Read a StringMap of data from XML.
    *
    * @param xml     the XML containing a StringMap
    * @param sMap    the name of the set of key/key mappings in the XML;
    *                the element does not have to exist in the XML
    * @param sEntry  the name of each key/key mapping in the XML; there
    *                do not have to be any key/key mappings
    *
    * @return a new StringMap, with the key/key mappings from the XML, if
    *         there were any
    *
    * @throws IOException  if there were any problems reading the StringMap
    */
    protected static StringMap readStringMap(XmlElement xml, String sMap, String sEntry)
            throws IOException
        {
        StringMap map = new StringMap();

        xml = xml.getElement(sMap);
        if (xml != null)
            {
            List listEntry = xml.getElementList();
            for (Iterator iter = listEntry.iterator(); iter.hasNext(); )
                {
                XmlElement xmlEntry = (XmlElement) iter.next();
                if (xmlEntry.getName().equals(sEntry))
                    {
                    String sKey   = readString(xmlEntry.getElement("map-from"));
                    String sValue = readString(xmlEntry.getElement("map-to"));
                    try
                        {
                        map.put(sKey, sValue);
                        }
                    catch (IllegalStringException e)
                        {
                        throw new IOException(e.getMessage());
                        }
                    }
                }
            }

        return map;
        }


    // ----- derivation/modification ----------------------------------------

    /**
    * Construct a blank Trait.  All traits implement this method.
    *
    * A blank derived trait is an "l-value" for resolve and extract
    * processing.  In other words, when resolve and extract create a result
    * trait (either a resolved or delta trait) it is this method which is
    * responsible for creating that trait in its initial state ("blank").
    *
    * The resolve and extract methods implemented by this class (Trait) are
    * responsible for instantiating a resulting derived trait, either a
    * RESOLVED, DERIVATION, or MODIFICATION trait, depending on which method
    * (resolve or extract) was called and the parameters that were passed.
    *
    * This virtual method (getBlankDerivedTrait) is a "virtual constructor".
    * In other words, it is responsible for instantiating the correct class,
    * which is the same class as the "this" parameter (the base trait).
    *
    * @param parent  the containing trait or null if the blank trait is
    *                not going to be contained by another trait
    * @param nMode   one of RESOLVED, DERIVATION, MODIFICATION
    *
    * @return a new blank derived trait of this trait's class with the
    *         specified parent and mode
    *
    * @see #resolve
    * @see #extract
    */
    protected abstract Trait getBlankDerivedTrait(Trait parent, int nMode);

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
        if (nMode == RESOLVED)
            {
            throw new IllegalArgumentException(CLASS + ".getNullDerivedTrait:  "
                    + ":  Invalid mode for null Trait construction (" + nMode + ")");
            }

        return getBlankDerivedTrait(parent, nMode);
        }

    /**
    * Resolve the delta trait relative to the base trait before resolving
    * the derived trait itself.  This is necessary for those situations where
    * the mode of the delta trait would "override" the mode of the base trait.
    *
    * Almost always this method will end up returning the original delta
    * trait passed in.  This method returns the extracted difference between
    * the base trait and the delta trait when the base trait has been modified
    * in such as way to cause the delta trait to override it.
    *
    * This method is called prior to calling the resolve method itself and must
    * be performed seperate so the sub-trait can access the potentially changed
    * delta trait.
    *
    * For clarity purposes, the resolveDelta implementation does not refer to
    * the "this" parameter except to re-label it as the "base" Trait.
    *
    * @param delta    the Trait derivation or modification
    * @param loader   the Loader object for JCS and CD dependencies
    * @param errlist  the error list object to log error information to
    *
    * @return the delta trait to be used during resolve
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    protected Trait resolveDelta(Trait delta, Loader loader, ErrorList errlist)
            throws ComponentException
        {
        // there are two known Trait instances:
        // base     - the base Trait to apply the delta Trait to (this)
        // delta    - the MODIFICATION or DERIVATION Trait to apply (passed)
        Trait base = this;

        // assertion:  base mode is legal
        switch (base.m_nMode)
            {
            case RESOLVED:
            case DERIVATION:
            case MODIFICATION:
                break;
            default:
                throw new IllegalStateException(CLASS + ".resolveDelta:  "
                        + "Illegal base mode (" + base.m_nMode + ") for resolving modification!");
            }

        // validate mode of the base and delta traits
        //      Dr = Br + Dd
        //      Dr = Br + Dm
        //      Dd = Bd + Dm
        //      Dm = Bm + Dm
        switch (delta.m_nMode)
            {
            case DERIVATION:
                if (base.m_nMode == RESOLVED)
                    {
                    break;
                    }
                // fall through
            case RESOLVED:
                // it is not possible to apply a derived Trait to anything
                // but a resolved Trait, furthermore, it is not possible to
                // apply a resolved trait to anything (resolved, derived, or
                // modified):
                //      Dr = Br + Dr    resolved + resolved
                //      Dd = Bd + Dr    derived  + resolved
                //      Dm = Bm + Dr    modified + resolved
                //      Dd = Bd + Dd    derived  + derived
                //      Dm = Bm + Dd    modified + derived
                // this can occur if the base is added after the derived
                // already exists; to solve this, keep whatever is keepable
                // by extracting the legal delta between the base and the
                // would-be delta, for example:
                //      Dr = Br + (Dr - Br)
                // the result may be a null derivation (no information could
                // be salvaged) but at least it is a legal operation

                // log an error for now, if this becomes intrusive then remove it
                //logError(RESOLVE_FORCEEXTRACT, WARNING, new Object[]
                //        {delta.toString(), delta.toPathString()}, errlist);

                delta = delta.extract(base, delta.m_parent, loader, errlist);
                break;

            case MODIFICATION:
                // modification traits can be applied to resolved,
                // derivation, or modification traits
                break;

            default:
                throw new IllegalArgumentException(CLASS + ".resolveDelta:  "
                        + "Illegal delta mode (" + delta.m_nMode + ")");
            }

        return delta;
        }

    /**
    * Create a derived/modified Trait by combining the information from this
    * Trait with the super or base level's Trait information.  Neither the
    * base ("this") nor the delta may be modified in any way.
    *
    * All Trait classes implement this method.  Their implementation must
    * first call resolveDelta to resolve any discrepencies between the delta
    * and the base Trait.  Then, the Trait class implementation must call
    * this implementation passing the delta Trait returned from resolveDelta
    * in order to create the resulting derived Trait.
    *
    * For clarity purposes, the resolve implementation does not refer to the
    * "this" parameter except to re-label it as the "base" Trait.
    *
    * @param delta    the Trait derivation or modification
    * @param parent   the Trait which will contain the resulting Trait or
    *                 null if the resulting Trait will not be contained
    * @param loader   the Loader object for JCS and CD dependencies
    * @param errlist  the error list object to log error information to
    *
    * @return the result of applying the specified delta Trait to this Trait
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    protected Trait resolve(Trait delta, Trait parent, Loader loader, ErrorList errlist)
            throws ComponentException
        {
        // there are four known Trait instances:
        // base     - the base Trait to apply the delta Trait to (this)
        // delta    - the MODIFICATION or DERIVATION Trait to apply (passed from resolveDelta)
        // parent   - the parent of the resulting derived Trait (passed)
        // derived  - the resulting Trait
        Trait base    = this;
        Trait derived = null;

        // create the derived trait with the specified parent; note that
        // resolve always results in a Trait with the same mode as the base
        derived = base.getBlankDerivedTrait(parent, base.m_nMode);

        // copy the tip from the delta; use the previous tip from the
        // delta if available, otherwise use the tip from the base as
        // the previous tip
        derived.m_sTip     = delta.m_sTip;
        derived.m_sPrevTip = (delta.m_sPrevTip != BLANK ? delta.m_sPrevTip
                                                        : base.m_sTip != BLANK ? base.m_sTip
                                                                               : base.m_sPrevTip);

        // copy the description from the delta; if the delta overrode its
        // base's description, then use its base's description as the
        // previous ("undo") description, otherwise combine the delta's
        // previous description with the base's description
        derived.m_sDesc        = delta.m_sDesc;
        derived.m_fReplaceDesc = delta.m_fReplaceDesc;
        if (delta.m_fPrevReplaceDesc)
            {
            derived.m_sPrevDesc        = delta.m_sPrevDesc;
            derived.m_fPrevReplaceDesc = true;
            }
        else
            {
            // build the description using what is already present in the
            // delta Trait (due to the application of one or more trait
            // modifications) and the description at the base level
            derived.m_sPrevDesc = getDescription(delta.m_sPrevDesc,
                    base.getDescription(), false);

            // determine if the base was replacing its base description
            derived.m_fPrevReplaceDesc = base.m_fReplaceDesc || base.m_fPrevReplaceDesc;
            }

        // verify that UIDs match
        if (base.m_uid == null)
            {
            derived.m_uid = delta.m_uid;
            }
        else if (!base.m_uid.equals(delta.m_uid))
            {
            logError(RESOLVE_UIDCHANGE, WARNING, new Object[]
                    {delta.toString(), delta.toPathString()}, errlist);
            }

        // determine level of origin
        //  1)  if base origin is super or if delta is a derivation then
        //      origin is super
        //  2)  otherwise origin is base
        derived.m_nOrigin = (base.isFromSuper() || delta.m_nMode == DERIVATION ? ORIGIN_SUPER
                                                                               : ORIGIN_BASE);

        // copy manual origin from the delta
        if (delta.isFromManual())
            {
            derived.setFromManual();
            }

        // mode of derived is always same as mode of base
        derived.m_nMode = base.m_nMode;

        // set the delta process state
        delta.m_nState = STATE_RESOLVING;

        return derived;
        }

    /**
    * Finalize the resolve process.  This means that this trait will not
    * be asked to resolve again.  A trait is considered designable after
    * it has finalized the resolve process.
    *
    * @param loader    the Loader object for JCS and CD dependencies
    * @param errlist   the error list object to log error information to
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    protected void finalizeResolve(Loader loader, ErrorList errlist)
            throws ComponentException
        {
        // default tip to the base's (the "previous") tip
        if (m_sTip == BLANK)
            {
            m_sTip = m_sPrevTip;
            }

        // make sure the trait is resolved
        if (m_nMode != RESOLVED)
            {
            logError(RESOLVE_FORCERESOLVE, WARNING, new Object[]
                {toString(), toPathString()}, errlist);
            m_nMode = RESOLVED;
            }

        // m_fPrevReplaceDesc is used only during resolve processing
        m_fPrevReplaceDesc = false;

        // flag us has having been finalize resolved
        m_nState = STATE_RESOLVED;
        }

    /**
    * Determine the mode of extraction assuming the passed base Trait were
    * being exctracted from this derived Trait.
    *
    * This method must be overridden by any Trait which can determine
    * whether the extraction is for a DERIVATION or MODIFICATION.
    * (Component is the only trait which can differentiate.)
    *
    * @param base  the base Trait to extract
    *
    * @return DERIVATION or MODIFICATION
    */
    protected int getExtractMode(Trait base)
        {
        if (this.m_nMode == RESOLVED && base.m_nMode == RESOLVED)
            {
            // could be either derivation or modification; ask the parent
            return this.m_parent.getExtractMode(base.m_parent);
            }

        return MODIFICATION;
        }

    /**
    * Create a derivation/modification Trait by determining the differences
    * between the derived Trait ("this") and the passed base Trait.  Neither
    * the derived Trait ("this") nor the base may be modified in any way.
    *
    * All Trait classes implement this method.  Their implementation must
    * first call this implementation in order to create the resulting Trait.
    *
    * For clarity purposes, the extract implementation does not refer to the
    * "this" parameter except to re-label it as the "derived" Trait.
    *
    * @param base     the base Trait to extract
    * @param parent   the Trait which will contain the resulting Trait or
    *                 null if the resulting Trait will not be contained
    * @param loader   the Loader object for JCS and CD dependencies
    * @param errlist  the error list object to log error information to
    *
    * @return the delta between this derived Trait and the base Trait
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    protected Trait extract(Trait base, Trait parent, Loader loader, ErrorList errlist)
            throws ComponentException
        {
        // assertion - validate mode
        int nMode = getExtractMode(base);
        if (nMode != DERIVATION && nMode != MODIFICATION)
            {
            throw new IllegalArgumentException(CLASS + ".extract:  "
                    + "Illegal extract mode (" + nMode + ")");
            }

        // there are four known Trait instances:
        // derived  - the Trait which extends the base Trait (this)
        // base     - the base Trait to extract from the derived Trait (passed)
        // parent   - the parent of the resulting delta Trait (passed)
        // delta    - the resulting MODIFICATION or DERIVATION Trait
        Trait derived = this;
        Trait delta   = base.getBlankDerivedTrait(parent, nMode);

        // extract the tip and previous tip values
        String sTip     = derived.m_sTip;
        String sPrevTip = derived.m_sPrevTip;

        // check if this is the first extract performed
        // against this trait
        if (derived.m_nState == STATE_RESOLVED)
            {
            // check if the tip value is different than the
            // previous tip value determined while resolving
            // this trait
            if (sTip.equals(sPrevTip))
                {
                sTip = BLANK;
                }
            // recalculate the previous tip value
            // while extracting
            sPrevTip = BLANK;
            }

        // if there is a tip value to worry about
        if (sTip != BLANK)
            {
            // if this base specified a tip, use that as the previous
            // tip value
            if (base.m_sTip != BLANK)
                {
                sPrevTip = base.m_sTip;
                }
            else if (base.m_sPrevTip != BLANK)
                {
                sPrevTip = base.m_sPrevTip;
                }
            }

        delta.m_sTip     = sTip;
        delta.m_sPrevTip = sPrevTip;

        boolean fReplaceDesc     = derived.m_fReplaceDesc;
        String  sDesc            = derived.m_sDesc;
        boolean fPrevReplaceDesc = derived.m_fPrevReplaceDesc;
        String  sPrevDesc        = derived.m_sPrevDesc;

        switch (derived.m_nState)
            {
            case STATE_RESOLVING:
                // we are extracting the changes between
                // two traits during a resolveDelta conflict
                if (fReplaceDesc == base.m_fReplaceDesc
                    && sDesc.equals(base.m_sDesc))
                    {
                    // we have two identical set of changes
                    fReplaceDesc = false;
                    sDesc        = BLANK;
                    }
                fPrevReplaceDesc = false;
                sPrevDesc        = BLANK;
                break;

            case STATE_RESOLVED:
                // discard the "previous" description and
                // recalculate on the way down
                fPrevReplaceDesc = false;
                sPrevDesc        = BLANK;
                // fall into default

            default:
                // TODO:  Remove when all is determined to be okay...
                // always carry the derived replace flag except for when creating
                // a modification and the base and derived are both replace
                //if (nMode == MODIFICATION
                //    && (base.m_fReplaceDesc || base.m_fPrevReplaceDesc)
                //    && fReplaceDesc)
                //    {
                //    fReplaceDesc = false;
                //    sDesc        = derived.getDescription();
                //    }

                // if there is a description to worry about
                if (fReplaceDesc || sDesc != BLANK)
                    {
                    // keep track of the last previous value
                    if (base.m_fReplaceDesc || base.m_sDesc != BLANK)
                        {
                        fPrevReplaceDesc = base.m_fReplaceDesc;
                        sPrevDesc        = base.m_sDesc;
                        }
                    else if (base.m_fPrevReplaceDesc || base.m_sPrevDesc != BLANK)
                        {
                        fPrevReplaceDesc = base.m_fPrevReplaceDesc;
                        sPrevDesc        = base.m_sPrevDesc;
                        }
                    }
            }

        delta.m_fReplaceDesc     = fReplaceDesc;
        delta.m_sDesc            = sDesc;
        delta.m_fPrevReplaceDesc = fPrevReplaceDesc;
        delta.m_sPrevDesc        = sPrevDesc;

        // verify that UIDs match
        if (base.m_uid == null)
            {
            delta.m_uid = derived.m_uid;
            }
        else if (!base.m_uid.equals(derived.m_uid))
            {
            logError(EXTRACT_UIDCHANGE, WARNING, new Object[]
                    {derived.toString(), derived.toPathString()}, errlist);
            }

        // set the delta process state
        delta.m_nState = STATE_EXTRACTING;

        return delta;
        }

    /**
    * Finalize the extract process.  This means that this trait will not
    * be asked to extract again.  A trait is considered persistable after
    * it has finalized the extract process.
    *
    * @param loader    the Loader object for JCS and CD dependencies
    * @param errlist   the error list object to log error information to
    *
    * @exception ComponentException  thrown only if a fatal error occurs
    */
    protected synchronized void finalizeExtract(Loader loader, ErrorList errlist)
            throws ComponentException
        {
        if (m_nMode != RESOLVED)
            {
            // don't store tip if it is redundant with the base level
            // calculated while extracting
            if (m_sTip.equals(m_sPrevTip))
                {
                m_sTip = BLANK;
                }
            // don't store the description if it is redundant with
            // the base level calculated while extracting
            if (m_fReplaceDesc == m_fPrevReplaceDesc
                && m_sDesc.equals(m_sPrevDesc))
                {
                m_fReplaceDesc = false;
                m_sDesc        = BLANK;
                }
            }

        // don't store the base level tip
        m_sPrevTip         = BLANK;

        // don't store the base description
        m_fPrevReplaceDesc = false;
        m_sPrevDesc        = BLANK;

        // discard all origin information (except for manual)
        m_nOrigin  &= ORIGIN_MANUAL;
        m_tblOrigin = null;
        }

    /**
    * Determine if the trait can be discarded.  This has two uses:
    * <ol>
    * <li>Determining if a resolved trait should not exist; for example
    *     a trait without an origin
    * <li>An extracted trait that contains no information (i.e. a "null
    *     derivation") is often discardable
    * </ol>
    * For a resolved trait, this request is only legal after the
    * finalizeResolve call.  Likewise, for a derivation/modification
    * trait, this request is only legal after the finalizeExtract call.
    *
    * @return true if the trait is discardable
    */
    protected boolean isDiscardable()
        {
        switch (m_nMode)
            {
            case RESOLVED:
                // resolved Traits are discardable if they have no origin;
                // however, it is possible that a resolved trait is a child
                // of a derivation/modification (i.e. it is a non-extractable
                // child, which means that extracting its parent trait does
                // not in turn extract this trait) in which case it is not
                // discardable because it represents delta information
                if (isFromNothing())
                    {
                    for (Trait trait = m_parent; trait != null; trait = trait.m_parent)
                        {
                        int nMode = trait.m_nMode;
                        if (nMode == DERIVATION || nMode == MODIFICATION)
                            {
                            return false;
                            }
                        }
                    }
                else
                    {
                    return false;
                    }
                break;

            case DERIVATION:
            case MODIFICATION:
                // check for "delta" information at this level
                if (m_fReplaceDesc || m_sTip != BLANK || m_sDesc != BLANK || isFromManual())
                    {
                    return false;
                    }

                // check if contained traits are discardable
                for (Enumeration enmr = getSubTraits(); enmr.hasMoreElements(); )
                    {
                    Trait trait = (Trait) enmr.nextElement();
                    if (!trait.isDiscardable())
                        {
                        return false;
                        }
                    }
                break;
            }

        return true;
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
        // the Trait contains no information that would modify the generation
        // of classes, but it does contain information that would modify the
        // generation of source listings or even related classes (such as
        // BeanInfo classes); it may seem odd that a delta in a description
        // could force class generation, but it is hard to foresee what
        // potential problems could exist if the opposite decision were made
        return this.getDescription().equals(base.getDescription())
            && this.getTip        ().equals(base.getTip        ());
        }

    /**
    * Helper for isClassDiscardable.
    */
    protected boolean isClassDiscardableFromSubtraitTable(StringTable tblThis, StringTable tblThat)
        {
        if (!tblThis.keysEquals(tblThat))
            {
            return false;
            }
        for (Enumeration enmr = tblThis.keys(); enmr.hasMoreElements(); )
            {
            String s = (String) enmr.nextElement();
            Trait traitThis = (Trait) tblThis.get(s);
            Trait traitThat = (Trait) tblThat.get(s);
            if (!traitThis.isClassDiscardable(traitThat))
                {
                return false;
                }
            }
        return true;
        }

    /**
    * Helper method to log derivation error information.
    *
    * @param sCode     the error code
    * @param nSeverity the error severity
    * @param aoParam   an array of replaceable parameters
    * @param errlist   the error list object to log error information to
    *
    * @exception DerivationException  thrown when the error list fills up
    */
    public void logError(String sCode, int nSeverity, Object[] aoParam, ErrorList errlist)
            throws DerivationException
        {
        if (errlist != null)
            {
            try
                {
                errlist.add(new ErrorList.Item(sCode, nSeverity, null, aoParam, null, RESOURCES));
                }
            catch (ErrorList.OverflowException e)
                {
                throw new DerivationException("Error list full");
                }
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine if the Trait is modifiable in any way.  Only resolved traits
    * are modifiable.  If the trait containing this trait is not modifiable,
    * then neither is this trait.
    *
    * There is an underlying assumption that the trait is RESOLVED.  Only
    * RESOLVED traits are manipulated by the "tools".  All other states
    * are either intermediate or used for storage.
    *
    * @return true if the Trait is modifiable, false otherwise
    */
    public boolean isModifiable()
        {
        // not modifiable if parent is not modifiable
        return m_parent == null || m_parent.isModifiable();
        }


    // ----- Containment

    /**
    * Determine the trait which contains this trait.
    *
    * @return the containing trait or null if this trait is not contained
    */
    protected Trait getParentTrait()
        {
        return m_parent;
        }

    /**
    * Determine all traits which are contained by this trait.  Any trait that
    * contains other traits must implement this method.
    *
    * @return the an enumeration of traits contained by this trait
    */
    protected Enumeration getSubTraits()
        {
        return NullImplementation.getEnumeration();
        }

    /**
    * Determine the closest Component which contains this trait.
    *
    * @return the containing Component or null if this trait is not contained
    *         by a Component
    */
    public Component getParentComponent()
        {
        Trait parent = getParentTrait();
        while (parent != null && !(parent instanceof Component))
            {
            parent = parent.getParentTrait();
            }

        return (Component) parent;
        }

    // ----- Attribute:  Mode

    /**
    * Determine whether the trait is a resolved trait, a derivation, or a
    * modification.
    *
    * Note:  It is also possible that the trait has been invalidated.
    *
    * @return one of RESOLVED, DERIVATION, MODIFICATION, or INVALID
    *
    * @see com.tangosol.dev.component.Constants#RESOLVED
    * @see com.tangosol.dev.component.Constants#DERIVATION
    * @see com.tangosol.dev.component.Constants#MODIFICATION
    * @see com.tangosol.dev.component.Constants#INVALID
    */
    public int getMode()
        {
        return m_nMode;
        }

    /**
    * Set the mode of this trait.
    *
    * @param nMode a valid trait mode
    */
    protected void setMode(int nMode)
        {
        if (nMode != m_nMode)
            {
            switch (nMode)
                {
                case INVALID:
                case RESOLVED:
                case DERIVATION:
                case MODIFICATION:
                    m_nMode = nMode;
                    break;

                default:
                    throw new IllegalArgumentException(CLASS + ".setMode:  "
                            + "Illegal mode value (" + nMode + ")");
                }
            }
        }

    // ----- Attribute:  Process State

    /**
    */
    protected int getProcessState()
        {
        return m_nState;
        }

    /**
    * Inform the trait that it is now valid.
    */
    protected void validate()
        {
        // only validate if parent is already valid
        if ((m_parent == null || m_parent.m_fDispatch) && !m_fDispatch)
            {
            m_fDispatch = true;

            // validate contained traits
            for (Enumeration enmr = getSubTraits(); enmr.hasMoreElements(); )
                {
                ((Trait) enmr.nextElement()).validate();
                }
            }
        }

    /**
    * Discard this trait and make sure it is marked as un-usable.  Deriving
    * traits implement this method.  Deriving traits should invoke this
    * implementation first (via super) before invalidating their own data.
    */
    protected void invalidate()
        {
        if (m_nMode != INVALID)
            {
            m_fDispatch = false;

            // discard listeners
            notifyPre.removeAll();
            notifyPost.removeAll();
            notifySubPre.removeAll();
            notifySubPost.removeAll();

            // mark as invalidated
            setMode(INVALID);

            // invalidate contained traits
            for (Enumeration enmr = getSubTraits(); enmr.hasMoreElements(); )
                {
                ((Trait) enmr.nextElement()).invalidate();
                }

            // discard reference data, insuring that this trait will not
            // affect previously related traits and other objects
            // (this also theoretically facilitates garbage collection)
            notifyPre      = null;
            notifyPost     = null;
            notifySubPre   = null;
            notifySubPost  = null;
            m_parent       = null;
            m_uid          = null;
            m_tblOrigin    = null;
            m_sTip         = null;
            m_sPrevTip     = null;
            m_sDesc        = null;
            m_sPrevDesc    = null;
            }
        }


    // ----- Attribute:  Origin

    /**
    * Determine whether the trait was added at this level or at a previous
    * level.  This is equivalent to "isNotFromSuperOrBase", whether or not
    * additional origins (manual, traits) exist at this level.
    *
    * @return true if the trait was added at this level
    */
    public boolean isDeclaredAtThisLevel()
        {
        return (m_nOrigin & ORIGIN_LEVEL) == ORIGIN_THIS;
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
        return (m_nOrigin & ORIGIN_LEVEL) == ORIGIN_BASE;
        }

    /**
    * Specify that the trait existed at a base level.  This method was added
    * to provide a means to fix traits that were deferred during resolve of
    * modificaitions, since deferral until the derivation processing causes
    * the origin to incorrectly be "this".
    */
    protected void setFromBase()
        {
        // assertion:  don't allow the trait to already be from super
        azzert((m_nOrigin & ORIGIN_LEVEL) != ORIGIN_SUPER);

        m_nOrigin = (m_nOrigin & ~ORIGIN_LEVEL) | ORIGIN_BASE;
        }

    /**
    * Determine if the trait existed at a super level.  This means that
    * the trait is the result of derivation.
    *
    * @return true if the trait existed at a super level
    */
    public boolean isFromSuper()
        {
        return (m_nOrigin & ORIGIN_LEVEL) == ORIGIN_SUPER;
        }

    /**
    * Determine whether other traits contribute to this trait's origin at
    * this level.
    *
    * @return true if this trait has other traits as part of its origin at
    *         this level
    */
    public boolean isFromTrait()
        {
        return (m_nOrigin & ORIGIN_TRAIT) != 0;
        }

    /**
    * Determine whether the specified trait contributes to this trait's
    * origin at this level.
    *
    * @return true if the specified trait is part of this trait's origin at
    *         this level
    */
    public boolean isFromTrait(Trait trait)
        {
        StringTable tbl = m_tblOrigin;
        return tbl != null && tbl.contains(trait.getUniqueDescription());
        }

    /**
    * Enumerate all traits that contribute to the origin of this trait at
    * this level.  (This means that only those traits declared at this
    * level will contribute to this trait's origin.)
    *
    * (These cannot be the traits themselves since that would make
    * persistence close to impossible, so instead the "unique description"
    * is used.)
    *
    * @return an enumeration of trait descriptions that contribute to this
    *         trait's origin
    */
    protected Enumeration getOriginTraits()
        {
        StringTable tbl = m_tblOrigin;
        return (tbl == null ? NullImplementation.getEnumeration()
                            : tbl.keys());
        }

    /**
    * Determine whether any traits with the specified trait descriptor
    * contributes to this trait's origin at this level.
    *
    * @return true if at least one origin trait has the specified descriptor
    */
    protected boolean isFromTraitDescriptor(String sDescriptor)
        {
        StringTable tbl = m_tblOrigin;
        return tbl != null && tbl.stringsStartingWith(sDescriptor + ' ').length > 0;
        }

    /**
    * Enumerate the names (not including descriptors) of all traits that
    * contribute to the origin of this trait at this level.
    *
    * @return an enumeration of trait names for origin traits that begin with
    *         the specified descriptor string
    */
    protected Enumeration getOriginTraits(String sDescriptor)
        {
        StringTable tbl = m_tblOrigin;
        if (tbl == null)
            {
            return NullImplementation.getEnumeration();
            }

        String[] asDesc = tbl.stringsStartingWith(sDescriptor + ' ');
        int      cDesc  = asDesc.length;
        if (cDesc < 1)
            {
            return NullImplementation.getEnumeration();
            }

        for (int i = 0; i < cDesc; ++i)
            {
            String sDesc = asDesc[i];
            asDesc[i] = sDesc.substring(sDesc.indexOf(' ') + 1);
            }

        return new SimpleEnumerator(asDesc);
        }

    /**
    * Add the specified trait to the list of traits contributing to this
    * trait's origin.
    *
    * @param trait  the trait contributing to this trait's origin
    */
    protected void addOriginTrait(Trait trait)
        {
        StringTable tbl = m_tblOrigin;

        if (tbl == null)
            {
            m_tblOrigin = tbl = new StringTable();
            m_nOrigin |= ORIGIN_TRAIT;
            }

        tbl.add(trait.getUniqueDescription());
        }

    /**
    * Remove the specified trait from the list of traits contributing to
    * this trait's origin.
    *
    * @param trait  the trait contributing to this trait's origin
    */
    protected void removeOriginTrait(Trait trait)
        {
        StringTable tbl = m_tblOrigin;
        if (tbl != null)
            {
            tbl.remove(trait.getUniqueDescription());

            if (tbl.isEmpty())
                {
                m_tblOrigin = null;
                m_nOrigin &= ~ORIGIN_TRAIT;
                }
            }
        }

    /**
    * Determines if the trait exists at this level regardless of whether
    * it exists from derivation, implements, dispatches, or integrates.
    *
    * @return true if there is a manual origin
    */
    public boolean isFromManual()
        {
        return (m_nOrigin & ORIGIN_MANUAL) != 0;
        }

    /**
    * Set the manual origin.
    */
    protected void setFromManual()
        {
        m_nOrigin |= ORIGIN_MANUAL;
        }

    /**
    * Clear the manual origin.
    */
    protected void clearFromManual()
        {
        m_nOrigin &= ~ORIGIN_MANUAL;
        }

    /**
    * Determines if the trait exists for any non-manual reason.
    *
    * @return true if there is any non-manual origin
    */
    public boolean isFromNonManual()
        {
        return (m_nOrigin & ~ORIGIN_MANUAL) != 0;
        }

    /**
    * Determine if the trait has no origin.
    *
    * @return true if the trait has no origin
    */
    public boolean isFromNothing()
        {
        return m_nOrigin == 0;
        }


    // ----- Attribute:  UID

    /**
    * Access the UID for this trait.  A UID provides an identification tag
    * which is unrelated to the trait information itself, and allows
    * derivation and modification traits to resolve major changes which
    * occurred to their super/base traits.  For example, if the data type of
    * a parameter trait of a behavior trait of a component definition trait
    * changes, the behavior's signature (calculated from the method name and
    * parameter types) changes, but the UID wouldn't.  The derived method
    * would not have a matching signature super/base to resolve with, but
    * using the UID, it could locate its super/base and resolve.  (The
    * alternative is to discard.)
    *
    * @return this trait's UID or null if no UID is assigned
    */
    public UID getUID()
        {
        return m_uid;
        }

    /**
    * Sets the UID for this trait.
    *
    * @param uid  the UID for this trait, or null to remove the trait's UID
    */
    protected void setUID(UID uid)
            throws PropertyVetoException
        {
        setUID(uid, true);
        }

    /**
    * Sets the UID for this trait.
    *
    * @param uid        the UID for this trait, or null to remove the
    *                   trait's UID
    * @param fVetoable  true if the setter can veto the value
    */
    protected synchronized void setUID(UID uid, boolean fVetoable)
            throws PropertyVetoException
        {
        UID prev = m_uid;

        if (uid == null ? prev == null : uid.equals(prev))
            {
            return;
            }

        if (fVetoable)
            {
            if (!isModifiable())
                {
                readOnlyAttribute(ATTR_UID, prev, uid);
                }

            fireVetoableChange(ATTR_UID, prev, uid);
            }

        m_uid = uid;
        firePropertyChange(ATTR_UID, prev, uid);
        }

    /**
    * Make sure the trait has a UID.
    */
    protected void assignUID()
        {
        if (m_uid == null)
            {
            try
                {
                setUID(new UID(), false);
                }
            catch (PropertyVetoException e)
                {
                throw new IllegalStateException(CLASS + ".assignUID:  "
                        + "Unexpected Veto Exception!");
                }
            }
        }

    /**
    * Make sure the trait doesn't have a UID.
    */
    protected void clearUID()
        {
        try
            {
            setUID(null, false);
            }
        catch (PropertyVetoException e)
            {
            throw new IllegalStateException(CLASS + ".assignUID:  "
                    + "Unexpected Veto Exception!");
            }
        }

    /**
    * Determine the unique name for this trait.
    *
    * A sub-trait that exists within a collection of sub-traits must have
    * two ways of being identified:
    * <ol>
    * <li> A unique name, which is a string identifier
    * <li> A UID as a secondary identifier (dog tag)
    * </ol>
    * @return the primary string identifier of the trait
    */
    protected abstract String getUniqueName();

    /**
    * Determine the unique description for this trait.  The unique
    * description is in the format:
    *
    *   <descriptor> + ' ' + <uniquename>
    *
    * All traits must be uniquely identifiable by a "description string".
    * This enables the origin implementation built into Trait to use the
    * description string to differentiate between Trait origins.
    *
    * @return the description string for the trait
    */
    protected abstract String getUniqueDescription();

    /**
    * Helper method to create a hash-table to look up sub-trait primary
    * string ids by their secondary UIDs (dog tags).
    *
    * @param enmr  an enumeration of sub-traits
    *
    * @return a hash-table whose key is UID and whose value is a unique
    *         string identifier of the trait
    */
    protected static Hashtable getUIDTable(Enumeration enmr)
        {
        // create a hash-table to store the Trait UID's in
        Hashtable tbl = new Hashtable();

        while (enmr.hasMoreElements())
            {
            Trait trait = (Trait) enmr.nextElement();
            tbl.put(trait.getUID(), trait.getUniqueName());
            }

        return tbl;
        }


    // ----- Attribute:  Tip

    /**
    * Access the tip which is present at this level.  A tip is a short
    * description suitable for displaying as a tool tip, in a status line,
    * etc.
    *
    * @return this trait's tip, which will be 0-length if no tip exists
    */
    public String getTip()
        {
        return m_sTip;
        }

    /**
    * Determine if the tip can be set.
    *
    * @return true if the tip is settable, false otherwise
    */
    public boolean isTipSettable()
        {
        return isModifiable();
        }

    /**
    * Stores the tip for this level.
    *
    * @param sTip  the tip for this level, or blank to use the tip from a
    *              previous level
    */
    public void setTip(String sTip)
            throws PropertyVetoException
        {
        setTip(sTip, true);
        }

    /**
    * Stores the tip for this level.
    *
    * @param sTip       the tip for this level, or blank to use the tip from
    *                   a previous level
    * @param fVetoable  true if the setter can veto the value
    */
    protected synchronized void setTip(String sTip, boolean fVetoable)
            throws PropertyVetoException
        {
        String sPrev = m_sTip;

        if (sTip == null)
            {
            sTip = BLANK;
            }

        if (sTip.equals(sPrev))
            {
            return;
            }

        if (fVetoable)
            {
            if (!isTipSettable())
                {
                readOnlyAttribute(ATTR_TIP, sPrev, sTip);
                }

            if (sTip.length() == 0)
                {
                sTip = m_sPrevTip;
                }

            fireVetoableChange(ATTR_TIP, sPrev, sTip);
            }

        m_sTip = sTip;
        firePropertyChange(ATTR_TIP, sPrev, sTip);
        }


    // ----- Attribute:  Text

    /**
    * Access the description which is present at this level.  To access the
    * entire description, use the getDescription method.
    *
    * @return this level's description, which will be 0-length if no
    *          description exists at this level
    */
    public String getText()
        {
        return m_sDesc;
        }

    /**
    * Determine if the full-length textual description for this level can be
    * set.
    *
    * @return true if the description is settable, false otherwise
    */
    public boolean isTextSettable()
        {
        return isModifiable();
        }

    /**
    * Stores the description for this level.
    *
    * @param sDesc  this description for this level
    */
    public void setText(String sDesc)
            throws PropertyVetoException
        {
        setText(sDesc, true);
        }

    /**
    * Stores the description for this level.
    *
    * @param sDesc      this description for this level
    * @param fVetoable  true if the setter can veto the value
    */
    protected synchronized void setText(String sDesc, boolean fVetoable)
            throws PropertyVetoException
        {
        String sPrev = getText();

        if (sDesc == null || sDesc.length() == 0)
            {
            sDesc = BLANK;
            }

        if (sDesc.equals(sPrev))
            {
            return;
            }

        if (fVetoable)
            {
            if (!isTextSettable())
                {
                readOnlyAttribute(ATTR_TEXT, sPrev, sDesc);
                }

            fireVetoableChange(ATTR_TEXT, sPrev, sDesc);
            }

        m_sDesc = sDesc;
        firePropertyChange(ATTR_TEXT, sPrev, sDesc);
        }


    // ----- Attribute:  ReplaceDescription

    /**
    * Determine if the description adds to any previous level's description
    * or if it replaces the description from previous levels.
    *
    * @return true if this level's description replaces the description from
    *         previous levels, false if it adds to the description from
    *         previous levels
    */
    public boolean isReplaceDescription()
        {
        return m_fReplaceDesc;
        }

    /**
    * Determine if the DescriptionReplaced attribute can be changed.
    *
    * @return true if the DescriptionReplaced attribute can be changed
    */
    public boolean isReplaceDescriptionSettable()
        {
        return isModifiable();
        }

    /**
    * Toggle whether the description adds to any previous level's description
    * or whether it replaces the description from previous levels.
    *
    * @param fReplaceDesc true if this level's description should replace
    *                     the description from previous levels, false if it
    *                     should add to the description from previous levels
    */
    public void setReplaceDescription(boolean fReplaceDesc)
            throws PropertyVetoException
        {
        setReplaceDescription(fReplaceDesc, true);
        }

    /**
    * Toggle whether the description adds to any previous level's description
    * or whether it replaces the description from previous levels.
    *
    * @param fReplaceDesc true if this level's description should replace
    *                     the description from previous levels, false if it
    *                     should add to the description from previous levels
    * @param fVetoable    true if the setter can veto the value
    */
    protected synchronized void setReplaceDescription(boolean fReplaceDesc, boolean fVetoable)
            throws PropertyVetoException
        {
        boolean fPrev = m_fReplaceDesc;

        if (fReplaceDesc == fPrev)
            {
            return;
            }

        Boolean value = toBoolean(fReplaceDesc);
        Boolean prev  = toBoolean(fPrev);

        if (fVetoable)
            {
            if (!isReplaceDescriptionSettable())
                {
                readOnlyAttribute(ATTR_REPDESC, prev, value);
                }

            fireVetoableChange(ATTR_REPDESC, prev, value);
            }

        m_fReplaceDesc = fReplaceDesc;
        firePropertyChange(ATTR_REPDESC, prev, value);
        }


    // ----- Attribute:  Description (calculated from Text and DescriptionReplaced)

    /**
    * Get the trait's description.
    *
    * @return this trait's description, which will be 0-length if no
    *          description exists
    */
    public String getDescription()
        {
        return getDescription(m_sDesc, m_sPrevDesc, m_fReplaceDesc);
        }

    /**
    * Build a trait description.
    *
    * @return the trait description
    */
    protected static String getDescription(String sDesc, String sPrevDesc, boolean fReplaceDesc)
        {
        if (fReplaceDesc || sPrevDesc == BLANK)
            {
            // this description fully replaces any previous level's
            return sDesc;
            }
        else if (sDesc == BLANK)
            {
            // there is no description at this level
            return sPrevDesc;
            }
        else
            {
            // this description builds on any previous level's
            return sPrevDesc + '\n' + sDesc;
            }
        }

    /**
    * Determine if the full-length textual description can be set.
    *
    * @return true if the description is settable, false otherwise
    */
    public boolean isDescriptionSettable()
        {
        return isTextSettable();
        }

    /**
    * Stores the trait's description.  The description attribute is a
    * combination of the Text and DescriptionReplaced attributes in
    * that the Text attribute only access this level's description
    * but the Description attribute returns the entire description and
    * potentially sets both this level's text and whether this level's
    * text overrides or adds to the super level's text.
    *
    * @param sDesc  the trait's description
    */
    public void setDescription(String sDesc)
            throws PropertyVetoException
        {
        setDescription(sDesc, true);
        }

    /**
    * Stores the trait's description.  The description attribute is a
    * combination of the Text and DescriptionReplaced attributes in
    * that the Text attribute only access this level's description
    * but the Description attribute returns the entire description and
    * potentially sets both this level's text and whether this level's
    * text overrides or adds to the super level's text.
    *
    * @param sDesc      the trait's description
    * @param fVetoable  true if the setter can veto the value
    */
    protected synchronized void setDescription(String sDesc, boolean fVetoable)
            throws PropertyVetoException
        {
        String sPrevDesc = getDescription();

        if (sDesc == null || sDesc.length() == 0)
            {
            sDesc = BLANK;
            }

        if (sDesc.equals(sPrevDesc))
            {
            return;
            }

        if (fVetoable)
            {
            if (!isDescriptionSettable())
                {
                readOnlyAttribute(ATTR_DESC, sPrevDesc, sDesc);
                }

            fireVetoableChange(ATTR_DESC, sPrevDesc, sDesc);
            }

        // determine if the description adds to or replaces the super
        // level's description
        m_fReplaceDesc = !sDesc.startsWith(m_sPrevDesc);

        // if the new description doesn't replace the super level's
        // description, then determine what is added
        if (!m_fReplaceDesc)
            {
            sDesc = extractDescription(sDesc, m_sPrevDesc);
            }

        m_sDesc = sDesc;

        firePropertyChange(ATTR_DESC, sPrevDesc, sDesc);
        }

    protected static String extractDescription(String sThisDesc, String sBaseDesc)
        {
        int cchThisDesc = sThisDesc.length();
        int cchBaseDesc = sBaseDesc.length();

        if (cchThisDesc == cchBaseDesc ||
            cchThisDesc == cchBaseDesc + 1 && sThisDesc.charAt(cchBaseDesc) == '\n')
            {
            sThisDesc = BLANK;
            }
        else if (cchBaseDesc > 0 && cchThisDesc > cchBaseDesc && sThisDesc.startsWith(sBaseDesc))
            {
            // keep everything after the super description/linefeed
            int cchLF = (sThisDesc.charAt(cchBaseDesc) == '\n' ? 1 : 0);
            sThisDesc = sThisDesc.substring(cchBaseDesc + cchLF);
            }

        return sThisDesc;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare this Trait's UID to another Trait's UID.
    *
    * @param that  the other Trait
    *
    * @return true if this Trait's UID matches that Trait's UID
    */
    public boolean equalsUID(Trait that)
        {
        return (this.m_uid == null ? that.m_uid == null
                                   : this.m_uid.equals(that.m_uid));
        }

    /**
    * Compare this Trait's origin to another Trait's origin.
    *
    * @param that  the other Trait
    *
    * @return true if this Trait's origin matches that Trait's origin
    */
    public boolean equalsOrigin(Trait that)
        {
        if (this.m_nOrigin != that.m_nOrigin)
            {
            return false;
            }

        if (this.m_tblOrigin != null)
            {
            return this.m_tblOrigin.equals(that.m_tblOrigin);
            }

        return true;
        }

    /**
    * Compare this Trait's origin to another Trait's origin, ignoring the
    * manual origin information.
    *
    * @param that  the other Trait
    *
    * @return true if this Trait's origin matches that Trait's origin
    *         (other than the manual origin)
    */
    protected boolean equalsOriginSansManual(Trait that)
        {
        if (((this.m_nOrigin ^ that.m_nOrigin) & ~ORIGIN_MANUAL) != 0)
            {
            return false;
            }

        if (this.m_tblOrigin != null)
            {
            return this.m_tblOrigin.equals(that.m_tblOrigin);
            }

        return true;
        }

    /**
    * Compare this Trait to another Object for equality.  This method is
    * implemented by each trait.
    *
    * @param obj  the other Object to compare to this
    *
    * @return true if this Trait equals that Object
    */
    public boolean equals(Object obj)
        {
        if (obj instanceof Trait)
            {
            Trait that = (Trait) obj;
            return this                     == that
                || this.m_nMode             == that.m_nMode
                && this.m_fReplaceDesc      == that.m_fReplaceDesc
                && this.m_sTip     .equals(that.m_sTip     )
                && this.m_sDesc    .equals(that.m_sDesc    )
                && this.equalsOrigin(that)
                && this.equalsUID(that);

            // PJM Removed the following because they are not persistent fields
            //&& this.m_fPrevReplaceDesc  == that.m_fPrevReplaceDesc
            //&& this.m_sPrevDesc.equals(that.m_sPrevDesc)
            //&& this.m_sPrevTip .equals(that.m_sPrevTip )
            }
        return false;
        }

    /**
    * Provide a short human-readable description of the trait.
    *
    * @return a human-readable description of this trait
    */
    public String toString()
        {
        return getUniqueDescription();
        }

    /**
    * Provide a short human-readable hierarchical path to the trait.
    *
    * @return a human-readable description of this trait
    */
    public String toPathString()
        {
        Trait parent = m_parent;
        return (parent == null ? toString()
                               : parent.toPathString() + ", " + toString());
        }


    // ----- notifications --------------------------------------------------

    /**
    * Add a VetoableChangeListener to the listener list.
    *
    * @param listener  The VetoableChangeListener to be added
    */
    public void addVetoableChangeListener(VetoableChangeListener listener)
        {
        notifyPre.add(listener);
        }

    /**
    * Remove a VetoableChangeListener from the listener list.
    *
    * @param listener  The VetoableChangeListener to be removed
    */
    public void removeVetoableChangeListener(VetoableChangeListener listener)
        {
        notifyPre.remove(listener);
        }

    /**
    * Add a PropertyChangeListener to the listener list.
    *
    * @param listener  The PropertyChangeListener to be added
    */
    public void addPropertyChangeListener(PropertyChangeListener listener)
        {
        notifyPost.add(listener);
        }

    /**
    * Remove a PropertyChangeListener from the listener list.
    *
    * @param listener  The PropertyChangeListener to be removed
    */
    public void removePropertyChangeListener(PropertyChangeListener listener)
        {
        notifyPost.remove(listener);
        }

    /**
    * Add a VetoableSubChangeListener to the listener list.
    *
    * @param listener  The VetoableSubChangeListener to be added
    */
    public void addVetoableSubChangeListener(VetoableSubChangeListener listener)
        {
        notifySubPre.add(listener);
        }

    /**
    * Remove a VetoableSubChangeListener from the listener list.
    *
    * @param listener  The VetoableSubChangeListener to be removed
    */
    public void removeVetoableSubChangeListener(VetoableSubChangeListener listener)
        {
        notifySubPre.remove(listener);
        }

    /**
    * Add a SubChangeListener to the listener list.
    *
    * @param listener  The SubChangeListener to be added
    */
    public void addSubChangeListener(SubChangeListener listener)
        {
        notifySubPost.add(listener);
        }

    /**
    * Remove a SubChangeListener from the listener list.
    *
    * @param listener  The SubChangeListener to be removed
    */
    public void removeSubChangeListener(SubChangeListener listener)
        {
        notifySubPost.remove(listener);
        }

    /**
    * This helper function makes sure that the boolean true is always
    * the Boolean TRUE and the boolean false is always the Boolean FALSE.
    *
    * This method is used by the setters for boolean properties in order
    * to pass an object to a property change event corresponding to the
    * previous and new boolean values of the proeprty.
    *
    * @param f  either true or false
    *
    * @return either Boolean.TRUE or Boolean.FALSE
    */
    protected static Boolean toBoolean(boolean f)
        {
        return f ? Boolean.TRUE : Boolean.FALSE;
        }

    /**
    * Throw an exception when an attempt is made to set a read-only property.
    *
    * @param sAttribute the attribute name
    * @param oPrevVal   the value of the attribute
    * @param oNewVal    the value that the caller attempted to set the
    *                   read-only attribute to
    *
    * @exception PropertyVetoException this exception is always thrown
    */
    protected void readOnlyAttribute(String sAttribute, Object oPrevVal, Object oNewVal)
            throws PropertyVetoException
        {
        String sDesc = RESOURCES.getString(ATTR_READONLY, new String[] {sAttribute},
                "The \"{0}\" attribute is not modifiable.");

        PropertyChangeEvent evt =
                new PropertyChangeEvent(this, sAttribute, oPrevVal, oNewVal);

        throw new PropertyVetoException(sDesc, evt);
        }

    /**
    * Throw an exception when an attempt is made to set property to an
    * illegal value.
    *
    * @param sAttribute the attribute name
    * @param oPrevVal   the value of the attribute
    * @param oNewVal    the value that the caller attempted to set the
    *                   attribute to
    *
    * @exception PropertyVetoException this exception is always thrown
    */
    protected void illegalAttributeValue(String sAttribute, Object oPrevVal, Object oNewVal)
            throws PropertyVetoException
        {
        String sDesc = RESOURCES.getString(ATTR_ILLEGAL, new String[] {sAttribute},
                "An attempt was made to set the \"{0}\" attribute to an illegal value.");

        PropertyChangeEvent evt =
                new PropertyChangeEvent(this, sAttribute, oPrevVal, oNewVal);

        throw new PropertyVetoException(sDesc, evt);
        }

    /**
    * Throw an exception when an invalid attempt is made to add a sub-trait.
    *
    * @param sAttribute the attribute name
    *
    * @exception PropertyVetoException this exception is always thrown
    */
    protected void subNotAddable(String sAttribute, Object oValue)
            throws PropertyVetoException
        {
        String sDesc = RESOURCES.getString(ATTR_NO_ADD, new String[] {sAttribute},
                "The \"{0}\" sub-trait is not addable.");

        PropertyChangeEvent evt =
                new PropertyChangeEvent(this, sAttribute, null, oValue);

        throw new PropertyVetoException(sDesc, evt);
        }

    /**
    * Throw an exception when an invalid attempt is made to remove a
    * sub-trait.
    *
    * @param sAttribute the attribute name
    *
    * @exception PropertyVetoException this exception is always thrown
    */
    protected void subNotRemovable(String sAttribute, Object oValue)
            throws PropertyVetoException
        {
        String sDesc = RESOURCES.getString(ATTR_NO_REMOVE, new String[] {sAttribute},
                "The \"{0}\" sub-trait is not removable.");

        PropertyChangeEvent evt =
                new PropertyChangeEvent(this, sAttribute, oValue, null);

        throw new PropertyVetoException(sDesc, evt);
        }

    /**
    * Throw an exception when an invalid attempt is made to un-remove a
    * sub-trait.
    *
    * @param sAttribute the attribute name
    *
    * @exception PropertyVetoException this exception is always thrown
    */
    protected void subNotUnremovable(String sAttribute, Object oValue)
            throws PropertyVetoException
        {
        String sDesc = RESOURCES.getString(ATTR_NO_UNREMOVE, new String[] {sAttribute},
                "The \"{0}\" sub-trait is not unremovable.");

        PropertyChangeEvent evt =
                new PropertyChangeEvent(this, sAttribute, null, oValue);

        throw new PropertyVetoException(sDesc, evt);
        }

    /**
    * Fire the vetoable change event for an attribute.
    *
    * @param sAttribute  name of attribute
    * @param oPrevVal    previous value of attribute
    * @param oNewVal     new value of attribute
    *
    * @exception PropertyVetoException if a notified object rejects the
    *            proposed attribute change
    */
    protected void fireVetoableChange(String sAttribute, Object oPrevVal, Object oNewVal)
            throws PropertyVetoException
        {
        fireAttributeChange(sAttribute, oPrevVal, oNewVal, CTX_VETO, null);
        }

    /**
    * Fire the property change event (non-vetoable) for an attribute.
    *
    * @param sAttribute  name of attribute
    * @param oPrevVal    previous value of attribute
    * @param oNewVal     new value of attribute
    */
    protected void firePropertyChange(String sAttribute, Object oPrevVal, Object oNewVal)
        {
        try
            {
            fireAttributeChange(sAttribute, oPrevVal, oNewVal, CTX_DONE, null);
            }
        catch (PropertyVetoException e)
            {
            throw new RuntimeException(CLASS + ".firePropertyChange:  "
                    + "Illegal PropertyVetoException: " + e.toString());
            }
        }

    /**
    * Fire the vetoable change event for an attribute.  Assumption is that
    * most of the time there are zero listeners, occasionally there may be
    * one listener, and very rarely there is more than one.
    *
    * @param sAttribute  name of attribute
    * @param oPrevVal    previous value of attribute
    * @param oNewVal     new value of attribute
    * @param nContext    one of CTX_VETO, CTX_UNDO, CTX_DONE
    * @param oStop       for undo events only, the last listener to issue an
    *                    undo event to
    *
    * @exception PropertyVetoException if a notified object rejects the
    *            proposed attribute change and the notification is vetoable
    */
    private void fireAttributeChange(String sAttribute, Object oPrevVal, Object oNewVal, int nContext, Object oStop)
            throws PropertyVetoException
        {
        // no events are dispatched until the trait is validated
        if (!m_fDispatch)
            {
            return;
            }

        // pre (veto/undo) or post (done)
        boolean fPre = (nContext != CTX_DONE);

        // fire property change event
        PropertyChangeEvent evtChange   = null;
        Listeners           notify      = (fPre ? notifyPre : notifyPost);
        EventListener[]     listeners   = notify.listeners();
        int                 cListeners  = listeners.length;
        for (int iListener = 0; iListener < cListeners; ++iListener)
            {
            if (evtChange == null)
                {
                // create event object
                evtChange = new PropertyChangeEvent(this, sAttribute, oPrevVal, oNewVal);
                }

            switch (nContext)
                {
                case CTX_VETO:
                    {
                    VetoableChangeListener listener = (VetoableChangeListener) listeners[iListener];
                    try
                        {
                        listener.vetoableChange(evtChange);
                        }
                    catch (PropertyVetoException e)
                        {
                        // issue undo events
                        fireAttributeChange(sAttribute, oNewVal, oPrevVal, CTX_UNDO, listener);

                        // re-throw veto
                        throw e;
                        }
                    }
                    break;

                case CTX_UNDO:
                    {
                    VetoableChangeListener listener = (VetoableChangeListener) listeners[iListener];
                    try
                        {
                        listener.vetoableChange(evtChange);
                        }
                    catch (PropertyVetoException e)
                        {
                        // undo cannot be vetoed
                        }

                    // check if this is the last listener to issue an undo to
                    if (listener == oStop)
                        {
                        return;
                        }
                    }
                    break;

                case CTX_DONE:
                    ((PropertyChangeListener) listeners[iListener]).propertyChange(evtChange);
                    break;
                }
            }

        // fire sub change event up the trait containment chain
        SubChangeEvent evtSub = null;
        for (Trait trait = m_parent; trait != null; trait = trait.m_parent)
            {
            notify      = (fPre ? trait.notifySubPre : trait.notifySubPost);
            listeners   = notify.listeners();
            cListeners  = listeners.length;
            for (int iListener = 0; iListener < cListeners; ++iListener)
                {
                if (evtSub == null)
                    {
                    if (evtChange == null)
                        {
                        evtChange = new PropertyChangeEvent(this, sAttribute, oPrevVal, oNewVal);
                        }

                    evtSub = new SubChangeEvent(this, this, SUB_CHANGE, nContext, evtChange);
                    }

                switch (nContext)
                    {
                    case CTX_VETO:
                        {
                        VetoableSubChangeListener listener = (VetoableSubChangeListener) listeners[iListener];
                        try
                            {
                            listener.vetoableSubChange(evtSub);
                            }
                        catch (PropertyVetoException e)
                            {
                            // issue undo events
                            fireAttributeChange(sAttribute, oNewVal, oPrevVal, CTX_UNDO, listener);

                            // re-throw veto
                            throw e;
                            }
                        }
                        break;

                    case CTX_UNDO:
                        {
                        VetoableSubChangeListener listener = (VetoableSubChangeListener) listeners[iListener];
                        try
                            {
                            listener.vetoableSubChange(evtSub);
                            }
                        catch (PropertyVetoException e)
                            {
                            // undo cannot be vetoed
                            }

                        // check if this is the last listener to issue an undo to
                        if (listener == oStop)
                            {
                            return;
                            }
                        }
                        break;

                    case CTX_DONE:
                        ((SubChangeListener) listeners[iListener]).subChange(evtSub);
                        break;
                    }
                }
            }
        }

    /**
    * Fire the sub trait change event which occurs when a trait is added,
    * removed, or un-removed from this trait.
    *
    * @param traitSub the sub-trait being added, removed, or un-removed
    * @param nAction  the action occuring to the sub-trait
    *
    * @exception PropertyVetoException if a notified object rejects the
    *            proposed action
    */
    protected void fireVetoableSubChange(Trait traitSub, int nAction)
            throws PropertyVetoException
        {
        fireSubChange(traitSub, nAction, CTX_VETO, null);
        }

    /**
    * Fire the traitSubChange event which occurs when a trait is added,
    * removed, or un-removed from this trait.
    *
    * @param traitSub the sub-trait being added, removed, or un-removed
    * @param nAction  the action occuring to the sub-trait
    */
    protected void fireSubChange(Trait traitSub, int nAction)
        {
        try
            {
            fireSubChange(traitSub, nAction, CTX_DONE, null);
            }
        catch (PropertyVetoException e)
            {
            throw new RuntimeException(CLASS + ".fireSubChange:  "
                    + "Illegal PropertyVetoException: " + e.toString());
            }
        }

    /**
    * Fire the vetoable sub change event for a trait.  Assumption is that
    * most of the time there are zero listeners, occasionally there may be
    * one listener, and very rarely there is more than one.
    *
    * @param traitSub  the sub-trait being added, removed, or un-removed
    * @param nAction   the action occuring to the sub-trait
    * @param nContext  one of CTX_VETO, CTX_UNDO, CTX_DONE
    * @param oStop     for undo events only, the last listener to issue an
    *                  undo event to
    *
    * @exception PropertyVetoException if a notified object rejects the
    *            proposed attribute change and the notification is vetoable
    */
    private void fireSubChange(Trait traitSub, int nAction, int nContext, Object oStop)
            throws PropertyVetoException
        {
        // no events are dispatched until the trait is validated
        if (!m_fDispatch)
            {
            return;
            }

        // fire sub change event up the trait containment chain
        SubChangeEvent evt = null;
        for (Trait trait = this; trait != null; trait = trait.m_parent)
            {
            Listeners       notify     = (nContext == CTX_DONE ? trait.notifySubPost
                                                               : trait.notifySubPre);
            EventListener[] listeners  = notify.listeners();
            int             cListeners = listeners.length;
            for (int iListener = 0; iListener < cListeners; ++iListener)
                {
                if (evt == null)
                    {
                    evt = new SubChangeEvent(this, traitSub, nAction, nContext, null);
                    }

                switch (nContext)
                    {
                    case CTX_VETO:
                        {
                        VetoableSubChangeListener listener = (VetoableSubChangeListener) listeners[iListener];
                        try
                            {
                            listener.vetoableSubChange(evt);
                            }
                        catch (PropertyVetoException e)
                            {
                            // issue undo events
                            int nUndoAction = (nAction == SUB_REMOVE ? SUB_ADD : SUB_REMOVE);
                            fireSubChange(traitSub, nUndoAction, CTX_UNDO, listener);

                            // re-throw veto
                            throw e;
                            }
                        }
                        break;

                    case CTX_UNDO:
                        {
                        VetoableSubChangeListener listener = (VetoableSubChangeListener) listeners[iListener];
                        try
                            {
                            listener.vetoableSubChange(evt);
                            }
                        catch (PropertyVetoException e)
                            {
                            // undo cannot be vetoed
                            }

                        // check if this is the last listener to issue an undo to
                        if (listener == oStop)
                            {
                            return;
                            }
                        }
                        break;

                    case CTX_DONE:
                        ((SubChangeListener) listeners[iListener]).subChange(evt);
                        break;
                    }
                }
            }
        }


    // ----- debugging ------------------------------------------------------

    /**
    * Print the entire set of trait information to standard output.
    */
    public void dump()
        {
        PrintWriter out = getOut();
        dumpTree(out, BLANK);
        out.println();
        dump(out, BLANK);
        }

    /**
    * Print the trait hierarchy to the specified PrintWriter object using
    * the specified indentation.
    *
    * @param out      the PrintWriter object to dump the information to
    * @param sIndent  a string used to indent each line of dumped information
    */
    public void dumpTree(PrintWriter out, String sIndent)
        {
        out.println(sIndent + getUniqueDescription());
        for (Enumeration enmr = getSubTraits(); enmr.hasMoreElements(); )
            {
            Trait trait = (Trait) enmr.nextElement();
            trait.dumpTree(out, nextIndent(sIndent));
            }
        }

    /**
    * Print the entire set of trait information to the specified PrintWriter
    * object using the specified indentation.
    *
    * @param out      the PrintWriter object to dump the information to
    * @param sIndent  a string used to indent each line of dumped information
    */
    public void dump(PrintWriter out, String sIndent)
        {
        final String NULL = "<null>";

        String sMode;
        switch (m_nMode)
            {
            case RESOLVED:
                sMode     = "resolved";
                break;
            case DERIVATION:
                sMode     = "derivation";
                break;
            case MODIFICATION:
                sMode     = "modification";
                break;
            case INVALID:
                sMode     = "invalid";
                break;
            default:
                sMode     = "<unknown>";
                break;
            }

        String sState;
        switch (m_nState)
            {
            case STATE_NEW:
                sState    = "new";
                break;
            case STATE_RESOLVING:
                sState    = "resolving";
                break;
            case STATE_RESOLVED:
                sState    = "resolved";
                break;
            case STATE_EXTRACTING:
                sState    = "extracting";
                break;
            default:
                sState    = "<unknown>";
                break;
            }

        out.print  (sIndent + "Parent=");
        out.println(m_parent == null ? NULL : m_parent.toString() + " (depth=" + getDepth() + ")");

        out.print  (sIndent + "Mode=" + sMode);
        out.print  (", Processing State=" + sState);
        out.print  (", Modifiable=" + toBoolean(isModifiable()).toString());
        out.println(", UID=" + (m_uid == null ? NULL : m_uid.toString()));

        String sLevel;
        switch (m_nOrigin & ORIGIN_LEVEL)
            {
            case ORIGIN_THIS:
                sLevel = "this";
                break;
            case ORIGIN_BASE:
                sLevel = "base";
                break;
            case ORIGIN_SUPER:
                sLevel = "super";
                break;
            default:
                sLevel = "<invalid>";
                break;
            }

        out.print  (sIndent + "Origin level=" + sLevel);
        out.print  (", Manual=" + toBoolean((m_nOrigin & ORIGIN_MANUAL) != 0).toString());
        out.print  (", Trait="  + toBoolean((m_nOrigin & ORIGIN_TRAIT ) != 0).toString());
        out.println(" " + m_tblOrigin);

        out.println(sIndent + "Tip=" + dump(m_sTip));
        out.println(sIndent + "Previous Tip=" + dump(m_sPrevTip));

        out.print  (sIndent + "Description ");
        out.print  (m_fReplaceDesc ? "(replaced)=" : "(added)=");
        out.println(indentString(dump(m_sDesc), sIndent, false));

        out.print  (sIndent + "Previous Description ");
        out.print  (m_fPrevReplaceDesc ? "(replaced)=" : "(added)=");
        out.println(indentString(dump(m_sPrevDesc), sIndent, false));
        }

    protected String dump(String s)
        {
        if (s == BLANK)
            {
            return "<blank>";
            }
        if (s == null)
            {
            return "<null>";
            }
        return '\"' + s + '\"';
        }


    /**
    * Print the entire set of trait information to the specified PrintWriter
    * object using the specified indentation.
    *
    * @param out      the PrintWriter object to dump the information to
    * @param sIndent  a string used to indent each line of dumped information
    * @param tbl      the StringTable whose elements are traits
    * @param sDesc    a descrition string
    */
    protected static void dump(PrintWriter out, String sIndent, StringTable tbl, String sDesc)
        {
        out.println(sIndent + (tbl == null ? "<null>" : "" + tbl.getSize()) + ' ' + sDesc);
        if (tbl == null)
            {
            return;
            }

        if (!tbl.isEmpty())
            {
            String[] as = tbl.strings();
            int      c  = as.length;
            for (int i = 0; i < c; ++i)
                {
                String sName = as[i];
                Trait  trait = (Trait) tbl.get(sName);
                out.print(sIndent + "[" + i + "] " + sName + ":");
                if (trait == null)
                    {
                    out.println("  <null>");
                    }
                else
                    {
                    out.println();
                    trait.dump(out, nextIndent(sIndent));
                    }
                }
            }
        }

    /**
    * Helper for dump to indent sub-trait dumps.
    *
    * @param sIndent  the current indentation string
    *
    * @return the next level's indentation string
    */
    protected static String nextIndent(String sIndent)
        {
        return sIndent + "  ";
        }

    /**
    * Determine the number of levels (parent/child) down this Trait is.
    * For example, a global Component Definition is at level 0, the child
    * of a global Component Definition is at level 1, and so forth.
    *
    * @return the number of levels down that this Trait is located
    */
    protected int getDepth()
        {
        int cLevels = 0;
        Trait trait = this;
        while (trait.m_parent != null)
            {
            trait = trait.m_parent;
            ++cLevels;
            }

        return cLevels;
        }

    /**
    * Helper for debugging methods to print an array of objects.
    *
    * @param ao an array of object to print
    *
    * @return a string containing the contents of the array
    */
    protected static String arrayDescription(Object[] ao)
        {
        if (ao == null)
            {
            return "<null>";
            }

        int c = ao.length;
        if (c == 0)
            {
            return "<none>";
            }

        StringBuffer sb = new StringBuffer();

        sb.append('[')
          .append(c)
          .append("] (");

        for (int i = 0; i < c; ++i)
            {
            if (i > 0)
                {
                sb.append(", ");
                }

            sb.append(ao[i].toString());
            }

        sb.append(')');

        return sb.toString();
        }

    /**
    * Helper for debugging methods to print a description of the flags.
    *
    * @param nFlags     corresponds to the bit flags used by the Component
    *                   Definition and various traits
    * @param nMask      specifies which flag attibutes to display; this is a
    *                   bitwise combination of xxx_SPECIFIED values
    * @param fSpecOnly  pass true to only display the attributes from nMask
    *                   which are specified in nFlags (mainly for
    *                   modifications)
    *
    * @return a string of flag descriptions
    */
    protected static String flagsDescription(int nFlags, int nMask, boolean fSpecOnly)
        {
        StringBuffer sb = new StringBuffer();

        if ((nMask & MISC_ISTHROWABLE) != 0 && (nFlags & MISC_ISTHROWABLE) != 0)
            {
            sb.append(" throwable");
            }

        if ((nMask & MISC_ISINTERFACE) != 0)
            {
            sb.append((nFlags & MISC_ISINTERFACE) == 0 ? " class" : " interface");
            }

        if ( (nMask  & EXISTS_SPECIFIED) != 0 &&
            ((nFlags & EXISTS_SPECIFIED) != 0 || !fSpecOnly))
            {
            switch (nFlags & EXISTS_MASK)
                {
                case EXISTS_NOT:
                    sb.append(" non-existent");
                    break;
                case EXISTS_INSERT:
                    sb.append(" insert");
                    break;
                case EXISTS_UPDATE:
                    sb.append(" update");
                    break;
                case EXISTS_DELETE:
                    sb.append(" delete");
                    break;
                default:
                    sb.append(" <illegal exists>");
                    break;
                }
            }

        if ( (nMask  & ACCESS_SPECIFIED) != 0 &&
            ((nFlags & ACCESS_SPECIFIED) != 0 || !fSpecOnly))
            {
            switch (nFlags & ACCESS_MASK)
                {
                case ACCESS_PUBLIC:
                    sb.append(" public");
                    break;
                case ACCESS_PROTECTED:
                    sb.append(" protected");
                    break;
                case ACCESS_PACKAGE:
                    sb.append(" package-private");
                    break;
                case ACCESS_PRIVATE:
                    sb.append(" private");
                    break;
                default:
                    sb.append(" <illegal access>");
                    break;
                }
            }

        if ( (nMask  & SYNC_SPECIFIED) != 0 &&
            ((nFlags & SYNC_SPECIFIED) != 0 || !fSpecOnly))
            {
            sb.append((nFlags & SYNC_MASK) == SYNC_MONITOR ? " synchronized" : " no-monitor");
            }

        if ( (nMask  & SCOPE_SPECIFIED) != 0 &&
            ((nFlags & SCOPE_SPECIFIED) != 0 || !fSpecOnly))
            {
            sb.append((nFlags & SCOPE_MASK) == SCOPE_INSTANCE ? " instance" : " static");
            }

        if ( (nMask  & IMPL_SPECIFIED) != 0 &&
            ((nFlags & IMPL_SPECIFIED) != 0 || !fSpecOnly))
            {
            sb.append((nFlags & IMPL_MASK) == IMPL_CONCRETE ? " concrete" : " abstract");
            }

        if ( (nMask  & DERIVE_SPECIFIED) != 0 &&
            ((nFlags & DERIVE_SPECIFIED) != 0 || !fSpecOnly))
            {
            sb.append((nFlags & DERIVE_MASK) == DERIVE_DERIVABLE ? " derivable" : " final");
            }

        if ( (nMask  & ANTIQ_SPECIFIED) != 0 &&
            ((nFlags & ANTIQ_SPECIFIED) != 0 || !fSpecOnly))
            {
            sb.append((nFlags & ANTIQ_MASK) == ANTIQ_CURRENT ? " current" : " deprecated");
            }

        if ( (nMask  & STG_SPECIFIED) != 0 &&
            ((nFlags & STG_SPECIFIED) != 0 || !fSpecOnly))
            {
            sb.append((nFlags & STG_MASK) == STG_PERSIST ? " persistent" : " transient");
            }

        if ( (nMask  & DIST_SPECIFIED) != 0 &&
            ((nFlags & DIST_SPECIFIED) != 0 || !fSpecOnly))
            {
            sb.append((nFlags & DIST_MASK) == DIST_LOCAL ? " local" : " remote");
            }

        if ( (nMask  & DIR_SPECIFIED) != 0 &&
            ((nFlags & DIR_SPECIFIED) != 0 || !fSpecOnly))
            {
            switch (nFlags & DIR_MASK)
                {
                case DIR_IN:
                    sb.append(" in");
                    break;
                case DIR_OUT:
                    sb.append(" out");
                    break;
                case DIR_INOUT:
                    sb.append(" inout");
                    break;
                default:
                    sb.append(" <illegal direction>");
                    break;
                }
            }

        if ( (nMask  & VIS_SPECIFIED) != 0 &&
            ((nFlags & VIS_SPECIFIED) != 0 || !fSpecOnly))
            {
            switch (nFlags & VIS_MASK)
                {
                case VIS_SYSTEM:
                    sb.append(" system");
                    break;
                case VIS_HIDDEN:
                    sb.append(" hidden");
                    break;
                case VIS_ADVANCED:
                    sb.append(" advanced");
                    break;
                case VIS_VISIBLE:
                    sb.append(" visible");
                    break;
                default:
                    sb.append(" <illegal visibility>");
                    break;
                }
            }

        if ( (nMask  & PROP_SPECIFIED) != 0 &&
            ((nFlags & PROP_SPECIFIED) != 0 || !fSpecOnly))
            {
            switch (nFlags & PROP_MASK)
                {
                case PROP_SINGLE:
                    sb.append(" single");
                    break;
                case PROP_INDEXED:
                    sb.append(" indexed");
                    break;
                case PROP_INDEXEDONLY:
                    sb.append(" indexed only");
                    break;
                default:
                    sb.append(" <illegal property>");
                    break;
                }
            }

        return (sb.length() > 0 ? sb.toString().substring(1) : "<none>");
        }


    // ----- public constants -----------------------------------------------

    /**
    * The UID attribute name.
    */
    public static final String ATTR_UID     = "UID";

    /**
    * The Tip attribute name.
    */
    public static final String ATTR_TIP     = "Tip";

    /**
    * The Text attribute name.
    */
    public static final String ATTR_TEXT    = "Text";

    /**
    * The ReplaceDescription attribute name.
    */
    public static final String ATTR_REPDESC = "ReplaceDescription";

    /**
    * The Description attribute name.
    */
    public static final String ATTR_DESC    = "Description";


    // ----- other constants ------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Trait";

    /**
    * Debug flag.  Allows for conditional debugging behavior.
    */
    protected static final boolean DEBUG;
    static
        {
        boolean fDebug = false;
        String  sDebug = Config.getProperty("coherence.component.debug");
        if (sDebug != null && sDebug.length() > 0)
            {
            char ch = sDebug.charAt(0);
            switch (ch)
                {
                case '1':
                case 'Y':
                case 'y':
                case 'T':
                case 't':
                    fDebug = true;
                }
            }
        DEBUG = fDebug;
        }

    /**
    * Bitmask:  What level did this trait originate at?
    */
    private static final int ORIGIN_LEVEL       = 0x03;

    /**
    * Bitmask:  This trait originated at this level.
    */
    private static final int ORIGIN_THIS        = 0x00;

    /**
    * Bitmask:  This trait originated at a base (modification) level.
    */
    private static final int ORIGIN_BASE        = 0x01;

    /**
    * Bitmask:  This trait originated at a super (derivation) level.
    */
    private static final int ORIGIN_SUPER       = 0x02;

    /**
    * Bitmask:  At this level, the trait exists for a "manual" reason.
    */
    private static final int ORIGIN_MANUAL      = 0x04;

    /**
    * Bitmask:  At this level, the trait's origin is implied by another
    * trait (or other traits).
    */
    private static final int ORIGIN_TRAIT       = 0x08;


    // ----- data members ---------------------------------------------------

    /**
    * A list of objects to notify with vetoable property changes.
    */
    private Listeners notifyPre = new Listeners();

    /**
    * A list of objects to notify with property changes.
    */
    private Listeners notifyPost = new Listeners();

    /**
    * A list of objects to notify with vetoable sub changes.
    */
    private Listeners notifySubPre = new Listeners();

    /**
    * A list of objects to notify with sub changes.
    */
    private Listeners notifySubPost = new Listeners();

    /**
    * The traits which contain this traits (or null if this trait is not
    * contained).
    */
    private Trait m_parent;

    /**
    * The mode of the trait:  RESOLVED, DERIVATION, or MODIFICATION.
    */
    private int m_nMode;

    /**
    * The trait's UID.
    */
    private UID m_uid = null;

    /**
    * The trait's origin.
    */
    private int m_nOrigin;

    /**
    * Other traits that are part of this trait's origin.
    */
    private StringTable m_tblOrigin;

    /**
    * The tip.
    */
    private String m_sTip = BLANK;

    /**
    * The tip from the previous extract level.
    */
    private transient String m_sPrevTip = BLANK;

    /**
    * The description.
    */
    private String m_sDesc = BLANK;

    /**
    * The description from previous levels.
    */
    private String m_sPrevDesc = BLANK;

    /**
    * The mode of the description; false adds to the description, true replaces it.
    */
    private boolean m_fReplaceDesc = false;

    /**
    * The mode of the previous description; this value is used only during resolve
    * processing.
    */
    private transient boolean m_fPrevReplaceDesc = false;

    /**
    * Until a trait is owned by another trait, it does not dispatch events.
    */
    private transient boolean m_fDispatch = false;

    /**
    * The processing state of the trait:  STATE_NEW, STATE_RESOLVING, STATE_RESOLVED,
    * or STATE_EXTRACTING.
    */
    private transient int m_nState = STATE_NEW;
    }
