/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter;



/**
* Macro column to include a Batch identifier into the filename. The batch identifier
* is incremented with each execution of the Report Group.  The batch identifier
* is helpful when needing to associate data from related reports.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class BatchView
        extends ColumnView
    {
    /**
    * BatchView is never visible in the report.  BatchLocator is used to include
    * the batch number in the report
    *
    * @return false
    */
    public boolean isVisible()
        {
        return false;
        }

    /**
    * Return a 10 digit batch number padded with zeros(0)
    *
    * @param oObject this is ignored
    * @return a string representing the batch number.
    */
    public String getOutputString(Object oObject)
        {
        long lBatch = ((JMXQueryHandler) m_handler).getBatch();
        String sRet = "0000000000" + String.valueOf(lBatch);
        return sRet.substring(sRet.length() - 10);
        }
    }
