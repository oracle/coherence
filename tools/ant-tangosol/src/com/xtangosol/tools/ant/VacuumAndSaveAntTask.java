/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.xtangosol.tools.ant;


import com.xtangosol.tools.VacuumAndSave;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;


/**
* Ant task that downloads and saves all content from a web site.
* <br>
* <table>
*   <tr>
*     <td><b>Attribute</b></td>
*     <td><b>Description</b></td>
*     <td><b>Required</b></td>
*   </tr>
*   <tr>
*     <td>auth</td>
*     <td>HTTP authentication information in the form: <tt>(username)
*         (password) (form URL) (username parameter) (password parameter)
*         [additional parameter]</tt>.</td>
*     <td>false</td>
*   </tr>
*   <tr>
*     <td>dir</td>
*     <td>Path to the directory in which content will be saved. If not
*         specified, a directory named after the target URL will be created
*         and used.</td>
*     <td>false</td>
*   </tr>
*   <tr>
*     <td>filter</td>
*     <td>An optional regular expression used to filter URLs.</td>
*     <td>false</td>
*   </tr>
*   <tr>
*     <td>url</td>
*     <td>The URL of the web site to vacuum.</td>
*     <td>true</td>
*   </tr>
* </table>
*
* @author jh  2006.01.25
*/
public class VacuumAndSaveAntTask
        extends Task
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public VacuumAndSaveAntTask()
        {
        }


    // ----- Task methods ---------------------------------------------------

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
        validateAttributes();

        String sAuth   = getAuthInfo();
        String sFilter = getFilter();
        String sUrl    = getUrl();
        String sDir;
        try
            {
            File fileDir = getDir();
            if (fileDir == null)
                {
                sDir = null;
                }
            else
                {
                sDir = fileDir.getCanonicalPath();
                }
            }
        catch (IOException e)
            {
            throw new BuildException("Error resolving target directory.", e);
            }

        List listArgs = new ArrayList(16);

        if (sAuth != null)
            {
            listArgs.add("-auth");

            String[] as = sAuth.split(" ");
            for (int i = 0, c = as.length; i < c; ++i)
                {
                listArgs.add(as[i]);
                }
            }

        if (sDir != null)
            {
            listArgs.add("-dir");
            listArgs.add(sDir);
            }

        if (sFilter != null)
            {
            listArgs.add("-filter");
            listArgs.add(sFilter);            
            }

        listArgs.add("-url");
        listArgs.add(sUrl);

        String[] asArgs = (String[]) listArgs.toArray(new String[listArgs.size()]);
        try
            {
            VacuumAndSave.main(asArgs);
            }
        catch (Throwable t)
            {
            throw new BuildException("Error running VacuumAndSave.", t);
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
        File fileDir = getDir();
        if (fileDir != null)
            {
            // make sure the specified directory is accessible
            if (fileDir.exists())
                {
                    if (!fileDir.isDirectory())
                    {
                    throw new BuildException("The destination path is not a directory.");
                    }
                }
            else if (!fileDir.mkdirs())
                {
                throw new BuildException("The destination directory could not be created.");
                }
            }

        // make sure a URL was specified
        String sUrl = getUrl();
        if (sUrl == null || sUrl.length() == 0)
            {
            throw new BuildException("Missing the required url attribute.");
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the HTTP authentication information used to authenticate the
    * vacuum HTTP client.
    */
    public String getAuthInfo()
        {
        return m_sAuthInfo;
        }

    /**
    * Return the directory in which content will be saved.
    *
    * @return the output directory
    */
    public File getDir()
        {
        return m_fileDir;
        }

    /**
    * Return the optional regular expression used to filter URLs.
    *
    * @return a regular expression or null if all URLs should be vacuumed
    */
    public String getFilter()
        {
        return m_sFilter;
        }

    /**
    * Return the URL of the web site to vacuum.
    *
    * @return the URL of the target web site
    */
    public String getUrl()
        {
        return m_sUrl;
        }

    /**
    * Set the HTTP authentication information used to authenticate the vacuum
    * HTTP client.
    */
    public void setAuthInfo(String sInfo)
        {
        m_sAuthInfo = sInfo;
        }

    /**
    * Set the directory in which content will be saved.
    *
    * @param fileDir  the new output directory
    */
    public void setDir(File fileDir)
        {
        m_fileDir = fileDir;
        }

    /**
    * Configure the optional regular expression used to filter URLs.
    *
    * @param sFilter  a regular expression or null if all URLs should be
    *                 vacuumed
    */
    public void setFilter(String sFilter)
        {
        m_sFilter = sFilter;
        }

    /**
    * Set the URL of the web site to vacuum.
    *
    * @param sUrl  the URL of the new target web site
    */
    public void setUrl(String sUrl)
        {
        m_sUrl = sUrl;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The directory in which to save content.
    */
    private File m_fileDir;

    /**
    * HTTP authentication information.
    */
    private String m_sAuthInfo;

    /**
    * Optional regular expression used to filter URLs.
    */
    private String m_sFilter;

    /**
    * The URL to vacuum.
    */
    private String m_sUrl;
    }
