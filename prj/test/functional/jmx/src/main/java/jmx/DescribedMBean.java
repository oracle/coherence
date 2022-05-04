/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package jmx;

import com.tangosol.net.management.annotation.Description;

/**
* A {@link DescribedMBean} is a standard MBean interface annotated
* with the {@link Description} annotation.
* <p>
* This interface is used in the functional tests.
*
* @author cf 2011-01-10
*/
@Description(DescribedMBean.MBEAN_DESCRIPTION)
public interface DescribedMBean
    {

    /**
    * An annotated operation.
    */
    @Description(OPER_DESCRIPTION)
    void operation();

    /**
    * An annotated attribute.
    *
    * @return the value of the annotated attribute
    */
    @Description(ATTR_DESCRIPTION)
    int getAttribute();

    /**
    * The MBean description.
    */
    String MBEAN_DESCRIPTION = "This is the mbean description";

    /**
    * The Attribute description.
    */
    String ATTR_DESCRIPTION = "Attribute description";

    /**
    * The Operation description.
    */
    String OPER_DESCRIPTION = "Operation description";
    }
