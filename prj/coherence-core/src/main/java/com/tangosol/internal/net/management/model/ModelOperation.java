/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management.model;

import javax.management.MBeanOperationInfo;

import java.util.function.BiFunction;

/**
 * A representation of an operation that can be executed
 * on {@link AbstractModel MBean model}.
 *
 * @param <M> the type of the model the operations will
 *            execute on
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public interface ModelOperation<M>
    {
    /**
     * Return the name of the operation.
     *
     * @return the name of the operation
     */
    String getName();

    /**
     * Returns the function to call when the operation is executed.
     *
     * @return the function to call when the operation is executed
     */
    BiFunction<M, Object[], ?> getFunction();

    /**
     * Return the MBeanOperationInfo for this operation.
     *
     * @return the MBeanOperationInfo for this operation
     */
    MBeanOperationInfo getOperation();
    }
