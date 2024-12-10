/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.inject;

/**
 * A class that can inject dependencies into another class.
 * <p>
 * {@link Injector} instances are discovered by the {@link InjectorProvider}
 * using the {@link java.util.ServiceLoader} and used by {@link Injectable}
 * instance to inject dependencies.
 * <p>
 * If multiple instances of {@link Injector} are discovered by the
 * {@link java.util.ServiceLoader} the one with annotated with the
 * {@link javax.annotation.Priority} annotation with the highest
 * value will be used. If all instances discovered have the same
 * or no priority the instance used will be random.
 * <p>
 * If no instances are discovered then no injection will take place
 * for {@link Injectable} classes.
 *
 * @author Jonathan Knight  2020.11.19
 * @since 20.12
 */
public interface Injector
    {
    /**
     * Inject dependencies into the target.
     *
     * @param target  the instance to have dependencies injected.
     */
    void inject(Object target);
    }
