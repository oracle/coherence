/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that an annotation type is a {@link com.tangosol.util.Filter}
 * binding type.
 *
 * <pre>
 * &#064;Inherited
 * &#064;FilterBinding
 * &#064;Target({TYPE, METHOD, CONSTRUCTOR})
 * &#064;Retention(RUNTIME)
 * public &#064;interface CustomerNameFilter {}
 * </pre>
 *
 * <p>
 * Filter bindings are intermediate annotations that may be used to associate
 * {@link com.tangosol.util.Filter}s with target beans.
 * <p>
 * Filter bindings are used by annotating a {@link FilterFactory} bean with the
 * binding type annotations. Wherever the same annotation is used at an
 * injection point that requires a {@link com.tangosol.util.Filter} the
 * corresponding factory's {@link FilterFactory#create(java.lang.annotation.Annotation)}
 * method is called to produce a {@link com.tangosol.util.Filter} instance.
 *
 * @author Jonathan Knight  2019.10.24
 * @since 20.06
 */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Documented
public @interface FilterBinding
    {
    }
