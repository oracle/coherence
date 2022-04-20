/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.evolvable;

import java.util.Date;

public interface DateTypes
    {
    Date getDate();

    Date getTime();

    Date getTimeWithZone();
    }
