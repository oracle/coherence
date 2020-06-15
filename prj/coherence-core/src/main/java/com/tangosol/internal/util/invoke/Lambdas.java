/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.oracle.coherence.common.internal.util.CanonicalNames;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.invoke.lambda.AbstractRemotableLambda;
import com.tangosol.internal.util.invoke.lambda.LambdaIdentity;
import com.tangosol.internal.util.invoke.lambda.RemotableLambdaGenerator;
import com.tangosol.internal.util.invoke.lambda.MethodReferenceIdentity;
import com.tangosol.internal.util.invoke.lambda.AnonymousLambdaIdentity;

import java.io.Serializable;

import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;

import java.lang.reflect.Method;

/**
 * Helpers for lambda remoting.
 *
 * @author as  2015.02.18
 * @since 12.2.1
 */
public abstract class Lambdas
    {
    /**
     * Return true if the specified object is a raw lambda.
     *
     * @param o  object to inspect
     *
     * @return true if object is a raw lambda
     */
    public static boolean isLambda(Object o)
        {
        return o != null && isLambdaClass(o.getClass());
        }

    /**
     * Return true if the specified Class represents a raw lambda.
     *
     * @param clz  class to inspect
     *
     * @return true if the class represents a raw lambda
     */
    public static boolean isLambdaClass(Class clz)
        {
        return clz != null && clz.getName().contains("$$Lambda$");
        }

    /**
     * Return a {@link SerializedLambda} based upon the provided lambda, or
     * throw an IllegalArgumentException if the provided lambda is deemed to
     * not be a lambda.
     *
     * @param oLambda  the lambda that the returned SerializedLambda represents
     *
     * @return SerializedLambda representing the provided lambda
     *
     * @throws IllegalArgumentException  if the provided lambda is deemed not to be a lambda
     * @throws IllegalStateException     if an issue arose in creating the SerializedLambda
     */
    public static SerializedLambda getSerializedLambda(Object oLambda)
        {
        if (oLambda == null || !isLambda(oLambda) || !(oLambda instanceof Serializable))
            {
            throw new IllegalArgumentException(
                "Specified object is not an instance of a serializable lambda");
            }

        try
            {
            Class<?> clzLambda = oLambda.getClass();
            Method   method    = clzLambda.getDeclaredMethod("writeReplace");
            method.setAccessible(true);

            return (SerializedLambda) method.invoke(oLambda);
            }
        catch (Exception e)
            {
            throw new IllegalStateException(
                "Unable to extract SerializedLambda from lambda: " + oLambda, e);
            }
        }

    /**
     * Return whether the provided method name is a name used by the JDK to
     * generate lambda implementations in the capturing class.
     *
     * @param sMethodName  the method name to inspect
     *
     * @return whether the provided method name represents a lambda implementation
     */
    public static boolean isLambdaMethod(String sMethodName)
        {
        return sMethodName.startsWith("lambda$");
        }

    /**
     * Return true if the provided {@link SerializedLambda} represents a method
     * reference.
     *
     * @param lambda  the SerializedLambda to inspect
     *
     * @return whether the SerializedLambda represents a method reference
     */
    public static boolean isMethodReference(SerializedLambda lambda)
        {
        return lambda.getImplMethodKind() == MethodHandleInfo.REF_newInvokeSpecial ||
               (!isLambdaMethod(lambda.getImplMethodName()) && lambda.getCapturedArgCount() == 0);
        }

    /**
     * Create a {@link ClassDefinition} for the provided raw Serializable lambda.
     *
     * @param id      the remote class identity
     * @param lambda  the raw Serializable lambda
     * @param loader  the ClassLoader used to load the capturing class containing
     *                the lambda implementation
     *
     * @return a LambdaDefinition that represents the Serializable lambda
     */
    public static ClassDefinition createDefinition(ClassIdentity id, Serializable lambda, ClassLoader loader)
        {
        SerializedLambda lambdaMetadata = getSerializedLambda(lambda);

        if (lambdaMetadata.getImplMethodKind() == MethodHandleInfo.REF_invokeStatic ||
            isMethodReference(lambdaMetadata))
            {
            ClassDefinition definition = new ClassDefinition(
                    id,
                    RemotableLambdaGenerator.createRemoteLambdaClass(id.getName(), lambdaMetadata, loader));

            definition.dumpClass(DUMP_LAMBDAS);

            return definition;
            }
        throw new IllegalArgumentException(
                "The specified lambda is referring to the enclosing class instance " +
                "or its fields and therefore cannot be marshalled across network boundaries (" +
                lambdaMetadata + ")");
        }

    /**
     * Return a {@link LambdaIdentity} based upon the provided
     * {@link SerializedLambda function metadata}.
     *
     * @param lambdaMetadata  function metadata
     * @param loader          ClassLoader used to load the capturing class
     *
     * @return an AbstractLambdaIdentity based upon the provided function metadata
     */
    public static LambdaIdentity createIdentity(SerializedLambda lambdaMetadata, ClassLoader loader)
        {
        return isMethodReference(lambdaMetadata)
               ? new MethodReferenceIdentity(lambdaMetadata, loader)
               : new AnonymousLambdaIdentity(lambdaMetadata, loader);
        }

    /**
     * Return captured arguments as an object array.
     *
     * @param lambdaMetadata  function metadata
     *
     * @return captured arguments
     */
    public static Object[] getCapturedArguments(SerializedLambda lambdaMetadata)
        {
        int      c      = lambdaMetadata.getCapturedArgCount();
        Object[] aoArgs = new Object[c];

        for (int i = 0; i < c; i++)
            {
            aoArgs[i] = lambdaMetadata.getCapturedArg(i);
            }

        return aoArgs;
        }

    /**
     * Ensure that the specified function is an instance of a {@link Remotable}
     * lambda.
     * <p>
     * If the specified function is not a lambda it will be ignored and returned
     * as is, which makes this method safe to use in the cases where the argument
     * could be either a lambda or an instance of a concrete class. Of course,
     * this assumes that the specified {@code function} does not need to implement
     * {@code Remotable} interface because it provides native support for
     * serialization and can be marshaled across process boundaries on its own.
     *
     * @param <T>       the type of the function
     * @param function  the function to convert into {@code Remotable} lambda
     *                  if necessary
     *
     * @return a {@code Remotable} lambda for the specified function, or the
     *         function itself if the specified function is not a lambda
     */
    public static <T extends Serializable> T ensureRemotable(T function)
        {
        if (function instanceof AbstractRemotableLambda || !isLambda(function))
            {
            return function;
            }

        RemotableSupport support = RemotableSupport.get(function.getClass().getClassLoader());
        return support.realize(support.createRemoteConstructor(function));
        }

    /**
     * Return the canonical name for a given lambda object.  If the object is not a lambda,
     * return null.  This is for the Coherence ValueExtractor.
     *
     * @param oLambda  the lambda object
     *
     * @return the lambda's canonical name, or null if the supplied object is not a lambda
     *
     * @since Coherence 19.1.0.0
     */
    public static <T extends Remotable> String getValueExtractorCanonicalName(Object oLambda)
        {
        if (oLambda instanceof AbstractRemotableLambda)
            {
            AbstractRemotableLambda lambda = (AbstractRemotableLambda) oLambda;
            return CanonicalNames.computeValueExtractorCanonicalName(((MethodReferenceIdentity) lambda.getId())
                        .getImplMethod() + CanonicalNames.VALUE_EXTRACTOR_METHOD_SUFFIX, null);
            }

        return null;
        }

    // ---- static members --------------------------------------------------

    /**
     * Path to the file system where generated lambdas should be written; purely for debug.
     */
    private static final String DUMP_LAMBDAS = Config.getProperty("coherence.remotable.dumpLambdas",
                                                                  Config.getProperty("coherence.remotable.dumpAll"));
    }
