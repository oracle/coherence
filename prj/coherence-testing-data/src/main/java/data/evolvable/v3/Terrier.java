/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.evolvable.v3;

import com.tangosol.io.pof.schema.annotation.PortableType;
import data.evolvable.Color;

/**
 * Class to test extending @PortableType
 * classes but sub-class has no fields.
 *
 * @author jk  2017.07.06
 */
@PortableType(id = 200)
public class Terrier
        extends Dog
    {
    public Terrier(String name, int age, Color color)
        {
        super(name, age, "Terrier", color);
        }
    }
