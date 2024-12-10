/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.introspect;

import com.tangosol.io.ClassLoaderAware;

import java.net.URL;

import java.util.Enumeration;
import java.util.Set;

/**
 * ResourceDiscoverer defines the contract for a client to request all
 * resources available from a root and complying to an expression.
 * Implementers will provide their own strategy for discovering resources
 * potentially against different targets.
 * <p>
 * Working from the root implementers will traverse their targets returning
 * all leaf resources that comply to the {@code sExpression}.
 * <p>
 * There may be scenarios in which the parameters of
 * {@link #discover(String, T)} have different meanings, i.e. in some cases
 * the {@code sExpression} may be a regular expression and in others it may
 * be a discriminator to find resources. Such is the case for
 * {@link ClassPathResourceDiscoverer}.
 *
 * @author hr  2011.10.18
 *
 * @since Coherence 12.1.2
 *
 * @param <T> the root element to operate against, hence the ability for this
 *            to be any object with the bounds of this type likely to be
 *            reduced by implementations
 *
 * @see ClassPathResourceDiscoverer
 */
public interface ResourceDiscoverer<T>
        extends ClassLoaderAware
    {
    /**
     * Starting from the {@code root} traverse the target evaluating all leaf
     * resources against {@code sExpression} returning complying resources.
     *
     * @param sExpression  the expression that each leaf-resource must match
     * @param root         the root element to start traversing from
     *
     * @return a number of resources that complied with the
     *         {@code sExpression} and belong to {@code root}
     */
    public Enumeration<URL> discover(String sExpression, T root);

    /**
     * For every {@code root} traverse the target evaluating all leaf
     * resources against {@code sExpression} returning complying resources.
     *
     * @param sExpression  the expression that each leaf-resource must match
     * @param setRoot      the set of root elements to start traversing from
     *
     * @return a number of resources that complied with the
     *         {@code sExpression} and belong to {@code root}
     */
    public Enumeration<URL> discover(String sExpression, Set<T> setRoot);
    }