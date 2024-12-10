/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.xtangosol.tools.ant;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;


/**
* Ant task that returns a unique IP address for the machine that this task
* runs on.
* <br>
* <table>
*   <tr>
*     <td><b>Attribute</b></td>
*     <td><b>Description</b></td>
*     <td><b>Required</b></td>
*   </tr>
*   <tr>
*     <td>property</td>
*     <td>Name of the property that will be set with the unique address.</td>
*     <td>true</td>
*   </tr>
*   <tr>
*     <td>multicast</td>
*     <td>true to indicate whether the returned address should be a multicast
*         address; false otherwise.</td>
*     <td>true</td>
*   </tr>
* </table>
*
* @author jhowes  2009.11.17
*/
public class UniqueAddressAntTask
        extends Task
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public UniqueAddressAntTask()
        {
        super();
        }


   // ----- Task methods ----------------------------------------------------

    /**
    * Called by the project to let the task do its work. This method may be
    * called more than once, if the task is invoked more than once.
    * For example, if target1 and target2 both depend on target3, then
    * running "ant target1 target2" will run all tasks in target3 twice.
    *
    * @throws BuildException on build error
    */
    public void execute()
            throws BuildException
        {
        // check all task attributes
        validateAttributes();

        // return the address via the specified property
        getProject().setNewProperty(getProperty(),
                AntUtils.generateUniqueAddress(isMulticast()));
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * Validate that all required attributes have been set to valid values.
    *
    * @throws BuildException if a required attribute is missing or has been
    *                        set to an invalid value
    */
    protected void validateAttributes()
            throws BuildException
        {
        // make sure a valid property name was specified
        String sAttribute = getProperty();
        if (sAttribute == null || sAttribute.length() == 0)
            {
            throw new BuildException("Missing the required property attribute.");
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the name of the property used to return the unique address.
    *
    * @return the name of the property
    */
    public String getProperty()
        {
        return m_sProperty;
        }

    /**
    * Set the name of the property used to return the unique address.
    *
    * @param sProperty the name of the property
    */
    public void setProperty(String sProperty)
        {
        m_sProperty = sProperty;
        }

    /**
    * Determine if the unique address should be a multicast address.
    *
    * @return  true if the unique address should be a multicast address
    */
    public boolean isMulticast()
        {
        return m_fMulticast;
        }

    /**
    * Configure whether or not the unique address should be a multicast
    * address.
    *
    * @param fMulticast  true if the unique address should be a multicast
    *                    address
    */
    public void setMulticast(boolean fMulticast)
        {
        m_fMulticast = fMulticast;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of the property used to return the unique address.
    */
    private String m_sProperty;

    /**
    * True to indicate whether the returned address should be a multicast
    * address; false otherwise.
    */
    private boolean m_fMulticast;
    }
