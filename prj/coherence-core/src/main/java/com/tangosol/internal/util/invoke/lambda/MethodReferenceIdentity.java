/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke.lambda;

import com.tangosol.dev.assembler.Method;

import com.tangosol.util.Base;

import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;

/**
 * {@code MethodReferenceIdentity} is a specialization of {@link LambdaIdentity}
 * that is used to identify method reference-based lambdas.
 * <p>
 * Unlike {@link AnonymousLambdaIdentity}, which can rely on uniqueness based
 * upon three characteristics (capturing class, method name, and MD5(capturing class),
 * method references must employ greater scrutiny in deriving uniqueness as the
 * SAM implementations must also be taken into consideration.
 * <p>
 * Consider the two method references below which should not be deemed the same:
 * <pre><code>
 *     Supplier<T>         s = T::new;
 *     Function<String, T> s = T::new;
 * </code></pre>
 *
 * @author hr/as  2015.06.01
 * @since 12.2.1
 *
 * @see AnonymousLambdaIdentity
 */
public class MethodReferenceIdentity
        extends LambdaIdentity
    {
    // ----- constructors -----------------------------------------------

    /**
     * Default constructor; used for deserialization.
     */
    public MethodReferenceIdentity()
        {
        }

    /**
     * Construct a MethodReferenceIdentity that represents the provided
     * {@link SerializedLambda function metadata}.
     *
     * @param lambdaMetadata  function metadata
     * @param loader          ClassLoader used to load the capturing class file
     */
    public MethodReferenceIdentity(SerializedLambda lambdaMetadata, ClassLoader loader)
        {
        super(getInvokeClass(lambdaMetadata),
              lambdaMetadata.getImplMethodName(),
              createVersion(lambdaMetadata));
        }

    // ----- private constants ----------------------------------------------

    /**
     * Return the internal class name for the target of the invoke instruction.
     *
     * @param lambdaMetadata  function metadata
     *
     * @return the internal class name for the target of the invoke instruction
     */
    private static String getInvokeClass(SerializedLambda lambdaMetadata)
        {
        int nInvokeKind = lambdaMetadata.getImplMethodKind();

        switch (nInvokeKind)
            {
            case MethodHandleInfo.REF_invokeInterface:
            case MethodHandleInfo.REF_invokeVirtual:
                {
                String sDestType = Method.toTypes(lambdaMetadata.getInstantiatedMethodType())[1];

                return sDestType.charAt(0) == 'L'
                        ? sDestType.substring(1, sDestType.length() - 1)
                        : sDestType;
                }
            default:
                return lambdaMetadata.getImplClass();
            }
        }

    /**
     * Create unique version string for the specified lambda.
     *
     * @param lambdaMetadata  function metadata
     *
     * @return a unique version string for the specified lambda
     */
    private static String createVersion(SerializedLambda lambdaMetadata)
        {
        String sReferredMethod = String.format(METHOD_REF_FORMAT,
                                          getInvokeClass(lambdaMetadata),
                                          lambdaMetadata.getImplMethodName(),
                                          lambdaMetadata.getImplMethodSignature());
        String sSAMImpl        = String.format(METHOD_REF_FORMAT,
                                          lambdaMetadata.getFunctionalInterfaceClass(),
                                          lambdaMetadata.getFunctionalInterfaceMethodName(),
                                          lambdaMetadata.getFunctionalInterfaceMethodSignature());

        return Base.toHex(md5(sReferredMethod + "$" + sSAMImpl));
        }

    /**
     * String format used to describe the method reference and SAM.
     */
    private static final String METHOD_REF_FORMAT  = "%s.%s:%s";
    }
