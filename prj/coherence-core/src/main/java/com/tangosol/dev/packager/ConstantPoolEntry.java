/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import com.tangosol.dev.component.ComponentClassLoader;
import com.tangosol.dev.component.ComponentException;

import com.tangosol.util.Base;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import java.util.Hashtable;


/**
* This class is used to read and resolve references in the constants
* section of a Java class file for automatically determining the things
* that a class uses or depends on.
*
* TODO use the assembler
*/
public abstract class ConstantPoolEntry
        extends Base
    {
    /**
    * Return the object denoted by the constant reference.
    */
    public Object getReferencedObject()
        {
        return(referencedObject);
        }

    /**
    * Return an array of ConstantPoolEntry that represents the constants
    * referenced in the class-file of the specified Java class in the context
    * of the specified ClassLoader.
    * Note that some elements of the array may be null, since long and
    * double values occupy two slots each.
    */
    public static ConstantPoolEntry[] getClassConstants(String javaClassName, ClassLoader classLoader)
            throws IOException
        {
        // [gg] somehow a signature comes in!
        if (javaClassName.startsWith("L") && javaClassName.endsWith(";"))
            {
            javaClassName = javaClassName.substring(1, javaClassName.length() - 1);
            }

        ///
        /// N.B.  Resource paths used with ClassLoaders should not start with
        ///       a leading slash as they would with Class.getResource().
        ///       The reason for this is that Class.getResource() will prepend
        ///       the class's package name to the front of a resource with a
        ///       relative pathname, but leave an absolute pathname (one with a
        ///       leading slash) alone.  ClassLoader.getResource() does no such
        ///       processing.
        ///

        InputStream in = null;

        if (classLoader instanceof ComponentClassLoader)
            {
            try
                {
                byte[] ab = ((ComponentClassLoader) classLoader).loadClassData(javaClassName);
                if (ab != null)
                    {
                    in = new ByteArrayInputStream(ab);
                    }
                }
            catch (ComponentException e)
                {
                throw new IOException(e.toString());
                }
            }
        else
            {
            String resourcePath  = javaClassName.replace('.', '/') + ".class";
            in = classLoader.getResourceAsStream(resourcePath);
            }

        if (in == null)
            {
            throw new IOException("Cannot find class: " + javaClassName);
            }

        DataInputStream dstream = new DataInputStream(in);

        // Read the magic number and verify it.
        int magicNumber = dstream.readInt();
        if (magicNumber != 0xCafeBabe)
            {
            throw new RuntimeException(javaClassName + ": Bad class-file magic number: " + Integer.toString(magicNumber, 16));
            }

        // read the minor and major version numbers
        int minorVersion = dstream.readUnsignedShort();
        int majorVersion = dstream.readUnsignedShort();

        // read the constant pool
        int constantPoolCount = dstream.readUnsignedShort();

        ConstantPoolEntry constantPool[] = new ConstantPoolEntry[constantPoolCount];

        constantPool[0] = null;
        for (int i = 1; i < constantPoolCount; i++)
            {
            try
                {
                byte tag = (byte)dstream.readUnsignedByte();
                constantPool[i] = ConstantPoolEntry.decode(tag, dstream, constantPool);

                if (tag == CONSTANT_LONG || tag == CONSTANT_DOUBLE)
                    {
                    // All 8-byte constants (longs and doubles) use 2 slots in the
                    // constant pool, so skip the next slot in these cases.
                    i++;
                    constantPool[i] = null;
                    }
                }
            catch (IOException e)
                {
                out(javaClassName + ": constantPool[" + i + "]: " + e.getMessage());
                out(e);
                }
            }

        // Run through the constant pool to resolve things as required.
        for (int i=1; i<constantPoolCount; i++)
            {
            // avoid null pool entries from longs and doubles
            if (constantPool[i] != null)
                {
                constantPool[i].resolve();
                }
            }

        // close the stream
        dstream.close();
        in.close();

        return(constantPool);
        }

    ///
    ///  Internals
    ///

    final byte getTag()
        {
        return(tag);
        }

    /**
    * Resolve the constant to a Class, Method, Field, etc.
    */
    void resolve()
        {
        }

    /**
    * Construct a ConstantPoolEntry
    */
    protected ConstantPoolEntry(byte tag, ConstantPoolEntry pool [])
        {
        this.tag = tag;
        this.pool = pool;
        }

    /**
    * Decode a ConstantPoolEntry from the provided DataInputStream.
    */
    static ConstantPoolEntry decode(byte tag, DataInputStream dstream, ConstantPoolEntry pool[])
            throws IOException
        {
        switch (tag)
            {
            case CONSTANT_CLASS:
                return(new ConstantClass(tag, pool, dstream.readUnsignedShort()));

            case CONSTANT_FIELDREF:
                return(new ConstantFieldRef(tag, pool, dstream.readUnsignedShort(), dstream.readUnsignedShort()));

            case CONSTANT_METHODREF:
                return(new ConstantMethodRef(tag, pool, dstream.readUnsignedShort(), dstream.readUnsignedShort()));

            case CONSTANT_INTERFACE_METHODREF:
                return(new ConstantInterfaceMethodRef(tag, pool, dstream.readUnsignedShort(), dstream.readUnsignedShort()));

            case CONSTANT_STRING:
                return(new ConstantString(tag, pool, dstream.readUnsignedShort()));

            case CONSTANT_INTEGER:
                return(new ConstantInteger(tag, pool, dstream.readInt()));

            case CONSTANT_FLOAT:
                return(new ConstantFloat(tag, pool, dstream.readFloat()));

            case CONSTANT_LONG:
                return(new ConstantLong(tag, pool, dstream.readLong()));

            case CONSTANT_DOUBLE:
                return(new ConstantDouble(tag, pool, dstream.readDouble()));

            case CONSTANT_NAME_AND_TYPE:
                return(new ConstantNameAndType(tag, pool, dstream.readUnsignedShort(), dstream.readUnsignedShort()));

            case CONSTANT_UTF8:
                return(new ConstantUtf8String(tag, pool, dstream.readUTF()));

            case CONSTANT_UNICODE:
                return(new ConstantUnicodeString(tag, pool, readUnicode(dstream)));

            default:
                throw new IOException("Bad constant tag: " + tag);
            }
        }

    /**
    * Decode a Unicode String from the DataInputStream.
    */
    static String readUnicode(DataInputStream dstream)
            throws IOException
        {
        int length = dstream.readUnsignedShort();
        StringBuffer buffer = new StringBuffer(length);
        for (int i=0; i<length; i++)
            {
            buffer.append((char)dstream.readUnsignedByte());
            }
        return(buffer.toString());
        }

    /**
    * Normalize VM class names by converting slashes to dots.
    */
    String normalize(String s1)
        {
        if (s1.indexOf('/') != -1)
            {
            return(s1.replace('/', '.'));
            }
        else
            {
            return(s1);
            }
        }

    final static int CONSTANT_UTF8                =  1;
    final static int CONSTANT_UNICODE             =  2;
    final static int CONSTANT_INTEGER             =  3;
    final static int CONSTANT_FLOAT               =  4;
    final static int CONSTANT_LONG                =  5;
    final static int CONSTANT_DOUBLE              =  6;
    final static int CONSTANT_CLASS               =  7;
    final static int CONSTANT_STRING              =  8;
    final static int CONSTANT_FIELDREF            =  9;
    final static int CONSTANT_METHODREF           = 10;
    final static int CONSTANT_INTERFACE_METHODREF = 11;
    final static int CONSTANT_NAME_AND_TYPE       = 12;

    byte                tag;
    ConstantPoolEntry[] pool;
    Object              referencedObject;

    /**
    * This class represents names in class files, and ensures that only
    * one instance for a given name is used.
    */
    static class Identifier
        {
        /**
        * Find or create an Identifier matching the provided name.
        */
        public static Identifier fromName(String name)
            {
            Identifier id = findName(name);
            if (id == null)
                {
                id = new Identifier(name);
                nameMap.put(name, id);
                }
            return(id);
            }

        private Identifier(String name)
            {
            this.name = name;
            }

        public String toString()
            {
            return(name);
            }

        private static Identifier findName(String name)
            {
            return((Identifier)nameMap.get(name));
            }

        private String name;
        private static Hashtable nameMap = new Hashtable();
        }

    /**
    * Represents a reference to a Java class
    */
    static class ConstantClass
            extends ConstantPoolEntry
        {
        private int nameIndex;

        ConstantClass(byte tag, ConstantPoolEntry pool [], int nameIndex)
            {
            super(tag, pool);
            this.nameIndex = nameIndex;
            }

        // find the Java Class
        void resolve()
            {
            if (referencedObject == null)
                {
                Identifier id = ((ConstantGenericString)pool[nameIndex]).getId();
                referencedObject = new ClassReference(id.toString());
                }
            }

        public String toString()
            {
            return(normalize(pool[nameIndex].toString()));
            }
        }

    /**
    * Represents a reference to a Field or Method or Constructor
    */
    static class ConstantGenericField
            extends ConstantPoolEntry
        {
        int classIndex;
        int nameAndTypeIndex;

        ConstantGenericField(byte tag, ConstantPoolEntry pool [], int classIndex, int nameAndTypeIndex)
            {
            super(tag, pool);
            this.classIndex = classIndex;
            this.nameAndTypeIndex = nameAndTypeIndex;
            }

        public String toString()
            {
            return JavaSignature.declString(getSignature(), getClassEntry() + "." + getName());
            }

        ConstantClass getClassEntry()
            {
            return((ConstantClass)pool[classIndex]);
            }

        String getName()
            {
            ConstantNameAndType nat = (ConstantNameAndType)pool[nameAndTypeIndex];
            return(normalize(nat.getName()));
            }

        String getFullyQualifiedName()
            {
            return(getClassEntry().toString() + "." + getName());
            }

        String getSignature()
            {
            ConstantNameAndType nat = (ConstantNameAndType)pool[nameAndTypeIndex];
            return(nat.getSignature());
            }
        }

    /**
    * Represents a reference to a Field
    */
    static class ConstantFieldRef
            extends ConstantGenericField
        {
        ConstantFieldRef(byte tag, ConstantPoolEntry pool [], int classIndex, int nameAndTypeIndex)
            {
            super(tag, pool, classIndex, nameAndTypeIndex);
            }

        void resolve()
            {
            // TBD: get the referenced Field
            }
        }

    /**
    * Represents a reference to a Method
    */
    static class ConstantMethodRef
            extends ConstantGenericField
        {
        ConstantMethodRef(byte tag, ConstantPoolEntry pool [], int classIndex, int nameAndTypeIndex)
            {
            super(tag, pool, classIndex, nameAndTypeIndex);
            }

        void resolve()
            {
            // TBD: get the referenced Method
            }
        }

    /**
    * Represents a reference to an interface Method
    */
    static class ConstantInterfaceMethodRef
            extends ConstantMethodRef
        {
        ConstantInterfaceMethodRef(byte tag, ConstantPoolEntry pool [], int classIndex, int nameAndTypeIndex)
            {
            super(tag, pool, classIndex, nameAndTypeIndex);
            }
        }

    /**
    * Represents a reference to a String
    */
    static class ConstantString
            extends ConstantPoolEntry
        {
        private int stringIndex;

        ConstantString(byte tag, ConstantPoolEntry pool [], int stringIndex)
            {
            super(tag, pool);
            this.stringIndex = stringIndex;
            }

        public String toString()
            {
            return("\"" + pool[stringIndex].toString() + "\"");
            }

        void resolve()
            {
            if (referencedObject == null)
                {
                referencedObject = pool[stringIndex].toString();
                }
            }
        }

    /**
    * Represents a reference to an int
    */
    static class ConstantInteger
            extends ConstantPoolEntry
        {
        private int value;

        ConstantInteger(byte tag, ConstantPoolEntry pool [], int value)
            {
            super(tag, pool);
            this.value = value;
            }

        public String toString()
            {
            return(Integer.toString(value));
            }

        void resolve()
            {
            if (referencedObject == null)
                {
                referencedObject = value;
                }
            }
        }

    /**
    * Represents a reference to a float
    */
    static class ConstantFloat
            extends ConstantPoolEntry
        {
        private float value;

        ConstantFloat(byte tag, ConstantPoolEntry pool [], float value)
            {
            super(tag, pool);
            this.value = value;
            }

        public String toString()
            {
            return(Float.toString(value));
            }

        void resolve()
            {
            if (referencedObject == null)
                {
                referencedObject = value;
                }
            }
        }

    /**
    * Represents a reference to a long
    */
    static class ConstantLong
            extends ConstantPoolEntry
        {
        private long value;

        ConstantLong(byte tag, ConstantPoolEntry pool [], long value)
            {
            super(tag, pool);
            this.value = value;
            }

        public String toString()
            {
            return("0x" + Long.toString(value, 16) + "L");
            }

        void resolve()
            {
            if (referencedObject == null)
                {
                referencedObject = value;
                }
            }
        }

    /**
    * Represents a reference to a double
    */
    static class ConstantDouble
            extends ConstantPoolEntry
        {
        private double value;

        ConstantDouble(byte tag, ConstantPoolEntry pool [], double value)
            {
            super(tag, pool);
            this.value = value;
            }

        public String toString()
            {
            return(Double.toString(value));
            }

        void resolve()
            {
            if (referencedObject == null)
                {
                referencedObject = value;
                }
            }
        }

    /**
    * Represents a reference to a signature as a name and type.
    */
    static class ConstantNameAndType
            extends ConstantPoolEntry
        {
        private int nameIndex;
        private int signatureIndex;

        ConstantNameAndType(byte tag, ConstantPoolEntry pool [], int nameIndex, int signatureIndex)
            {
            super(tag, pool);
            this.nameIndex = nameIndex;
            this.signatureIndex = signatureIndex;
            }

        public String toString()
            {
            return(getSignature() + " " + getName());
            }

        String getName()
            {
            return(normalize(pool[nameIndex].toString()));
            }

        String getSignature()
            {
            return(pool[signatureIndex].toString());
            }

        Identifier getNameId()
            {
            return(((ConstantGenericString)pool[nameIndex]).getId());
            }

        Identifier getSignatureId()
            {
            return(((ConstantGenericString)pool[signatureIndex]).getId());
            }

        void resolve()
            {
            if (referencedObject == null)
                {
                referencedObject = toString();
                }
            }
        }

    /**
    * Represents a reference to a String
    */
    static class ConstantGenericString
            extends ConstantPoolEntry
        {
        protected String value;
        private Identifier id;

        final Identifier getId()
            {
            if (id == null)
                {
                id = Identifier.fromName(normalize(value));
                }
            return(id);
            }

        ConstantGenericString(byte tag, ConstantPoolEntry pool [], String value)
            {
            super(tag, pool);
            this.value = value;
            }

        public String toString()
            {
            return(value);
            }

        void resolve()
            {
            if (referencedObject == null)
                {
                referencedObject = value;
                }
            }
        }

    /**
    * Represents a reference to a UTF8 String
    */
    static class ConstantUtf8String
            extends ConstantGenericString
        {
        ConstantUtf8String(byte tag, ConstantPoolEntry pool [], String value)
            {
            super(tag, pool, value);
            }
        }

    /**
    * Represents a reference to a Unicode String
    */
    static class ConstantUnicodeString
            extends ConstantGenericString
        {
        ConstantUnicodeString(byte tag, ConstantPoolEntry pool [], String value)
            {
            super(tag, pool, value);
            }
        }

    /**
    * This class is used to render JVM signatures in a more familiar and
    * readable form.
    */
    static class JavaSignature
        {
        String	fullDecl;

        JavaSignature(String name, String mangledSig)
            {
            try
                {
                StringReader strRdr = new StringReader(mangledSig);
                parse(strRdr);
                fullDecl = declString(mangledSig, name);
                }
            catch(IOException e)
                {
                // that'll never really happen
                }
            }

        static String declString(String mangledSig, String name)
            {
            try
                {
                String nameAndType = null;
                StringReader strRdr = new StringReader(mangledSig);
                if (name == null)
                    {
                    nameAndType = nameAndTypeString(strRdr, "");
                    }
                else
                    {
                    nameAndType = nameAndTypeString(strRdr, " " + name);
                    }
                return(nameAndType);
                }
            catch (IOException e)
                {
                return(null);
                }
            }

        static String nameAndTypeString(StringReader strRdr, String name)
                throws IOException
            {
            int byteVal = strRdr.read();

            switch (byteVal)
                {
                case 'B':
                    return("byte" + name);

                case 'C':
                    return("char" + name);

                case 'D':
                    return("double" + name);

                case 'F':
                    return("float" + name);

                case 'I':
                    return("int" + name);

                case 'J':
                    return("long" + name);

                case 'S':
                    return("short" + name);

                case 'V':
                    return("void" + name);

                case 'Z':
                    return("boolean" + name);

                case '[':
                    return(nameAndTypeString(strRdr, name) + "[]");

                case 'L':
                    {
                    // class name terminated by semicolon
                    StringBuffer buf = new StringBuffer();
                    int c = strRdr.read();
                    while ((c != -1) && (c != ';'))
                        {
                        // translate slashes in class names to dots
                        buf.append((char)((c == '/') ? '.' : c));
                        c = strRdr.read();
                        }
                    return(buf + name);
                    }

                case '(':
                    {
                    String args = "(";
                    String arg;
                    while ((arg = nameAndTypeString(strRdr,"")) != null)
                        {
                        if (args.length() > 1)
                            {
                            args += ", ";
                            }
                        args += arg;
                        }
                    args += ")";
                    return(nameAndTypeString(strRdr, "") + name + args);
                    }

                case ')':
                    // only happens during a argument list parse
                    return(null);

                default:
                    throw new IOException();
                }
            }

        /**
        * Parse the JVM signature and return number of words represented by
        * the type read.
        */
        int parse(StringReader strRdr)
                throws IOException
            {
            int byteVal = strRdr.read();

            switch (byteVal)
                {
                case 'B':	// byte
                case 'C':	// char
                case 'I':	// int
                case 'F':	// float
                case 'S':	// short
                case 'Z':	// boolean
                    return(1);

                case 'D':	// double
                case 'J':	// long
                    return(2);

                case 'V':	// void
                    return(0);

                case '[':	// array
                    parse(strRdr);	// read and discard element type
                    return(1);

                case 'L':
                    { // class name terminated by semicolon
                    int c = strRdr.read();
                    while ((c != -1) && (c != ';'))
                        {
                        c = strRdr.read();
                        }
                    return(1);
                    }

                case '(':
                    {
                    int arg;
                    while ((arg = parse(strRdr)) != 0)
                        {
                        // nothing more needs to be done
                        }
                    // return the number of words in the result type
                    return(parse(strRdr));
                    }

                case ')':
                    // only happens during a argument-list parse
                    return(0);

                default:
                    throw new IOException();
                }
            }
        }
    }
