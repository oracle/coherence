/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.tangosol.io.ClassLoaderAware;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.ResolvingObjectInputStream;
import com.tangosol.io.SerializationSupport;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerAware;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import java.util.Arrays;

import javax.json.bind.annotation.JsonbProperty;

/**
 * RemoteConstructor represents both a ClassDefinition for a {@link Remotable}
 * class and the state required to construct an instance of that class.
 * <p>
 * It is used as a wire transport for the {@code Remotable} classes using a
 * combination of {@link Remotable#writeReplace()} and {@link #readResolve()}
 * methods, and enables automatic serialization of all {@link Remotable} objects.
 *
 * @see SerializationSupport
 * @see Remotable#writeReplace()
 *
 * @author hr/as  2015.06.01
 * @since 12.2.1
 */
public class RemoteConstructor<T>
        implements ExternalizableLite, PortableObject, SerializationSupport, SerializerAware
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public RemoteConstructor()
        {
        }

    /**
     * Construct a {@code RemoteConstructor} based on the provided
     * {@link ClassDefinition} and constructor arguments.
     *
     * @param definition  the {@code ClassDefinition} for the {@link Remotable}
     *                    class this {@code RemoteConstructor} represents
     * @param aoArgs      the arguments for this {@code RemoteConstructor}
     */
    public RemoteConstructor(ClassDefinition definition, Object[] aoArgs)
        {
        m_definition = definition;

        for (int i = 0; i < aoArgs.length; i++)
            {
            Object arg = aoArgs[i];
            aoArgs[i] = Lambdas.isLambda(arg)
                        ? Lambdas.ensureRemotable((Serializable) arg)
                        : arg;
            }
        m_aoArgs = aoArgs;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the {@link ClassIdentity} for the {@link Remotable} class this
     * {@code RemoteConstructor} represents.
     *
     * @return the {@code ClassIdentity} for the {@code Remotable} class this
     *         {@code RemoteConstructor} represents
     */
    public ClassIdentity getId()
        {
        return getDefinition().getId();
        }

    /**
     * Return the {@link ClassDefinition} for the {@link Remotable} class this
     * {@code RemoteConstructor} represents.
     *
     * @return the {@code ClassDefinition} for the {@code Remotable} class this
     *         {@code RemoteConstructor} represents
     */
    public ClassDefinition getDefinition()
        {
        return m_definition;
        }

    /**
     * Return the arguments for this {@code RemoteConstructor}.
     *
     * @return the arguments for this {@code RemoteConstructor}
     */
    public Object[] getArguments()
        {
        return m_aoArgs;
        }

    // ----- public methods -------------------------------------------------

    /**
     * Create a new instance of the {@link Remotable} class represented by
     * this {@code RemoteConstructor}.
     *
     * @return a new instance of the {@code Remotable} class
     */
    public T newInstance()
        {
        RemotableSupport support = RemotableSupport.get(getClassLoader());
        return support.realize(this);
        }
    
    /**
     * Return the ClassLoader that should be used to define the Class this RemoteConstructor 
     * represents. The absence of a ClassLoader from the Serializer will result in
     * using the Thread's context ClassLoader.
     *
     * @return an appropriate ClassLoader
     */
    protected ClassLoader getClassLoader()
        {
        ClassLoader loader = m_loader;
        return loader == null ? Base.getContextClassLoader(this) : loader; 
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (o instanceof RemoteConstructor)
            {
            RemoteConstructor<?> that = (RemoteConstructor<?>) o;

            return this == that ||
                   this.getClass() == that.getClass() &&
                   Base.equals(m_definition, that.m_definition) &&
                   Base.equalsDeep(m_aoArgs, that.m_aoArgs);
            }

        return false;
        }

    @Override
    public int hashCode()
        {
        int nHash = m_definition.hashCode();
        nHash = 31 * nHash + Arrays.hashCode(m_aoArgs);
        return nHash;
        }

    @Override
    public String toString()
        {
        return "RemoteConstructor{" +
               "definition=" + m_definition +
               ", arguments=" + Arrays.toString(m_aoArgs) +
               '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_definition = ExternalizableHelper.readObject(in);

        int cArgs = ExternalizableHelper.readInt(in);
        Base.azzert(cArgs < 256, "Unexpected number of constructor arguments.");

        Object[] aoArgs = m_aoArgs = new Object[cArgs];
        for (int i = 0; i < aoArgs.length; i++)
            {
            aoArgs[i] = ExternalizableHelper.readObject(in);
            }
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_definition);

        Object[] aoArgs = m_aoArgs;

        ExternalizableHelper.writeInt(out, aoArgs.length);
        for (Object o : aoArgs)
            {
            ExternalizableHelper.writeObject(out, o);
            }
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_definition  = in.readObject(0);
        m_aoArgs      = in.readArray(1, Object[]::new);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_definition);
        out.writeObjectArray(1, m_aoArgs);
        }

    // ----- SerializationSupport interface ---------------------------------

    /**
     * {@inheritDoc}
     *
     * It provides deserialization support for {@link Remotable}
     * objects by converting this {@code RemoteConstructor} into the appropriate
     * {@code Remotable} instance upon deserialization.
     *
     * @throws NotSerializableException if {@link Lambdas#LAMBDAS_SERIALIZATION_MODE} is {@code true}.
     */
    @Override
    public Object readResolve() throws ObjectStreamException
        {
        // protect against a remote client with dynamic lambdas enabled
        if (!Lambdas.isDynamicLambdas())
            {
            throw new NotSerializableException(RemoteConstructor.class.getName());
            }
        return newInstance();
        }

    // ----- SerializerAware interface --------------------------------------
    
    @Override
    public Serializer getContextSerializer()
        {
        return m_serializer;
        }

    @Override
    public void setContextSerializer(Serializer serializer)
        {
        m_serializer = serializer;
        if (serializer instanceof ClassLoaderAware)
            {
            m_loader = ((ClassLoaderAware) serializer).getContextClassLoader();
            }
        }

    // ----- Serializable methods -------------------------------------------

    /**
     * See {@link Serializable} for documentation on this method.
     *
     * @param inputStream  the input stream
     *
     * @throws NotSerializableException, ClassNotFoundException, IOException
     *
     * @since 12.2.1.4.22
     */
    @java.io.Serial
    private void readObject(ObjectInputStream inputStream) throws ClassNotFoundException, IOException
        {
        if (inputStream instanceof ResolvingObjectInputStream || ExternalizableHelper.s_tloInEHDeserialize.get())
            {
            // deserialization was initiated via ExternalizableHelper; proceed
            inputStream.defaultReadObject();
            }
        else
            {
            // this class is not intended for "external" use
            throw new NotSerializableException();
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The ClassDefinition for the Remotable class.
     * <p>
     * It is expected for the same ClassDefinition to be referenced by many
     * RemoteConstructor instances.
     */
    @JsonbProperty("definition")
    protected ClassDefinition m_definition;

    /**
     * The constructor arguments to use when creating new Remotable instance.
     */
    @JsonbProperty("args")
    protected Object[] m_aoArgs;
    
    /**
     * The Serializer used to deserialize this instance.
     */
    private transient Serializer m_serializer;
    
    /**
     * The context ClassLoader used by the Serializer.
     */
    protected transient ClassLoader m_loader;
    }
