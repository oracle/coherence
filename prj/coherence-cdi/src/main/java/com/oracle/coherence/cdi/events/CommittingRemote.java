/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.events;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.util.AnnotationLiteral;

import javax.inject.Qualifier;

/**
 * A qualifier annotation used for any COMMITTING_REMOTE event.
 *
 * @author Aleks Seovic  2020.04.13
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface CommittingRemote
    {
    /**
     * An annotation literal for the {@link CommittingRemote}
     * annotation.
     */
    class Literal
            extends AnnotationLiteral<CommittingRemote>
            implements CommittingRemote
        {
        public static final Literal INSTANCE = new Literal();
        }
    }
