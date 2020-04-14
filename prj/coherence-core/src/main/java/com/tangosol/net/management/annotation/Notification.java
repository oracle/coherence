/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Notification annotation provides a means for an MBean interface to
 * describe the notification it emits. This annotation is expected to be used
 * with {@link com.tangosol.net.management.AnnotatedStandardEmitterMBean
 * AnnotatedStandardEmitterMBean}.
 * <p>
 * An example of using this annotation is provided below:
 * <pre><code>
 *     &#64;Notification("Example notifications", types = ExampleMBean.NOTIFY_TYPE)
 *     class ExampleMBean {...}
 * </code></pre>
 *
 * @author hr  2014.01.27
 * @since Coherence 12.2.1
 *
 * @see com.tangosol.net.management.AnnotatedStandardEmitterMBean
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Notification
    {
    /**
     * A description of the notification.
     *
     * @return a description of the notification
     */
    String description();

    /**
     * The notification event types the MBean may emit.
     *
     * @return the notification event types the MBean may emit
     */
    String[] types();

    /**
     * The fully qualified class name of the notification object the MBean
     * may emit.
     *
     * @return the fully qualified class name of the notification object the
     *         MBean may emit.
     */
    String className() default "javax.management.Notification";
    }
