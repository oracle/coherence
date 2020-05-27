/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.coherence.cache.grpc.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;

import javax.inject.Qualifier;

/**
 * An qualifier annotation to specify the name of a gRPC remote session to inject.
 *
 * @since 14.1.2
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface RemoteSession
    {
    /**
     * Obtain the name of the session.
     *
     * @return name of the session
     */
    @Nonbinding String value() default GrpcRemoteSession.DEFAULT_NAME;

    /**
     * An {@link javax.enterprise.util.AnnotationLiteral} for the
     * {@link RemoteSession} annotation.
     */
    class Literal
            extends AnnotationLiteral<RemoteSession>
            implements RemoteSession
        {
        // ----- constructors -----------------------------------------------

        protected Literal(String sName)
            {
            this.f_sName = sName;
            }

        // ----- RemoteSession interface ------------------------------------

        @Override
        public String value()
            {
            return f_sName;
            }

        // ----- public methods ---------------------------------------------

        /**
         * The singleton instance of {@link Literal}.
         */
        public static Literal defaultName()
            {
            return new Literal(GrpcRemoteSession.DEFAULT_NAME);
            }

        /**
         * The singleton instance of {@link Literal}.
         */
        public static Literal of(String name)
            {
            return new Literal(name);
            }

        // ----- constants --------------------------------------------------

        private static final long serialVersionUID = 1L;

        // ----- data members -----------------------------------------------

        /**
         * The literal name.
         */
        private final String f_sName;
        }
    }
