/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.run.xml;


import com.oracle.coherence.common.base.Logger;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Resources;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import  java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.XMLConstants;

import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import org.xml.sax.AttributeList;
import org.xml.sax.DocumentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.ParserFactory;


/**
* A simple XML parser.  The public interface consists of nearly identical
* methods: parseXml(...) which produce a tree of SimpleElement objects
*
* @author gg  2000.10.23
*/
public class SaxParser
        extends Base
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a SaxParser.
    */
    public SaxParser()
        {
        this(false);
        }

    /**
    * Construct a SaxParser.
    *
    * @param  fAllowComments  if true, the resulting tree may contain
    *                         the XMLValue nodes that contain comments;
    *                         otherwize all comments are ignored
    */
    public SaxParser(boolean fAllowComments)
        {
        if (fAllowComments)
            {
            throw new UnsupportedOperationException("XML comments are not supported");
            }
        }

    /**
    * Unit test: create a simple parser, parse and output the result.
    *
    * @param asParam  an array of parameters
    */
    public static void main(String[] asParam)
        {
        if (asParam.length > 0)
            {
            SaxParser parser = new SaxParser();

            try
                {
                FileInputStream in = new FileInputStream(asParam[0]);

                XmlElement root = parser.parseXml(in);

                root.writeXml(getOut(), true);
                out();
                }
            catch (Exception e)
                {
                out(e);
                }
            }
        }


    // ----- public API    --------------------------------------------------

    /**
    * Parse the specified String into a tree of XmlElement objects
    * ignoring any XML nodes other than elements, text or comments
    * (in a case of SaxParser that allows comments).
    * In addition, the text value is trimmed for all nodes except leafs.
    *
    * @param sXml  the XML as string
    *
    * @return the generated XmlElement
    *
    * @throws SAXException  if SAX error occurs
    */
    public XmlElement parseXml(String sXml)
            throws SAXException
        {
        return parseXml(new InputSource(new StringReader(sXml)), null);
        }

    /**
    * Parse the specified String into a tree of XmlElement objects
    * (same as above) having the specified [empty] XmlElement a root.
    *
    * Note: this method is used by de-serialization
    *       (see SimpleElement#readExternal)
    *
    * @param sXml    the XML as string
    * @param elRoot  the XML root
    *
    * @return the generated XmlElement
    *
    * @throws SAXException  if SAX error occurs
    */
    public XmlElement parseXml(String sXml, XmlElement elRoot)
            throws SAXException
        {
        return parseXml(new InputSource(new StringReader(sXml)), elRoot);
        }

    /**
    * Parse the specified InputStream into a tree of XmlElement objects
    * ignoring any XML nodes other than elements, text or comments
    * (in a case of SaxParser that allows comments).
    * In addition, the text value is trimmed for all nodes except leafs.
    *
    * @param input  the InputStream
    *
    * @return the generated XmlElement
    *
    * @throws SAXException  if SAX error occurs
    */
    public XmlElement parseXml(InputStream input)
            throws SAXException
        {
        return parseXml(new InputSource(input), null);
        }

    /**
    * Parse the specified Reader into a tree of XmlElement objects
    * ignoring any XML nodes other than elements, text or comments
    * (in a case of SaxParser that allows comments).
    * In addition, the text value is trimmed for all nodes except leafs.
    *
    * @param input  the input Reader
    *
    * @return the generated XmlElement
    *
    * @throws SAXException  if SAX error occurs
    */
    public XmlElement parseXml(Reader input)
            throws SAXException
        {
        return parseXml(new InputSource(input), null);
        }

    /**
    * Parse the specified InputSource into a tree of XmlElement objects
    * ignoring any XML nodes other than elements, text or comments
    * (in a case of SaxParser that allows comments).
    * In addition, the text value is trimmed for all nodes except leafs.
    *
    * @param input  the InputSource
    *
    * @return the generated XmlElement
    *
    * @throws SAXException  if SAX error occurs
    */
    public XmlElement parseXml(InputSource input)
            throws SAXException
        {
        return parseXml(input, null);
        }

    /**
    * Actual implementation...
    *
    * @param input    the InputSource
    * @param xmlRoot  the XML root
    *
    * @return the generated XmlElement
    *
    * @throws SAXException  if SAX error occurs
    */
    protected XmlElement parseXml(InputSource input, XmlElement xmlRoot)
            throws SAXException
        {
        try
            {
            Parser        parser  = getParser();
            SimpleHandler handler = new SimpleHandler(xmlRoot);

            parser.setDocumentHandler(handler);
            parser.setErrorHandler(handler);

            parser.parse(input);

            xmlRoot = handler.m_root;

            if (xmlRoot == null)
                {
                throw new SAXException("Empty document");
                }
            return xmlRoot;
            }
        catch (Exception e)
            {
            throw (e instanceof SAXException ? (SAXException) e : new SAXException(e));
            }
        }

    /**
    * XSD aware parsing routine; if XML contains an XSD reference
    * to a schemeLocation/noNamespaceSchemaLocation then parse XML
    * using provided XSD for validation.
    *
    * @param sXml  the XML to parse (as a string)
    * @param xml   the XML document object used to obtain schema locations
    *
    * @throws SAXException  if XML contains an XSD reference and does not
    *                       pass validation
    * @throws IOException   if XML contains a schema that cannot be loaded
    * @throws ParserConfigurationException  if a parser cannot be created
    */
    public void validateXsd(String sXml, XmlDocument xml)
            throws SAXException, IOException, ParserConfigurationException
        {
        if (sXml != null)
            {
            List<String> listSchemaURIs = XmlHelper.getSchemaLocations(
                    xml, XmlHelper.getNamespacePrefix(xml,
                            XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI));

            // only validate if we have schemaLocations specified
            if (listSchemaURIs.isEmpty())
                {
                return;
                }

            ResourceResolver resolver      = null;
            SchemaFactory    schemaFactory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            if (s_fJdk22 && "strict".equalsIgnoreCase(System.getProperty("javax.xml.catalog.resolve")))
                {
                // specifying a custom resolver is a workaround for issue reported in OWLS-116652
                resolver = new ResourceResolver(this.getClass());
                schemaFactory.setResourceResolver(resolver);
                }

            Schema            schema    = schemaFactory.newSchema(resolveSchemaSources(listSchemaURIs));
            Source            source    = new StreamSource(new StringReader(sXml));
            Validator         validator = schema.newValidator();
            ValidationHandler handler   = new ValidationHandler();

            if (ATTEMPT_RESTRICT_EXTERNAL.get())
                {
                try
                    {
                    // Disable access during parsing to external resolution to avoid XXE vulnerabilities

                    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                    }
                catch (Exception e)
                    {
                    // property not supported, warn once and don't attempt to set property again
                    if (ATTEMPT_RESTRICT_EXTERNAL.compareAndSet(true, false))
                        {
                        Logger.warn("Validator does not support JAXP 1.5 properties to restrict access to external XML DTDs and Schemas." + System.lineSeparator() +
                            "To guard against XXE vulnerabilities, ensure provided XML parser is secure." + System.lineSeparator() +
                            "Validator: " + validator.getClass().getCanonicalName() + System.lineSeparator() +
                            "Error: " + e.getLocalizedMessage());
                        }
                    }
                }
            validator.setErrorHandler(handler);
            validator.validate(source);

            if (resolver != null)
                {
                resolver.closeStreams();
                }

            // optimize error handling to report all errors
            // prior to failing; this is easier for user that
            // has multiple problems to config files.
            if (handler.isError())
                {
                throw (handler.getException());
                }
            }
        }

    /**
    * For a given set of XSD URIs, return the {@link Source}s to be
    * used by the XML parser to validate an XML document.
    *
    * @param listUri  list of XSD URIs to convert
    *
    * @return an array of {@link Source}s to be used by the XML parser
    *
    * @throws IOException  if the resource cannot be located or loaded
    */
    protected Source[] resolveSchemaSources(List<String> listUri)
            throws IOException
        {
        List<Source> listSources = new ArrayList<Source>();
        for (String sUri : listUri)
            {
            URL url = Resources.findFileOrResource(
                    sUri, getClass().getClassLoader());

            // do not load schemas over http or https;
            // strip the URL to just ending file name and load from classpath
            if (url != null && ("http".equalsIgnoreCase(url.getProtocol()) ||
                                "https".equalsIgnoreCase(url.getProtocol())))
                {
                url = Resources.findFileOrResource(
                        sUri.substring(sUri.lastIndexOf('/') + 1),
                        getClass().getClassLoader());
                }

            if (url == null)
                {
                throw new IOException("The specified schema "
                        + sUri + " cannot be found.");
                }

            try
                {
                URLConnection con = url.openConnection();
                con.setConnectTimeout(30000);

                StreamSource source = new StreamSource(con.getInputStream());
                source.setSystemId(url.toString());
                listSources.add(source);
                }
            catch (Throwable t)
                {
                throw new IOException("Unexpected exception resolving schema uri: " + sUri, t);
                }
            }
        return listSources.toArray(new Source[listUri.size()]);
        }


    // ----- inner class: ValidationHandler ---------------------------------

    /**
    * An inner class Error Handler that is registered in
    * parser that performs validation. It gets called when
    * on warning, error, or fatalError.
    */
    protected class ValidationHandler
            implements ErrorHandler
        {
        // ----- ErrorHandler interface -------------------------------------

        /**
        * Routine called when a warning occurs in parser. Logs
        * the warning message.
        *
        * @param e  SAXParseException is warning exception.
        */
        @Override
        public void warning(SAXParseException e)
                throws SAXException
            {
            // Don't fail on warning,  display message
            Logger.warn("Warning " + e.toString() + " - line  " + e.getLineNumber());
            }

        /**
        * Routine called when a error occurs in parser. Logs
        * the error message, saves the first exception and
        * increments an errorCounter. Error count and
        * exception can be retrieved when parsing is complete.
        *
        * @param e  SAXParseException is error exception.
        */
        @Override
        public void error(SAXParseException e)
                throws SAXException
            {
            // Display all errors before failing
            Logger.err("Error " + e.toString() + " - line " + e.getLineNumber());
            m_cError++;
            if (m_eParser == null)
                {
                m_eParser = e;
                }
            }

        /**
         * Routine called when a fatal error occurs in parser. Logs
         * the fatal error message and throws the exception.
         *
         * @param e  SAXParseException is fatal error exception.
         *
         * @throws SAXException  if SAX error occurs
         */
        @Override
        public void fatalError(SAXParseException e)
                throws SAXException
            {
            Logger.err("Fatal Error " + e.toString() + " - line " + e.getLineNumber());
            throw e;
            }


        // ----- accessors --------------------------------------------------

        /**
        * Returns the number of Errors encountered.
        *
        * @return int error count
        */
        public int getErrorCount()
            {
            return m_cError;
            }

        /**
        * Returns a saved parser exception.
        *
        * @return SAXParseException Parser exception
        */
        public SAXParseException getException()
            {
            return m_eParser;
            }

        /**
        * Returns boolean indicating if an error has occurred.
        *
        * @return boolean true if error else false
        */
        public boolean isError()
            {
            return (m_cError > 0) && (m_eParser != null);
            }


        // ----- data members -----------------------------------------------

        /**
        * Saved parser exception.
        */
        private SAXParseException m_eParser;

        /**
        * Error counter.
        */
        private int               m_cError;
        }


    // ----- factory methods ------------------------------------------------

    /**
    * Instantiate an XmlElement implementation for a root element.
    *
    * @param sRoot root name
    * @return a new XmlElement to be used as the root element
    */
    protected XmlElement instantiateRoot(String sRoot)
        {
        return new SimpleElement(sRoot, null);
        }

    /**
    * Get an instance of non-validating SAX parser.
    *
    * @return a SAX parser
    *
    * @throws Exception  if an error occurs
    */
    protected static Parser getParser()
            throws Exception
        {
        Parser parser = s_parser;

        if (parser == null)
            {
            // first try to use the SAX plugability layer
            // (using reflection to allow for a legacy environment like WL5.1)
            // if that fails, use the SAX API directly
            try
                {
                // SAXParserFactory factory = SAXParserFactory.newInstance();
                Class  clzFactory = Class.forName("javax.xml.parsers.SAXParserFactory");
                Object factory    = ClassHelper.invokeStatic(clzFactory,
                                        "newInstance", ClassHelper.VOID);

                // factory.setValidating(false);
                ClassHelper.invoke(factory,
                    "setValidating", new Object[] {Boolean.FALSE});

                // parser = factory.newSAXParser().getParser();
                Object SAXParser = ClassHelper.invoke(factory,
                    "newSAXParser", ClassHelper.VOID);
                parser = (Parser) ClassHelper.invoke(SAXParser,
                    "getParser", ClassHelper.VOID);
                }
            catch (Throwable e)
                {
                }

            if (parser == null)
                {
                parser = ParserFactory.makeParser();
                }

            s_parser = parser;
            }
        return parser;
        }


    // ----- data members ---------------------------------------------------

    /*
    * Non-validating SAX parser
    */
    private static Parser s_parser;


    // ----- SimpleHandler inner class --------------------------------------

    class SimpleHandler
            implements DocumentHandler, ErrorHandler
        {
        // ----- Constructors -----------------------------------------------

        SimpleHandler(XmlElement root)
            {
            azzert(root == null || root.isMutable());

            m_root = root;
            }

        // ----- DocumentHandler interface ----------------------------------

        /**
        * Receive an object for locating the origin of SAX document events.
        */
        public void setDocumentLocator(Locator l)
            {
            }

        /**
        * Receive notification of the beginning of a document.
        */
        public void startDocument()
                throws SAXException
            {
            }

        /**
        * Receive notification of the end of a document.
        */
        public void endDocument()
                throws SAXException
            {
            azzert(m_current == null);
            }

        /**
        * Receive notification of the beginning of an element.
        */
        public void startElement(String sTag, AttributeList attrs)
                throws SAXException
            {
            XmlElement el = m_current;
            if (el == null)
                {
                if (m_root == null)
                    {
                    m_root = instantiateRoot(sTag);
                    }
                else
                    {
                    m_root.setName(sTag);
                    }
                el = m_root;
                }
            else
                {
                el = el.addElement(sTag);
                }

            int cAttrs = attrs.getLength();
            for (int i = 0; i < cAttrs; i++)
                {
                String sName  = attrs.getName(i);
                String sValue = attrs.getValue(i);
                el.addAttribute(sName).setString(sValue);
                }
            m_current = el;
            }

        /**
        * Receive notification of the end of an element.
        */
        public void endElement(String sTag)
                throws SAXException
            {
            XmlElement el = m_current;

            if (!el.getElementList().isEmpty())
                {
                // trim the text at nodes that are not leafs
                String sText = el.getString();
                if (sText != null)
                    {
                    el.setString(sText.trim());
                    }
                }

            m_current = el.getParent();
            }

        /**
        * Receive notification of character data.
        *
        * <p>The Parser will call this method to report each chunk of
        * character data.  SAX parsers may return all contiguous character
        * data in a single chunk, or they may split it into several
        * chunks; however, all of the characters in any single event
        * must come from the same external entity, so that the Locator
        * provides useful information.</p>
        *
        * <p>The application must not attempt to read from the array
        * outside of the specified range.</p>
        *
        * <p>Note that some parsers will report whitespace using the
        * ignorableWhitespace() method rather than this one (validating
        * parsers must do so).</p>
        *
        * @param ach    The characters from the XML document.
        * @param offset The start position in the array.
        * @param cnt    The number of characters to read from the array.
        * @exception org.xml.sax.SAXException Any SAX exception, possibly
        *            wrapping another exception.
        */
        public void characters(char ach[], int offset, int cnt)
                throws SAXException
            {
            if (cnt > 0)
                {
                XmlElement el = m_current;

                String sValue = el.getString();
                String sText  = new String(ach, offset, cnt);

                el.setString(sValue + sText);
                }
            }

        /**
        * Receive notification of ignorable whitespace in element content.
        *
        * <p>Validating Parsers must use this method to report each chunk
        * of ignorable whitespace (see the W3C XML 1.0 recommendation,
        * section 2.10): non-validating parsers may also use this method
        * if they are capable of parsing and using content models.</p>
        *
        * <p>SAX parsers may return all contiguous whitespace in a single
        * chunk, or they may split it into several chunks; however, all of
        * the characters in any single event must come from the same
        * external entity, so that the Locator provides useful
        * information.</p>
        *
        * <p>The application must not attempt to read from the array
        * outside of the specified range.</p>
        *
        * @param buf    The characters from the XML document
        * @param offset The start position in the array
        * @param len    The number of characters to read from the array
        * @exception org.xml.sax.SAXException Any SAX exception, possibly
        *            wrapping another exception
        * @see #characters
        */
        public void ignorableWhitespace(char buf[], int offset, int len)
                throws SAXException
            {
            }

        /**
        * Receive notification of the beginning of a document.
        */
        public void processingInstruction(String target, String data)
                throws SAXException
            {
            }

        // ----- ErrorHandler interface -------------------------------------

        /**
        * Receive notification of a warning.
        */
        public void warning(SAXParseException exception)
                throws SAXException
            {
            }

        /**
        * Receive notification of a recoverable error.
        */
        public void error(SAXParseException exception)
                throws SAXException
            {
            throw exception;
            }

        /**
        * Receive notification of a non-recoverable error.
        */
        public void fatalError(SAXParseException exception)
                throws SAXException
            {
            throw exception;
            }

        private XmlElement m_root;
        private XmlElement m_current;
        }

    // ----- inner class: Input ---------------------------------------------

    /**
     * An LSInput implementation that is used when Java 22+ strict XML catalog resolve is enabled.
     * This class is required to work around the issue that is described in OWLS-116652
     *
     * @since 24.03
     */
    public static class Input
            implements LSInput
        {
        // ----- constructors -----------------------------------------------

        public Input(InputStream is, String publicId, String systemId)
            {
            this.is       = is;
            this.publicId = publicId;
            this.systemId = systemId;
            }

        // ----- LSInput interface ------------------------------------------

        @Override
        public Reader getCharacterStream()
            {
            return null;
            }

        @Override
        public void setCharacterStream(Reader characterStream)
            {
            }

        @Override
        public InputStream getByteStream()
            {
            return this.is;
            }

        @Override
        public void setByteStream(InputStream byteStream)
            {
            }

        @Override
        public String getStringData()
            {
            return null;
            }

        @Override
        public void setStringData(String stringData)
            {
            }

        @Override
        public String getSystemId()
            {
            return this.systemId;
            }

        @Override
        public void setSystemId(String systemId)
            {
            }

        @Override
        public String getPublicId()
            {
            return this.publicId;
            }

        @Override
        public void setPublicId(String publicId)
            {
            }
        @Override
        public String getBaseURI()
            {
            return null;
            }

        @Override
        public void setBaseURI(String baseURI)
            {
            }

        @Override
        public String getEncoding()
            {
            return null;
            }

        @Override
        public void setEncoding(String encoding)
            {
            }

        @Override
        public boolean getCertifiedText()
            {
            return false;
            }

        @Override
        public void setCertifiedText(boolean certifiedText)
            {
            }

        // ----- data members -----------------------------------------------

        private InputStream is;

        private String publicId;

        private String systemId;
        }

    // ----- inner class: ResourceResolver ----------------------------------

    /**
     * An ResourceResolver implementation that is used when Java 22+ strict XML catalog resolve is enabled.
     * This class is required to work around the issue that is described in OWLS-116652
     *
     * @since 24.03
     */
    public static class ResourceResolver
            implements LSResourceResolver
        {
        // ----- constructors -----------------------------------------------

        ResourceResolver(Class<?> clazz)
            {
            this.clazz = clazz;
            }

        // ----- LSResourceResolver interface -------------------------------

        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI)
            {
            InputStream is = this.clazz.getResourceAsStream("/" + systemId);

            if (is == null)
                {
                Logger.warn("SaxParser.ResourceResolver.resolveResource: failed to resolve \"/" + systemId + " resolution: " + baseURI + "/" + systemId);
                return null;
                }
            else
                {
                this.streamsToClose.add(is);
                return new Input(is, publicId, systemId);
                }
            }

        /**
         * Close all streams created by this resolver.
         */
        void closeStreams()
            {
            Iterator iter = this.streamsToClose.iterator();

            while (iter.hasNext())
                {
                InputStream is = (InputStream) iter.next();
                if (is != null)
                    {
                    try
                        {
                        is.close();
                        }
                    catch (IOException e)
                        {
                        }
                    }
                }
            }

        // ----- data members -----------------------------------------------

        private List<InputStream> streamsToClose = Collections.synchronizedList(new ArrayList());

        private Class<?> clazz;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Record if resolved SaxParser supports JAXP 1.5 {@link XMLConstants#ACCESS_EXTERNAL_DTD} and {@link XMLConstants#ACCESS_EXTERNAL_SCHEMA} properties. Only report warning once if does not.
     */
    private static final AtomicBoolean ATTEMPT_RESTRICT_EXTERNAL = new AtomicBoolean(true);

    /**
     * True iff if jvm runtime is JDK 22 or higher.
     */
    private static boolean s_fJdk22 = Runtime.version().feature() > 21;
    }
