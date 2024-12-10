/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util.invoke.lambda;

import com.tangosol.internal.util.ExceptionHelper;
import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.SerializationSupport;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.HashHelper;

import javax.json.bind.annotation.JsonbProperty;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.io.ObjectStreamException;

import java.lang.ReflectiveOperationException;

import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Objects;

/**
 * Serialized form of a static lambda expression.
 * <p>
 * The properties of this class represent the information that is present at the
 * lambda factory site, including static metafactory arguments such as the
 * identity of the primary functional interface method and the identity of the
 * implementation method, as well as dynamic metafactory arguments such as values
 * captured from the lexical scope at the time of lambda capture.
 *
 * @see SerializationSupport
 * @see com.tangosol.internal.util.invoke.Remotable#writeReplace()
 * @see Lambdas#ensureSerializable(Object)
 *
 * @author as  2014.07.02
 * @author jf  2020.06.16
 *
 * @since 14.1.1.0.2
 */
public class StaticLambdaInfo<T>
    implements ExternalizableLite, PortableObject, SerializationSupport
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public StaticLambdaInfo()
        {
        }

    /**
     * Create a {@code StaticLambdaInfo} from the low-level information present at the
     * lambda factory site.
     *
     * @param  clz       lamdba class
     * @param  function  lambda function
     */
    public StaticLambdaInfo(Class<T> clz, T function)
        {
        SerializedLambda lambda = Lambdas.getSerializedLambda(function);

        m_sCapturingClass                     = lambda.getCapturingClass();
        m_sFunctionalInterfaceClass           = lambda.getFunctionalInterfaceClass();
        m_sFunctionalInterfaceMethodName      = lambda.getFunctionalInterfaceMethodName();
        m_sFunctionalInterfaceMethodSignature = lambda.getFunctionalInterfaceMethodSignature();
        m_nImplMethodKind                     = lambda.getImplMethodKind();
        m_sImplClass                          = lambda.getImplClass();
        m_sImplMethodName                     = lambda.getImplMethodName();
        m_sImplMethodSignature                = lambda.getImplMethodSignature();
        m_sInstantiatedMethodType             = lambda.getInstantiatedMethodType();
        m_aoCapturedArgs                      = getCapturedArguments(lambda);
        }

    // ----- StaticLambdaInfo methods ---------------------------------------

    /**
     * Get the name of the class that captured this lambda.
     *
     * @return the name of the class that captured this lambda
     */
    public String getCapturingClass()
        {
        return m_sCapturingClass;
        }

    /**
     * Get the name of the invoked type to which this lambda has been converted
     *
     * @return the name of the functional interface class to which this lambda
     * has been converted
     */
    public String getFunctionalInterfaceClass()
        {
        return m_sFunctionalInterfaceClass;
        }

    /**
     * Get the name of the primary method for the functional interface to which
     * this lambda has been converted.
     *
     * @return the name of the primary methods of the functional interface
     */
    public String getFunctionalInterfaceMethodName()
        {
        return m_sFunctionalInterfaceMethodName;
        }

    /**
     * Get the signature of the primary method for the functional interface to
     * which this lambda has been converted.
     *
     * @return the signature of the primary method of the functional interface
     */
    public String getFunctionalInterfaceMethodSignature()
        {
        return m_sFunctionalInterfaceMethodSignature;
        }

    /**
     * Get the name of the class containing the implementation method.
     *
     * @return the name of the class containing the implementation method
     */
    public String getImplClass()
        {
        return m_sImplClass;
        }

    /**
     * Get the name of the implementation method.
     *
     * @return the name of the implementation method
     */
    public String getImplMethodName()
        {
        return m_sImplMethodName;
        }

    /**
     * Get the signature of the implementation method.
     *
     * @return the signature of the implementation method
     */
    public String getImplMethodSignature()
        {
        return m_sImplMethodSignature;
        }

    /**
     * Get the method handle kind (see {@link MethodHandleInfo}) of the
     * implementation method.
     *
     * @return the method handle kind of the implementation method
     */
    public int getImplMethodKind()
        {
        return m_nImplMethodKind;
        }

    /**
     * Get the signature of the primary functional interface method after type
     * variables are substituted with their instantiation from the capture
     * site.
     *
     * @return the signature of the primary functional interface method after
     * type variable processing
     */
    public final String getInstantiatedMethodType()
        {
        return m_sInstantiatedMethodType;
        }

    /**
     * Get dynamic arguments to the lambda capture site.
     *
     * @return the dynamic arguments to the lambda capture site
     */
    public Object[] getCapturedArgs()
        {
        return m_aoCapturedArgs;
        }

    /**
     * Get the count of dynamic arguments to the lambda capture site.
     *
     * @return the count of dynamic arguments to the lambda capture site
     */
    public int getCapturedArgCount()
        {
        return m_aoCapturedArgs.length;
        }

    // ----- SerializationSupport interface ---------------------------------

    /**
     * {@inheritDoc}
     *
     * It provides deserialization support for {@link StaticLambdaInfo}
     * objects by converting this {@code StaticLambdaInfo} into a lambda.
     */
    @Override
    public Object readResolve() throws ObjectStreamException
        {
        String           sName            = m_sCapturingClass.replace('/', '.');
        SerializedLambda serializedLambda = null;
        try
            {
            // optimization: inline call to createLambda(toSerializedLambda(Base.getContextClassLoader(this))) to avoid calling loadClass twice.
            final Class clzCapturing = Base.getContextClassLoader(this).loadClass(sName);

            serializedLambda =  new SerializedLambda(
                    clzCapturing,
                    m_sFunctionalInterfaceClass,
                    m_sFunctionalInterfaceMethodName,
                    m_sFunctionalInterfaceMethodSignature,
                    m_nImplMethodKind,
                    m_sImplClass,
                    m_sImplMethodName,
                    m_sImplMethodSignature,
                    m_sInstantiatedMethodType,
                    m_aoCapturedArgs);

            // inlined from private SerializedLambda.readResolve method to avoid requiring --add-opens java.base/java.lang.invoke=com.oracle.coherence JPMS.
            @SuppressWarnings("removal")
            Method deserialize = AccessController.doPrivileged(new PrivilegedExceptionAction<>() {
                @Override
                public Method run() throws Exception
                    {
                    // Since all JDK generated lambda methods are private, applications using Coherence distributed lambda
                    // must open themselves to module com.oracle.coherence for reflection call below to succeed.
                    Method m = clzCapturing.getDeclaredMethod("$deserializeLambda$", SerializedLambda.class);
                    m.setAccessible(true);
                    return m;
                    }
                });

            return deserialize.invoke(null, serializedLambda);
            }
        catch (ClassNotFoundException e)
            {
            throw new RuntimeException("Failed to deserialize static lambda " +
                                       m_sImplClass + "$" + m_sImplMethodName + m_sImplMethodSignature +
                                       " due to missing context class " + sName + ".", e);
            }
        catch (ReflectiveOperationException e)
            {
            throw ExceptionHelper.createInvalidObjectException("Exception resolving static lambda " + serializedLambda, e);
            }
        catch (PrivilegedActionException e)
            {
            throw Base.ensureRuntimeException(e.getCause(), "Exception in StaticLambdaInfo.readResolve processing " + serializedLambda);
            }
        }
    
    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sCapturingClass                     = ExternalizableHelper.readSafeUTF(in);
        m_sFunctionalInterfaceClass           = ExternalizableHelper.readSafeUTF(in);
        m_sFunctionalInterfaceMethodName      = ExternalizableHelper.readSafeUTF(in);
        m_sFunctionalInterfaceMethodSignature = ExternalizableHelper.readSafeUTF(in);
        m_nImplMethodKind                     = ExternalizableHelper.readInt(in);
        m_sImplClass                          = ExternalizableHelper.readSafeUTF(in);
        m_sImplMethodName                     = ExternalizableHelper.readSafeUTF(in);
        m_sImplMethodSignature                = ExternalizableHelper.readSafeUTF(in);
        m_sInstantiatedMethodType             = ExternalizableHelper.readSafeUTF(in);

        int cCapturedArgs = ExternalizableHelper.readInt(in);
        Base.azzert(cCapturedArgs < 256, "Unexpected number of captured arguments.");

        Object[] aoCapturedArgs = cCapturedArgs == 0 ? EMPTY_OBJECT_ARRAY : new Object[cCapturedArgs];

        for (int i = 0; i < cCapturedArgs; i++)
            {
            aoCapturedArgs[i] = ExternalizableHelper.readObject(in);
            }
        m_aoCapturedArgs = aoCapturedArgs;
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        Object[] aoCapturedArgs = m_aoCapturedArgs;
        int      cCapturedArgs  = aoCapturedArgs == null ? 0 : aoCapturedArgs.length;

        ExternalizableHelper.writeSafeUTF(out, m_sCapturingClass);
        ExternalizableHelper.writeSafeUTF(out, m_sFunctionalInterfaceClass);
        ExternalizableHelper.writeSafeUTF(out, m_sFunctionalInterfaceMethodName);
        ExternalizableHelper.writeSafeUTF(out, m_sFunctionalInterfaceMethodSignature);
        ExternalizableHelper.writeInt    (out, m_nImplMethodKind);
        ExternalizableHelper.writeSafeUTF(out, m_sImplClass);
        ExternalizableHelper.writeSafeUTF(out, m_sImplMethodName);
        ExternalizableHelper.writeSafeUTF(out, m_sImplMethodSignature);
        ExternalizableHelper.writeSafeUTF(out, m_sInstantiatedMethodType);
        ExternalizableHelper.writeInt    (out, cCapturedArgs);
        for (int i = 0; i < cCapturedArgs; i++)
            {
            ExternalizableHelper.writeObject(out, m_aoCapturedArgs[i]);
            }
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader reader) throws IOException
        {
        m_sCapturingClass                     = reader.readString(0);
        m_sFunctionalInterfaceClass           = reader.readString(1);
        m_sFunctionalInterfaceMethodName      = reader.readString(2);
        m_sFunctionalInterfaceMethodSignature = reader.readString(3);
        m_nImplMethodKind                     = reader.readInt(4);
        m_sImplClass                          = reader.readString(5);
        m_sImplMethodName                     = reader.readString(6);
        m_sImplMethodSignature                = reader.readString(7);
        m_sInstantiatedMethodType             = reader.readString(8);
        m_aoCapturedArgs                      = reader.readArray(9, Object[]::new);
        }

    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeString(0, m_sCapturingClass);
        writer.writeString(1, m_sFunctionalInterfaceClass);
        writer.writeString(2, m_sFunctionalInterfaceMethodName);
        writer.writeString(3, m_sFunctionalInterfaceMethodSignature);
        writer.writeInt(4, m_nImplMethodKind);
        writer.writeString(5, m_sImplClass);
        writer.writeString(6, m_sImplMethodName);
        writer.writeString(7, m_sImplMethodSignature);
        writer.writeString(8, m_sInstantiatedMethodType);
        writer.writeObjectArray(9, m_aoCapturedArgs);
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public boolean equals(Object oThat)
        {
        if (this == oThat)
            {
            return true;
            }
        if (oThat == null || getClass() != oThat.getClass())
            {
            return false;
            }

        StaticLambdaInfo info = (StaticLambdaInfo) oThat;

        return m_nImplMethodKind == info.m_nImplMethodKind &&
            Base.equals(m_sCapturingClass, info.m_sCapturingClass) &&
            Base.equals(m_sFunctionalInterfaceClass,info.m_sFunctionalInterfaceClass) &&
            Base.equals(m_sFunctionalInterfaceMethodName, m_sFunctionalInterfaceMethodName) &&
            Base.equals(m_sFunctionalInterfaceMethodSignature, m_sFunctionalInterfaceMethodSignature) &&
            Base.equals(m_sImplClass, m_sImplClass) &&
            Base.equals(m_sImplMethodName, m_sImplMethodName) &&
            Base.equals(m_sImplMethodSignature, m_sImplMethodSignature) &&
            Base.equals(m_sInstantiatedMethodType, m_sInstantiatedMethodType);
        }

    @Override
    public int hashCode()
        {
        int nResult = HashHelper.hash(m_sCapturingClass.hashCode(), 31);

        nResult = HashHelper.hash(m_sFunctionalInterfaceClass.hashCode(), nResult);
        nResult = HashHelper.hash(m_sFunctionalInterfaceMethodName.hashCode(), nResult);
        nResult = HashHelper.hash(m_sFunctionalInterfaceMethodSignature.hashCode(), nResult);
        nResult = HashHelper.hash(m_sImplClass.hashCode(), nResult);
        nResult = HashHelper.hash(m_sImplMethodName.hashCode(), nResult);
        nResult = HashHelper.hash( m_sImplMethodSignature.hashCode(), nResult);
        nResult = HashHelper.hash(m_nImplMethodKind, nResult);
        nResult = HashHelper.hash( m_sInstantiatedMethodType.hashCode(), nResult);
        return nResult;
        }

    @Override
    public String toString()
        {
        return "StaticLambdaInfo{" +
            "capturingClass='" + m_sCapturingClass + '\'' +
            ", functionalInterfaceClass='" + m_sFunctionalInterfaceClass + '\'' +
            ", functionalInterfaceMethodName='" + m_sFunctionalInterfaceMethodName + '\'' +
            ", functionalInterfaceMethodSignature='" + m_sFunctionalInterfaceMethodSignature + '\'' +
            ", implClass='" + m_sImplClass + '\'' +
            ", implMethodName='" + m_sImplMethodName + '\'' +
            ", implMethodSignature='" + m_sImplMethodSignature + '\'' +
            ", implMethodKind=" + m_nImplMethodKind +
            ", instantiatedMethodType='" + m_sInstantiatedMethodType + '\'' +
            ", capturedArgs=" + Arrays.toString(m_aoCapturedArgs) +
            '}';
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Create a lambda from the provided {@link SerializedLambda}.
     *
     * @param serializedLambda  Java serialization wire format for static lambda
     *
     * @return lambda created from {@link SerializedLambda}
     *
     * @throws IllegalArgumentException  if parameter is null
     * @throws IllegalStateException     if an issue arose in creating the lambda
     *
     * @since 14.1.1.0.2
     */
    @Deprecated
    protected static Object createLambda(SerializedLambda serializedLambda)
        {
        Objects.requireNonNull(serializedLambda);

        // inefficient to use this method now, only keeping for backwards compatibility.
        // this code is inlined in #readResolve() to avoid having to loadClass twice.
        try
            {
            Method deserialize = AccessController.doPrivileged(new PrivilegedExceptionAction<Method>()
                {
                public Method run() throws Exception
                    {
                    String sName = serializedLambda.getCapturingClass().replace('/', '.');
                    Method m     = Base.getContextClassLoader(serializedLambda).loadClass(sName)
                                        .getDeclaredMethod("$deserializeLambda$", SerializedLambda.class);
                    m.setAccessible(true);
                    return m;
                    }
                });
            return deserialize.invoke(null, serializedLambda);
            }
        catch (ReflectiveOperationException e)
            {
            throw new RuntimeException(ExceptionHelper.createInvalidObjectException("Exception resolving static lambda " + serializedLambda, e));
            }
        catch (PrivilegedActionException e)
            {
            throw Base.ensureRuntimeException(e.getCause(), "Exception in StaticLambdaInfo.readResolve processing " + serializedLambda);
            }
        catch (Exception e)
            {
            throw new RuntimeException("Exception resolving static lambda " + serializedLambda, e);
            }
        }

    /**
     * Return captured arguments as an array;
     *
     * @param lambda  serialized lambda containing captured arguments
     *
     * @return captured arguments as an array
     */
    private static Object[] getCapturedArguments(SerializedLambda lambda)
        {
        Object[] args = new Object[lambda.getCapturedArgCount()];
        for (int i = 0; i < args.length; i++)
            {
            args[i] = lambda.getCapturedArg(i);
            }
        return args;
        }

    /**
     * Convert this object to a SerializedLambda instance.
     *
     * @param loader  class loader to use
     *
     * @return  a SerializedLambda instance
     */
    protected SerializedLambda toSerializedLambda(ClassLoader loader)
        {
        String sName = m_sCapturingClass.replace('/', '.');

        try
            {
            return new SerializedLambda(
                loader.loadClass(sName),
                m_sFunctionalInterfaceClass,
                m_sFunctionalInterfaceMethodName,
                m_sFunctionalInterfaceMethodSignature,
                m_nImplMethodKind,
                m_sImplClass,
                m_sImplMethodName,
                m_sImplMethodSignature,
                m_sInstantiatedMethodType,
                m_aoCapturedArgs);
            }
        catch (ClassNotFoundException e)
            {
            throw new RuntimeException("Failed to deserialize static lambda " +
                m_sImplClass + "$" + m_sImplMethodName + m_sImplMethodSignature +
                " due to missing context class " + sName + ".", e);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * For Java serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Empty object array.
     */
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    // ---- data members ----------------------------------------------------

    /**
     * The name of the capturing class.
     */
    @JsonbProperty("capturingClass")
    protected String m_sCapturingClass;

    /**
     * The name of the functional interface class.
     */
    @JsonbProperty("functionalInterfaceClass")
    protected String m_sFunctionalInterfaceClass;

    /**
     * The name of the functional interface method.
     */
    @JsonbProperty("functionalInterfaceMethodName")
    protected String m_sFunctionalInterfaceMethodName;

    /**
     * The signature of the functional interface method.
     */
    @JsonbProperty("functionalInterfaceMethodSignature")
    protected String m_sFunctionalInterfaceMethodSignature;

    /**
     * The name of the implementation class.
     */
    @JsonbProperty("implClass")
    protected String m_sImplClass;

    /**
     * The name of the implementation method.
     */
    @JsonbProperty("implMethodName")
    protected String m_sImplMethodName;

    /**
     * The signature of the implementation method.
     */
    @JsonbProperty("implMethodSignature")
    protected String m_sImplMethodSignature;

    /**
     * The kind of implementation method.
     */
    @JsonbProperty("implMethodKind")
    protected int m_nImplMethodKind;

    /**
     * The signature of the instantiated method.
     */
    @JsonbProperty("instantiatedMethodType")
    protected String m_sInstantiatedMethodType;

    /**
     * Captured arguments.
     */
    @JsonbProperty("capturedArgs")
    protected Object[] m_aoCapturedArgs;
    }
