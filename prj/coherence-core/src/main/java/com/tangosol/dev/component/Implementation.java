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

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ErrorList;
import com.tangosol.run.xml.XmlElement;


/**
* This class represents the implementation (e.g. scripting) of a behavior.
*
* An Implementation trait is not subject to derivation/modification in the
* manner that other traits are.  Implementations are additive within a
* Component such that, given a Component Modification M which applies to a
* Component Derivation D which derives from a base Component B, the resolved
* Component D would have the Implementations of M plus the Implementations
* of D.  (The Implementations of the base Component are not carried to the
* derived Component.  This reflects .class generation in which each Component
* results in a .class and each reachable Implementation becomes a Java method
* within the .class that results from the Component which contains the
* Implementation.)
*
* @version 0.50, 12/16/97
* @version 1.00, 08/13/98
* @author  Cameron Purdy
*/
public class Implementation
        extends Trait
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct an Implementation.  This is the "default" constructor.
    *
    * @param behavior  the behavior that has this implementation
    * @param sLang     the script language identifier
    * @param sScript   the unicode script
    */
    protected Implementation(Behavior behavior, String sLang, String sScript)
        {
        super(behavior, RESOLVED);

        if (behavior == null)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Containing behavior required.");
            }

        if (sLang == null || sLang.length() == 0)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Language identifier required.");
            }

        if (sScript == null || sScript.length() == 0)
            {
            sScript = BLANK;
            }

        m_sLang   = sLang;
        m_sScript = sScript;

        // origin is manual
        setFromManual();
        }

    /**
    * Construct a blank Implementation Trait.  Implementations are never
    * derived.
    *
    * @param base      the base Implementation to derive from
    * @param behavior  the containing behavior
    * @param nMode     one of RESOLVED, MODIFICATION
    *
    * @see #getBlankDerivedTrait(Trait, int)
    */
    protected Implementation(Implementation base, Behavior behavior, int nMode)
        {
        super(base, behavior, nMode);

        // assertion:  (remove after testing) mode is not derived
        if (nMode == DERIVATION)
            {
            throw new IllegalArgumentException(CLASS +
                    ":  Invalid mode for blank Implementation construction (" + nMode + ")");
            }

        this.m_sLang   = base.m_sLang;
        this.m_sScript = base.m_sScript;
        }

    /**
    * Copy constructor.
    *
    * @param behavior  the Behavior containing the new Implementation
    * @param that      the Implementation to copy from
    */
    protected Implementation(Behavior behavior, Implementation that)
        {
        super(behavior, that);

        this.m_sLang   = that.m_sLang;
        this.m_sScript = that.m_sScript;
        }

    /**
    * Construct an Implementation from a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param behavior  the containing behavior
    * @param stream    the stream to read this Implementation from
    * @param nVersion  version of the data structure in the stream
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            reading the Implementation from the stream or if the
    *            stream does not contain a valid Implementation
    */
    protected Implementation(Behavior behavior, DataInput stream, int nVersion)
            throws IOException
        {
        super(behavior, stream, nVersion);

        m_sLang   = readString(stream);
        m_sScript = readString(stream);
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
    protected Implementation(Trait parent, XmlElement xml, int nVersion)
            throws IOException
        {
        super(parent, xml, nVersion);

        String sLang = readString(xml.getElement("language"));
        if (sLang.length() == 0)
            {
            sLang = "java";
            }
        String sScript = readString(xml.getElement("script"));

        m_sLang   = sLang;
        m_sScript = sScript;
        }


    // ----- persistence ----------------------------------------------------

    /**
    * Save the Implementation to a stream.
    *
    * This is a custom serialization implementation that is unrelated to the
    * Serializable interface and the Java implementation of persistence.
    *
    * @param stream   the stream to write this Implementation to
    *
    * @exception IOException An IOException is thrown if an error occurs
    *            writing the Implementation to the stream
    */
    protected synchronized void save(DataOutput stream)
            throws IOException
        {
        super.save(stream);

        stream.writeUTF(m_sLang  );
        stream.writeUTF(m_sScript);
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
        super.save(xml);

        if (!equals(m_sLang, "java"))
            {
            xml.addElement("language").setString(m_sLang);
            }

        XmlElement xmlScript = xml.addElement("script");

        // add a human-readable description attribute
        Behavior  bhvr  = getBehavior();
        Component cd    = bhvr.getComponent();
        String    sDesc = cd.isGlobal() ? bhvr.getName() :
            '$' + cd.getName() + '.' + bhvr.getName();
        xmlScript.addAttribute("desc").setString(sDesc);

        xmlScript.setString(m_sScript);
        }


    // ----- derivation/modification ----------------------------------------

    /**
    * Construct a blank Implementation from this base.
    *
    * @param parent  the containing behavior
    * @param nMode   either RESOLVED or MODIFICATION
    *
    * @return a new blank derived trait of this trait's class with the
    *         specified parent and mode
    */
    protected Trait getBlankDerivedTrait(Trait parent, int nMode)
        {
        return new Implementation(this, (Behavior) parent, nMode);
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
        Implementation base    = this;
        Implementation delta   = (Implementation) resolveDelta(traitDelta, loader, errlist);
        Implementation derived = (Implementation) super.resolve(delta, parent, loader, errlist);

        // ignore the delta; implementations can be modified on at their
        // declaration level
        derived.m_sLang   = base.m_sLang;
        derived.m_sScript = base.m_sScript;

        return derived;
        }

    /**
    * Implementations are never asked to extract.
    */
    protected int getExtractMode(Trait base)
        {
        throw new UnsupportedOperationException(CLASS + ".getExtractMode:  "
                + "Implementations do not extract!");
        }

    /**
    * Implementations are never asked to extract.
    */
    protected Trait extract(Trait base, Trait parent, Loader loader, ErrorList errlist)
            throws ComponentException
        {
        throw new UnsupportedOperationException(CLASS + ".extract:  "
                + "Implementations do not extract!");
        }


    // ----- miscellaneous Trait methods ------------------------------------

    /**
    * Reset state, discarding all information.  An implementation is invalid
    * after being reset.
    */
    protected synchronized void invalidate()
        {
        super.invalidate();

        m_sLang   = null;
        m_sScript = null;
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
        // unfortunately there is no other unique identifier
        // (implementations are identified by position, which can change)
        return String.valueOf(getUID());
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
        return DESCRIPTOR + "[" + getPosition() + "] " + getUniqueName();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the behavior which contains this Implementation.
    *
    * @return the behavior trait that contains this Implementation
    */
    public Behavior getBehavior()
        {
        return (Behavior) getParentTrait();
        }

    /**
    * Determine if the Implementation is modifiable in any way.  An
    * implementation is not modifiable if it is not from this modification
    * level.
    *
    * @return true if the Implementation is modifiable, false otherwise
    */
    public boolean isModifiable()
        {
        return isDeclaredAtThisLevel() && super.isModifiable();
        }

    /**
    * Get position for this implementation.
    *
    * @return the implementation position, zero based.
    */
    public int getPosition()
        {
        Behavior bhvr = getBehavior();
        return bhvr == null ? -1 : bhvr.getImplementationPosition(this);
        }

    // ----- language

    /**
    * Access the implementation language identifier.
    *
    * @return the language identifier for the script
    */
    public String getLanguage()
        {
        return m_sLang;
        }

    /**
    * Determine if the implementation language can be set.
    *
    * @return true if the language identifier can be set
    */
    public boolean isLanguageSettable()
        {
        return isModifiable();
        }

    /**
    * Determine if the specified language is a legal implementation language
    * identifier.
    *
    * @return true if the specified language identifier is legal
    */
    public static boolean isLanguageLegal(String sLang)
        {
        return ClassHelper.isPartialNameLegal(sLang);
        }

    /**
    * Set the implementation language.
    *
    * @param sLang the new language for the implementation
    */
    public void setLanguage(String sLang)
            throws PropertyVetoException
        {
        setLanguage(sLang, true);
        }

    /**
    * Set the implementation language.
    *
    * @param sLang      the new language for the implementation
    * @param fVetoable  true if the setter can veto the value
    */
    protected synchronized void setLanguage(String sLang, boolean fVetoable)
            throws PropertyVetoException
        {
        String sPrev = m_sLang;

        if (sLang.equals(sPrev))
            {
            return;
            }

        if (fVetoable)
            {
            if (!isLanguageSettable())
                {
                readOnlyAttribute(ATTR_LANG, sPrev, sLang);
                }

            if (!isLanguageLegal(sLang))
                {
                illegalAttributeValue(ATTR_LANG, sPrev, sLang);
                }

            fireVetoableChange(ATTR_LANG, sPrev, sLang);
            }

        m_sLang = sLang;
        firePropertyChange(ATTR_LANG, sPrev, sLang);
        }


    // ----- script

    /**
    * Access the implementation script.
    *
    * @return the script
    */
    public String getScript()
        {
        return m_sScript;
        }

    /**
    * Determine if the implementation script can be set.
    *
    * @return true if the script can be set
    */
    public boolean isScriptSettable()
        {
        return isModifiable();
        }

    /**
    * Determine if the specified script is a legal script.  This does not
    * check syntax.  Currently, all scripts are considered legal.
    *
    * @return true if the script is legal
    */
    public static boolean isScriptLegal(String sScript)
        {
        return sScript != null;
        }

    /**
    * Set the implementation script.
    *
    * @param sScript the new script for the implementation
    */
    public void setScript(String sScript)
            throws PropertyVetoException
        {
        setScript(sScript, true);
        }

    /**
    * Set the implementation script.
    *
    * @param sScript    the new script for the implementation
    * @param fVetoable  true if the setter can veto the value
    */
    protected synchronized void setScript(String sScript, boolean fVetoable)
            throws PropertyVetoException
        {
        String sPrev = m_sScript;

        if (sScript.equals(sPrev))
            {
            return;
            }

        if (fVetoable)
            {
            if (!isScriptSettable())
                {
                readOnlyAttribute(ATTR_SCRIPT, sPrev, sScript);
                }

            if (!isScriptLegal(sScript))
                {
                illegalAttributeValue(ATTR_SCRIPT, sPrev, sScript);
                }

            fireVetoableChange(ATTR_SCRIPT, sPrev, sScript);
            }

        m_sScript = sScript;
        firePropertyChange(ATTR_SCRIPT, sPrev, sScript);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare this Implementation to another Object for equality.
    *
    * @param obj  the other Object to compare to this
    *
    * @return true if this Implementation equals that Object
    */
    public boolean equals(Object obj)
        {
        if (obj instanceof Implementation)
            {
            Implementation that = (Implementation) obj;
            return this == that
                || this.m_sLang  .equals(that.m_sLang  )
                && this.m_sScript.equals(that.m_sScript)
                && super.equals(that);
            }
        return false;
        }

    /**
    * Provide a human-readable description of the Implementation.
    *
    * @return a string describing the Implementation
    */
    public String toString()
        {
        return getUniqueDescription() + " (" + m_sLang + ")";
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
        out.print(sIndent + "Implementation language=" + m_sLang + ", script=");

        if (m_sScript == null)
            {
            out.println("<null>");
            }
        else if (m_sScript == BLANK)
            {
            out.println("<none>");
            }
        else
            {
            String sIndentScript = nextIndent(sIndent);
            out.println();
            out.println(sIndentScript + "{");
            out.println(indentString(m_sScript, sIndentScript));
            out.println(sIndentScript + "}");
            }

        super.dump(out, sIndent);
        }


    // ----- public constants  ----------------------------------------------

    /**
    * The Language attribute.
    */
    public static final String ATTR_LANG = "Language";

    /**
    * The Script attribute.
    */
    public static final String ATTR_SCRIPT = "Script";


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Implementation";

    /**
    * The Implementation's descriptor string.
    */
    protected static final String DESCRIPTOR = "Script";

    /**
    * The Implementation's scripting language identifier.
    */
    private String m_sLang;

    /**
    * The Implementation's script.
    */
    private String m_sScript;
    }
