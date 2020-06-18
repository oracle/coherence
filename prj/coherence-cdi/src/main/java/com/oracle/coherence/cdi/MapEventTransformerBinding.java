/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 *
 */
package com.oracle.coherence.cdi;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that an annotation type is a {@link com.tangosol.util.MapEventTransformer}
 * binding type.
 *
 * <pre>
 * &#064;Inherited
 * &#064;MapEventTransformerBinding
 * &#064;Target({TYPE, METHOD, CONSTRUCTOR})
 * &#064;Retention(RUNTIME)
 * public &#064;interface CustomerEventTransformer {}
 * </pre>
 *
 * <p>
 * MapEventTransformer bindings are intermediate annotations that may be used to
 * associate {@link com.tangosol.util.MapEventTransformer}s with target beans.
 * <p>
 * MapEventTransformer bindings are used by annotating a {@link
 * MapEventTransformerFactory} bean with the binding type
 * annotations. Wherever the same annotation is used at an injection point that
 * requires a {@link com.tangosol.util.MapEventTransformer} the corresponding
 * factory's {@link MapEventTransformerFactory#create(java.lang.annotation.Annotation)}
 * method is called to produce a {@link com.tangosol.util.MapEventTransformer}
 * instance.
 *
 * @author Jonathan Knight  2020.06.16
 * @since 20.06
 */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Documented
public @interface MapEventTransformerBinding
    {
    }
