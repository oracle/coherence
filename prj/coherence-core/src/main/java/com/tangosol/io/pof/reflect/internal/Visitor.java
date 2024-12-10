/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect.internal;

/**
 * Visitor pattern description. This pattern implementation is targeted at
 * builders that require {@link Class} information.
 *
 * @author hr
 *
 * @since 3.7.1
 *
 * @param <B> the type to pass to the visitor
 */
public interface Visitor<B>
    {
    /**
     * Visit the given builder {@code B} and optionally mutate it using
     * information contained within the given Class.
     *
     * @param builder  the builder being visited
     * @param clz      the Class used to enrich the builder
     * @param <C>      the Class type being visited
     */
    public <C> void visit(B builder, Class<C> clz);

    // ----- inner interface: Recipient -------------------------------------

    /**
     * A recipient informs a visitor of it's willingness to be visited.
     *
     * @param <B> the type to pass to the visitor
     */
    public interface Recipient<B>
        {
        /**
         * Accept the given visitor.
         * 
         * @param visitor  Visitor that is requesting to visit this recipient
         * @param clz      the Class that can be used by the visitor
         * @param <C>      the Class type being visited
         */
        public <C> void accept(Visitor<B> visitor, Class<C> clz);
        }
    }
