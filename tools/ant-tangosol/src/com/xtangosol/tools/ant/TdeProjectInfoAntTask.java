/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.xtangosol.tools.ant;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


/**
* Ant task that returns information about a TDE project:
* <br>
* <table>
*   <tr>
*     <td><b>Attribute</b></td>
*     <td><b>Description</b></td>
*     <td><b>Required</b></td>
*   </tr>
*   <tr>
*     <td>path</td>
*     <td>Path to the target TDE project XML descriptor.</td>
*     <td>true</td>
*   </tr>
*   <tr>
*     <td>query</td>
*     <td>Information to be returned (e.g. DefaultTarget).</td>
*     <td>true</td>
*   </tr>
*   <tr>
*     <td>path</td>
*     <td>Name of the property used to return the requested information.</td>
*     <td>true</td>
*   </tr>
* </table>
*
* @author jhowes 11.24.2004
*/
public class TdeProjectInfoAntTask
        extends Task
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public TdeProjectInfoAntTask()
        {
        super();
        }


   // ----- Task methods ----------------------------------------------------

    /**
    * Called by the project to let the task do its work. This method may be
    * called more than once, if the task is invoked more than once.
    * For example, if target1 and target2 both depend on target3, then running
    * "ant target1 target2" will run all tasks in target3 twice.
    *
    * @throws BuildException on build error
    */
    public void execute()
            throws BuildException
        {
        // check all task attributes
        validateAttributes();

        // parse the project file using DOM and return the desired information.
        File   file  = getPath();
        String sInfo = null;
        try
            {
            // get a DocumentBuilder instance
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();

            // parse the project descriptor
            Document document = builder.parse(file);
            Element  element  = document.getDocumentElement();

            // extract the desired information.
            NodeList nodeList = element.getElementsByTagName(getQuery());
            if (nodeList != null && nodeList.getLength() > 0)
                {
                Node node = nodeList.item(0);
                while (node != null && node.getNodeType() != Node.TEXT_NODE)
                    {
                    node = node.getFirstChild();
                    }
                sInfo = node.getNodeValue();
                }
            }
        catch (SAXException e)
            {
            Exception x = e;
            if (e.getException() != null)
                {
                x = e.getException();
                }
            throw new BuildException(x);
            }
        catch (ParserConfigurationException e)
            {
            throw new BuildException(e);
            }
        catch (IOException e)
            {
            throw new BuildException(e);
            }

        // return the extracted information via the specified property
        if (sInfo != null)
            {
            getProject().setProperty(getProperty(), sInfo);
            }
        }


    // ----- internal helpers -----------------------------------------------

    /**
    * Validate that all required attributes have been set to valid values.
    *
    * @throws BuildException if a required attribute is missing or has been set
    *                        to an invalid value
    */
    protected void validateAttributes()
            throws BuildException
        {
        // make sure the specified project descriptor exists
        File file = getPath();
        if (file == null || !file.exists() || !file.isFile())
            {
            throw new BuildException("The given project path is invalid.");
            }

        // make sure a valid property name was specified
        String sAttribute = getProperty();
        if (sAttribute == null || sAttribute.length() == 0)
            {
            throw new BuildException("Missing the required property attribute.");
            }

        // make sure a valid query string was specified
        sAttribute = getQuery();
        if (sAttribute == null || sAttribute.length() == 0)
            {
            throw new BuildException("Missing the required query attribute.");
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the path to the target TDE project XML descriptor.
    *
    * @return the path to the target TDE project XML descriptor
    */
    public File getPath()
        {
        return m_fileProject;
        }

    /**
    * Get the name of the property used to return information about the target
    * TDE project.
    *
    * @return the name of the property
    */
    public String getProperty()
        {
        return m_sProperty;
        }

    /**
    * Get the query string.
    *
    * @return the query string
    */
    public String getQuery()
        {
        return m_sQuery;
        }

    /**
    * Set the path to the target TDE project XML descriptor.
    *
    * @param file the path to the target TDE project XML descriptor
    */
    public void setPath(File file)
        {
        if (file.isDirectory())
            {
            m_fileProject = new File(file, "project.xml");
            }
        else
            {
            m_fileProject = file;
            }
        }

    /**
    * Set the name of the property used to return information about the target
    * TDE project.
    *
    * @param sProperty the name of the property
    */
    public void setProperty(String sProperty)
        {
        m_sProperty = sProperty;
        }

    /**
    * Set the query string.
    *
    * @param sQuery the query string
    */
    public void setQuery(String sQuery)
        {
        m_sQuery = sQuery;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The path to the target TDE project XML descriptor.
    */
    private File m_fileProject;

    /**
    * The information to return.
    */
    private String m_sQuery;

    /**
    * The name of the property used to return information about the target TDE
    * project.
    */
    private String m_sProperty;
    }