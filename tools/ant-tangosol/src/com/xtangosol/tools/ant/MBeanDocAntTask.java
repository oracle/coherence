/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.xtangosol.tools.ant;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;


/**
* Ant task that parameterizes a Java source file with JavaDoc for the
* attributes and operations of a specified set of MBean components.
* <br>
* <table>
*   <tr>
*     <td><b>Attribute</b></td>
*     <td><b>Description</b></td>
*     <td><b>Required</b></td>
*   </tr>
*   <tr>
*     <td>file</td>
*     <td>Path to the Java source file to be parameterized.</td>
*     <td>true</td>
*   </tr>
*   <tr>
*     <td>mbeans</td>
*     <td>Comma or space delimited list of MBean components to document.</td>
*     <td>true</td>
*   </tr>
*   <tr>
*     <td>token</td>
*     <td>The token in the Java source file that will be replaced with the
*         generated JavaDoc.</td>
*     <td>true</td>
*   </tr>
* </table>
*
* @author jhowes 11.24.2004
*/
public class MBeanDocAntTask
        extends Task
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public MBeanDocAntTask()
        {
        super();
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
        // check all task attributes
        validateAttributes();

        try
            {
            File         file  = getFile();
            String       sData = readFile(file);
            StringBuffer sb    = new StringBuffer();

            // generate JavaDoc for each Manageable class
            StringTokenizer st = new StringTokenizer(getMBeans(), ", ");
            while (st.hasMoreTokens())
                {
                String sMBean = st.nextToken();

                log("Generating JavaDoc for MBean: " + sMBean);
                sb.append(generateJavaDoc(sMBean));
                }

            // replace the token with the generated JavaDoc
            sData = sData.replaceFirst(getToken(), sb.toString());

            // write out the new contents of the Java source file
            writeFile(file, sData);
            }
        catch (IOException e)
            {
            throw new BuildException(e);
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
        // make sure the specified Java source file exists
        File file = getFile();
        if (file == null || !file.exists() || !file.isFile())
            {
            throw new BuildException("The specified Java source file is invalid.");
            }

        // make sure the list of MBean components was specified
        String sMBeans = getMBeans();
        if (sMBeans == null || sMBeans.length() == 0)
            {
            throw new BuildException("Missing the required mbeans attribute.");
            }

        // make sure a token was specified
        String sToken = getToken();
        if (sToken == null || sToken.length() == 0)
            {
            throw new BuildException("Missing the required token attribute.");
            }
        }

    /**
    * Read and return the contents of the specified file.
    *
    * @return the contents of the specified file
    *
    * @throws IOException on I/O error
    */
    protected String readFile(File file)
            throws IOException
        {
        BufferedReader reader = null;
        StringBuffer   sb     = new StringBuffer();
        try
            {
            reader = new BufferedReader(new FileReader(file));

            for (String sData = reader.readLine();
                 sData != null;
                 sData = reader.readLine())
                {
                sb.append(sData);
                sb.append('\n');
                }
            }
        finally
            {
            if (reader != null)
                {
                try
                    {
                    reader.close();
                    }
                catch (IOException e) {}
                }
            }

        return sb.toString();
        }

    /**
    * Replace the contents of the specified file with the given string data.
    *
    * @param file  the file to modify
    * @param sData the new contents of the file
    *
    * @throws IOException on I/O error
    */
    protected void writeFile(File file, String sData)
            throws IOException
        {
        // clear the contents of the file
        RandomAccessFile raFile = new RandomAccessFile(file, "rw");
        try
            {
            raFile.setLength(0);
            }
        finally
            {
            try
                {
                raFile.close();
                }
            catch (IOException e) {}
            }

        // write the new contents
        FileWriter writer = new FileWriter(file);
        try
            {
            writer.write(sData);
            }
        finally
            {
            try
                {
                writer.close();
                }
            catch (IOException e) {}
            }
        }


    /**
    * Generate the JavaDoc for the specified MBean component. The JavaDoc
    * will include information about the manageable attributes and operations.
    *
    * @param sMBean the name of the MBean to document
    *
    * @return the generated JavaDoc
    *
    * @throws BuildException on JavaDoc generation error
    */
    protected static String generateJavaDoc(String sMBean)
            throws BuildException
        {
        String sClassName = MBEAN_PACKAGE + "." + sMBean;

        // create a new instance of the specified class
        Object oMBean = null;
        try
            {
            oMBean = Class.forName(sClassName).newInstance();
            }
        catch (Exception e)
            {
            throw new BuildException(e);
            }

        // make sure the class is an instance of javax.management.DynamicMBean
        if (!(oMBean instanceof DynamicMBean))
            {
            throw new BuildException("The object '" + oMBean
                    + "' is not an instance of javax.management.DynamicMBean.");
            }

        // get MBean metadata
        MBeanInfo            mbeanInfo      = ((DynamicMBean) oMBean).getMBeanInfo();
        MBeanAttributeInfo[] aAttributeInfo = mbeanInfo.getAttributes();
        MBeanOperationInfo[] aOperationInfo = mbeanInfo.getOperations();

        // generate the heading JavaDoc
        StringBuffer sb = new StringBuffer("* <p><strong>")
                .append(sMBean)
                .append(" Attributes and Operations</strong></p>");

        // generate JavaDoc for the attributes
        int cAttributes = aAttributeInfo == null ? 0 : aAttributeInfo.length;
        if (cAttributes > 0)
            {
            // sort the attributes by name
            aAttributeInfo = (MBeanAttributeInfo[]) sortMBeanFeatureInfo(aAttributeInfo);

            sb.append("\n* <blockquote>\n* <table border>")
              .append("\n* <caption>" + sMBean + " attributes</caption>\n* <tr>")
              .append("\n* <th>Name</th>")
              .append("\n* <th>Type</th>")
              .append("\n* <th>Access</th>")
              .append("\n* <th>Description</th>");

            for (int i = 0; i < cAttributes; i++)
                {
                MBeanAttributeInfo attributeInfo = aAttributeInfo[i];
                String             sName         = attributeInfo.getName();
                String             sDescription  = attributeInfo.getDescription();
                String             sType         = attributeInfo.getType();
                boolean            fReadable     = attributeInfo.isReadable();
                boolean            fWritable     = attributeInfo.isWritable();

                sb.append("\n* <tr>\n* <td>")
                  .append(sName)
                  .append("</td>\n* <td align='center'>")
                  .append(formatType(sType))
                  .append("</td>\n* <td align='center'>")
                  .append(fReadable && fWritable ? "RW" : fReadable ? "RO" : "WO")
                  .append("</td>\n* <td>")
                  .append(escapeHtml(sDescription))
                  .append("</td>\n* </tr>");
                }

            sb.append("\n* </table>\n* </blockquote>\n");
            }

        // generate JavaDoc for the operations
        int cOperations = aOperationInfo == null ? 0 : aOperationInfo.length;
        if (cOperations > 0)
            {
            // sort the operations by name
            aOperationInfo = (MBeanOperationInfo[]) sortMBeanFeatureInfo(aOperationInfo);

            sb.append("\n* <blockquote>\n* <table border>")
              .append("\n* <caption>" + sMBean + " operations</caption>\n* <tr>")
              .append("\n* <th>Name</th>")
              .append("\n* <th>Parameters</th>")
              .append("\n* <th>Return&nbsp;Type</th>")
              .append("\n* <th>Description</th>");

            for (int i = 0; i < cOperations; i++)
                {
                MBeanOperationInfo   operationInfo  = aOperationInfo[i];
                MBeanParameterInfo[] aParameterInfo = operationInfo.getSignature();
                String               sName          = operationInfo.getName();
                String               sType          = operationInfo.getReturnType();
                String               sDescription   = operationInfo.getDescription();

                sb.append("\n* <tr>\n* <td>")
                  .append(sName)
                  .append("</td>\n* <td align='center'>");

                int cParams = aParameterInfo.length;
                if (cParams > 0)
                    {
                    MBeanParameterInfo parameterInfo = aParameterInfo[0];
                    sb.append(formatType(parameterInfo.getType()))
                      .append(' ')
                      .append(parameterInfo.getName());

                    for (int j = 1; j < cParams; j++)
                        {
                        parameterInfo = aParameterInfo[j];
                        sb.append(", ")
                          .append(parameterInfo.getType())
                          .append(' ')
                          .append(parameterInfo.getName());
                        }
                    }
                else
                    {
                    sb.append("void");
                    }

                sb.append("</td>\n* <td align='center'>")
                  .append(formatType(sType))
                  .append("</td>\n* <td>")
                  .append(escapeHtml(sDescription))
                  .append("</td>\n* </tr>");
                }

            sb.append("\n* </table>\n* </blockquote>\n");
            }

        return sb.toString();
        }

    /**
     * Sort the supplied array of MBeanFeatureInfo instances.
     *
     * @param aInfo the array to sort
     *
     * @return the sorted array
     */
    protected static MBeanFeatureInfo[] sortMBeanFeatureInfo(MBeanFeatureInfo[] aInfo)
        {
        int  cInfo = aInfo.length;
        List list  = new ArrayList(cInfo);

        for (int i = 0; i < cInfo; i++)
            {
            list.add(aInfo[i]);
            }

        Collections.sort(list, new MBeanFeatureInfoComparator());

        return (MBeanFeatureInfo[]) list.toArray(aInfo);
        }

    /**
    * Format the given type string as returned from an MBeanAttributeInfo or
    * MBeanOperationInfo.
    *
    * @param sType the type string to format
    *
    * @return the formated type string
    */
    protected static String formatType(String sType)
        {
        if (sType == null || sType.length() < 1)
            {
            throw new IllegalArgumentException();
            }

        if (sType.length() == 1)
            {
            switch (sType.charAt(0))
                {
                case 'Z': // boolean
                    return "boolean";
                case 'C': // char
                    return "char";
                case 'B': // byte
                    return "byte";
                case 'S': // short
                    return "short";
                case 'I': // int
                    return "int";
                case 'J': // long
                    return "long";
                case 'F': // float
                    return "float";
                case 'D': // double
                    return "double";
                case 'V':
                    return "void";
                }
            }

        if (sType.charAt(0) == '[')
            {
            return formatType(sType.substring(1, sType.length())) + "[]";
            }

        if (sType.charAt(0) == 'L' && sType.endsWith(";"))
            {
            sType = sType.substring(1, sType.length() - 1);
            }

        // drop the package name from the class name
        sType = sType.substring(sType.lastIndexOf('.') + 1);

        // display the Void type as void
        if (sType.equals("Void"))
            {
            sType = "void";
            }

        return sType;
        }

    /**
    * Escape the given HTML string.
    *
    * @param str  input String
    *
    * @return an escaped string
    */
    protected static String escapeHtml(String str)
        {
        if (str == null || str.length() == 0)
            {
            return str;
            }

        StringBuilder sb = new StringBuilder(str.length());
        for (char c : str.toCharArray())
            {
            if (c > 127 || c == '<' || c == '>' || c == '&')
                {
                sb.append("&#").append((int)c).append(';');
                }
            else
                {
                sb.append(c);
                }
            }

        return sb.toString();
        }


    // ----- MBeanFeatureInfoComparator inner class -------------------------

    /**
    * Comparator implementation that compares two MBeanFeatureInfo instances.
    */
    public static class MBeanFeatureInfoComparator
            implements Comparator
        {
        // ----- constructors -----------------------------------------------

        /**
        * Default constructor.
        */
        public MBeanFeatureInfoComparator()
            {
            }


        // ----- Comparator implementation ----------------------------------

        /**
        * Compare the two supplied objects for order.  Return a negative
        * integer, zero, or a positive integer as the first argument is less
        * than, equal to, or greater than the second.
        *
        * @param o1  the first object to compare
        * @param o2  the second object to compare
        *
        * @return the result of the comparision
        */
        public int compare(Object o1, Object o2)
            {
            MBeanFeatureInfo info1 = (MBeanFeatureInfo) o1;
            MBeanFeatureInfo info2 = (MBeanFeatureInfo) o2;

            return info1.getName().compareTo(info2.getName());
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the path to the target Java source file.
    *
    * @return the path to the target Java source file
    */
    public File getFile()
        {
        return m_file;
        }

    /**
    * Set the path to the target Java source file.
    *
    * @param file the path to the target Java source file
    */
    public void setFile(File file)
        {
        m_file = file;
        }

    /**
    * Get the comma or space delimited list of MBean components to document.
    *
    * @return the list of MBean components
    */
    public String getMBeans()
        {
        return m_sMBeans;
        }

    /**
    * Set the comma or space delimited list of MBean components to document.
    *
    * @param sMBeans the list of MBean components
    */
    public void setMBeans(String sMBeans)
        {
        m_sMBeans = sMBeans;
        }

    /**
    * Get the token in the Java source file that will be replaced with the
    * generated JavaDoc.
    *
    * @return the name of the token
    */
    public String getToken()
        {
        return m_sToken;
        }

    /**
    * Set the token in the Java source file that will be replaced with the
    * generated JavaDoc.
    *
    * @param sToken the token to replace
    */
    public void setToken(String sToken)
        {
        m_sToken = sToken;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The package that contains MBean components.
    */
    public static final String MBEAN_PACKAGE =
            "com.tangosol.coherence.component.manageable.modelAdapter";

    /**
    * The path to the Java source file to be parameterized.
    */
    private File m_file;

    /**
    * A comma or space delimited list of MBean components to document.
    */
    private String m_sMBeans;

    /**
    * The name of a token in the Java source file that will be replaced with
    * the generated JavaDoc.
    */
    private String m_sToken;
    }
