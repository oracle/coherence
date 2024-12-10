/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.util.Set;
import java.util.Enumeration;
import java.util.HashSet;

import com.tangosol.util.NullImplementation;
import com.tangosol.util.SimpleEnumerator;


/**
* The LABEL op is a target of a branching instruction, switch case, or
* exception catch.  A label can also be referred to as a code context in
* the following situations:
* <p>
* <ol>
* <li>The label is the first op in the code (it is the "main" context)
* <li>The label is a target of a JSR op (it is a subroutine context)
* </ol>
* <p><code><pre>
* JASM op         :  LABEL       (0xfb)
* JVM byte code(s):  n/a
* Details         :
* </pre></code>
*
* @version 0.50, 06/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Label extends Op implements Constants, Comparable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    */
    public Label(String sName)
        {
        super(LABEL);
        m_sName = sName;
        }

    /**
    * Construct the op.
    */
    public Label()
        {
        super(LABEL);
        }


    // ----- Op operations --------------------------------------------------

    /**
    * Determine if the op is discardable.  Label ops, like Begin/End, are
    * never considered discardable since they carry additional required
    * information and since they do not affect execution.
    *
    * @return false always
    */
    protected boolean isDiscardable()
        {
        return false;
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toString()
        {
        return format(format(), null, null);
        }

    /**
    * Produce a human-readable string describing the byte code.
    *
    * @return a string describing the byte code
    */
    public String toJasm()
        {
        return null;
        }

    /**
    * Produce a label name.
    *
    * @return the label name
    */
    public String format()
        {
        String sName = m_sName;

        // hash code should be unique enough
        if (sName == null)
            {
            sName = String.valueOf(hashCode());
            }

        return sName;
        }


    // ----- Comparable operations ------------------------------------------

    /**
    * Compares this Object with the specified Object for order.  Returns a
    * negative integer, zero, or a positive integer as this Object is less
    * than, equal to, or greater than the given Object.
    *
    * @param   obj the <code>Object</code> to be compared.
    *
    * @return  a negative integer, zero, or a positive integer as this Object
    *          is less than, equal to, or greater than the given Object.
    *
    * @exception ClassCastException the specified Object's type prevents it
    *            from being compared to this Object.
    */
    public int compareTo(Object obj)
        {
        // order by depth
        Label that = (Label) obj;

        int nThis = this.getDepth();
        int nThat = that.getDepth();

        return (nThis < nThat ? -1 : (nThis > nThat ? +1 : 0));
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine if the label is the start of a subroutine.
    *
    * @return true if a reachable JSR op specifies this label as a target
    */
    protected boolean isSubroutine()
        {
        return m_fSub;
        }

    /**
    * Determine the subroutine depth.
    *
    * @return the maximum depth in the subroutine call chain that this
    *         subroutine could be found
    */
    protected int getDepth()
        {
        int iDepth = m_iDepth;

        if (iDepth == UNKNOWN)
            {
            iDepth = 0;

            // determine the depth by asking each of the JSR's how deep this
            // subroutine is
            for (Enumeration enmr = getCallers(); enmr.hasMoreElements(); )
                {
                Jsr jsr = (Jsr) enmr.nextElement();
                int iNewDepth = jsr.getDepth();
                if (iNewDepth > iDepth)
                    {
                    iDepth = iNewDepth;
                    }
                }

            m_iDepth = iDepth;
            }

        return iDepth;
        }


    // ----- subroutine:  terminating RET op

    /**
    * Get the RET op which returns from this subroutine.  This method is
    * used only during assembly.
    *
    * @return the RET op which returns from this subroutine, or null if this
    *         label is not the entry point of a subroutine or if the RET op
    *         for the subroutine is not reachable
    */
    protected Ret getRet()
        {
        return m_ret;
        }

    /**
    * Set the RET op which returns from this subroutine.  This method is used
    * only during assembly.
    *
    * @param ret  the RET op which returns from this subroutine
    */
    protected void setRet(Ret ret)
        {
        if (!m_fSub)
            {
            throw new IllegalStateException(CLASS + ".setRet:  " +
                    "RET without JSR!");
            }
        else if (m_ret == null)
            {
            m_ret = ret;
            }
        else if (m_ret != ret)
            {
            throw new IllegalStateException(CLASS + ".setRet:  " +
                    "It is illegal to have multiple RET instructions for a JSR!");
            }
        }


    // ----- subroutine:  stack height when RET op encountered

    /**
    * Determine the stack height when the terminating RET op encountered.
    *
    * @return the height when the JSR op continues to the op following it
    */
    protected int getRetHeight()
        {
        return m_cwRetHeight;
        }

    /**
    * Specify the current stack height when the terminating RET op for the
    * subroutine is encountered.
    *
    * @param cw  the height of the stack being returned to the JSR op
    */
    protected void setRetHeight(int cw)
        {
        m_cwRetHeight = cw;
        }


    // ----- callers (JSR ops that call this label)

    /**
    * Specify that the label is the start of a subroutine invoked by the
    * specified JSR op.
    *
    * @param jsr  a reachable op that invokes this subroutine
    */
    protected void addCaller(Jsr jsr)
        {
        if (!m_fSub)
            {
            m_fSub       = true;
            m_setCallers = new HashSet();
            }

        m_setCallers.add(jsr);
        m_iDepth = UNKNOWN;
        }

    /**
    * Enumeration the JSR ops which call this subroutine.
    *
    * @return an enumeration of JSR ops which specify this label
    */
    protected Enumeration getCallers()
        {
        Set set = m_setCallers;
        if (set == null)
            {
            return NullImplementation.getEnumeration();
            }
        else
            {
            return new SimpleEnumerator(set.toArray());
            }
        }


    // ----- for identifying code contexts

    /**
    * Get the code context's index.
    *
    * @return the index assigned to the code context
    */
    protected int getContextIndex()
        {
        return m_iCtx;
        }

    /**
    * Set the code context's index.
    *
    * @param iCtx  the index assigned to the code context
    */
    protected void setContextIndex(int iCtx)
        {
        m_iCtx = iCtx;
        }


    // ----- for variable allocation

    /**
    * Get the first available variable slot.
    *
    * @return the first available variable slot
    */
    protected int getFirstSlot()
        {
        return m_iSlot;
        }

    /**
    * Set the first available variable slot.
    *
    * @param iSlot  the first available variable slot
    */
    protected void setFirstSlot(int iSlot)
        {
        m_iSlot = iSlot;
        }


    // ----- code context

    /**
    * Determine the code context of the label.
    *
    * @return the label that starts the code context containing this label
    */
    protected Label getContext()
        {
        return m_labelContext;
        }

    /**
    * Specify the code context for the label.
    *
    * @param label  the label that starts the code context containing this
    *               label
    */
    protected void setContext(Label label)
        {
        m_labelContext = label;
        }


    // ----- for definite assignment

    /**
    * Internal use by assembler.  For all labels.
    */
    protected HashSet getVariables()
        {
        return m_setVars;
        }

    /**
    * Internal use by assembler.  For all labels.
    */
    protected void setVariables(HashSet setVars)
        {
        m_setVars = setVars;
        }

    /**
    * Determine if the label has been visited by the definite assignment
    * pass.
    *
    * @return true if visited
    */
    protected boolean isVisited()
        {
        return m_fVisited;
        }

    /**
    * Specify that the label has been visited by the definite assignment
    * pass.
    *
    * @param fVisited  true if visited
    */
    protected void setVisited(boolean fVisited)
        {
        m_fVisited = fVisited;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Label";

    /**
    * For all labels, the optional name of the label.
    */
    private String m_sName;

    /**
    * For all labels, the code context which this label is reachable within.
    */
    private Label m_labelContext;

    /**
    * For all labels, what variables are currently in scope and (for definite
    * assignment determination) which variables do and do not have a value.
    */
    private HashSet m_setVars;

    /**
    * For all labels, has this label been visited by the definite assignment
    * determination pass?
    */
    private boolean m_fVisited;

    /**
    * For a code context (a label which starts either the main code context
    * or a label which is the entry point of a subroutine), this value is an
    * arbitrary index assigned by the assembler to differentiate contexts.
    */
    private int m_iCtx = UNKNOWN;

    /**
    * For a code context (a label which starts either the main code context
    * or a label which is the entry point of a subroutine), this value is
    * the first absolute register slot that will be assigned to a variable.
    */
    private int m_iSlot = UNKNOWN;

    /**
    * Is this label the entry point of a subroutine?
    */
    private boolean m_fSub;

    /**
    * If this label is the entry point of a subroutine, how deep is the
    * subroutine?
    */
    private int m_iDepth = UNKNOWN;

    /**
    * If this label is the entry point of a subroutine, this is the RET
    * instruction that returns from the subroutine or null if no RET
    * instruction is reachable.
    */
    private Ret m_ret;

    /**
    * If this label is the entry point of a subroutine, this is the height
    * of the stack when the RET is encountered.
    */
    private int m_cwRetHeight = UNKNOWN;

    /**
    * If this label is the entry point of a subroutine, this is the set of
    * reachable JSR ops which invoke this subroutine.
    */
    private Set m_setCallers;
    }
