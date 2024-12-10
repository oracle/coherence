/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.xtangosol.tools.ant;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;


/**
* Ant task that returns a unique IP port for the machine that this task
* runs on.
* <br>
* <table>
*   <tr>
*     <td><b>Attribute</b></td>
*     <td><b>Description</b></td>
*     <td><b>Required</b></td>
*   </tr>
*   <tr>
*     <td>count</td>
*     <td>The number of unique ports to generate. If greater than 1, the
*         ports will be returned via properties in the form
*         <tt>[property name].n</tt>, where <tt>[property name]</tt> is the
*         value of the <tt>property</tt> attribute and <tt>n</tt> is an
*         integer value between 1 and the configured count (inclusive).</td>
*     <td>false</td>
*   </tr>
*   <tr>
*     <td>property</td>
*     <td>Name of the property that will be set with the unique port.</td>
*     <td>true</td>
*   </tr>
* </table>
*
* @author jhowes  2009.11.17
*/
public class UniquePortAntTask
        extends Task
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public UniquePortAntTask()
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

        // return the port(s) via the specified property(ies)
        int cPort = getCount();
        if (cPort == 1)
            {
            getProject().setNewProperty(getProperty(),
                    String.valueOf(AntUtils.generateUniquePort()));
            }
        else
            {
            Project project   = getProject();
            String  sProperty = getProperty();
            int[]   anPort    = AntUtils.generateUniquePorts(cPort);
            for (int i = 0; i < cPort; ++i)
                {
                project.setNewProperty(sProperty + '.' + (i + 1),
                        String.valueOf(anPort[i]));
                }
            }
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
        // make sure a valid count was specified
        if (getCount() < 1)
            {
            throw new BuildException("Count must be greater than 0.");
            }

        // make sure a valid property name was specified
        String sAttribute = getProperty();
        if (sAttribute == null || sAttribute.length() == 0)
            {
            throw new BuildException("Missing the required property attribute.");
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the number of unique ports to generate.
    *
    * @return the number of ports to generate
    */
    public int getCount()
        {
        return m_cPort;
        }

    /**
    * Configure the number of unique ports to generate.
    *
    * @param c  the number of ports to generate
    */
    public void setCount(int c)
        {
        m_cPort = c;
        }

    /**
    * Get the name of the property used to return the unique port.
    *
    * @return the name of the property
    */
    public String getProperty()
        {
        return m_sProperty;
        }

    /**
    * Set the name of the property used to return the unique port.
    *
    * @param sProperty  the name of the property
    */
    public void setProperty(String sProperty)
        {
        m_sProperty = sProperty;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The number of unique ports to generate. Defaults to 1.
    */
    private int m_cPort = 1;

    /**
    * The name of the property used to return the unique port.
    */
    private String m_sProperty;
    }
