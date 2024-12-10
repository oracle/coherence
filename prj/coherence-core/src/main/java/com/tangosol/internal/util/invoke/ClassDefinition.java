/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

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
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_id);
        out.writeByteArray(1, m_abClass);
        }

    // ----- data members ---------------------------------------------------

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
