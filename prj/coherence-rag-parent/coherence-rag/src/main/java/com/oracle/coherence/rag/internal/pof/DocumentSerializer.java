/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.internal.pof;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * POF serializer for LangChain4J Document objects.
 * <p/>
 * This class provides serialization and deserialization support for
 * {@link Document} objects using Coherence POF (Portable Object Format).
 * It enables efficient storage and transmission of document objects
 * across Coherence cluster nodes while preserving all document content
 * and metadata.
 * <p/>
 * The serializer handles both the document text content and its associated
 * metadata map, ensuring that all document information is preserved
 * during the serialization process.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
public class DocumentSerializer implements PofSerializer<Document>
    {
    /**
     * Serializes a Document object to the POF stream.
     * <p/>
     * This method writes the document's text content and metadata to the
     * POF output stream in a structured format that can be efficiently
     * read back during deserialization.
     * 
     * @param out the POF writer to serialize the document to
     * @param document the Document object to serialize
     * 
     * @throws IOException if an I/O error occurs during serialization
     */
    public void serialize(PofWriter out, Document document)
            throws IOException
        {
        out.writeString(0, document.text());
        out.writeMap(1, document.metadata().toMap(), String.class);
        out.writeRemainder(null);
        }

    /**
     * Deserializes a Document object from the POF stream.
     * <p/>
     * This method reads the document's text content and metadata from the
     * POF input stream and reconstructs a complete Document object with
     * all original information preserved.
     * 
     * @param in the POF reader to deserialize the document from
     * 
     * @return a new Document object with the deserialized content and metadata
     * 
     * @throws IOException if an I/O error occurs during deserialization
     */
    public Document deserialize(PofReader in) throws IOException
        {
        String   text     = in.readString(0);
        Metadata metadata = Metadata.from(in.readMap(1, new LinkedHashMap<>()));
        in.readRemainder();

        return Document.from(text, metadata);
        }
    }
