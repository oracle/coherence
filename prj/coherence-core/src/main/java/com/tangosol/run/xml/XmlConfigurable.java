/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


/**
* An interface for XML configuration.
* <p>
* <strong>This class has now been deprecated.  All parameters passed to a custom object
* will be done using the custom object's constructor.
* For example, the following cache configuration XML will result in 2 parameters being
* passed to the constructor (the cache name and 'Param2'):</strong>
* <pre>
* {@code
* <instance>
*   <class-name>MyCustomObject</class-name>
*   <init-params>
*     <init-param>
*       <param-type>java.lang.String</param-type>
*       <param-value>{cache-name}</param-value>
*     </init-param>
*     <init-param>
*       <param-type>java.lang.String</param-type>
*       <param-value>Param2</param-value>
*     </init-param>
*   </init-params>
* </instance>}
* </pre>
* <p>
*
* @author cp  2002.08.20
*/
@Deprecated
public interface XmlConfigurable
    {
    /**
    * Determine the current configuration of the object.
    *
    * @return the XML configuration or null
    */
    public XmlElement getConfig();

    /**
    * Specify the configuration for the object.
    *
    * @param xml  the XML configuration for the object
    *
    * @exception IllegalStateException  if the object is not in a state that
    *            allows the configuration to be set; for example, if the
    *            object has already been configured and cannot be reconfigured
    */
    public void setConfig(XmlElement xml);
    }