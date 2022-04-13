/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.assembler;

import java.io.IOException;
import java.io.EOFException;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import java.util.Arrays;
import java.util.Enumeration;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ReadBuffer;

import com.tangosol.util.StringTable;

/**
* Represents a Java .class structure (the JVM Class file format).  For
* reference purposes, download the JVM specification from
* <a href="http://java.sun.com/docs/index.html">http://java.sun.com/docs/index.html</a>.
* <p>
* Description:
* <p>
*   The .class structure is hierachical in nature with a single internally
*   shared resource, the constant pool.  In other words, with the exception
*   of the constant pool, all portions of the .class structure only refer
*   to themselves or structures which are contained within themselves.  The
*   hierarchy is as follows:
* <p>
* <pre>
*   1) ClassFile
*       1)  header
*           1)  magic cookie
*           2)  expected JVM version
*       2)  Constant[] (the ConstantPool)
*       3)  class information
*           1)  AccessFlags
*           2)  this class/interface identity
*           3)  super class/interface identity
*           4)  implemented interface identity[]
*       4)  Field[]
*           1)  AccessFlags
*           2)  field identity
*           3)  field data type
*           4)  Attribute[]
*               1)  JVM supports a single "ConstantValue" attribute
*       5)  Method[]
*           1)  AccessFlags
*           2)  method identity
*           3)  method parameter/return signature
*           4)  Attribute[]
*               1)  JVM supports a single "Code" attribute
*                   1)  frame stack area size
*                   2)  frame local variable area size
*                   3)  Op[]
*                   4)  TryCatch[]
*                       1)  Op "try" range [pc,pc)
*                       2)  Handler pc
*                       3)  catchable class identity
*                   5)  Attribute[] (yes, the "Code" attribute has attributes!)
*                       1)  JVM supports a single "LineNumberTable" attribute
*                           - cross-references pc and source line number
*                             values
*                       2)  JVM supports a single "LocalVariableTable" attribute
*                           - cross-references local variable index and local
*                             variable name/data type values
*                       3)  JVM supports a single "LocalVariableTypeTable" attribute
*                           - cross-references local variable index and local
*                             variable name/data signature values
*               2)  JVM supports a single "Exceptions" attribute
*                   1)  declared exception identity[]
*       6)  Attribute[]
*           1)  JVM specification defines (but JVM does not support) a single
*               "SourceFile" attribute
*               - specifies the OS source file name (without any path)
* </pre>
* <p>
*   WARNING!!!  Other attributes have been introduced since the specification
*   was last updated.  These attributes are similar in structure and naming
*   conventions to the current attributes and appear to all be related to the
*   JDK1.1 "inner class" feature.
* <p>
*   Note:  Other attributes than those defined above are legal but ignored
*   by both the JVM and any other process examining/manipulating/using a
*   .class structure.  Custom-defined attributes must follow the package
*   and class naming conventions, e.g. "com.tangosol.examples.BreakPointTable".
* <p>
* Requirements:
* <p>
* <pre>
*   1)  The role of the class is to support the contruction, reflection,
*       modification, and transport (i.e. transient storage) of Java
*       ClassFile (aka ".class file") structures
*
*       1)  An assembler will construct ClassFile structures
*
*       2)  The Component Definition will create JCS structures and a
*           disassembler will display detailed information using the
*           reflection information provided by ClassFile structures
*
*       3)  A deployment tool or the class loader will patch up package
*           names by modifying the ClassFile structure
*
*       4)  ClassFile structures provide a type-safe reference type for
*           compiled classes (typically used for passing a compiled class
*           as a parameter or as the type of a return value)
*
*   2)  Efficiency, especially when the ClassFile structure is used only
*       for transport, means that the disassembly of .class structures and
*       the final assembly and linking of accessor-supplied information must
*       be deferred until necessary
*
*   3)  Production of dependency and relationship information which would
*       enable partial compilation, selective re-compilation, BOM/where-used
*       and impact analysis
*
*   4)  Support for partial, incremental, and method (re-)compilation
* </pre>
* <p>
*   WARNING!!!  This implementation is not intended to be thread-safe.  Do
*   not access a single instance of this class (or any of the other classes
*   in this package) from more than one thread unless synchronization is
*   handled by the caller.
* <p>
*   WARNING!!!  This implementation is not intended to be idiot-safe.  It is
*   the responsibility of the caller to provide legal ClassFile information,
*   and not the responsibility of this implementation to detect it.
*
* @version 0.10, 02/05/98, prototype dis-assembler
* @version 0.50, 05/07/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class ClassFile extends VMStructure implements Constants
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a new ClassFile object.
    *
    * @param sThis       fully qualified name of this class
    * @param sSuper      name of the super class, or null if there is no
    *                    super class (e.g. java.lang.Object or an interface)
    * @param fInterface  true means that this is an interface
    */
    public ClassFile(String sThis, String sSuper, boolean fInterface)
        {
        init();
        setLoaded(true);
        setName(sThis);
        setSuper(sSuper);
        setInterface(fInterface);
        setSynchronized(true);
        }

    /**
    * Constructs a ClassFile from a .class structure stored in a byte array.
    *
    * This constructor is preferred to the DataInput-based constructor.
    *
    * @param  abClazz  the byte array containing the .class structure
    */
    public ClassFile(byte[] abClazz)
        {
        setBytes(abClazz);
        }

    /**
    * Constructs a ClassFile from a stream containing a .class structure.
    *
    * @param  stream  the java.io.DataInput stream containing the .class
    *                 structure
    */
    public ClassFile(DataInput stream)
            throws IOException
        {
        load(stream);
        }


    // ----- persistence ----------------------------------------------------

    /**
    * Read the .class structure out of the passed stream.
    *
    * @param stream  the object implementing java.io.DataInput and containing
    *                the .class structure
    *
    * @throws java.io.IOException if an error (besides EOF) occurs reading
    *         from the stream
    */
    public void load(DataInput stream)
            throws IOException
        {
        final int             MAX       = 8192;
        byte[]                abBuf     = new byte[MAX];
        ByteArrayOutputStream streamRaw = new ByteArrayOutputStream(MAX);

        int of = 0;
        try
            {
            while (true)
                {
                byte b = stream.readByte();

                abBuf[of++] = b;
				if (of == MAX)
                    {
                    streamRaw.write(abBuf, 0, MAX);
                    of = 0;
                    }
                }
            }
        catch (EOFException e)
            {
            if (of > 0)
                {
                streamRaw.write(abBuf, 0, of);
                }
            }


        setBytes(streamRaw.toByteArray());
        }

    /**
    * Write the .class structure into a stream.
    *
    * @param stream  the object implementing java.io.DataOutput to which
    *                the .class structure will be written
    *
    * @throws java.io.IOException if an error occurs writing the stream
    */
    public void save(DataOutput stream)
            throws IOException
        {
        stream.write(getBytes());
        }


    // ----- VMStructure operations -----------------------------------------

    /**
    * The disassembly process reads the structure from the passed input
    * stream and uses the constant pool to dereference any constant
    * references.
    * <p>
    * The structure is defined by the Java Virtual Machine Specification as:
    * <code><pre>
    *   ClassFile
    *       {
    *       u4 magic;
    *       u2 minor_version;
    *       u2 major_version;
    *       u2 constant_pool_count;
    *       cp_info constant_pool[constant_pool_count-1];
    *       u2 access_flags;
    *       u2 this_class;
    *       u2 super_class;
    *       u2 interfaces_count;
    *       u2 interfaces[interfaces_count];
    *       u2 fields_count;
    *       field_info fields[fields_count];
    *       u2 methods_count;
    *       method_info methods[methods_count];
    *       u2 attributes_count;
    *       attribute_info attributes[attributes_count];
    *       }
    * </pre></code>
    *
    * @param stream  the stream implementing java.io.DataInput from which
    *                to read the assembled VM structure
    * @param pool    the constant pool for the class which contains any
    *                constants referenced by this VM structure
    */
    protected void disassemble(DataInput stream, ConstantPool pool)
            throws IOException
        {
        // check header
        int nCookie = stream.readInt();
        if (nCookie != CLASS_COOKIE)
            {
            throw new IOException(CLASS + ".disassemble:  Class cookie not found!");
            }

        m_nVersionMinor = stream.readUnsignedShort();
        m_nVersionMajor = stream.readUnsignedShort();

        if ( m_nVersionMajor <  VERSION_MAJOR_MIN
         || (m_nVersionMajor == VERSION_MAJOR_MIN &&
             m_nVersionMinor < VERSION_MINOR_MIN)
         ||  m_nVersionMajor >  VERSION_MAJOR_MAX
         || (m_nVersionMajor == VERSION_MAJOR_MAX &&
             m_nVersionMinor > VERSION_MINOR_MAX))
            {
            throw new IOException(CLASS + ".disassemble:  Version (" + m_nVersionMajor + '.' + m_nVersionMinor+ ") not supported!");
            }

        // constant pool
        pool.disassemble(stream, pool);
        m_pool = pool;

        // access flags
        AccessFlags flags = m_flags;
        flags.disassemble(stream, pool);

        // identity (this/super)
        m_clzName  = (ClassConstant) pool.getConstant(stream.readUnsignedShort());
        m_clzSuper = (ClassConstant) pool.getConstant(stream.readUnsignedShort());

        // interfaces
        m_tblInterface.clear();
        int c = stream.readUnsignedShort();
        for (int i = 0; i < c; ++i)
            {
            ClassConstant clz = (ClassConstant) pool.getConstant(stream.readUnsignedShort());
            m_tblInterface.put(clz.getValue(), clz);
            }

        // fields
        m_tblField.clear();
        c = stream.readUnsignedShort();
        for (int i = 0; i < c; ++i)
            {
            Field field = new Field();
            field.disassemble(stream, pool);
            m_tblField.put(field.getIdentity(), field);
            }

        // methods
        m_tblMethod.clear();
        c = stream.readUnsignedShort();
        String sClass = m_clzName.getValue();
        for (int i = 0; i < c; ++i)
            {
            Method method = new Method(sClass, flags.isInterface());
            method.disassemble(stream, pool);
            m_tblMethod.put(method.getIdentity(), method);
            }

        // attributes
        m_tblAttribute.clear();
        c = stream.readUnsignedShort();
        for (int i = 0; i < c; ++i)
            {
            Attribute attribute = Attribute.loadAttribute(this, stream, pool);
            m_tblAttribute.put(attribute.getIdentity(), attribute);
            }

        resetModified();
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
        // register constants used by the class
        pool.registerConstant(m_clzName);
        if (m_clzSuper != null)
            {
            pool.registerConstant(m_clzSuper);
            }

        Enumeration enmr = m_tblInterface.elements();
        while (enmr.hasMoreElements())
            {
            pool.registerConstant((ClassConstant) enmr.nextElement());
            }

        // the constant pool doesn't have to register itself
        // m_pool.preassemble(pool);

        // register constants used by the fields, methods, and attributes
        StringTable[] atbl = CONTAINED_TABLE;
        for (int i = 0; i < atbl.length; ++i)
            {
            StringTable tbl = atbl[i];
            enmr = tbl.elements();
            while (enmr.hasMoreElements())
                {
                ((VMStructure) enmr.nextElement()).preassemble(pool);
                }
            }
        }

    /**
    * The assembly process assembles and writes the structure to the passed
    * output stream, resolving any dependencies using the passed constant
    * pool.
    * <p>
    * The structure is defined by the Java Virtual Machine Specification as:
    * <code><pre>
    *   ClassFile
    *       {
    *       u4 magic;
    *       u2 minor_version;
    *       u2 major_version;
    *       u2 constant_pool_count;
    *       cp_info constant_pool[constant_pool_count-1];
    *       u2 access_flags;
    *       u2 this_class;
    *       u2 super_class;
    *       u2 interfaces_count;
    *       u2 interfaces[interfaces_count];
    *       u2 fields_count;
    *       field_info fields[fields_count];
    *       u2 methods_count;
    *       method_info methods[methods_count];
    *       u2 attributes_count;
    *       attribute_info attributes[attributes_count];
    *       }
    * </pre></code>
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
        // header
        int nVersionMajor = m_nVersionMajor;

        stream.writeInt(CLASS_COOKIE);
        stream.writeShort(m_nVersionMinor);
        stream.writeShort(m_nVersionMajor);

        // constant pool
        m_pool.assemble(stream, pool);

        // access flags
        if (nVersionMajor >= 52)
            {
            // as of 52.0 ACC_SUPER should always be set
            m_flags.setSynchronized(true);
            }
        m_flags.assemble(stream, pool);

        // identity (this/super)
        stream.writeShort(pool.findConstant(m_clzName));
        stream.writeShort(m_clzSuper == null ? 0 : pool.findConstant(m_clzSuper));

        // interfaces
        stream.writeShort(m_tblInterface.getSize());
        Enumeration enmr = m_tblInterface.elements();
        while (enmr.hasMoreElements())
            {
            ClassConstant clz = (ClassConstant) enmr.nextElement();
            stream.writeShort(pool.findConstant(clz));
            }

        // fields, methods, attributes
        StringTable[] atbl = CONTAINED_TABLE;
        for (int i = 0; i < atbl.length; ++i)
            {
            StringTable tbl = atbl[i];
            stream.writeShort(tbl.getSize());
            enmr = tbl.elements();
            while (enmr.hasMoreElements())
                {
                ((VMStructure) enmr.nextElement()).assemble(stream, pool);
                }
            }
        }


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the ClassFile.
    *
    * @return a string describing the ClassFile
    */
    public String toString()
        {
        StringBuffer sb = new StringBuffer();

        boolean fInterface = isInterface();
        String sMod = m_flags.toString(fInterface ? ACC_INTERFACE : ACC_CLASS);
        if (sMod.length() > 0)
            {
            sb.append(sMod)
              .append(' ');
            }

        sb.append(fInterface ? "interface " : "class ")
          .append(m_clzName.getJavaName());

        ClassConstant clzSuper = m_clzSuper;
        if (clzSuper != null)
            {
            String sSuper = clzSuper.getValue();
            if (!sSuper.equals(DEFAULT_SUPER))
                {
                sb.append(" extends ")
                  .append(clzSuper.getJavaName());
                }
            }

        if (!m_tblInterface.isEmpty())
            {
            sb.append(fInterface? " extends " : " implements ");
            Enumeration enmr = m_tblInterface.elements();
            boolean fTrailing = false;
            while (enmr.hasMoreElements())
                {
                if (fTrailing)
                    {
                    sb.append(", ");
                    }
                sb.append(((ClassConstant) enmr.nextElement()).getJavaName());
                fTrailing = true;
                }
            }

        return sb.toString();
        }

    /**
    * Compare this object to another object for equality.
    *
    * @param obj  the other object to compare to this
    *
    * @return true if this object equals that object
    */
    public boolean equals(Object obj)
        {
        if (obj instanceof ClassFile)
            {
            ClassFile that = (ClassFile) obj;
            if (this == that)
                {
                return true;
                }

            // equal iff .class structure is identical
            byte[] abThis = this.getBytes();
            byte[] abThat = that.getBytes();
            return Arrays.equals(abThis, abThat);
            }
        return false;
        }

    // ----- ClassFile operations -------------------------------------------

    /**
    * Internal initialization.
    */
    protected void init()
        {
        m_fModified     = false;
        m_fLoaded       = false;
        m_abClazz       = null;
        m_clzName       = null;
        m_clzSuper      = null;
        m_pool          = new ConstantPool(this);
        m_flags         = new AccessFlags();
        m_tblInterface  = new StringTable();
        m_tblField      = new StringTable();
        m_tblMethod     = new StringTable();
        m_tblAttribute  = new StringTable();

        CONTAINED_TABLE[0] = m_tblField;
        CONTAINED_TABLE[1] = m_tblMethod;
        CONTAINED_TABLE[2] = m_tblAttribute;
        }

    /**
    * Using the constant pool resolution feature, change all references to
    * the specified package.
    *
    * @param sPkg  dot-delimited package name, not null
    */
    public void relocate(String sPkg)
        {
        if (!sPkg.replace('.', '/').equals(Relocator.PACKAGE))
            {
            resolve(new Relocator(sPkg));
            }
        }

    /**
    * Allow constants in the pool to be replaced by a callback.
    *
    * @param resolver  the callback
    */
    public void resolve(Resolver resolver)
        {
        // get the .class structure
        byte[] ab = getBytes();

        // wipe any disassembled information -- we are going to go directly
        // after the constant pool portion of the binary .class structure
        if (isLoaded())
            {
            init();
            }

        try
            {
            ByteArrayReadBuffer    in        = new ByteArrayReadBuffer(ab);
            ByteArrayOutputStream  out       = new ByteArrayOutputStream((int) (ab.length * 1.25));
            ReadBuffer.BufferInput streamIn  = in.getBufferInput();
            DataOutput             streamOut = new DataOutputStream(out);

            // the portion of the .class structure before the pool
            int ofStart = 8;
            streamIn.setOffset(ofStart);
            out.write(ab, 0, ofStart);

            // the constant pool is in the middle (offset 8 bytes) of the .class
            int cConst = streamIn.readUnsignedShort();
            streamOut.writeShort(cConst);

            for (int i = 1; i < cConst; ++i)
                {
                Constant constant = null;
                int nTag = streamIn.readUnsignedByte();
                switch (nTag)
                    {
                    case CONSTANT_UTF8:
                        constant = new UtfConstant();
                        break;
                    case CONSTANT_INTEGER:
                        constant = new IntConstant();
                        break;
                    case CONSTANT_FLOAT:
                        constant = new FloatConstant();
                        break;
                    case CONSTANT_LONG:
                        constant = new LongConstant();
                        ++i;    // uses two constant slots
                        break;
                    case CONSTANT_DOUBLE:
                        constant = new DoubleConstant();
                        ++i;    // uses two constant slots
                        break;

                    case CONSTANT_CLASS:
                    case CONSTANT_STRING:
                    case CONSTANT_METHODTYPE:
                    case CONSTANT_MODULE:
                    case CONSTANT_PACKAGE:
                        streamOut.writeByte(nTag);
                        streamOut.writeShort(streamIn.readUnsignedShort());
                        break;

                    case CONSTANT_FIELDREF:
                    case CONSTANT_METHODREF:
                    case CONSTANT_INTERFACEMETHODREF:
                    case CONSTANT_NAMEANDTYPE:
                    case CONSTANT_INVOKEDYNAMIC:
                        streamOut.writeByte(nTag);
                        streamOut.writeShort(streamIn.readUnsignedShort());
                        streamOut.writeShort(streamIn.readUnsignedShort());
                        break;

                    case CONSTANT_METHODHANDLE:
                        streamOut.writeByte(nTag);
                        streamOut.writeByte(streamIn.readUnsignedByte());
                        streamOut.writeShort(streamIn.readUnsignedShort());
                        break;

                    default:
                        throw new IOException("Invalid constant tag " + nTag);
                    }

                if (constant != null)
                    {
                    constant.disassemble(streamIn, null);
                    constant = resolver.resolve(constant);
                    constant.assemble(streamOut, null);
                    }
                }

            // the portion of the .class structure after the pool
            int ofStop = streamIn.getOffset();
            out.write(ab, ofStop, ab.length - ofStop);
            ab = out.toByteArray();
            }
        catch (IOException e)
            {
            throw new RuntimeException("Illegal .class structure!\n" + e.toString());
            }

        // store the new .class structure
        setBytes(ab);
        }

    /**
    * Write java like descriptions of the byte codes for this class file not
    * including attributes for each info structure (class | field | method).
    *
    * @param out  the {@link PrintWriter} to write the text to
    */
    public void dump(PrintWriter out)
        {
        // class header
        boolean fInterface = isInterface();
        String sMod = m_flags.toString(fInterface ? ACC_INTERFACE : ACC_CLASS);
        if (sMod.length() > 0)
            {
            out.write(sMod);
            out.write(' ');
            }

        out.write(fInterface ? "interface " : "class ");
        out.write(m_clzName.getJavaName());

        ClassConstant clzSuper = m_clzSuper;
        if (clzSuper != null)
            {
            String sSuper = clzSuper.getValue();
            if (!sSuper.equals(DEFAULT_SUPER))
                {
                out.write(" extends ");
                out.write(clzSuper.getJavaName());
                }
            }

        if (!m_tblInterface.isEmpty())
            {
            out.write(fInterface? " extends " : " implements ");
            Enumeration enmr = m_tblInterface.elements();
            boolean fTrailing = false;
            while (enmr.hasMoreElements())
                {
                if (fTrailing)
                    {
                    out.write(", ");
                    }
                out.write(((ClassConstant) enmr.nextElement()).getJavaName());
                fTrailing = true;
                }
            }

        String sIndent = "    ";

        out.println(toString());
        out.println(sIndent + '{');

        String sBreak = "";

        // fields
        Enumeration enmr = m_tblField.elements();
        while (enmr.hasMoreElements())
            {
            Field field = (Field) enmr.nextElement();

            out.print(sIndent + field.toString());

            if (field.isConstant())
                {
                out.println(" = " + field.getConstantValue() + ';');
                }
            else
                {
                out.println(';');
                }

            sBreak = "\n";
            }

        out.print(sBreak);

        // methods
        enmr = m_tblMethod.elements();
        while (enmr.hasMoreElements())
            {
            //((Method) enmr.nextElement()).dump(out, sIndent);
            out.print(sIndent);
            out.println(enmr.nextElement());
            }

        out.println(sIndent + '}');
        }


    // ----- accessor:  classfile version -----------------------------------

    /**
    * Get the Classfile format major version number of this class.
    *
    * @return  the major version number
    */
    public int getMajorVersion()
        {
        return m_nVersionMajor;
        }

    /**
    * Set the Classfile format major version number of this class.
    *
    * @param nVersionMajor  the major version number
    */
    public void setMajorVersion(int nVersionMajor)
        {
        m_nVersionMajor = nVersionMajor;
        setModified(true);
        }
    
    /**
    * Get the Classfile format minor version number of this class.
    *
    * @return  the minor version number
    */
    public int getMinorVersion()
        {
        return m_nVersionMinor;
        }
    
    /**
    * Set the Classfile format minor version number of this class.
    *
    * @param nVersionMinor  the minor version number
    */
    public void setMinorVersion(int nVersionMinor)
        {
        m_nVersionMinor = nVersionMinor;
        setModified(true);
        }    

    // ----- accessor:  loaded ----------------------------------------------

    /**
    * The loaded property refers to the .class structure stored internally
    * as a byte array with respect to the information stored in the ClassFile
    * object.  If the ClassFile is constructed from a byte array or an input
    * stream, the information is not expanded immediately in case the reason
    * for the creation of the ClassFile object is as a type-safe mechanism
    * for passing compiled .class structures.  Before any operations can take
    * place agains the ClassFile, the .class structure stored in the byte
    * array must be expanded.
    *
    * @return false if the .class structure must be expanded before
    *         queries/modifications against the ClassFile can commence
    *
    * @see #ensureLoaded()
    */
    protected boolean isLoaded()
        {
        return m_fLoaded;
        }

    /**
    * Set or reset the loaded flag on the ClassFile.
    *
    * @param fLoaded  true to set the loaded flag, false to reset it
    */
    protected void setLoaded(boolean fLoaded)
        {
        m_fLoaded = fLoaded;
        }

    /**
    * Ensure that the ClassFile object is ready to be queried/modified.
    */
    protected void ensureLoaded()
        {
        if (!m_fLoaded)
            {
            if (m_abClazz != null)
                {
                ByteArrayInputStream streamRaw = new ByteArrayInputStream(m_abClazz);
                DataInput stream = new DataInputStream(streamRaw);
                try
                    {
                    disassemble(stream, m_pool);
                    }
                catch (IOException e)
                    {
                    throw ensureRuntimeException(e);
                    }
                }

            m_fLoaded = true;
            }
        }


    // ----- accessor:  modified --------------------------------------------

    /**
    * The modified property refers to the information stored in the ClassFile
    * object with respect to the potentially cached .class structure stored
    * internally as a byte array.  If a modification has occurred to the
    * ClassFile information such that the .class structure (the byte array)
    * must be re-built, the ClassFile is considered to be modified.  When the
    * .class structure is re-built, the modified flag is reset.
    *
    * @return true if the ClassFile information has been modified
    */
    public boolean isModified()
        {
        // a class is only modifiable after it is loaded
        if (!m_fLoaded)
            {
            return m_fModified;
            }

        if (m_fModified || m_abClazz == null || m_pool.isModified())
            {
            return true;
            }

        // check all other VM sub-structures
        StringTable[] atbl = CONTAINED_TABLE;
        for (int i = 0; i < atbl.length; ++i)
            {
            Enumeration enmr = atbl[i].elements();
            while (enmr.hasMoreElements())
                {
                if (((VMStructure) enmr.nextElement()).isModified())
                    {
                    return true;
                    }
                }
            }

        return false;
        }

    /**
    * Set or reset the modified flag on the ClassFile.
    *
    * @param fModified  true to set the modified flag, false to reset it
    */
    public void setModified(boolean fModified)
        {
        if (fModified)
            {
            m_fModified = fModified;
            }
        else
            {
            resetModified();
            }
        }

    /**
    * Reset the modified flag on the ClassFile.
    */
    protected void resetModified()
        {
        m_pool.resetModified();

        // reset all other VM sub-structures
        StringTable[] atbl = CONTAINED_TABLE;
        for (int i = 0; i < atbl.length; ++i)
            {
            Enumeration enmr = atbl[i].elements();
            while (enmr.hasMoreElements())
                {
                ((VMStructure) enmr.nextElement()).resetModified();
                }
            }

        m_fModified = false;
        }


    // ----- accessor:  .class structure ------------------------------------

    /**
    * Access the .class structure as a byte array.  Do not modify the return
    * value from this accessor method.
    *
    * @return the .class structure in the form of a byte array
    */
    public byte[] getBytes()
        {
        if (isModified())
            {
            // collect all constants
            ConstantPool pool      = m_pool;
            boolean      fOptimize = !pool.isOrderImportant();
            if (fOptimize)
                {
                if (pool != null)
                    {
                    // detach the previous pool from the ClassFile
                    pool.setClassFile(null);
                    }
                pool = new ConstantPool(this);
                }
            preassemble(pool);
            if (fOptimize)
                {
                pool.sort();
                }
            m_pool = pool;

            // assemble the class into bytes
            ByteArrayOutputStream streamRaw = new ByteArrayOutputStream(8192);
            DataOutputStream stream = new DataOutputStream(streamRaw);
            try
                {
                assemble(stream, pool);
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            m_abClazz = streamRaw.toByteArray();

            resetModified();
            }

        return m_abClazz;
        }

    /**
    * Supply the class structure as a byte array.
    *
    * @param abClazz  the .class structure in the form of a byte array
    */
    public void setBytes(byte[] abClazz)
        {
        if (abClazz == null)
            {
            throw new IllegalArgumentException(CLASS + ".setBytes: byte array is required");
            }

        init();
        m_abClazz = abClazz;
        setLoaded(false);
        }


    // ----- accessor:  .class identity -------------------------------------

    /**
    * Determine the fully qualified name of this class.
    *
    * @return this class name
    */
    public String getName()
        {
        ensureLoaded();
        return m_clzName.getValue();
        }

    /**
    * Set the fully qualified name of this class.
    *
    * @param sName  the new class name
    */
    public void setName(String sName)
        {
        ensureLoaded();
        m_clzName = new ClassConstant(sName);
        setModified(true);
        }

    /**
    * Determine the class constant for this class.
    *
    * @return this class's class constant
    */
    public ClassConstant getClassConstant()
        {
        ensureLoaded();
        return m_clzName;
        }


    // ----- accessor:  .class derivation -----------------------------------

    /**
    * Determine the fully qualified name of the super class.
    *
    * @return the super class name
    */
    public String getSuper()
        {
        ensureLoaded();
        return (m_clzSuper == null ? (String) null : m_clzSuper.getValue());
        }

    /**
    * Set the fully qualified name of the super class.
    *
    * @param sSuper  the new super class name
    */
    public void setSuper(String sSuper)
        {
        ensureLoaded();
        m_clzSuper = (sSuper == null ? (ClassConstant) null : new ClassConstant(sSuper));
        setModified(true);
        }

    /**
    * Determine the class constant for the super class.
    *
    * @return the super class's class constant
    */
    public ClassConstant getSuperClassConstant()
        {
        ensureLoaded();
        return m_clzSuper;
        }


    // ----- accessor:  implements ------------------------------------------

    /**
    * Add an implemented interface.  (For interfaces, this corresponds to the
    * Java language "extends" keyword.)
    *
    * @param sInterface  the interface name
    */
    public void addImplements(String sInterface)
        {
        ensureLoaded();
        m_tblInterface.put(sInterface.replace('.', '/'), new ClassConstant(sInterface));
        setModified(true);
        }

    /**
    * Remove an implemented interface.
    *
    * @param sInterface  the interface name
    */
    public void removeImplements(String sInterface)
        {
        ensureLoaded();
        m_tblInterface.remove(sInterface.replace('.', '/'));
        setModified(true);
        }

    /**
    * Access the set of implemented interfaces.
    *
    * @return an enumeration of interface names
    */
    public Enumeration getImplements()
        {
        ensureLoaded();
        return m_tblInterface.keys();
        }


    // ----- accessor:  interface -------------------------------------------

    /**
    * Determine if the interface attribute is set.
    *
    * @return true if interface
    */
    public boolean isInterface()
        {
        ensureLoaded();
        return m_flags.isInterface();
        }

    /**
    * Set the interface attribute.
    *
    * @param fInterface  true to set to interface, false to set to class
    */
    public void setInterface(boolean fInterface)
        {
        ensureLoaded();
        m_flags.setInterface(fInterface);
        setModified(true);
        }


    // ----- accessor:  access ----------------------------------------------

    /**
    * Get the class/method/field accessibility value.
    *
    * @return one of ACC_PUBLIC, ACC_PROTECTED, ACC_PRIVATE, or ACC_PACKAGE
    */
    public int getAccess()
        {
        ensureLoaded();
        return m_flags.getAccess();
        }

    /**
    * Set the class/method/field accessibility value.
    *
    * @param nAccess  should be one of ACC_PUBLIC, ACC_PROTECTED,
    *                 ACC_PRIVATE, or ACC_PACKAGE
    */
    public void setAccess(int nAccess)
        {
        ensureLoaded();
        m_flags.setAccess(nAccess);
        setModified(true);
        }

    /**
    * Determine if the accessibility is public.
    *
    * @return true if the accessibility is public
    */
    public boolean isPublic()
        {
        ensureLoaded();
        return m_flags.isPublic();
        }

    /**
    * Set the accessibility to public.
    */
    public void setPublic()
        {
        ensureLoaded();
        m_flags.setPublic();
        setModified(true);
        }

    /**
    * Determine if the accessibility is protected.
    *
    * @return true if the accessibility is protected
    */
    public boolean isProtected()
        {
        ensureLoaded();
        return m_flags.isProtected();
        }

    /**
    * Set the accessibility to protected.
    */
    public void setProtected()
        {
        ensureLoaded();
        m_flags.setProtected();
        setModified(true);
        }

    /**
    * Determine if the accessibility is package private.
    *
    * @return true if the accessibility is package private
    */
    public boolean isPackage()
        {
        ensureLoaded();
        return m_flags.isPackage();
        }

    /**
    * Set the accessibility to package private.
    */
    public void setPackage()
        {
        ensureLoaded();
        m_flags.setPackage();
        setModified(true);
        }

    /**
    * Determine if the accessibility is private.
    *
    * @return true if the accessibility is private
    */
    public boolean isPrivate()
        {
        ensureLoaded();
        return m_flags.isPrivate();
        }

    /**
    * Set the accessibility to private.
    */
    public void setPrivate()
        {
        ensureLoaded();
        m_flags.setPrivate();
        setModified(true);
        }


    // ----- accessor:  Static -------------------------------------------

    /**
    * Determine if the Static attribute is set.
    *
    * @return true if Static
    */
    public boolean isStatic()
        {
        ensureLoaded();
        return m_flags.isStatic();
        }

    /**
    * Set the Static attribute.
    *
    * @param fStatic  true to set to Static, false otherwise
    */
    public void setStatic(boolean fStatic)
        {
        ensureLoaded();
        m_flags.setStatic(fStatic);
        setModified(true);
        }


    // ----- accessor:  abstract -------------------------------------------

    /**
    * Determine if the Abstract attribute is set.
    *
    * @return true if Abstract
    */
    public boolean isAbstract()
        {
        ensureLoaded();
        return m_flags.isAbstract();
        }

    /**
    * Set the Abstract attribute.
    *
    * @param fAbstract  true to set to Abstract, false otherwise
    */
    public void setAbstract(boolean fAbstract)
        {
        ensureLoaded();
        m_flags.setAbstract(fAbstract);
        setModified(true);
        }


    // ----- accessor:  final -------------------------------------------

    /**
    * Determine if the Final attribute is set.
    *
    * @return true if Final
    */
    public boolean isFinal()
        {
        ensureLoaded();
        return m_flags.isFinal();
        }

    /**
    * Set the Final attribute.
    *
    * @param fFinal  true to set to Final, false otherwise
    */
    public void setFinal(boolean fFinal)
        {
        ensureLoaded();
        m_flags.setFinal(fFinal);
        setModified(true);
        }


    // ----- accessor:  synchronized -------------------------------------------

    /**
    * Determine if the synchronized attribute is set.
    *
    * @return true if synchronized
    */
    public boolean isSynchronized()
        {
        ensureLoaded();
        return m_flags.isSynchronized();
        }

    /**
    * Set the synchronized attribute.
    *
    * @param fSynchronized  true to set to synchronized, false otherwise
    */
    public void setSynchronized(boolean fSynchronized)
        {
        ensureLoaded();
        m_flags.setSynchronized(fSynchronized);
        setModified(true);
        }


    // ----- accessor:  field -----------------------------------------------

    /**
    * Access a Java .class field structure.
    *
    * @param sName  the field name
    *
    * @return the specified field or null if the field does not exist
    */
    public Field getField(String sName)
        {
        ensureLoaded();
        return (Field) m_tblField.get(sName);
        }

    /**
    * Add a Java .class field structure.
    *
    * @param sName  the field name
    * @param sType  the field type
    *
    * @return  the new field
    */
    public Field addField(String sName, String sType)
        {
        ensureLoaded();
        Field field = new Field(sName, sType);
        m_tblField.put(field.getIdentity(), field);
        setModified(true);
        return field;
        }

    /**
    * Remove a field.
    *
    * @param sName  the field name
    */
    public void removeField(String sName)
        {
        ensureLoaded();
        m_tblField.remove(sName);
        setModified(true);
        }

    /**
    * Access the set of fields.
    *
    * @return an enumeration of fields (not field names)
    */
    public Enumeration getFields()
        {
        ensureLoaded();
        return m_tblField.elements();
        }

    /**
    * Get a field constant for the specified class/interface field.
    *
    * @param sName  the field name
    *
    * @return the specified field constant or null if the field does not exist
    */
    public FieldConstant getFieldConstant(String sName)
        {
        Field field = getField(sName);

        return field == null ? null :
            new FieldConstant(getName(), sName, field.getType());
        }

    // ----- accessor:  method -----------------------------------------------

    /**
    * Access a Java .class method structure.
    *
    * @param sName  the method name
    * @param sSig   the method signature
    *
    * @return the specified method or null if the method does not exist
    */
    public Method getMethod(String sName, String sSig)
        {
        return getMethod(sName + sSig.replace('.', '/'));
        }

    /**
    * Access a Java .class method structure.
    *
    * @param sSig   the complete JVM method signature, including the
    *               method name
    *
    * @return the specified method or null if the method does not exist
    */
    public Method getMethod(String sSig)
        {
        ensureLoaded();
        return (Method) m_tblMethod.get(sSig);
        }

    /**
    * Add a Java .class method structure.
    *
    * @param sSig   the complete JVM method signature, including the
    *               method name
    *
    * @return  the new method
    */
    public Method addMethod(String sSig)
        {
        int of = sSig.indexOf('(');
        return addMethod(sSig.substring(0, of), sSig.substring(of));
        }

    /**
    * Add a Java .class method structure.
    *
    * @param sName  the method name
    * @param sSig   the method signature
    *
    * @return  the new method
    */
    public Method addMethod(String sName, String sSig)
        {
        ensureLoaded();
        Method method = new Method(sName, sSig, m_flags.isInterface());
        m_tblMethod.put(method.getIdentity(), method);
        setModified(true);
        return method;
        }

    /**
    * Remove a method.
    *
    * @param sName  the method name
    * @param sSig   the method signature
    */
    public void removeMethod(String sName, String sSig)
        {
        removeMethod(sName + sSig);
        }

    /**
    * Remove a method.
    *
    * @param sSig   the complete JVM method signature, including the
    *               method name
    */
    public void removeMethod(String sSig)
        {
        ensureLoaded();
        m_tblMethod.remove(sSig);
        setModified(true);
        }

    /**
    * Access the set of methods.
    *
    * @return an enumeration of methods (not method names)
    */
    public Enumeration getMethods()
        {
        ensureLoaded();
        return m_tblMethod.elements();
        }


    /**
    * Get a method constant for the specified method
    *
    * @param sSig  the complete JVM method signature, including the
    *               method name
    *
    * @return the specified method constant or null if the method does not exist
    */
    public MethodConstant getMethodConstant(String sSig)
        {
        Method method = getMethod(sSig);

        return method == null ? null :
            new MethodConstant(getName(), method.getName(), method.getType());
        }

    // ----- accessor:  attribute -------------------------------------------

    /**
    * Access a Java .class attribute structure.
    *
    * @param sName  the attribute name
    *
    * @return the specified attribute or null if the attribute does not exist
    */
    public Attribute getAttribute(String sName)
        {
        ensureLoaded();
        return (Attribute) m_tblAttribute.get(sName);
        }

    /**
    * Add a Java .class attribute structure.
    *
    * @param sName  the attribute name
    *
    * @return  the new attribute
    */
    public Attribute addAttribute(String sName)
        {
        ensureLoaded();

        Attribute attribute;
        if (sName.equals(ATTR_FILENAME))
            {
            attribute = new SourceFileAttribute(this);
            }
        else if (sName.equals(ATTR_DEPRECATED))
            {
            attribute = new DeprecatedAttribute(this);
            }
        else if (sName.equals(ATTR_SYNTHETIC))
            {
            attribute = new SyntheticAttribute(this);
            }
        else if (sName.equals(ATTR_INNERCLASSES))
            {
            attribute = new InnerClassesAttribute(this);
            }
        else if (sName.equals(ATTR_ENCMETHOD))
            {
            attribute = new EnclosingMethodAttribute(this);
            }
        else if (sName.equals(ATTR_SIGNATURE))
            {
            attribute = new SignatureAttribute(this);
            }
        else if (sName.equals(ATTR_RTVISANNOT))
            {
            attribute = new RuntimeVisibleAnnotationsAttribute(this);
            }
        else if (sName.equals(ATTR_RTINVISANNOT))
            {
            attribute = new RuntimeInvisibleAnnotationsAttribute(this);
            }
        else if (sName.equals(ATTR_RTVISTANNOT))
            {
            attribute = new RuntimeVisibleTypeAnnotationsAttribute(this);
            }
        else if (sName.equals(ATTR_RTINVISTANNOT))
            {
            attribute = new RuntimeInvisibleTypeAnnotationsAttribute(this);
            }
        else if (sName.equals(ATTR_BOOTSTRAPMETHODS))
            {
            attribute = new BootstrapMethodsAttribute(this);
            }
        else
            {
            attribute = new Attribute(this, sName);
            }

        m_tblAttribute.put(attribute.getIdentity(), attribute);
        setModified(true);

        return attribute;
        }

    /**
    * Remove a attribute.
    *
    * @param sName  the attribute name
    */
    public void removeAttribute(String sName)
        {
        ensureLoaded();
        m_tblAttribute.remove(sName);
        setModified(true);
        }

    /**
    * Access the set of attributes.
    *
    * @return an enumeration of attributes (not attribute names)
    */
    public Enumeration getAttributes()
        {
        ensureLoaded();
        return m_tblAttribute.elements();
        }


    // ----- accessor:  attribute helpers -----------------------------------

    /**
    * Determine if the class is deprecated.
    *
    * @return true if deprecated, false otherwise
    */
    public boolean isDeprecated()
        {
        ensureLoaded();
        return m_tblAttribute.contains(ATTR_DEPRECATED);
        }

    /**
    * Toggle if the class is deprecated.
    *
    * @param  fDeprecated  pass true to deprecate, false otherwise
    */
    public void setDeprecated(boolean fDeprecated)
        {
        if (fDeprecated)
            {
            addAttribute(ATTR_DEPRECATED);
            }
        else
            {
            removeAttribute(ATTR_DEPRECATED);
            }
        }

    /**
    * Determine if the class is synthetic.
    *
    * @return true if synthetic, false otherwise
    */
    public boolean isSynthetic()
        {
        ensureLoaded();
        return m_tblAttribute.contains(ATTR_SYNTHETIC);
        }

    /**
    * Toggle if the class is synthetic.
    *
    * @param  fSynthetic  pass true to set synthetic, false otherwise
    */
    public void setSynthetic(boolean fSynthetic)
        {
        if (fSynthetic)
            {
            addAttribute(ATTR_SYNTHETIC);
            }
        else
            {
            removeAttribute(ATTR_SYNTHETIC);
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "ClassFile";

    /**
    * Access flags applicable to a class.
    */
    private static final int ACC_CLASS      = AccessFlags.ACC_PUBLIC       |
                                              AccessFlags.ACC_PRIVATE      |
                                              AccessFlags.ACC_PROTECTED    |
                                              AccessFlags.ACC_STATIC       |
                                              AccessFlags.ACC_FINAL        |
                                              AccessFlags.ACC_ABSTRACT;

    /**
    * Access flags applicable to an interface.
    */
    private static final int ACC_INTERFACE  = AccessFlags.ACC_PUBLIC;

    /**
    * Major version of this class.
    */
    private int     m_nVersionMajor = VERSION_MAJOR;

    /**
    * Minor version of this class.
    */
    private int     m_nVersionMinor = VERSION_MINOR;
        
    /**
    * Has the class information been modified?
    */
    private boolean m_fModified;

    /**
    * Is the class in a "loaded" state?
    */
    private boolean m_fLoaded;

    /**
    * The .class structure stored (cached) as a byte array.
    */
    private byte[] m_abClazz;

    /**
    * The constant pool for the class.
    */
    private ConstantPool m_pool;

    /**
    * The fully qualified name of the Java .class being managed by this data
    * structure.
    */
    private ClassConstant m_clzName;

    /**
    * The fully qualified name of the Java .class from which the Java .class
    * being managed by this data structure derives.
    */
    private ClassConstant m_clzSuper;

    /**
    * The class/interface access flags.
    */
    private AccessFlags m_flags;

    /**
    * The interface names implemented by the Java .class.
    */
    private StringTable m_tblInterface;

    /**
    * The fields of the Java .class.
    */
    private StringTable m_tblField;

    /**
    * The methods of the Java .class.
    */
    private StringTable m_tblMethod;

    /**
    * The attributes of the Java .class.
    */
    private StringTable m_tblAttribute;

    /**
    * Three tables contained by the ClassFile storing VM structures which
    * are often manipulated iteratively and identically by the ClassFile.
    */
    private final StringTable[] CONTAINED_TABLE = new StringTable[3];


    // ----- inner classes --------------------------------------------------

    /**
    * A callback interface for swapping out constants in the constant pool.
    *
    * @see ClassFile#resolve(com.tangosol.dev.assembler.ClassFile.Resolver)
    */
    public static interface Resolver
        {
        Constant resolve(Constant constOrig);
        }

    /**
    * An implementation of Resolver which relocates a package.
    *
    * @see ClassFile#resolve(com.tangosol.dev.assembler.ClassFile.Resolver)
    */
    public static class Relocator implements Resolver
        {
        public static final String PACKAGE = "_package/";

        private String sPkg;

        public Relocator(String sPkg)
            {
            if (sPkg.length() > 0)
                {
                sPkg = sPkg.replace('.', '/');
                if (!sPkg.endsWith("/"))
                    {
                    sPkg += '/';
                    }
                }

            this.sPkg = sPkg;
            }

        public Constant resolve(Constant constOrig)
            {
            if (constOrig instanceof UtfConstant)
                {
                UtfConstant utf = (UtfConstant) constOrig;
                String      s   = utf.getValue();
                int         of  = s.indexOf(PACKAGE);
                if (of >= 0)
                    {
                    do
                        {
                        s = s.substring(0, of) + sPkg + s.substring(of + PACKAGE.length());
                        of = s.indexOf(PACKAGE);
                        }
                    while (of >= 0);
                    return new UtfConstant(s);
                    }
                }

            return constOrig;
            }
        }
    }
