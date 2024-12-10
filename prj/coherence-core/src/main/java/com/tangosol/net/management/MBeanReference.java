/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.management;


import javax.management.ObjectName;
import javax.management.MBeanServer;

/**
* MBeanReference holds all necessary information to uniquely identify an MBean.
*
* @author ew 2007.11.07
* @since Coherence 3.4
*/
public class MBeanReference
    {
   // ----- constructors ---------------------------------------------------

    /**
    * Construct an instance of MBeanReference.
    *
    * @param oname  the ObjectName
    * @param mbs    the MBeanServer where the ObjectName exists
    */
    public MBeanReference(ObjectName oname, MBeanServer mbs)
        {
        m_oname = oname;
        m_mbs   = mbs;
        }

    /**
    * Return the ObjectName of the qualified name.
    *
    * @return the ObjectName reference
    */
    public ObjectName getObjectName()
        {
        return m_oname;
        }

    /**
    * Return the MbeanServer where the ObjectName is located.
    *
    * @return the MBeanServer reference
    */
    public MBeanServer getMBeanServer()
        {
        return m_mbs;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The MBean ObjectName.
    */
    private ObjectName m_oname;

    /**
    * The MBeanServer.
    */
    private MBeanServer m_mbs;
    }
