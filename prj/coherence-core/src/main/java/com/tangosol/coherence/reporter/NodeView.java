/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.Cluster;

/**
* Column to include the execution node as a macro within the report
* description or output filename.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class NodeView
        extends ColumnView
    {
    /**
    * @inheritDoc
    */
    public String getOutputString(Object oObject)
        {
        if (m_sNodeId == null || m_sNodeId.length() == 0)
            {
            Cluster cluster = CacheFactory.ensureCluster();

            String sNodeFmt = "00000" + Integer.toString(cluster.getLocalMember().getId());
            m_sNodeId = sNodeFmt.substring(sNodeFmt.length()-5, sNodeFmt.length());
            }
        return m_sNodeId;
        }

    /**
    * @inheritDoc
    */
    public boolean isVisible()
        {
        return false;
        }

    // ----- data members ----------------------------------------------------
    /**
    * Formatted Node string
    */
    protected String m_sNodeId;
    }

