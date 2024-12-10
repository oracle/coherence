/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.annotation;

import com.tangosol.io.pof.PofAnnotationSerializer;
import com.tangosol.io.pof.schema.annotation.PortableType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Portable marks a class as being eligible for use by a
 * {@link PofAnnotationSerializer}. This annotation is only permitted at the
 * class level and is a marker annotation with no members. The following
 * class illustrates how to use {@link Portable} and {@link PortableProperty}
 * annotations.
 * <pre>
 * &#64;Portable
 * public class Person
 *     {
 *     &#64;PortableProperty(0)
 *     public String getFirstName()
 *         {
 *         return m_firstName;
 *         }
 *
 *     private String m_firstName;
 *     &#64;PortableProperty(1)
 *     private String m_lastName;
 *     &#64;PortableProperty(2)
 *     private int m_age;
 *     }
 * </pre>
 *
 * @deprecated Since Coherence 14.1.2. Use {@link PortableType} annotation instead.
 *
 * @author hr
 * @since  3.7.1
 * @see  PortableProperty
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface Portable
    {
    }
