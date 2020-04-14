/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.xtangosol.tools.ant;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;


/**
 * Ant task that processes XSD files.  The following is performed:
 * <ol>
 *   <li>XSD file is parsed into a DOM tree
 *   <li>All <tt>&lt;xsd:include schemaLocation=...</tt> attributes are processed
 *       to include the full URL to the file on the Oracle website (as specified by urlprefix).
 *       For example, <tt>coherence-cache-config.xsd</tt> may be converted to
 *       <tt>http://xmlns.oracle.com/coherence/coherence-cache-config/1.0/coherence-cache-config.xsd</tt>
 *   <li>The file is written out to the directory specified by outputdir using the format
 *       <tt>[outputdir]/coherence-cache-config/1.0/coherence-cache-config.xsd</tt>
 * </ol>
 * To specify the input directory, configure a <tt>&lt;fileset&gt;</tt> element.
 * <br>
 * <table>
 *   <tr>
 *     <td><b>Attribute</b></td>
 *     <td><b>Description</b></td>
 *     <td><b>Required</b></td>
 *   </tr>
 *   <tr>
 *     <td>outputdir</td>
 *     <td>Directory to write XSD files</td>
 *     <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>urlprefix</td>
 *     <td>The URL that XSD files will be published to
 *         (for example http://xmlns.oracle.com/coherence)</td>
 *     <td>true</td>
 *   </tr>
 * </table>
 *
 * @author pp  2011.03.02
 */
public class XsdProcessTask
        extends Task
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public XsdProcessTask()
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

        try
            {
            List<String> listFiles = m_listFiles;
            for (String sFile : listFiles)
                {
                Document doc      = parseXml(sFile);
                String   sVersion = doc.getDocumentElement().getAttribute("version");

                processIncludes(doc, sVersion);

                String sPath = m_sOutputDir + generateFullPath(stripPath(sFile), sVersion);
                if (File.separatorChar != '/')
                    {
                    sPath = sPath.replace('/', File.separatorChar);
                    }

                writeFile(doc, sPath);
                }
            }
        catch (Exception e)
            {
            throw new BuildException(e);
            }
        }

    /**
     * Ant callback to process the <tt>&lt;fileset&gt;</tt> element which is
     * used to indicate the source directory for XSD files.
     *
     * @param fileset
     */
    public void addConfiguredFileset(FileSet fileset)
        {
        m_project = fileset.getProject();

        List<String> listFiles = m_listFiles;

        for (Iterator iter = fileset.iterator(); iter.hasNext();)
            {
            FileResource fileResource = (FileResource) iter.next();
            listFiles.add(fileResource.getFile().getAbsolutePath());
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
        if (getOutputDir() == null)
            {
            throw new BuildException("outputdir attribute required");
            }
        if (getUrlPrefix() == null)
            {
            throw new BuildException("urlprefix attribute required");
            }
        }

    /**
     * Parse the file indicated by sFile
     *
     * @param sFile  source XSD file
     *
     * @return DOM   representation of XSD file
     *
     * @throws Exception  if an error occurs while processing
     */
    protected Document parseXml(String sFile)
             throws Exception
         {
         getAntProject().log("Parsing '" + sFile + "'", Project.MSG_DEBUG);

         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
         DocumentBuilder        db  = dbf.newDocumentBuilder();
         InputSource            is  = new InputSource(new FileReader(sFile));
         return db.parse(is);
         }

    /**
     * Process all <tt>xsd:include</tt> tags.  For each <tt>schemaLocation<tt>
     * element, ensure that the full URL is substituted.
     *
     * @param doc  the XML DOM that contains the XSD
     *
     * @return the XML DOM with the updated schemaLocation attributes
     */
    protected Document processIncludes(Document doc, String sVersion)
        {
        NodeList   includes  = doc.getElementsByTagName("xsd:include");
        NodeList   redefines = doc.getElementsByTagName("xsd:redefine");
        List<Node> listNodes = new ArrayList<Node>();

        for (int i = 0; i < includes.getLength(); i++)
            {
            listNodes.add(includes.item(i));
            }
        for (int i = 0; i < redefines.getLength(); i++)
            {
            listNodes.add(redefines.item(i));
            }

        for (Node node : listNodes)
            {
            NamedNodeMap attributes         = node.getAttributes();
            Node         nodeSchemaLocation = attributes.getNamedItem("schemaLocation");
            String       sSchemaLocation    = nodeSchemaLocation.getNodeValue();

            nodeSchemaLocation.setNodeValue(m_sUrlPrefix +
                    generateFullPath(sSchemaLocation, sVersion));
            }
        return doc;
        }

    /**
     * Write the XML represented by the DOM object to the specified file.
     *
     * @param doc    DOM object that contains XML to write
     * @param sPath  file location to write to
     *
     * @throws Exception  if an exception occurs while writing file
     */
    protected void writeFile(Document doc, String sPath)
            throws Exception
        {
        // ensure that path exists
        File f = new File(sPath.substring(0, sPath.lastIndexOf(File.separatorChar))) ;
        f.mkdirs();

        DOMSource    source      = new DOMSource(doc);
        Transformer  transformer = SAXTransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        getAntProject().log("Creating '" + sPath + "'");

        StreamResult res =  new StreamResult(new FileWriter(sPath));
        transformer.transform(source, res);
        }

    /**
     * For a given schema file name, return the full path the file will reside in.
     * This method uses forward slash (/) as a path separator.
     *
     * @param sFileName  name of schema file including extension
     *                   (for example, coherence-cache-config.xsd)
     * @param sVersion   version of schema file (for example 1.0)
     *
     * @return  full path of schema file
     *          (for example coherence-cache-config/1.0/coherence-cache-config.xsd)
     */
    protected String generateFullPath(String sFileName, String sVersion)
        {
        String sSchemaName = sFileName.substring(0, sFileName.indexOf(".xsd"));

        StringBuilder builder = new StringBuilder();
        builder.append(sSchemaName).append('/').append(sVersion).append('/').append(sFileName);
        return builder.toString();
        }

    /**
     * Return the file name indicated by a path.  For example, passing in
     * <tt>a/b/c.txt</tt> will return <tt>c.txt</tt>.  The OS
     * path separator is assumed.
     *
     * @param sFile  full path to file name
     *
     * @return  file name without the path prefix
     */
    protected String stripPath(String sFile)
        {
        return sFile.substring(sFile.lastIndexOf(File.separatorChar) + 1);
        }


    // ----- accessors ------------------------------------------------------

    /**
     * Get the Ant project object.
     *
     * @return  Ant project object
     */
    public Project getAntProject()
        {
        return m_project;
        }

    /**
     * Set the output directory for processed XSD files.
     *
     * @param sOutputDir  output directory for processed XSD files
     */
    public void setOutputDir(String sOutputDir)
        {
        m_sOutputDir = sOutputDir.endsWith("/") ? sOutputDir : sOutputDir + "/";
        }

    /**
     * Get the output directory for processed XSD files.
     *
     * @return  the output directory for processed XSD files.
     */
    public String getOutputDir()
        {
        return m_sOutputDir;
        }

    /**
     * Set the URL prefix for XSD files published to the external website.
     *
     * @param sUrlPrefix  the URL prefix for XSD files published to the external website.
     */
    public void setUrlPrefix(String sUrlPrefix)
        {
        m_sUrlPrefix = sUrlPrefix.endsWith("/") ? sUrlPrefix : sUrlPrefix + "/";
        }

    /**
     * Get the URL prefix for XSD files published to the external website.
     *
     * @return  the URL prefix for XSD files published to the external website.
     */
    public String getUrlPrefix()
        {
        return m_sUrlPrefix;
        }


    // ----- data members ---------------------------------------------------

    /**
     * Ant Project; used to perform logging.
     */
    private Project m_project;

    /**
     * The output directory for processed XSD files.
     */
    private String m_sOutputDir;

    /**
     * The URL prefix for XSD files published to the external website.
     */
    private String m_sUrlPrefix;

    /**
     * List of XSD files to process; likely located in TDE resource directory.
     */
    private List<String> m_listFiles = new ArrayList<String>();
    }
