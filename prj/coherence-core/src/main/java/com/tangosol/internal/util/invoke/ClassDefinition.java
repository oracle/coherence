/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.json.bind.annotation.JsonbProperty;

/**
 * A {@code ClassDefinition} entirely encapsulates a {@link Remotable} class
 * such that a receiver of a {@code ClassDefinition} will be able to define
 * the class for subsequent use.
 * <p>
 * The {@code ClassDefinition} is absent of instance information, which is
 * required for the instantiation of the class, and is captured by the
 * {@link RemoteConstructor} instance and applied to the {@code ClassDefinition}
 * when calling {@link #createInstance(Object[]) createInstance} method.
 * <p>
 * The common interactions with a ClassDefinition are as follows:
 * <table>
 *     <tr><td>Role</td><td>Method</td></tr>
 *     <tr><td>Function Submitter</td><td>{@link #ClassDefinition(ClassIdentity, byte[])}</td></tr>
 *     <tr><td>Function Receiver - Class Loading Phase</td><td>{@link #setRemotableClass(Class)}</td></tr>
 *     <tr><td>Function Receiver - Execution Phase</td><td>{@link #createInstance(Object[])}</td></tr>
 * </table>
 *
 * @author hr/as  2015.06.01
 * @since 12.2.1
 */
public class ClassDefinition
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public ClassDefinition()
        {
        }

    /**
     * Construct a ClassDefinition with the provided {@link ClassIdentity id}
     * and byte array (representing the {@link Remotable} Class File).
     *
     * @param id       a unique identity for the {@link Remotable} Class
     * @param abClass  a {@link Remotable} Class File byte array
     */
    public ClassDefinition(ClassIdentity id, byte[] abClass)
        {
        m_id      = id;
        m_abClass = abClass;

        // just to please Harvey ;-)
        String sClassName = id.getName();
        Base.azzert(sClassName.length() < 0xFFFF,
                    "The generated class name is too long:\n" + sClassName);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the {@link ClassIdentity identity} of the function.
     *
     * @return the identity of the function
     */
    public ClassIdentity getId()
        {
        return m_id;
        }

    /**
     * Return the Class File byte array.
     *
     * @return the Class File byte array
     */
    public byte[] getBytes()
        {
        return m_abClass;
        }

    /**
     * Return the {@link Remotable} Class this definition represents.
     *
     * @return the {@code Remotable} Class this definition represents
     */
    public Class<? extends Remotable> getRemotableClass()
        {
        return m_clz;
        }

    /**
     * Set the {@link Remotable} Class this definition represents.
     *
     * @param clz  the {@link Remotable} Class this definition represents
     */
    public void setRemotableClass(Class<? extends Remotable> clz)
        {
        m_clz = clz;

        Constructor<?>[] aCtor = clz.getDeclaredConstructors();
        if (aCtor.length == 1)
            {
            try
                {
                MethodType ctorType = MethodType.methodType(void.class, aCtor[0].getParameterTypes());
                m_mhCtor = MethodHandles.publicLookup().findConstructor(clz, ctorType);
                }
            catch (NoSuchMethodException | IllegalAccessException e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }
        }

    /**
     * Create an instance of the {@link Remotable} class represented by this
     * definition.
     *
     * @param aoArgs  an array of constructor arguments
     *
     * @return an instance of the {@code Remotable} class represented by this
     *         definition
     */
    public Object createInstance(Object... aoArgs)
        {
        try
            {
            return getConstructor(aoArgs).invokeWithArguments(aoArgs);
            }
        catch (NoSuchMethodException e)
            {
            // let's try constructors with a matching number of arguments one by one
            Constructor[] aCtors = m_clz.getDeclaredConstructors();
            for (Constructor ctor : aCtors)
                {
                if (ctor.getParameterTypes().length == aoArgs.length)
                    {
                    try
                        {
                        return ctor.newInstance(aoArgs);
                        }
                    catch (InstantiationException | InvocationTargetException |
                           IllegalAccessException | IllegalArgumentException ignore)
                        {
                        }
                    }
                }

            // no matching constructor found, rethrow the exception
            throw Base.ensureRuntimeException(e);
            }
        catch (Throwable t)
            {
            throw Base.ensureRuntimeException(t);
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return a {@link MethodHandle} to the constructor of the {@link Remotable}
     * class represented by this definition.
     * <p>
     * Once constructed, the class may be 'used', which generally translates
     * into the invocation of its methods via some interface.
     *
     * @param aoArgs  the constructor arguments
     *
     * @return a {@code MethodHandle} to the constructor of the {@code Remotable} class
     */
    protected MethodHandle getConstructor(Object[] aoArgs)
            throws NoSuchMethodException
        {
        if (m_mhCtor != null)
            {
            // we have a cached constructor handle, so just return it
            return m_mhCtor;
            }

        // otherwise, we need to find the constructor based on argument types
        // if we do find one, we intentionally do not cache it for future use
        // because, unlike lambdas or scripts, a remotable Class could have
        // several constructor overloads that can be called using multiple
        // RemoteConstructor instances against a single ClassDefinition
        Class[] aParamTypes = ClassHelper.getClassArray(aoArgs);
        try
            {
            // try with primitive types first
            MethodType ctorType = MethodType.methodType(void.class, ClassHelper.unwrap(aParamTypes));
            return MethodHandles.publicLookup().findConstructor(m_clz, ctorType);
            }
        catch (NoSuchMethodException e)
            {
            try
                {
                // and with wrapper types second
                MethodType ctorType = MethodType.methodType(void.class, aParamTypes);
                return MethodHandles.publicLookup().findConstructor(m_clz, ctorType);
                }
            catch (IllegalAccessException e1)
                {
                throw Base.ensureRuntimeException(e1);
                }
            }
        catch (IllegalAccessException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Write the ClassFile represented by this ClassDefinition to the
     * specified directory.
     * <p>
     * If the dump directory is {@code null}, or a path to a class directory
     * can not be created, the ClassFile will not be written to the file system.
     *
     * @param sDir  the root directory for class dumps
     */
    public void dumpClass(String sDir)
        {
        if (sDir != null)
            {
            File    dirDump   = new File(sDir, m_id.getPackage());
            boolean fDisabled = dirDump.isFile() ||
                                  !dirDump.exists() && !dirDump.mkdirs();
            if (!fDisabled)
                {
                try (OutputStream os = new FileOutputStream(new File(dirDump, m_id.getSimpleName() + ".class")))
                    {
                    os.write(m_abClass);
                    }
                catch (IOException ignore)
                    {
                    } // we tried
                }
            }
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (o instanceof ClassDefinition)
            {
            ClassDefinition that = (ClassDefinition) o;

            return this == that ||
                   this.getClass() == that.getClass() &&
                   Base.equals(m_id, that.m_id);
            }

        return false;
        }

    @Override
    public int hashCode()
        {
        return m_id.hashCode();
        }

    @Override
    public String toString()
        {
        return "ClassDefinition{" +
               "id=" + m_id +
               '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_id      = ExternalizableHelper.readObject(in);
        m_abClass = ExternalizableHelper.readByteArray(in);
        validateClassFile(m_abClass);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_id);
        ExternalizableHelper.writeByteArray(out, m_abClass);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_id      = in.readObject(0);
        m_abClass = in.readByteArray(1);
        validateClassFile(m_abClass);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_id);
        out.writeByteArray(1, m_abClass);
        }

    // ----- class-file validation --------------------------------------------

    /**
     * Validate the class-file header and structural byte counts.
     *
     * @param abClass  the class-file bytes
     *
     * @throws IOException if the bytes are not a well-formed class-file shell
     */
    private static void validateClassFile(byte[] abClass)
            throws IOException
        {
        if (abClass == null || abClass.length < CLASS_FILE_MIN_LENGTH)
            {
            rejectClassFile(REASON_INVALID_SHAPE);
            }

        if ((abClass[0] & 0xFF) != 0xCA || (abClass[1] & 0xFF) != 0xFE
                || (abClass[2] & 0xFF) != 0xBA || (abClass[3] & 0xFF) != 0xBE)
            {
            rejectClassFile(REASON_MISSING_MAGIC);
            }

        int nMajor = readU2(abClass, 6);
        // widen this upper bound as new JVM class-file versions ship
        if (nMajor < CLASS_FILE_MAJOR_MIN || nMajor > CLASS_FILE_MAJOR_MAX)
            {
            rejectClassFile(REASON_INVALID_VERSION);
            }

        int of = walkConstantPool(abClass, 10, readU2(abClass, 8));
        of = require(of, 6, abClass.length);
        of = require(of, 2, abClass.length);
        int cInterfaces = readU2(abClass, of - 2);
        of = require(of, cInterfaces * 2, abClass.length);
        of = walkMembers(abClass, of);
        of = walkMembers(abClass, of);
        of = walkAttributes(abClass, of);

        if (of != abClass.length)
            {
            rejectClassFile(REASON_TRAILING_BYTES);
            }
        }

    /**
     * Walk class-file constant-pool entries.
     *
     * @param abClass  the class-file bytes
     * @param of       the first entry offset
     * @param cEntries the constant-pool entry count
     *
     * @return the offset immediately after the constant pool
     *
     * @throws IOException if the pool overruns the byte array
     */
    private static int walkConstantPool(byte[] abClass, int of, int cEntries)
            throws IOException
        {
        for (int i = 1; i < cEntries; i++)
            {
            of = require(of, 1, abClass.length);
            int nTag = abClass[of - 1] & 0xFF;
            switch (nTag)
                {
                case 1: // CONSTANT_Utf8
                    of = require(of, 2, abClass.length);
                    of = require(of, readU2(abClass, of - 2), abClass.length);
                    break;

                case 3:  // CONSTANT_Integer
                case 4:  // CONSTANT_Float
                case 9:  // CONSTANT_Fieldref
                case 10: // CONSTANT_Methodref
                case 11: // CONSTANT_InterfaceMethodref
                case 12: // CONSTANT_NameAndType
                case 17: // CONSTANT_Dynamic
                case 18: // CONSTANT_InvokeDynamic
                    of = require(of, 4, abClass.length);
                    break;

                case 5: // CONSTANT_Long
                case 6: // CONSTANT_Double
                    of = require(of, 8, abClass.length);
                    i++;
                    break;

                case 7:  // CONSTANT_Class
                case 8:  // CONSTANT_String
                case 16: // CONSTANT_MethodType
                case 19: // CONSTANT_Module
                case 20: // CONSTANT_Package
                    of = require(of, 2, abClass.length);
                    break;

                case 15: // CONSTANT_MethodHandle
                    of = require(of, 3, abClass.length);
                    break;

                default:
                    rejectClassFile(REASON_INVALID_SHAPE);
                }
            }
        return of;
        }

    /**
     * Walk field_info or method_info entries.
     *
     * @param abClass  the class-file bytes
     * @param of       the offset of the count field
     * @return the offset immediately after the entries
     *
     * @throws IOException if any entry overruns the byte array
     */
    private static int walkMembers(byte[] abClass, int of)
            throws IOException
        {
        of = require(of, 2, abClass.length);
        int cEntries = readU2(abClass, of - 2);
        for (int i = 0; i < cEntries; i++)
            {
            of = require(of, 8, abClass.length);
            of = walkAttributes(abClass, of, readU2(abClass, of - 2));
            }
        return of;
        }

    /**
     * Walk attribute_info entries.
     *
     * @param abClass  the class-file bytes
     * @param of       the attribute-count offset
     *
     * @return the offset immediately after the attributes
     *
     * @throws IOException if any attribute overruns the byte array
     */
    private static int walkAttributes(byte[] abClass, int of)
            throws IOException
        {
        of = require(of, 2, abClass.length);
        return walkAttributes(abClass, of, readU2(abClass, of - 2));
        }

    /**
     * Walk attribute_info entries.
     *
     * @param abClass     the class-file bytes
     * @param of          the first attribute offset
     * @param cAttributes the attribute count
     *
     * @return the offset immediately after the attributes
     *
     * @throws IOException if any attribute overruns the byte array
     */
    private static int walkAttributes(byte[] abClass, int of, int cAttributes)
            throws IOException
        {
        for (int i = 0; i < cAttributes; i++)
            {
            of = require(of, 6, abClass.length);
            long cb = readU4(abClass, of - 4);
            of = require(of, cb, abClass.length);
            }
        return of;
        }

    /**
     * Require a byte span to fit in the class-file buffer.
     *
     * @param of       the current offset
     * @param cb       the byte count
     * @param cbClass  the buffer length
     *
     * @return the advanced offset
     *
     * @throws IOException if the span overruns the buffer
     */
    private static int require(int of, long cb, int cbClass)
            throws IOException
        {
        if (of < 0 || cb < 0 || of + cb > cbClass)
            {
            rejectClassFile(REASON_INVALID_SHAPE);
            }
        return (int) (of + cb);
        }

    /**
     * Read an unsigned two-byte big-endian value.
     *
     * @param abClass  the class-file bytes
     * @param of       the offset
     *
     * @return the unsigned value
     */
    private static int readU2(byte[] abClass, int of)
        {
        return ((abClass[of] & 0xFF) << 8) | (abClass[of + 1] & 0xFF);
        }

    /**
     * Read an unsigned four-byte big-endian value.
     *
     * @param abClass  the class-file bytes
     * @param of       the offset
     *
     * @return the unsigned value
     */
    private static long readU4(byte[] abClass, int of)
        {
        return ((long) (abClass[of] & 0xFF) << 24)
               | ((long) (abClass[of + 1] & 0xFF) << 16)
               | ((long) (abClass[of + 2] & 0xFF) << 8)
               | (abClass[of + 3] & 0xFF);
        }

    /**
     * Reject invalid class-file bytes.
     *
     * @param sReason  the rejection reason
     *
     * @throws IOException always
     */
    private static void rejectClassFile(String sReason)
            throws IOException
        {
        Logger.warn(String.format("route=class-definition, gate=class-validation, reason=%s", sReason));
        throw new IOException("Invalid class definition: " + sReason);
        }

    // ----- data members ---------------------------------------------------

    /**
     * Minimum class-file length: magic, minor, major, and constant-pool count.
     */
    private static final int CLASS_FILE_MIN_LENGTH = 10;

    /**
     * First supported JVM class-file major version.
     */
    private static final int CLASS_FILE_MAJOR_MIN = 45;

    /**
     * Last supported JVM class-file major version for Java 26.
     */
    private static final int CLASS_FILE_MAJOR_MAX = 70;

    /**
     * Rejection reason for a missing class-file magic header.
     */
    private static final String REASON_MISSING_MAGIC = "missing-magic";

    /**
     * Rejection reason for an unsupported class-file version.
     */
    private static final String REASON_INVALID_VERSION = "invalid-version";

    /**
     * Rejection reason for malformed structural counts.
     */
    private static final String REASON_INVALID_SHAPE = "invalid-shape";

    /**
     * Rejection reason for trailing bytes after the class-file structure.
     */
    private static final String REASON_TRAILING_BYTES = "trailing-bytes";

    /**
     * The {@link Remotable} Class represented by this definition.
     */
    protected transient Class<? extends Remotable> m_clz;

    /**
     * A cached {@link MethodHandle} to the constructor of the remotable Class
     */
    protected transient MethodHandle m_mhCtor;

    /**
     * A unique identity for the {@link Remotable} Class represented by this
     * definition.
     */
    @JsonbProperty("id")
    protected ClassIdentity m_id;

    /**
     * The bytes of the {@link Remotable} Class File.
     */
    @JsonbProperty("code")
    protected byte[] m_abClass;
    }
