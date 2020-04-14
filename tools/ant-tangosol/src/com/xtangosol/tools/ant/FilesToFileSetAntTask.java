/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.xtangosol.tools.ant;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;

import org.apache.tools.ant.types.resources.Files;


/**
* Ant task that adapts a files resource collection to a fileset.
* <br>
* <table>
*   <tr>
*     <td><b>Attribute</b></td>
*     <td><b>Description</b></td>
*     <td><b>Required</b></td>
*   </tr>
*   <tr>
*     <td>filesrefid</td>
*     <td>The identifier of the files resource collection to adapt.</td>
*     <td>true</td>
*   </tr>
*   <tr>
*     <td>id</td>
*     <td>The identifier of the fileset to create.</td>
*     <td>true</td>
*   </tr>
* </table>
*
* @author jh  2007.05.16
*/
public class FilesToFileSetAntTask
        extends Task
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public FilesToFileSetAntTask()
        {
        }


    // ----- Task methods ---------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void execute()
            throws BuildException
        {
        String sFilesRefId = getFilesRefId();
        if (sFilesRefId == null || sFilesRefId.length() == 0)
            {
            throw new BuildException("Missing required 'filesrefid' attribute");
            }

        String sId = getId();
        if (sId == null || sId.length() == 0)
            {
            throw new BuildException("Missing required 'id' attribute");
            }

        Project project = getProject();

        // look up the files resource collection by reference ID
        Object oFiles = project.getReference(sFilesRefId);
        if (oFiles == null)
            {
            throw new BuildException("Unknown reference '" + sFilesRefId + "'");
            }
        if (!(oFiles instanceof Files))
            {
            throw new BuildException("The '" + sFilesRefId
                    + "' reference is not a files resource collection");
            }

        // create and add a reference to the new FilesFileSet adapter
        FilesFileSet fs = new FilesFileSet((Files) oFiles);
        fs.setProject(project);
        project.addReference(sId, fs);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the identifier of the files resource collection to adapt.
    *
    * @return the identifier of the files resource collection to adapt
    */
    public String getFilesRefId()
        {
        return sFilesRefId;
        }

    /**
    * Configure the identifier of the files resource collection to adapt.
    *
    * @param sFilesRefId  the identifier of the files resource collection to
    *                     adapt
    */
    public void setFilesRefId(String sFilesRefId)
        {
        this.sFilesRefId = sFilesRefId;
        }

    /**
    * Return the identifier of the fileset to create.
    *
    * @return the identifier of the fileset to create
    */
    public String getId()
        {
        return sId;
        }

    /**
    * Configure the identifier of the fileset to create.
    *
    * @param sId  the identifier of the fileset to create
    */
    public void setId(String sId)
        {
        this.sId = sId;
        }


    // ----- FilesFileSet inner class ---------------------------------------

    /**
    * FileSet extension that adapts a Files instance to a FileSet.
    */
    public static class FilesFileSet
            extends FileSet
        {
        // ----- constructors -------------------------------------------

        /**
        * Create a new FilesFileSet that adapts the given Files instance to
        * a FileSet.
        *
        * @param files  the Files instance to adapt
        */
        public FilesFileSet(Files files)
            {
            assert files != null;
            m_files = files;
            }

        // ----- AbstractFileSet methods --------------------------------

        /**
        * {@inheritDoc}
        */
        public DirectoryScanner getDirectoryScanner(Project project)
            {
            DirectoryScanner ds;
            synchronized (this)
                {
                if (((ds = m_ds) == null) || project != getProject())
                    {
                    Files files = m_files;

                    ds = new DirectoryScanner();
                    PatternSet ps = files.mergePatterns(getProject());
                    ds.setIncludes(ps.getIncludePatterns(getProject()));
                    ds.setExcludes(ps.getExcludePatterns(getProject()));
                    ds.setSelectors(files.getSelectors(getProject()));
                    if (files.getDefaultexcludes()) {
                        ds.addDefaultExcludes();
                    }
                    ds.setCaseSensitive(files.isCaseSensitive());
                    ds.setFollowSymlinks(files.isFollowSymlinks());
                    }
                }

            ds.scan();
            return ds;
            }

        // ----- data members -------------------------------------------

        /**
        * The source Files object.
        */
        protected final Files m_files;

        /**
        * The cached DirectoryScanner.
        */
        protected transient DirectoryScanner m_ds;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The identifier of the files resource collection to adapt.
    */
    private String sFilesRefId;

    /**
    * The identifier of the files resource collection to adapt.
    */
    private String sId;
    }
