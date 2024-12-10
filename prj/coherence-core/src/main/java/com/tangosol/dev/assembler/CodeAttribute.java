/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import com.tangosol.util.StringTable;


/**
* Represents a Java Virtual Machine "code" attribute which contains Java
* Byte Codes.
* <p>
* The Code Attribute is defined by the Java Virtual Machine Specification as
* follows:
* <p>
* <code><pre>
*   Code_attribute
*       {
*       u2 attribute_name_index;
*       u4 attribute_length;
*       u2 max_stack;
*       u2 max_locals;
*       u4 code_length;
*       u1 code[code_length];
*       u2 exception_table_length;
*           {
*           u2 start_pc;
*           u2 end_pc;
*           u2  handler_pc;
*           u2  catch_type;
*           } exception_table[exception_table_length];
*       u2 attributes_count;
*       attribute_info attributes[attributes_count];
*       }
* </pre></code>
*
* @version 0.50, 05/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class CodeAttribute extends Attribute implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a code attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected CodeAttribute(VMStructure context)
        {
        super(context, ATTR_CODE);
        }


    // ----- VMStructure operations -----------------------------------------

    /**
    * The disassembly process reads the structure from the passed input
    * stream and uses the constant pool to dereference any constant
    * references.
    *
    * @param stream  the stream implementing java.io.DataInput from which
    *                to read the assembled VM structure
    * @param pool    the constant pool for the class which contains any
    *                constants referenced by this VM structure
    */
    protected void disassemble(DataInput stream, ConstantPool pool)
            throws IOException
        {
        stream.readInt();

        // read the information related to the method's frame size;
        // this includes the number of local variable slots and the
        // maximum stack size
        m_cwStack = stream.readUnsignedShort();
        m_cVars   = stream.readUnsignedShort();

        // read the Java byte codes
        int cbCode = stream.readInt();
        if (cbCode <= 0 || cbCode > 0xFFFF)
            {
            throw new IllegalStateException("Method code attribute is restricted "
                    + "to the range 0-65535 bytes");
            }

        m_abCode = new byte[cbCode];
        stream.readFully(m_abCode);

        // store the constant pool for later reference (when the byte codes
        // are disassembled)
        m_pool = pool;

        // read the exception table; each entry in the exception table
        // specifies a range of byte codes which are guarded, the exception
        // being guarded for, and the byte code to transfer execution to if
        // that exception occurs
        m_vectCatch.clear();
        int cExcept = stream.readUnsignedShort();
        for (int i = 0; i < cExcept; ++i)
            {
            GuardedSection section = new GuardedSection();
            section.disassemble(stream, pool);
            m_vectCatch.add(section);
            }

        // the Code attribute also contains attributes; there are only two
        // expected (aka supported by JVM) attributes:  LineNumberTable and
        // LocalVariableTable
        m_tblAttribute.clear();
        int cAttr = stream.readUnsignedShort();
        for (int i = 0; i < cAttr; ++i)
            {
            Attribute attribute = Attribute.loadAttribute(this, stream, pool);
            m_tblAttribute.put(attribute.getIdentity(), attribute);
            }

        m_nState = LOADED;
        }

    /**
    * The pre-assembly step collects the necessary entries for the constant
    * pool.  During this step, all constants used by this VM structure and
    * any sub-structures are registered with (but not yet bound by position
    * in) the constant pool.
    *
    * @param pool  the constant pool for the class which needs to be
    *              populated with the constants required to build this
    *              VM structure
    */
    protected void preassemble(ConstantPool pool)
        {
        pool.registerConstant(super.getNameConstant());

        // byte code
        ensureOps();
        preassembleOps(pool);

        // guarded sections
        for (Enumeration enmr = m_vectCatch.elements(); enmr.hasMoreElements(); )
            {
            ((GuardedSection) enmr.nextElement()).preassemble(pool);
            }

        // sub-attributes
        for (Enumeration enmr = m_tblAttribute.elements(); enmr.hasMoreElements(); )
            {
            ((Attribute) enmr.nextElement()).preassemble(pool);
            }
        }

    /**
    * The assembly process assembles and writes the structure to the passed
    * output stream, resolving any dependencies using the passed constant
    * pool.
    *
    * @param stream  the stream implementing java.io.DataOutput to which to
    *                write the assembled VM structure
    * @param pool    the constant pool for the class which by this point
    *                contains the entire set of constants required to build
    *                this VM structure
    */
    protected void assemble(DataOutput stream, ConstantPool pool)
            throws IOException
        {
        // assemble the ops into byte codes; this also sets up the guarded
        // section information, frame (stack size/variable count)
        // information, and the default sub-attributes
        assembleOps(pool);

        // assemble the sub-attributes
        byte[] abAttr = NO_BYTES;
        int    cAttrs = m_tblAttribute.getSize();
        if (cAttrs > 0)
            {
            ByteArrayOutputStream streamRaw  = new ByteArrayOutputStream();
            DataOutput            streamAttr = new DataOutputStream(streamRaw);
            for (Enumeration enmr = m_tblAttribute.elements(); enmr.hasMoreElements(); )
                {
                ((Attribute) enmr.nextElement()).assemble(streamAttr, pool);
                }
            abAttr = streamRaw.toByteArray();
            }

        // header (name of attribute, length of attribute)
        stream.writeShort(pool.findConstant(super.getNameConstant()));
        stream.writeInt(2                         // max stack
                      + 2                         // max locals
                      + 4                         // code length
                      + m_abCode.length           // code
                      + 2                         // guarded section count
                      + 8 * m_vectCatch.size()    // guarded sections
                      + 2                         // attribute count
                      + abAttr.length);           // attributes

        // body
        stream.writeShort(m_cwStack);
        stream.writeShort(m_cVars);
        stream.writeInt(m_abCode.length);
        stream.write(m_abCode);
        stream.writeShort(m_vectCatch.size());
        for (Enumeration enmr = m_vectCatch.elements(); enmr.hasMoreElements(); )
            {
            ((GuardedSection) enmr.nextElement()).assemble(stream, pool);
            }
        stream.writeShort(cAttrs);
        stream.write(abAttr);
        }

    /**
    * Determine if the attribute has been modified.
    *
    * @return true if the attribute has been modified
    */
    public boolean isModified()
        {
        if (m_fModified)
            {
            return true;
            }

        // attributes
        Enumeration enmr = m_tblAttribute.elements();
        while (enmr.hasMoreElements())
            {
            Attribute attr = (Attribute) enmr.nextElement();
            if (attr.isModified())
                {
                return true;
                }
            }

        return false;
        }

    /**
    * Reset the modified state of the VM structure.
    *
    * This method must be overridden by sub-classes which do not maintain
    * the attribute as binary.
    */
    protected void resetModified()
        {
        m_fModified = false;

        // attributes
        Enumeration enmr = m_tblAttribute.elements();
        while (enmr.hasMoreElements())
            {
            ((Attribute) enmr.nextElement()).resetModified();
            }
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the attribute.
    *
    * @return a string describing the attribute
    */
    public String toString()
        {
        return "Code attribute for " + getContext().toString();
        }


    // ----- internal state management --------------------------------------

    /**
    * Make sure that the ops are disassembled if the code is from a
    * disassembled class.
    */
    protected void ensureOps()
        {
        if (m_nState == LOADED || m_nState == ASSEMBLED)
            {
            disassembleOps();
            }
        }

    /**
    * Disassemble the byte code into JASM ops.  This is done the first time
    * that an operation requires the byte code to be disassembled.
    */
    protected void disassembleOps()
        {
        // assertion
        if (m_abCode == null || m_opFirst != null)
            {
            throw new IllegalStateException(CLASS +
                    ".disassembleOps:  No code exists or ops already exist!");
            }

        // at this point, all the inputs exist to disassemble the code
        // m_abCode       - the byte code
        // m_cVars        - number of local variables (actually, the number
        //                  of words)
        // m_vectCatch    - guarded sections, which become try/catch ops
        // m_tblAttribute - the attributes table, which may contain the line
        //                  number and local variable information
        // m_pool         - the constant pool
        try
            {
            // get a linked list of ops
            Method method = (Method) getContext();

            // build an array of data types for the parameters
            String[] asTypes = method.getTypes();
            if (method.isStatic())
                {
                // return param is in element 0; remove it
                int      cNew  = asTypes.length - 1;
                String[] asNew = new String[cNew];
                System.arraycopy(asTypes, 1, asNew, 0, cNew);
                asTypes = asNew;
                }
            else
                {
                // return param is in element 0; replace it with "this"
                asTypes[0] = 'L' + method.getClassName() + ';';
                }

            Op[] aopBounds = new Op[2];

            Op.disassembleOps(
                    m_abCode,
                    m_cVars,
                    asTypes,
                    m_vectCatch,
                    (LineNumberTableAttribute)        m_tblAttribute.get(ATTR_LINENUMBERS),
                    (LocalVariableTableAttribute)     m_tblAttribute.get(ATTR_VARIABLES  ),
                    (LocalVariableTypeTableAttribute) m_tblAttribute.get(ATTR_VARIABLETYPES),
                    m_pool,
                    aopBounds);

            m_opFirst = aopBounds[0];
            m_opLast  = aopBounds[1];
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }

        m_abCode = null;
        m_nState = DISASSEMBLED;
        }

    /**
    * Make a quick pass at the JASM ops to remove dead code and register
    * any constants.
    *
    * @param pool  the constant pool for the class
    */
    protected void preassembleOps(ConstantPool pool)
        {
        // assert:  expected state (unassembled) and something to assemble
        if (!((m_nState == ADDED || m_nState == DISASSEMBLED) && m_opFirst != null))
            {
            throw new IllegalStateException(CLASS + ".preassembleOps:  Illegal state (" +
                    m_nState + ") or no ops to assemble!");
            }

        // always start the code with a label; this makes it easier to
        // enforce type safety since all non-linear execution (including
        // the initial method invocation) will start with a LABEL op
        if (!(m_opFirst instanceof Label))
            {
            Label label = new Label();
            label.setNext(m_opFirst);
            m_opFirst = label;
            }

        // collect all code contexts starting with the main context
        Label   labelMain   = (Label) m_opFirst;
        HashSet setContexts = new HashSet();
        setContexts.add(labelMain);

        // calculate the maximum stack size (also marks dead code, etc.)
        int cwStack = checkStack(labelMain, 0, 0, labelMain, setContexts, 0);

        // assign an integer index to each context
        int cContexts = 0;
        for (Iterator iter = setContexts.iterator(); iter.hasNext(); )
            {
            ((Label) iter.next()).setContextIndex(cContexts++);
            }

        // attributes of the code attribute which support debugging
        LineNumberTableAttribute    attrLines = new LineNumberTableAttribute(this);
        LocalVariableTableAttribute attrVars  = null;
        if (m_nState == DISASSEMBLED)
            {
            // use the disassembled local variable table (it is close
            // to impossible to reconstruct scope etc. from assembled
            // code so assume the original information was correct)
            attrVars = (LocalVariableTableAttribute) m_tblAttribute.get(ATTR_VARIABLES);
            }
        if (attrVars == null)
            {
            attrVars = new LocalVariableTableAttribute(this);
            }

        // iterate through the ops, removing dead code, registering
        // constants, matching up BEGINs/ENDs (scope) and associating scopes
        // with variable declarations, assigning register (variable) offsets,
        // and preparing for the definite assignment determination
        Begin     begin     = null;                 // current scope
        HashSet   setVars   = new HashSet();        // currently in-scope variables
        boolean   fDebugInfo= false;                // set to true if any debug info
        Vector    vectVar   = new Vector();         // all variable declaration ops
        int[]     aiVar     = new int[cContexts];   // variable pool (by code context)
        int       cMaxSlots = 0;                    // number of variable slots
        int       iPrevLine = 0;                    // line number of the previous op
        Vector    vectCatch = new Vector();         // guarded session information
pass:   for (Op op = m_opFirst, opPrev = null; op != null; op = (opPrev = op).getNext())
            {
            // remove unreachable ops (dead code)
            if (op.isDiscardable())
                {
                do
                    {
                    op = op.getNext();
                    if (op == null)
                        {
                        opPrev.setNext(null);
                        break pass;
                        }
                    }
                while (op.isDiscardable());

                // fix the linked list, removing all dead code
                opPrev.setNext(op);
                }

            // specific handling for certain ops
            int iOp = op.getValue();
            switch (iOp)
                {
                case BEGIN:
                    {
                    Begin beginNew = (Begin) op;

                    // store the current variable pool state with the scope
                    beginNew.setVariablePool((int[]) aiVar.clone());

                    // push this begin onto the scope stack
                    beginNew.setOuterScope(begin);

                    // use the new begin as the start of the current scope
                    begin = beginNew;
                    }
                    break;

                case END:
                    {
                    End end = (End) op;

                    // END must have a matching BEGIN
                    if (begin == null)
                        {
                        throw new IllegalStateException(CLASS + ".preassembleOps:  " +
                                "END without BEGIN!");
                        }
                    begin.setEnd(end);

                    // update currently declared variable list
                    // (used later by definite assignment)
                    for (Enumeration enmr = begin.getDeclarations(); enmr.hasMoreElements(); )
                        {
                        OpDeclare decl = (OpDeclare) enmr.nextElement();
                        setVars.remove(decl);
                        }

                    // restore the variable pool
                    aiVar = begin.getVariablePool();

                    // pop a begin off of the scope stack
                    begin = begin.getOuterScope();
                    }
                    break;

                case IVAR:
                case LVAR:
                case FVAR:
                case DVAR:
                case AVAR:
                case RVAR:
                    {
                    OpDeclare decl = (OpDeclare) op;

                    // must have begin
                    if (begin == null)
                        {
                        throw new IllegalStateException(CLASS + ".preassembleOps:  " +
                                "Declaration (?VAR) without BEGIN!");
                        }

                    // add the local variable to the list of all variables
                    // which do not have an absolute register index
                    vectVar.addElement(decl);

                    // add the local variable to the scope
                    begin.addDeclaration(decl);

                    // inform the variable of its scope
                    decl.setBegin(begin);

                    // update currently declared variable list
                    // (used later by definite assignment)
                    if (decl.hasDebugInfo())
                        {
                        fDebugInfo = true;
                        setVars.add(decl);
                        }

                    int iVar = decl.getSlot();
                    if (iVar == UNKNOWN)
                        {
                        // assign a register offset (variable slot offset)
                        // note:  this is not the actual variable slot index
                        // unless the context is the main context;
                        // the index is calculated later based on the number
                        // of variable slots already used when the variable
                        // declaration's code context is entered
                        int iCtx = decl.getContext().getContextIndex();
                        iVar = aiVar[iCtx];
                        decl.setSlot(iVar);
                        aiVar[iCtx] = iVar + decl.getWidth();
                        }
                    else
                        {
                        int cSlots = iVar + decl.getWidth();
                        if (cSlots > cMaxSlots)
                            {
                            cMaxSlots = cSlots;
                            }
                        }
                    }
                    break;

                case ILOAD:
                case LLOAD:
                case FLOAD:
                case DLOAD:
                case ALOAD:
                case ISTORE:
                case LSTORE:
                case FSTORE:
                case DSTORE:
                case ASTORE:
                case RSTORE:
                case IINC:
                case RET:
                    {
                    OpDeclare decl = ((OpVariable)op).getVariable();

                    // if begin is unknown then no var declaration
                    // if end op is known then var is out of scope
                    if (decl.getBegin() == null || decl.getEnd() != null)
                        {
                        throw new IllegalStateException(CLASS + ".preassembleOps:  " +
                                "Variable used out of scope! " + op);
                        }
                    }
                    break;

                case JSR:
                    {
                    Jsr jsr = (Jsr) op;

                    // store the next available variable number for the
                    // context containing the JSR op (to help determine
                    // the variable number base for the subroutine context
                    // which is branched to by the JSR op)
                    int iCtx = jsr.getContext().getContextIndex();
                    jsr.setFirstSlot(aiVar[iCtx]);
                    }
                    break;

                case LABEL:
                    {
                    Label label = (Label) op;

                    // register all currently in-scope variables with the
                    // label; this information is used during the definite
                    // assignment check
                    label.setVariables((HashSet) setVars.clone());
                    }
                    break;

                case CATCH:
                    {
                    // the guarded sections are built in the order that the Catch
                    // ops are encounted; this reflects Java language compilation
                    Catch          opCatch = (Catch) op;
                    Try            opTry   = opCatch.getTry();
                    ClassConstant  clz     = opCatch.getExceptionClass();
                    Label          label   = opCatch.getLabel();
                    GuardedSection section = new GuardedSection(opTry, opCatch, clz, label);
                    vectCatch.addElement(section);
                    }
                    break;
                }

            // build line number information
            int iCurLine = op.getLine();
            if (iCurLine != iPrevLine && iCurLine > 0 && op.hasSize())
                {
                attrLines.add(op);
                iPrevLine = iCurLine;
                }

            // register any constants
            op.preassemble(pool);
            }

        // assert:  verify that all begins were matched with ends
        if (begin != null)
            {
            throw new IllegalStateException(CLASS + ".preassembleOps:  " +
                    "BEGIN without END!");
            }

        // assign variable slots (unless they were pre-assigned, e.g. the ops
        // were disassembled and therefore already had variable slot numbers)
        if (cMaxSlots == 0)
            {
            // organize contexts by depth
            Label[] alabelContext = (Label[]) setContexts.toArray(LABEL_ARRAY);
            Arrays.sort(alabelContext);

            // assert:  number of contexts hasn't changed
            if (alabelContext.length != cContexts)
                {
                throw new IllegalStateException(CLASS + ".preassembleOps:  " +
                        "Unexpected number of contexts!");
                }

            // assert:  first context is main (depth 0) and if second context
            // exists, it is depth >0
            if (alabelContext[0].getDepth() != 0 ||
                    cContexts > 1 && alabelContext[1].getDepth() != 1)
                {
                throw new IllegalStateException(CLASS + ".preassembleOps:  " +
                        "Unexpected depths of contexts!");
                }

            // determine register base for each non-main context
            for (int i = 0; i < cContexts; ++i)
                {
                Label labelContext = alabelContext[i];
                int iFirstSlot = 0;
                for (Enumeration enmr = labelContext.getCallers(); enmr.hasMoreElements(); )
                    {
                    Jsr jsr   = (Jsr) enmr.nextElement();
                    int iSlot = jsr.getContext().getFirstSlot() + jsr.getFirstSlot();
                    if (iSlot > iFirstSlot)
                        {
                        iFirstSlot = iSlot;
                        }
                    }
                labelContext.setFirstSlot(iFirstSlot);
                }

            // assign absolute register slots to variables
            // determine number of local variables ("slots")
            for (Enumeration enmr = vectVar.elements(); enmr.hasMoreElements(); )
                {
                OpDeclare decl         = (OpDeclare) enmr.nextElement();
                int       iSlotOffset  = decl.getSlot();
                Label     labelContext = decl.getContext();

                int iSlot  = labelContext.getFirstSlot() + iSlotOffset;
                int cSlots = iSlot + decl.getWidth();

                decl.setSlot(iSlot);
                if (cSlots > cMaxSlots)
                    {
                    cMaxSlots = cSlots;
                    }
                }

            if (fDebugInfo)
                {
                // definite assignment:  build the local variable table
                //  1)  determine the set of automatically-assigned variables
                //      (the parameters to the method)
                Method   method   = (Method) getContext();
                String[] asType   = method.getTypes();
                int      cTypes   = asType.length;
                int      cwParams = (method.isStatic() ? 0 : 1);
                for (int i = 0; i < cTypes; ++i)
                    {
                    char chType = asType[i].charAt(0);
                    cwParams += OpDeclare.getJavaWidth(chType);
                    }

                //  2)  determine what variables are assigned at each label
                //      (checkAssignment)
                checkAssignment(labelMain, setVars, cwParams);

                //  3)  iterate through all ops, building a map keyed by
                //      variable with the value being the op where the
                //      variable became definitely assigned (either a label
                //      or a ?STORE op)
                //  4)  when a variable located in the map is un-assigned
                //      either by a label which does not contain that
                //      variable in its assigned list or by an END op which
                //      ends the scope of the variable, add a new range
                //      to the local variable table
                HashMap mapVars = new HashMap();
                for (Op op = m_opFirst; op != null; op = op.getNext())
                    {
                    switch (op.getValue())
                        {
                        case IVAR:
                        case LVAR:
                        case FVAR:
                        case DVAR:
                        case AVAR:
                        case RVAR:
                            {
                            OpDeclare decl = (OpDeclare) op;

                            // parameters are definitely assigned
                            if (decl.hasDebugInfo()
                                    && decl.getSlot() < cwParams
                                    && !mapVars.containsKey(decl))
                                {
                                mapVars.put(decl, decl);
                                }
                            }
                            break;

                        case ISTORE:
                        case LSTORE:
                        case FSTORE:
                        case DSTORE:
                        case ASTORE:
                        case RSTORE:
                            {
                            OpStore   stor = (OpStore) op;
                            OpDeclare decl = stor.getVariable();

                            // the ?STORE ops definitely assign a variable
                            if (decl.hasDebugInfo() && !mapVars.containsKey(decl))
                                {
                                mapVars.put(decl, stor);
                                }
                            }
                            break;

                        case LABEL:
                            {
                            Label   label        = (Label) op;
                            HashSet setLabelVars = label.getVariables();

                            // a LABEL can unassign variables which are in
                            // the current map but which are not in its set
                            Iterator iterator = mapVars.entrySet().iterator();
                            while (iterator.hasNext())
                                {
                                Map.Entry entry = (Map.Entry) iterator.next();
                                OpDeclare decl  = (OpDeclare) entry.getKey();
                                if (!setLabelVars.contains(decl))
                                    {
                                    Op opInit = (Op) entry.getValue();
                                    attrVars.add(decl, opInit, label);
                                    iterator.remove();
                                    }
                                }

                            // a LABEL can assign variables which are in its
                            // set of definitely assigned variables but which
                            // are not in the current map
                            if (setLabelVars.size() != mapVars.size())
                                {
                                iterator = setLabelVars.iterator();
                                while (iterator.hasNext())
                                    {
                                    OpDeclare decl = (OpDeclare) iterator.next();
                                    if (!mapVars.containsKey(decl))
                                        {
                                        mapVars.put(decl, label);
                                        }
                                    }
                                }
                            }
                            break;

                        case END:
                            {
                            End end = (End) op;

                            // an END op unassigns all variables that go out
                            // of scope
                            Iterator iterator = mapVars.entrySet().iterator();
                            while (iterator.hasNext())
                                {
                                Map.Entry entry = (Map.Entry) iterator.next();
                                OpDeclare decl  = (OpDeclare) entry.getKey();
                                if (decl.getEnd() == end)
                                    {
                                    Op opInit = (Op) entry.getValue();
                                    attrVars.add(decl, opInit, end);
                                    iterator.remove();
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }

        // store the frame size (register count and stack height)
        m_cVars   = cMaxSlots;
        m_cwStack = cwStack;

        // store the guarded section information
        m_vectCatch = vectCatch;

        // store the line number table
        if (attrLines.isEmpty())
            {
            m_tblAttribute.remove(attrLines.getIdentity());
            }
        else
            {
            m_tblAttribute.put(attrLines.getIdentity(), attrLines);
            }

        // store the local variable table (but only if there is any
        // line number information)
        if (attrLines.isEmpty() || attrVars.isEmpty())
            {
            m_tblAttribute.remove(attrVars.getIdentity());
            }
        else
            {
            m_tblAttribute.put(attrVars.getIdentity(), attrVars);
            }
        }

    /**
    * Assemble the JASM ops into byte code.  This is done when the attribute
    * is asked to assemble.
    *
    * @param pool    the constant pool for the class which contains any
    *                constants referenced by the byte code
    */
    protected void assembleOps(ConstantPool pool)
            throws IOException
        {
        // assert:  expected state (unassembled) and something to assemble
        if (!((m_nState == ADDED || m_nState == DISASSEMBLED) && m_opFirst != null))
            {
            throw new IllegalStateException(CLASS + ".assembleOps:  Illegal state (" +
                    m_nState + ") or no ops to assemble!");
            }

        // assign PC's (byte code offsets) to each op
        int of = 0;
        for (Op op = m_opFirst; op != null; op = op.getNext())
            {
            op.setOffset(of);
            op.calculateSize(pool);
            of += op.getSize();
            }

        // assemble the byte code
        ByteArrayOutputStream streamRaw = new ByteArrayOutputStream();
        DataOutput            stream    = new DataOutputStream(streamRaw);
        for (Op op = m_opFirst; op != null; op = op.getNext())
            {
            op.assemble(stream, pool);
            }

        // store the byte code
        m_abCode = streamRaw.toByteArray();
        m_nState = ASSEMBLED;
        }

    /**
    * Walk the code as the JVM would, following all possible branches, and
    * verify that the stack is maintained properly by the code.
    *
    * @param labelFirst    the label to start checking the stack from
    * @param cwCurStack    the size of the stack immediately before opFirst
    * @param cwMaxStack    the maximum size of the stack encountered so far
    * @param labelContext  the label starting the code (either main or sub)
    * @param setContexts   the set of all reachable code contexts
    * @param iSubDepth     the current depth in the subroutine call stack
    *                      (0==main)
    *
    * @return  the maximum stack size encountered
    */
    protected int checkStack(Label labelFirst, int cwCurStack, int cwMaxStack, Label labelContext, Set setContexts, int iSubDepth)
        {
        // check for a new max stack (e.g. from JSR pushing the return address)
        if (cwCurStack > cwMaxStack)
            {
            cwMaxStack = cwCurStack;
            }

        // if the label isn't a new code context, verify that the label
        // is not reached by a JSR
        if (labelFirst != labelContext && labelFirst.isSubroutine())
            {
            throw new IllegalStateException(CLASS + ".checkStack:  " +
                    "Label reachable both by subroutine invocation and otherwise!");
            }

        for (Op op = labelFirst; op != null; op = op.getNext())
            {
            // check if this op has already been visited
            boolean fVisited = op.isReachable();

            // set the expected stack height for the op
            op.setStackHeight(cwCurStack);

            // in order to compute the stack change for a JSR, trace the
            // branch to its conclusion (i.e. the RET)
            int iOp = op.getValue();
            if (iOp == JSR)
                {
                Jsr     jsr   = (Jsr) op;
                Label   label = jsr.getLabel();
                boolean fSub  = label.isSubroutine();
                boolean fPrev = label.isReachable();

                // each reachable JSR is within a specific code context;
                // the JSR must know its code context in order to help with
                // the calculation of subroutine depth
                jsr.setContext(labelContext);

                // each subroutine label knows all reachable JSR ops which
                // invoke the subroutine
                label.addCaller(jsr);

                // there are several possibilities:
                //  1)  the label has already been reached
                //      1)  it is not marked as a subroutine so the code is
                //          illegal (a subroutine must only be accessed by
                //          a JSR statement)
                //      2)  it is marked as a subroutine but the RET height
                //          at the label is unknown, which means that either
                //          the call is recursive or the subroutine does not
                //          reach a RET
                //      3)  it is marked as a subroutine and the RET height
                //          is already known so use it
                //  2)  the label has not already been reached so mark the
                //      label as a subroutine and calculate its RET height
                //      by checking the stack from there
                if (!fSub)
                    {
                    // verify that the label has not already been reached by
                    // some non-JSR method
                    if (fPrev)
                        {
                        throw new IllegalStateException(CLASS + ".checkStack:  " +
                                "Label reachable both by subroutine invocation and otherwise!");
                        }

                    // keep track of all subroutines (each is a code context)
                    setContexts.add(label);

                    // JSR pushes one word onto the stack before transferring
                    // control to the label
                    cwMaxStack = checkStack(label, cwCurStack + 1, cwMaxStack, label, setContexts, iSubDepth + 1);
                    }

                // if a RET was not found for the JSR, then the JSR does
                // not continue
                if (label.getRet() == null)
                    {
                    return cwMaxStack;
                    }
                }

            // update the size of the stack based on the affect of this op
            cwCurStack += op.getStackChange();

            // check for new max stack
            if (cwCurStack > cwMaxStack)
                {
                cwMaxStack = cwCurStack;
                }
            // check for stack underflow
            else if (cwCurStack < 0)
                {
                throw new IllegalStateException(CLASS + ".checkStack:  "
                        + "Stack underflow on " + op.toString() + " (#"
                        + op.getValue() + " @" + op.getOffset() + ")!");
                }

            switch (iOp)
                {
                case LABEL:
                    {
                    Label label = (Label) op;

                    // check if we've already been here; if so then return
                    if (fVisited)
                        {
                        if (labelContext != label.getContext())
                            {
                            throw new IllegalStateException(CLASS + ".checkStack:  "
                                    + "Label " + label + " is reachable within two "
                                    + "different code contexts!");
                            }

                        return cwMaxStack;
                        }
                    else
                        {
                        label.setContext(labelContext);
                        }
                    }
                    break;

                case RET:
                    // must be here from a JSR
                    if (iSubDepth > 0)
                        {
                        labelContext.setRet((Ret)op);
                        labelContext.setRetHeight(cwCurStack);
                        return cwMaxStack;
                        }
                    else
                        {
                        throw new IllegalStateException(CLASS + ".checkStack:  " +
                                "RET without JSR!");
                        }

                case GOTO:
                    return checkStack(((Goto)op).getLabel(), cwCurStack, cwMaxStack, labelContext, setContexts, iSubDepth);

                case IFEQ:
                case IFNE:
                case IFLT:
                case IFGE:
                case IFGT:
                case IFLE:
                case IF_ICMPEQ:
                case IF_ICMPNE:
                case IF_ICMPLT:
                case IF_ICMPGE:
                case IF_ICMPGT:
                case IF_ICMPLE:
                case IF_ACMPEQ:
                case IF_ACMPNE:
                case IFNULL:
                case IFNONNULL:
                    cwMaxStack = checkStack(((OpBranch)op).getLabel(), cwCurStack, cwMaxStack, labelContext, setContexts, iSubDepth);
                    break;

                case SWITCH:
                case TABLESWITCH:
                case LOOKUPSWITCH:
                    {
                    OpSwitch opswitch = (OpSwitch)op;

                    // check default branch
                    cwMaxStack = checkStack(opswitch.getLabel(), cwCurStack, cwMaxStack, labelContext, setContexts, iSubDepth);

                    // check cases
                    Case[] aopCase = opswitch.getCases();
                    int    cCases  = aopCase.length;
                    for (int i = 0; i < cCases; ++i)
                        {
                        cwMaxStack = checkStack(aopCase[i].getLabel(), cwCurStack, cwMaxStack, labelContext, setContexts, iSubDepth);
                        }
                    }
                    return cwMaxStack;

                case TRY:
                    {
                    Try     optry    = (Try) op;
                    Catch[] aopCatch = optry.getCatches();
                    int     cCatches = aopCatch.length;
                    for (int i = 0; i < cCatches; ++i)
                        {
                        // there is always exactly one word on the stack when
                        // an exception handler is invoked
                        cwMaxStack = checkStack(aopCatch[i].getLabel(), 1, cwMaxStack, labelContext, setContexts, iSubDepth);
                        }
                    }
                    break;

                case IRETURN:
                case LRETURN:
                case FRETURN:
                case DRETURN:
                case ARETURN:
                case RETURN:
                case ATHROW:
                    return cwMaxStack;

                case IVAR:
                case LVAR:
                case FVAR:
                case DVAR:
                case AVAR:
                case RVAR:
                    {
                    // associate each declaration with a code context
                    OpDeclare decl = (OpDeclare) op;
                    decl.setContext(labelContext);
                    }
                    break;
                }
            }

        throw new IllegalStateException(CLASS + ".checkStack:  " +
                "Code not terminated properly!  (e.g. no return)");
        }

    /**
    * Walk the code as the JVM would, following all possible branches, and
    * determine which variables are assigned at each label.
    *
    * @param labelFirst  the op to start checking from
    * @param setVars     the current set of variables that have a value
    * @param cwParams    the number of parameter words to the method
    */
    protected void checkAssignment(Label labelFirst, HashSet setVars, int cwParams)
        {
        for (Op op = labelFirst; op != null; op = op.getNext())
            {
            int iOp = op.getValue();
            switch (iOp)
                {
                case IVAR:
                case LVAR:
                case FVAR:
                case DVAR:
                case AVAR:
                case RVAR:
                    {
                    OpDeclare decl = (OpDeclare) op;

                    // parameters are definitely assigned
                    if (decl.getSlot() < cwParams)
                        {
                        setVars.add(decl);
                        }
                    }
                    break;

                case ISTORE:
                case LSTORE:
                case FSTORE:
                case DSTORE:
                case ASTORE:
                case RSTORE:
                    {
                    OpStore   stor = (OpStore) op;

                    // the ?STORE ops definitely assign a variable
                    OpDeclare decl = stor.getVariable();
                    setVars.add(decl);
                    }
                    break;

                case LABEL:
                    {
                    Label   label        = (Label) op;
                    HashSet setLabelVars = label.getVariables();

                    // determine the intersection of the two sets of
                    // assigned variables;
                    // if the set of definitely assigned variables at the
                    // label did not change, and if we've been here before,
                    // then return (no further changes to make on this path)
                    if (!setLabelVars.retainAll(setVars) && label.isVisited())
                        {
                        return;
                        }

                    // make sure the current set of definitely-assigned,
                    // in-scope variables is up-to-date
                    if (setVars.size() != setLabelVars.size())
                        {
                        setVars = (HashSet) setLabelVars.clone();
                        }

                    label.setVisited(true);
                    }
                    break;

                case JSR:
                    {
                    Jsr   jsr   = (Jsr) op;
                    Label label = jsr.getLabel();
                    Ret   ret   = label.getRet();

                    // check variable assignments in the subroutine
                    // (Note:  assignments made by the subroutine are not
                    // reflected when control returns to this point)
                    checkAssignment(label, (HashSet) setVars.clone(), cwParams);

                    // if the flow of control cannot return, don't continue
                    if (ret == null)
                        {
                        return;
                        }

                    // merge the definitely assigned variables at the RET
                    // with the current set; (union of in-scope assigned
                    // vars, although some of the RET vars may not be in-
                    // scope; that is automatically corrected on the next
                    // encountered label)
                    setVars.addAll(ret.getVariables());
                    }
                    break;

                case RET:
                    {
                    Ret ret = (Ret) op;

                    // keep an intersection of the current set of assigned
                    // variables with the set already stored on the RET
                    HashSet setRetVars = ret.getVariables();
                    if (setRetVars == null)
                        {
                        // none registered on the ret; store the current set
                        ret.setVariables(setVars);
                        }
                    else
                        {
                        // compute intersection of in-scope assigned vars
                        setRetVars.retainAll(setVars);
                        }
                    }
                    return;

                case GOTO:
                    // since the flow of control is transferring uncondition-
                    // ally, pass the current set of assigned variables
                    checkAssignment(((Goto)op).getLabel(), setVars, cwParams);
                    return;

                case IFEQ:
                case IFNE:
                case IFLT:
                case IFGE:
                case IFGT:
                case IFLE:
                case IF_ICMPEQ:
                case IF_ICMPNE:
                case IF_ICMPLT:
                case IF_ICMPGE:
                case IF_ICMPGT:
                case IF_ICMPLE:
                case IF_ACMPEQ:
                case IF_ACMPNE:
                case IFNULL:
                case IFNONNULL:
                    // the flow of control can branch at this point
                    checkAssignment(((OpBranch)op).getLabel(), (HashSet) setVars.clone(), cwParams);
                    break;

                case SWITCH:
                case TABLESWITCH:
                case LOOKUPSWITCH:
                    {
                    OpSwitch opswitch = (OpSwitch)op;

                    // check cases
                    Case[] aopCase = opswitch.getCases();
                    int    cCases  = aopCase.length;
                    for (int i = 0; i < cCases; ++i)
                        {
                        checkAssignment(aopCase[i].getLabel(), (HashSet) setVars.clone(), cwParams);
                        }

                    // check default branch
                    checkAssignment(opswitch.getLabel(), setVars, cwParams);
                    }
                    return;

                case TRY:
                    {
                    Try     optry    = (Try) op;
                    Catch[] aopCatch = optry.getCatches();
                    int     cCatches = aopCatch.length;
                    for (int i = 0; i < cCatches; ++i)
                        {
                        checkAssignment(aopCatch[i].getLabel(), (HashSet) setVars.clone(), cwParams);
                        }
                    }
                    break;

                case IRETURN:
                case LRETURN:
                case FRETURN:
                case DRETURN:
                case ARETURN:
                case RETURN:
                case ATHROW:
                    return;
                }
            }

        throw new IllegalStateException(CLASS + ".checkAssignment:  " +
                "Code not terminated properly!  (e.g. no return)");
        }


    // ----- assembler interface --------------------------------------------

    /**
    * Clear any ops and associated structures.
    */
    public void clear()
        {
        m_abCode  = null;
        m_opFirst = null;
        m_opLast  = null;
        m_iLine   = 0;
        m_vectCatch.clear();

        m_nState    = CLEAR;
        m_fModified = true;
        }

    /**
    * Add an op to the code.  This is the primary means of building byte
    * code.
    *
    * @param op  an instance of Op (or an Op sub-class)
    *
    * @return  the op that was added
    */
    public Op add(Op op)
        {
        // assertions
        if (m_nState > ADDED)
            {
            throw new IllegalStateException(CLASS + ".add:  Illegal state (" + m_nState + ")");
            }
        if (op == null)
            {
            throw new IllegalArgumentException(CLASS + ".add:  Op is required!");
            }
        if (op instanceof Case)
            {
            // a Case op follows only a Switch or Case op
            if (!(m_opLast instanceof Switch || m_opLast instanceof Case))
                {
                throw new IllegalArgumentException(CLASS +
                        ".add:  A Case Op can only follow a Switch or a Case op!");
                }
            }

        // set the source code line for the op
        op.setLine(m_iLine);

        // add the op to the linked list
        if (m_opFirst == null)
            {
            m_opFirst = op;
            m_opLast  = op;
            }
        else
            {
            m_opLast.setNext(op);
            m_opLast = op;
            }

        // update state
        m_nState    = ADDED;
        m_fModified = true;

        return op;
        }

    /**
    * Get the current "source code line".
    *
    * @return  the current line of code
    */
    public int getLine()
        {
        return m_iLine;
        }

    /**
    * Set the current "source code line".
    *
    * @param iLine  the current line of code
    */
    public void setLine(int iLine)
        {
        m_iLine = iLine;
        }

    /**
    * Advance to the next "source code line".
    */
    public void nextLine()
        {
        ++m_iLine;
        }

    /**
    * Return the first op-code for this CodeAttribute
    */
    public Op getFirstOp()
        {
        ensureOps();
        return m_opFirst;
        }

    /**
    * For debugging or listing purposes, print the code details.
    */
    public void print()
        {
        out("Code Listing:");
        for (Op op = m_opFirst; op != null; op = op.getNext())
            {
            op.print();
            }

        LocalVariableTableAttribute attr =
                (LocalVariableTableAttribute) m_tblAttribute.get(ATTR_VARIABLES);
        if (attr != null)
            {
            out("Local variable table:");
            out(attr);
            }
        }

    /**
    * For script display purposes, format the JASM code.
    */
    public String toJasm()
        {
        StringBuffer sb = new StringBuffer(8192);
        switch (m_nState)
            {
            case LOADED:
                ensureOps();
                // fall through

            case DISASSEMBLED:
                if (m_opFirst != null)
                    {
                    int cDigits = getMaxDecDigits(m_opLast.getOffset());
                    int nLine   = 0;
                    for (Op op = m_opFirst; op != null; op = op.getNext())
                        {
                        int nOpLine = op.getLine();
                        if (nOpLine != nLine)
                            {
                            nLine = nOpLine;
                            sb.append('\n')
                              .append(toDecString(op.getOffset(), cDigits))
                              .append(": // line ")
                              .append(nLine);
                            }

                        String s = op.toJasm();
                        if (s != null)
                            {
                            sb.append('\n')
                              .append(toDecString(op.getOffset(), cDigits))
                              .append(": ")
                              .append(s);
                            }
                        }
                    break;
                    }
                // fall through

            default:
                for (Op op = m_opFirst; op != null; op = op.getNext())
                    {
                    sb.append("\n[")
                      .append(op.getOffset())
                      .append("] (")
                      .append(op.getLine())
                      .append(") ")
                      .append(op.toString());
                    }
                break;
            }

        return sb.substring(1);
        }

    // ----- accessor:  attribute -------------------------------------------

    /**
    * Access an attribute structure.
    *
    * @param sName  the attribute name
    *
    * @return the specified attribute or null if the attribute does not exist
    */
    public Attribute getAttribute(String sName)
        {
        return (Attribute) m_tblAttribute.get(sName);
        }

    /**
    * Add an attribute structure.
    *
    * @param sName  the attribute name
    *
    * @return  the new attribute
    */
    public Attribute addAttribute(String sName)
        {
        Attribute attribute;
        if (sName.equals(ATTR_LINENUMBERS))
            {
            attribute = new LineNumberTableAttribute(this);
            }
        else if (sName.equals(ATTR_VARIABLES))
            {
            attribute = new LocalVariableTableAttribute(this);
            }
        else if (sName.equals(ATTR_VARIABLETYPES))
            {
            attribute = new LocalVariableTypeTableAttribute(this);
            }
        else if (sName.equals(ATTR_STACKMAPTABLE))
            {
            attribute = new StackMapTableAttribute(this);
            }
        else if (sName.equals(ATTR_RTVISTANNOT))
            {
            attribute = new RuntimeVisibleTypeAnnotationsAttribute(this);
            }
        else if (sName.equals(ATTR_RTINVISTANNOT))
            {
            attribute = new RuntimeInvisibleTypeAnnotationsAttribute(this);
            }
        else
            {
            attribute = new Attribute(this, sName);
            }

        m_tblAttribute.put(attribute.getIdentity(), attribute);
        m_fModified = true;

        return attribute;
        }

    /**
    * Remove a attribute.
    *
    * @param sName  the attribute name
    */
    public void removeAttribute(String sName)
        {
        if (m_tblAttribute.remove(sName) != null)
            {
            m_fModified = true;
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "CodeAttribute";

    /**
    * An empty byte array.
    */
    private static final byte[] NO_BYTES = new byte[0];

    /**
    * An empty label array.
    */
    private static final Label[] LABEL_ARRAY = new Label[0];

    /**
    * State:  Initialized.
    */
    private static final int CLEAR          = 0;
    /**
    * State:  The user of this class has "assembled" (added) ops.
    */
    private static final int ADDED          = 1;
    /**
    * State:  This attribute has disassembled, but the binary code itself
    * was not disassembled; it is in m_abCode.
    */
    private static final int LOADED         = 2;
    /**
    * State:  The binary code in m_abCode was disassembled into ops.
    */
    private static final int DISASSEMBLED   = 3;
    /**
    * State:  The ops in the linked list were assembled into m_abCode.
    */
    private static final int ASSEMBLED      = 4;

    /**
    * Line number table sub-attribute.
    */
    private static final UtfConstant CONST_LINENUMBERS = new UtfConstant(ATTR_LINENUMBERS);
    /**
    * Local variable table sub-attribute.
    */
    private static final UtfConstant CONST_VARIABLES   = new UtfConstant(ATTR_VARIABLES);

    /**
    * State of the code attribute.
    */
    private int m_nState;

    /**
    * The constant pool supplied for disassembly.  This is necessary for when
    * the byte code must be fully disassembled.
    */
    private ConstantPool m_pool;

    /**
    * Tracks modifications to the code attribute.
    */
    private boolean m_fModified;


    /**
    * Number of variables (as read or assembled).
    */
    private int m_cVars;

    /**
    * Size of stack (as read or assembled).
    */
    private int m_cwStack;

    /**
    * The byte code (as read or assembled).
    */
    private byte[] m_abCode;

    /**
    * The first op (the head of a linked list).
    */
    private Op m_opFirst;

    /**
    * The last op (the tail of a linked list).
    * n/a for disassembled ops.
    */
    private Op m_opLast;

    /**
    * The current "source code line".
    * n/a for disassembled ops.
    */
    private int m_iLine;

    /**
    * Guarded sections (each represents information about a Java "catch" or
    * "finally" construct) (as read or assembled).
    */
    private Vector m_vectCatch = new Vector();

    /**
    * Sub-attributes.
    */
    private StringTable m_tblAttribute = new StringTable();
    }
