/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.Coherence;
import com.tangosol.net.SessionConfiguration;

import javax.annotation.Priority;

import javax.inject.Named;

import java.util.Optional;

/**
 * An interface that should be implemented by custom Coherence scoped session
 * configurations in order to enable their discovery and automatic initialization
 * at startup.
 * <p>
 * Each class implementing this interface must be annotated with
 * {@link Named @Named} annotation representing the name of the scope being
 * initialized, and can optionally be annotated with {@link ConfigUri @ConfigUri}
 * annotation if a non-default configuration resource should be used.
 * <p>
 * If annotated with the {@link Scope @Scope} annotation the scope value will be used
 * for the session scope, otherwise the session name will also be used as the scope.
 *
 * @author Aleks Seovic  2020.06.15
 * @since 20.06
 */
public interface SessionInitializer
        extends SessionConfiguration
    {
    @Override
    default String getName()
        {
        Named named = getClass().getAnnotation(Named.class);
        return named == null ? Coherence.DEFAULT_NAME : named.value();
        }

    @Override
    default String getScopeName()
        {
        Scope scope = getClass().getAnnotation(Scope.class);
        return scope == null ? Coherence.DEFAULT_SCOPE :  scope.value();
        }

    @Override
    default int getPriority()
        {
        Priority priority = getClass().getAnnotation(Priority.class);
        return priority == null ? 0 : priority.value();
        }

    @Override
    default Optional<String> getConfigUri()
        {
        ConfigUri configUri = getClass().getAnnotation(ConfigUri.class);
        return configUri == null ? Optional.of(CacheFactoryBuilder.URI_DEFAULT) : Optional.of(configUri.value());
        }
    }
