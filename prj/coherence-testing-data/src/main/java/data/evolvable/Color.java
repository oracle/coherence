/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.evolvable;

import com.tangosol.io.pof.schema.annotation.PortableType;

@PortableType(id = 5, version = 1)
public enum Color
    {
    BRINDLE,
    FAWN,
    BLACK,
    WHITE,
    BROWN,
    GRAY,
    YELLOW
}
