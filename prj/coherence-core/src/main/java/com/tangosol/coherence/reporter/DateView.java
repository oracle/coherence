/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter;


import java.text.SimpleDateFormat;

import java.util.Date;


/**
* Class to include execution date as a macro in the Filename.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class DateView
        extends ColumnView
    {
    /**
    * Obtain the start date time for the report.
    *
    * @param oObject ignored
    * @return the date the report is executed in YYYYMMDD format
    */
    public String getOutputString(Object oObject)
        {
        long ldtStartTime = ((JMXQueryHandler) m_handler).getStartTime();
        return new SimpleDateFormat("yyyyMMddHH")
                    .format(new Date(ldtStartTime));
        }

    /**
    * DateView is never visible in the report.  It is only used as a "macro" in
    * the output filename.
    *
    * @return false
    */
    public boolean isVisible()
        {
        return false;
        }


    }
