/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.inject;

import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.options.WithConfiguration;
import com.tangosol.net.options.WithName;
import com.tangosol.net.options.WithScopeName;

import javax.annotation.Priority;
import javax.inject.Named;

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
        if (scope == null)
            {
            String sName = getName();
            return Coherence.DEFAULT_NAME.equals(sName) ? Coherence.DEFAULT_SCOPE : sName;
            }
        else
            {
            return scope.value();
            }
        }

    @Override
    default int getPriority()
        {
        Priority priority = getClass().getAnnotation(Priority.class);
        return priority == null ? 0 : priority.value();
        }

    @Override
    default Session.Option[] getOptions()
        {
        String        sName      = getName();
        String        sScopeName = getScopeName();
        Class<?>      cls        = getClass();
        ConfigUri     configUri  = cls.getAnnotation(ConfigUri.class);
        String        sConfigUri = configUri == null ? CacheFactoryBuilder.URI_DEFAULT : configUri.value();

        return new Session.Option[]
            {
            WithName.of(sName),
            WithScopeName.of(sScopeName),
            WithConfiguration.using(sConfigUri)};
            }
    }
