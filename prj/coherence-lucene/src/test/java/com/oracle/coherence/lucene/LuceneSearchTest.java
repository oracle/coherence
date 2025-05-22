 /*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.lucene;

 import com.tangosol.io.pof.ConfigurablePofContext;
 import com.tangosol.io.pof.PofContext;
 import com.tangosol.io.pof.PofReader;
 import com.tangosol.io.pof.PofWriter;
 import com.tangosol.io.pof.PortableObject;
 import com.tangosol.util.ExternalizableHelper;
 import com.tangosol.util.Filters;
 import com.tangosol.util.ValueExtractor;
 import com.tangosol.util.extractor.UniversalExtractor;

 import java.io.ByteArrayInputStream;
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
 import java.io.IOException;

 import org.apache.lucene.search.Query;
 
 import org.junit.jupiter.api.Assertions;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;

 import static org.junit.jupiter.api.Assertions.assertEquals;
 
 /**
  * Unit tests for the {@link LuceneSearch} class.
  *
  * @author Aleks Seovic  2025.05.17
  */
 public class LuceneSearchTest
     {
     private static final ValueExtractor<String, String> CONTENT = new UniversalExtractor<>("content");
     private static final LuceneQueryParser QUERY_PARSER = LuceneQueryParser.create(CONTENT);

     private Query query;
     private PofContext pofContext;

     @BeforeEach
     void setUp()
         {
         query      = QUERY_PARSER.parse("machine learning");
         pofContext = new ConfigurablePofContext();
         }

     @Test
     void shouldHandlePofSerializationAndDeserialization()
         {
         // Test serialization of the index
         var search = new LuceneSearch<>(CONTENT, query, 10).filter(Filters.always());
         var binary = ExternalizableHelper.toBinary(search, pofContext);

         LuceneSearch<String, TestDocument> deserializedSearch = ExternalizableHelper.fromBinary(binary, pofContext);

         // Verify the deserialized index
         assertEquals(search.getExtractor(), deserializedSearch.getExtractor());
         assertEquals(search.getMaxResults(), deserializedSearch.getMaxResults());
         assertEquals(search.getFilter(), deserializedSearch.getFilter());
         assertEquals(search.getQuery(), deserializedSearch.getQuery());
         }

     @Test
     void shouldFailSerializationUnlessPofIsUsed()
         {
         try
             {
             new LuceneSearch<>(CONTENT, query, 10).readExternal(new DataInputStream(new ByteArrayInputStream(new byte[1])));
             Assertions.fail("Expected IOException");
             }
         catch (IOException e)
             {
             assertEquals("FullTextSearch requires POF serialization", e.getMessage());
             }
         }

     @Test
     void shouldFailDeserializationUnlessPofIsUsed()
         {
         try
             {
             new LuceneSearch<>(CONTENT, query, 10).writeExternal(new DataOutputStream(null));
             Assertions.fail("Expected IOException");
             }
         catch (IOException e)
             {
             assertEquals("FullTextSearch requires POF serialization", e.getMessage());
             }
         }

     /**
      * Test document class for unit tests.
      */
     @SuppressWarnings("unused")
     public static class TestDocument implements PortableObject
         {
         private String content;

         public TestDocument()
             {
             }

         public TestDocument(String content)
             {
             this.content = content;
             }

         public String getContent()
             {
             return content;
             }

         public void readExternal(PofReader in) throws IOException
             {
             content = in.readString(0);
             }

         public void writeExternal(PofWriter out) throws IOException
             {
             out.writeString(0, content);
             }
         }
     }
