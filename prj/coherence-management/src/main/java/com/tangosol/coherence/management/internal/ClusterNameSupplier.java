/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal;

import org.glassfish.jersey.internal.inject.AbstractBinder;

import org.glassfish.jersey.process.internal.RequestScoped;

import java.util.Set;

import java.util.function.Supplier;

/**
 * A supplier of sets of cluster names.
 * <p>
 * This class is used when management over REST is deployed
 * in a multi-cluster environment, for example WebLogic.
 *
 * @author Jonathan Knight  2022.01.25
 * @since 22.06
 */
@FunctionalInterface
public interface ClusterNameSupplier
        extends Supplier<Set<String>>
    {

    @Override
    Set<String> get();

    // ----- inner class: Binder --------------------------------------------

    /**
     * An {@link AbstractBinder} to bind a {@link ClusterNameSupplier}
     * to a resource.
     */
    class Binder extends AbstractBinder
        {
        // ----- constructors -----------------------------------------------

        public Binder(ClusterNameSupplier supplier)
            {
            f_supplier = supplier;
            }

        // ----- AbstractBinder methods -------------------------------------

        @Override
        protected void configure()
            {
            bindFactory(new Supplier<ClusterNameSupplier>()
                    {
                    @RequestScoped
                    public ClusterNameSupplier get()
                        {
                        return f_supplier;
                        }
                    })
                .to(ClusterNameSupplier.class);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link ClusterNameSupplier} to bind.
         */
        private final ClusterNameSupplier f_supplier;
        }
    }
