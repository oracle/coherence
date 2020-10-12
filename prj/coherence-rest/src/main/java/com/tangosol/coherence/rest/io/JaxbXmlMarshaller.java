/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.io;

import com.tangosol.coherence.config.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import javax.xml.transform.stream.StreamSource;

/**
 * JAXB-based marshaller that marshals object to/from XML.
 *
 * @author as  2011.07.10
 */
public class JaxbXmlMarshaller<T>
        extends AbstractMarshaller<T>
    {

    // ---- constructors ----------------------------------------------------

    /**
     * Construct an JaxbXmlMarshaller instance.
     *
     * @param clzRoot  class of the root object this marshaller is for
     */
    public JaxbXmlMarshaller(Class<T> clzRoot)
        {
        super(clzRoot);
        try
            {
            m_ctx = JAXBContext.newInstance(clzRoot);
            }
        catch (JAXBException e)
            {
            throw new IllegalArgumentException(
                    "error creating JAXB context for class \""
                    + clzRoot.getName() + "\"", e);
            }
        }

    // ---- Marshaller implementation ---------------------------------------

    @Override
    public void marshal(T value, OutputStream out, MultivaluedMap<String, Object> httpHeaders) throws IOException
        {
        try
            {
            javax.xml.bind.Marshaller marshaller = m_ctx.createMarshaller();
            configureJaxbMarshaller(marshaller);
            marshaller.marshal(value, out);
            }
        catch (JAXBException e)
            {
            throw new IOException(e);
            }
        }

    @Override
    public void marshalAsFragment(T value, OutputStream out, MultivaluedMap<String, Object> httpHeaders) throws IOException
        {
        try
            {
            javax.xml.bind.Marshaller marshaller = m_ctx.createMarshaller();
            configureJaxbMarshaller(marshaller);
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FRAGMENT, true);
            marshaller.marshal(value, out);
            }
        catch (JAXBException e)
            {
            throw new IOException(e);
            }
        }

    @Override
    public T unmarshal(InputStream in, MediaType mediaType) throws IOException
        {
        try
            {
            Unmarshaller unmarshaller = m_ctx.createUnmarshaller();
            configureJaxbUnmarshaller(unmarshaller);

            XMLInputFactory xif = XMLInputFactory.newFactory();
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, Config.getBoolean("coherence.rest.xml.allowDTD"));
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Config.getBoolean("coherence.rest.xml.allowExternalEntities"));
            return unmarshaller.unmarshal(xif.createXMLStreamReader(new StreamSource(in)), getRootClass()).getValue();
            }
        catch (JAXBException e)
            {
            throw new IOException(e);
            }
        catch (XMLStreamException e)
            {
            throw new IOException("XML stream reader exception.", e);
            }
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Configure a JAXB marshaller.
     *
     * @param marshaller  marshaller to configure
     *
     * @throws PropertyException when there is an error processing a property
     *         or value
     */
    protected void configureJaxbMarshaller(javax.xml.bind.Marshaller marshaller)
            throws PropertyException
        {
        marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT,
                               Config.getBoolean(FORMAT_OUTPUT));
        }

    /**
     * Configure a JAXB unmarshaller.
     *
     * @param unmarshaller  unmarshaller to configure
     *
     * @throws PropertyException when there is an error processing a property
     *         or value
     */
    protected void configureJaxbUnmarshaller(Unmarshaller unmarshaller)
            throws PropertyException
        {
        }

    // ---- data members ----------------------------------------------------

    /**
     * JAXB context to use for marshalling.
     */
    private JAXBContext m_ctx;
    }
