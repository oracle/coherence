/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level annotation specifying {@link ExternalizableLiteSerializer serializer}
 * for this type.
 *
 * @see ExternalizableLiteSerializer
 * @see ExternalizableLite
 *
 * @author jf  2023.06.06
 * @since  23.09
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExternalizableType
    {
    /**
     * The class of the serializer for this type.
     *
     * @return the class of the serializer for this type
     */
    Class<? extends ExternalizableLiteSerializer> serializer();
    }
