/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.run.xml;


import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.util.Base;
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

import javax.xml.XMLConstants;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

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
import org.xml.sax.XMLReader;
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

            ResourceResolver resolver      = new ResourceResolver(this.getClass());
            SchemaFactory    schemaFactory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            configureRequiredSchemaFactoryProtections(schemaFactory);

            schemaFactory.setResourceResolver(resolver);
            // allows only local schema includes, so bundled XSDs can include
            // other bundled XSDs while remote schemas remain blocked
            try
                {
                Schema            schema    = schemaFactory.newSchema(resolveSchemaSources(listSchemaURIs));
                Source            source    = new StreamSource(new StringReader(sXml));
                Validator         validator = schema.newValidator();
                ValidationHandler handler   = new ValidationHandler();

                configureRequiredValidatorProtections(validator);
                validator.setErrorHandler(handler);
                validator.validate(source);

                // optimize error handling to report all errors
                // prior to failing; this is easier for user that
                // has multiple problems to config files.
                if (handler.isError())
                    {
                    throw (handler.getException());
                    }
                }
            finally
                {
                resolver.closeStreams();
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

            if (url != null && isExternalSchemaSourceUrl(url))
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

    private static boolean isExternalSchemaSourceUrl(URL url)
        {
        return isExternalSchemaSourceSpec(url.toExternalForm(), false);
        }

    private static boolean isExternalSchemaSourceSpec(String sSpec, boolean fInJar)
        {
        int ofColon = sSpec.indexOf(':');
        if (ofColon <= 0)
            {
            return fInJar;
            }

        String sProtocol = sSpec.substring(0, ofColon);
        if ("http".equalsIgnoreCase(sProtocol) || "https".equalsIgnoreCase(sProtocol))
            {
            return true;
            }
        if ("file".equalsIgnoreCase(sProtocol))
            {
            return false;
            }
        if ("jar".equalsIgnoreCase(sProtocol))
            {
            return isExternalJarSchemaSourceSpec(sSpec.substring(ofColon + 1));
            }
        return fInJar;
        }

    private static boolean isExternalJarSchemaSourceSpec(String sSpec)
        {
        int ofSeparator = sSpec.indexOf("!/");
        String sNested = ofSeparator < 0 ? sSpec : sSpec.substring(0, ofSeparator);
        return sNested.isEmpty() || isExternalSchemaSourceSpec(sNested, true);
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
     * Apply the required parser-factory protections.
     *
     * @param factory  the parser factory to protect
     *
     * @throws SAXException if a required protection cannot be applied
     */
    static void configureRequiredParserFactoryProtections(SAXParserFactory factory)
            throws SAXException
        {
        for (RequiredXmlProtection protection : REQUIRED_XML_PROTECTIONS)
            {
            if (protection.isParserFeature())
                {
                try
                    {
                    factory.setFeature(protection.getName(), protection.getBooleanValue());
                    }
                catch (Exception e)
                    {
                    handleRequiredProtectionFailure("parser factory", factory.getClass(), protection, e);
                    }
                }
            }
        }

    /**
     * Apply the required parser protections.
     *
     * @param parser  the parser to protect
     *
     * @throws SAXException if a required protection cannot be applied
     */
    static void configureRequiredParserProtections(Parser parser)
            throws SAXException
        {
        for (RequiredXmlProtection protection : REQUIRED_XML_PROTECTIONS)
            {
            if (protection.isParserFeature())
                {
                try
                    {
                    setParserFeature(parser, protection);
                    }
                catch (Exception e)
                    {
                    handleRequiredProtectionFailure("parser", parser.getClass(), protection, e);
                    }
                }
            }
        }

    /**
     * Apply the required schema-factory protections.
     *
     * @param factory  the schema factory to protect
     *
     * @throws SAXException if a required protection cannot be applied
     */
    static void configureRequiredSchemaFactoryProtections(SchemaFactory factory)
            throws SAXException
        {
        for (RequiredXmlProtection protection : REQUIRED_XML_PROTECTIONS)
            {
            if (protection.isValidationProperty())
                {
                try
                    {
                    factory.setProperty(protection.getName(), protection.getStringValue());
                    }
                catch (Exception e)
                    {
                    handleRequiredProtectionFailure("schema factory", factory.getClass(), protection, e);
                    }
                }
            }
        }

    /**
     * Apply the required validator protections.
     *
     * @param validator  the validator to protect
     *
     * @throws SAXException if a required protection cannot be applied
     */
    static void configureRequiredValidatorProtections(Validator validator)
            throws SAXException
        {
        for (RequiredXmlProtection protection : REQUIRED_XML_PROTECTIONS)
            {
            if (protection.isValidationProperty())
                {
                try
                    {
                    validator.setProperty(protection.getName(), protection.getStringValue());
                    }
                catch (Exception e)
                    {
                    handleRequiredProtectionFailure("validator", validator.getClass(), protection, e);
                    }
                }
            }
        }

    /**
     * Reset static parser state for tests.
     */
    static void resetForTesting()
        {
        synchronized (SaxParser.class)
            {
            s_parser     = null;
            s_parserMode = null;
            }
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
        CoherenceMode mode   = CoherenceMode.current();
        Parser        parser = s_parser;

        if (parser == null || s_parserMode != mode)
            {
            synchronized (SaxParser.class)
                {
                parser = s_parser;
                if (parser == null || s_parserMode != mode)
                    {
                    parser = createParser();
                    configureRequiredParserProtections(parser);
                    s_parser     = parser;
                    s_parserMode = mode;
                    }
                }
            }
        return parser;
        }

    /**
     * Create a parser instance using the modern factory path, with LEGACY
     * retaining the old fallback.
     *
     * @return a parser instance
     *
     * @throws Exception if the parser cannot be created safely
     */
    private static Parser createParser()
            throws Exception
        {
        SAXParserFactory factory = null;
        try
            {
            factory = SAXParserFactory.newInstance();

            factory.setValidating(false);
            configureRequiredParserFactoryProtections(factory);
            return factory.newSAXParser().getParser();
            }
        catch (Exception e)
            {
            if (CoherenceMode.isXmlExternalEntityProtectionRequired())
                {
                // keeps DEV/PROD from falling back to a parser that may not
                // support the required XXE protections
                throw new SAXException("Unable to create a SAX parser with required XML external-entity protections", e);
                }
            String sFactoryClass = factory == null ? "unavailable" : factory.getClass().getName();
            Logger.warn("SaxParser legacy compatibility is using the deprecated parser fallback after protected "
                    + "parser factory setup failed. Parser factory: " + sFactoryClass
                    + System.lineSeparator() + "Error: " + e.getLocalizedMessage());
            return ParserFactory.makeParser();
            }
        }

    /**
     * Set a parser feature on either a SAX2-capable parser or a legacy parser
     * implementation with a compatible reflective method.
     *
     * @param parser      the parser
     * @param protection  the protection to apply
     *
     * @throws Exception if the feature cannot be applied
     */
    private static void setParserFeature(Parser parser, RequiredXmlProtection protection)
            throws Exception
        {
        if (parser instanceof XMLReader)
            {
            ((XMLReader) parser).setFeature(protection.getName(), protection.getBooleanValue());
            }
        else
            {
            parser.getClass().getMethod("setFeature", String.class, boolean.class)
                    .invoke(parser, protection.getName(), protection.getBooleanValue());
            }
        }

    /**
     * Fail closed in hardened modes or keep LEGACY warn-and-continue
     * compatibility.
     *
     * @param sTarget     the protected target type
     * @param clzTarget   the protected target class
     * @param protection  the missing protection
     * @param e           the failure
     *
     * @throws SAXException if the current mode requires the protection
     */
    private static void handleRequiredProtectionFailure(String sTarget, Class<?> clzTarget,
                                                       RequiredXmlProtection protection, Exception e)
            throws SAXException
        {
        String sMessage = "XML protection '" + protection.getName() + "' could not be applied to "
                + sTarget + " " + clzTarget.getName();

        if (CoherenceMode.isXmlExternalEntityProtectionRequired())
            {
            // requires DEV/PROD to fail closed instead of parsing XML when an
            // XXE protection is unavailable
            throw new SAXException(sMessage, e);
            }

        Logger.warn(sMessage + "; continuing in LEGACY compatibility mode"
                + System.lineSeparator() + "Error: " + e.getLocalizedMessage());
        }


    // ----- data members ---------------------------------------------------

    /*
    * Non-validating SAX parser
    */
    private static Parser s_parser;

    /*
    * Coherence mode used when the cached SAX parser was created
    */
    private static CoherenceMode s_parserMode;

    /**
     * Required parser and validation protections for XXE hardening.
     */
    private static final RequiredXmlProtection[] REQUIRED_XML_PROTECTIONS =
        {
        RequiredXmlProtection.parserFeature("http://apache.org/xml/features/disallow-doctype-decl", true),
        RequiredXmlProtection.parserFeature("http://xml.org/sax/features/external-general-entities", false),
        RequiredXmlProtection.parserFeature("http://xml.org/sax/features/external-parameter-entities", false),
        RequiredXmlProtection.parserFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false),
        RequiredXmlProtection.validationProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""),
        RequiredXmlProtection.validationProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file")
        };

    // ----- inner class: RequiredXmlProtection -----------------------------

    /**
     * One member of the central XML hardening set.
     */
    private static final class RequiredXmlProtection
        {
        private RequiredXmlProtection(String sName, Boolean oBooleanValue, String sStringValue, int nTarget)
            {
            f_sName         = sName;
            f_oBooleanValue = oBooleanValue;
            f_sStringValue  = sStringValue;
            f_nTarget       = nTarget;
            }

        /**
         * Create a parser feature protection.
         *
         * @param sName   the feature name
         * @param fValue  the required value
         *
         * @return the parser feature protection
         */
        static RequiredXmlProtection parserFeature(String sName, boolean fValue)
            {
            return new RequiredXmlProtection(sName, Boolean.valueOf(fValue), null, TARGET_PARSER_FEATURE);
            }

        /**
         * Create a validation property protection.
         *
         * @param sName   the property name
         * @param sValue  the required value
         *
         * @return the validation property protection
         */
        static RequiredXmlProtection validationProperty(String sName, String sValue)
            {
            return new RequiredXmlProtection(sName, null, sValue, TARGET_VALIDATION_PROPERTY);
            }

        /**
         * Return the protection name.
         *
         * @return the protection name
         */
        String getName()
            {
            return f_sName;
            }

        /**
         * Return the required boolean value.
         *
         * @return the required boolean value
         */
        boolean getBooleanValue()
            {
            return f_oBooleanValue.booleanValue();
            }

        /**
         * Return the required string value.
         *
         * @return the required string value
         */
        String getStringValue()
            {
            return f_sStringValue;
            }

        /**
         * Return {@code true} if this protection is a parser feature.
         *
         * @return {@code true} for parser features
         */
        boolean isParserFeature()
            {
            return f_nTarget == TARGET_PARSER_FEATURE;
            }

        /**
         * Return {@code true} if this protection is a validation property.
         *
         * @return {@code true} for validation properties
         */
        boolean isValidationProperty()
            {
            return f_nTarget == TARGET_VALIDATION_PROPERTY;
            }

        private static final int TARGET_PARSER_FEATURE      = 1;
        private static final int TARGET_VALIDATION_PROPERTY = 2;

        private final String  f_sName;
        private final Boolean f_oBooleanValue;
        private final String  f_sStringValue;
        private final int     f_nTarget;
        }


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

    }
