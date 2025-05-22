 /*
  * Copyright (c) 2025 Oracle and/or its affiliates.
  *
  * Licensed under the Universal Permissive License v 1.0 as shown at
  * https://oss.oracle.com/licenses/upl.
  */

 package com.oracle.coherence.lucene;

 import com.tangosol.internal.util.invoke.ClassDefinition;
 import com.tangosol.internal.util.invoke.RemoteConstructor;
 import com.tangosol.internal.util.invoke.lambda.AnonymousLambdaIdentity;
 import com.tangosol.internal.util.invoke.lambda.MethodReferenceIdentity;
 import com.tangosol.internal.util.invoke.lambda.StaticLambdaInfo;
 import com.tangosol.io.Serializer;
 import com.tangosol.io.pof.PofReader;
 import com.tangosol.io.pof.PofWriter;
 import com.tangosol.io.pof.PortableObject;
 import com.tangosol.io.pof.PortableObjectSerializer;
 import com.tangosol.io.pof.SimplePofContext;
 import com.tangosol.net.BackingMapContext;
 import com.tangosol.net.BackingMapManagerContext;
 import com.tangosol.util.Binary;
 import com.tangosol.util.BinaryEntry;
 import com.tangosol.util.MapIndex;
 import com.tangosol.util.ObservableMap;
 import com.tangosol.util.ValueExtractor;
 import com.tangosol.util.ValueUpdater;
 import com.tangosol.util.extractor.UniversalExtractor;

 import java.io.ByteArrayInputStream;
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
 import java.io.IOException;
 import java.nio.file.Path;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.Map;

 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.fr.FrenchAnalyzer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.TextField;
 import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.queryparser.classic.QueryParser;
 import org.apache.lucene.sandbox.search.QueryProfilerIndexSearcher;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.similarities.BM25Similarity;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.store.MMapDirectory;
 import org.apache.lucene.util.BytesRef;
 
 import org.junit.jupiter.api.Assertions;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;

 import static com.tangosol.util.ExternalizableHelper.fromBinary;
 import static com.tangosol.util.ExternalizableHelper.toBinary;
 
 import static org.junit.jupiter.api.Assertions.assertEquals;
 import static org.junit.jupiter.api.Assertions.assertNotNull;
 import static org.junit.jupiter.api.Assertions.assertTrue;

 /**
  * Unit tests for the {@link LuceneIndex} class.
  *
  * @author Aleks Seovic  2025.05.16
  */
 @SuppressWarnings({"unchecked", "rawtypes"})
 public class LuceneIndexTest
     {
     private LuceneIndex<String, TestDocument> index;
     private ValueExtractor<TestDocument, String> extractor;
     private Map<ValueExtractor<TestDocument, String>, MapIndex> indexMap;
     private SimplePofContext pofContext;
     private LuceneQueryParser queryParser;

     @BeforeEach
     void setUp()
         {
         extractor = new UniversalExtractor<>("content");
         index = new LuceneIndex<>(extractor);
         indexMap = new HashMap<>();
         queryParser = LuceneQueryParser.create(extractor);

         pofContext = new SimplePofContext();
         pofContext.registerUserType(1, TestDocument.class, new PortableObjectSerializer(1));
         pofContext.registerUserType(2, LuceneIndex.class, new PortableObjectSerializer(2));
         pofContext.registerUserType(3, LuceneIndex.Config.class, new PortableObjectSerializer(3));

         pofContext.registerUserType(10, UniversalExtractor.class, new PortableObjectSerializer(10));
         pofContext.registerUserType(11, RemoteConstructor.class, new PortableObjectSerializer(11));
         pofContext.registerUserType(12, ClassDefinition.class, new PortableObjectSerializer(12));
         pofContext.registerUserType(13, AnonymousLambdaIdentity.class, new PortableObjectSerializer(13));
         pofContext.registerUserType(14, MethodReferenceIdentity.class, new PortableObjectSerializer(14));
         pofContext.registerUserType(15, StaticLambdaInfo.class, new PortableObjectSerializer(15));
         }

     @Test
     void shouldCreateAndSearchIndex()
         {
         // Create index
         var mapIndex = (LuceneIndex<String, TestDocument>.LuceneMapIndex) index.createIndex(false, null, indexMap, null);

         // Setup and insert test document
         var entry = new SimpleBinaryEntry<>("doc1", new TestDocument("This is a test document about machine learning"), pofContext);
         mapIndex.insert(entry);

         // Search using NlpQueryBuilder
         var query   = queryParser.parse("machine learning");
         var results = mapIndex.search(query, 10);

         assertNotNull(results);
         assertEquals(1, results.size());
         assertTrue(results.containsKey(entry.getBinaryKey()));
         }

     @Test
     void shouldInsertUpdateAndDeleteDocument()
         {
         // Create index
         var mapIndex = (LuceneIndex<String, TestDocument>.LuceneMapIndex) index.createIndex(false, null, indexMap, null);

         // Setup initial document
         var entry = new SimpleBinaryEntry<>("doc1", new TestDocument("Initial document about artificial intelligence"), pofContext);
         mapIndex.insert(entry);
         dumpIndex("After insert:", mapIndex);

         // Verify initial document is searchable
         var query   = queryParser.parse("artificial intelligence");
         var results = mapIndex.search(query, 10);
         assertEquals(1, results.size());

         // Update document
         entry.setValue(new TestDocument("Updated document about machine learning"));
         mapIndex.update(entry);
         dumpIndex("After update:", mapIndex);

         // Verify update
         query   = queryParser.parse("artificial intelligence");
         results = mapIndex.search(query, 10);
         assertTrue(results.isEmpty());

         query = queryParser.parse("machine learning");
         assertEquals(1, mapIndex.search(query, 10).size());

         // Delete document
         mapIndex.delete(entry);
         dumpIndex("After delete:", mapIndex);

         // Verify deletion
         results = mapIndex.search(query, 10);
         assertTrue(results.isEmpty());
         }

     @Test
     void shouldHandlePofSerializationAndDeserialization() throws IOException
         {
         var index = new LuceneIndex<>(extractor)
                 .analyzer(FrenchAnalyzer::new)
                 .directory(nPart ->
                                {
                                try
                                    {
                                    return MMapDirectory.open(Path.of(".lucene", "index-test", String.valueOf(nPart)));
                                    }
                                catch (IOException e)
                                    {
                                    throw new RuntimeException(e);
                                    }
                                })
                 .configureIndexWriter(cfg -> cfg.setSimilarity(new BM25Similarity(1.2f, 0.3f)))
                 .searcher((cur, prev) -> new QueryProfilerIndexSearcher(cur));

         // Test serialization of the index
         Binary binary = toBinary(index, pofContext);
         LuceneIndex<String, TestDocument> deserializedIndex = fromBinary(binary, pofContext);

         // Verify the deserialized index
         assertEquals(index.getValueExtractor(), deserializedIndex.getValueExtractor());
         assertEquals(index.isInverseMapEnabled(), deserializedIndex.isInverseMapEnabled());
         assertConfig(index.getConfig(), deserializedIndex.getConfig());
         }

     private void assertConfig(LuceneIndex.Config c1, LuceneIndex.Config c2)
             throws IOException
         {
         assertEquals(roundTrip(c1.analyzerSupplier()), c2.analyzerSupplier());
         assertEquals(roundTrip(c1.directorySupplier()), c2.directorySupplier());
         assertEquals(roundTrip(c1.searcherSupplier()), c2.searcherSupplier());
         assertEquals(roundTrip(c1.writerConfigurer()), c2.writerConfigurer());

         IndexWriterConfig cfg = new IndexWriterConfig(c2.analyzerSupplier().get());
         c2.writerConfigurer().accept(cfg);

         assertTrue(cfg.getAnalyzer() instanceof FrenchAnalyzer);
         assertEquals(new BM25Similarity(1.2f, 0.3f).toString(), cfg.getSimilarity().toString());

         try (Directory dir = c2.directorySupplier().apply(1))
             {
             assertTrue(dir instanceof MMapDirectory);

             IndexWriter writer = new IndexWriter(dir, cfg);
             Document doc = new Document();
             doc.add(new TextField("body", "Lucene is awesome", Field.Store.YES));
             writer.addDocument(doc);
             writer.commit();

             try (DirectoryReader reader = DirectoryReader.open(dir))
                 {
                 assertTrue(c2.searcherSupplier().apply(reader, null) instanceof QueryProfilerIndexSearcher);
                 }
             }
         catch (IOException e)
             {
             throw new RuntimeException(e);
             }
         }

     private Object roundTrip(Object o)
         {
         return fromBinary(toBinary(o, pofContext), pofContext);
         }

     @Test
     void shouldFailSerializationUnlessPofIsUsed()
         {
         try
             {
             new LuceneIndex<>(extractor).readExternal(new DataInputStream(new ByteArrayInputStream(new byte[1])));
             Assertions.fail("Expected IOException");
             }
         catch (IOException e)
             {
             assertEquals("LuceneIndex requires POF serialization", e.getMessage());
             }
         }

     @Test
     void shouldFailDeserializationUnlessPofIsUsed()
         {
         try
             {
             new LuceneIndex<>(extractor).writeExternal(new DataOutputStream(null));
             Assertions.fail("Expected IOException");
             }
         catch (IOException e)
             {
             assertEquals("LuceneIndex requires POF serialization", e.getMessage());
             }
         }

     @Test
     void shouldConfigureCustomSearcherSupplier()
         {
         // Configure index with custom searcher supplier
         index.searcher((cur, prev) ->
                 {
                 IndexSearcher searcher = new IndexSearcher(cur);
                 // Configure custom similarity as an example
                 searcher.setSimilarity(new BM25Similarity(1.2f, 0.75f));
                 return searcher;
                 });

         // Create index and add a test document
         var mapIndex = (LuceneIndex<String, TestDocument>.LuceneMapIndex) index.createIndex(false, null, indexMap, null);

         var entry = new SimpleBinaryEntry<>("doc1", new TestDocument("Custom searcher test document"), pofContext);
         mapIndex.insert(entry);

         // Verify that search still works with custom searcher
         var query   = queryParser.parse("custom searcher");
         var results = mapIndex.search(query, 10);

         assertNotNull(results);
         assertEquals(1, results.size());
         assertTrue(results.containsKey(entry.getBinaryKey()));
         }

    /**
     * Test French analyzer's stemming behavior.
     * <p>
     * The French analyzer in Lucene applies different stemming rules to different verb forms,
     * preserving some grammatical distinctions. For example, with the verb "étudier":
     * - Present tense "étudient" -> "etudient"
     * - Infinitive "étudier" -> "etudi"
     * - Present tense "étudie" -> "etud"
     * - Present tense "étudions" -> "etudion"
     * <p>
     * This means that searching for the infinitive form won't match conjugated forms in the text,
     * and vice versa. This is by design, as the French analyzer tries to balance stemming with
     * preserving meaningful grammatical differences.
     */
    @Test
    void shouldVerifyAnalyzerEffects() throws Exception
        {
        // Create test document with French text containing "étudient" (they study)
        String frenchText = "Les élèves étudient le français à l'école";
        var entry = new SimpleBinaryEntry<>("doc1", new TestDocument(frenchText), pofContext);

        // Configure index to use FrenchAnalyzer for both indexing and searching
        LuceneIndex<String, TestDocument> frenchIndex = new LuceneIndex<String, TestDocument>(extractor).analyzer(FrenchAnalyzer::new);
        var frenchMapIndex = (LuceneIndex<String, TestDocument>.LuceneMapIndex) frenchIndex.createIndex(false, null, indexMap, null);
        frenchMapIndex.insert(entry);
        frenchMapIndex.commit();

        try
            {
            // Create analyzer and query parser instances that match the ones used by the index
            var frenchAnalyzer = new FrenchAnalyzer();
            var frenchQueryBuilder = new QueryParser(extractor.getCanonicalName(), frenchAnalyzer);

            // Part 1: Verify exact form matching
            // When searching for "étudient", it should match because both the indexed text
            // and the query term are stemmed to "etudient"
            var exactQuery = frenchQueryBuilder.parse("étudient");
            var exactResults = frenchMapIndex.search(exactQuery, 10);
            assertEquals(1, exactResults.size(),
                         "French analyzer should match exact form 'étudient'");

            // Part 2: Verify non-matching forms
            // When searching for "étudier" (infinitive), it should NOT match because it gets
            // stemmed to "etudi" while the text form is stemmed to "etudient"
            var infinitiveQuery = frenchQueryBuilder.parse("étudier");
            var infinitiveResults = frenchMapIndex.search(infinitiveQuery, 10);
            assertEquals(0, infinitiveResults.size(),
                         "French analyzer should not match infinitive form 'étudier' as it has a different stem");

            // Part 3: Demonstrate complete stemming behavior
            System.out.println("\nDemonstrating French analyzer stemming behavior:");

            // First show how the document text is analyzed
            System.out.println("\nDocument text: " + frenchText);
            TokenStream stream = frenchAnalyzer.tokenStream("content", frenchText);
            CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            System.out.println("Document terms after stemming:");
            while (stream.incrementToken())
                {
                System.out.println("  " + termAttr.toString());
                }
            stream.close();

            // Then show how different forms of the same verb are stemmed differently
            String[] verbForms = {"étudient", "étudier", "étudie", "étudions"};
            for (String form : verbForms)
                {
                stream = frenchAnalyzer.tokenStream("content", form);
                termAttr = stream.addAttribute(CharTermAttribute.class);
                stream.reset();
                stream.incrementToken();
                System.out.println(form + " -> " + termAttr.toString());
                stream.close();
                }
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        finally
            {
            frenchMapIndex.close();
            }
        }

     /**
      * Debug helper to dump Lucene index contents.
      */
     private void dumpIndex(String message, LuceneIndex<String, TestDocument>.LuceneMapIndex mapIndex)
         {
         try
             {
             System.out.println("\n" + message);
             IndexSearcher searcher = mapIndex.getSearcher();
             try
                 {
                 IndexReader reader = searcher.getIndexReader();
                 System.out.println("Total docs: " + reader.numDocs());
                 System.out.println("Max doc: " + reader.maxDoc());
                 System.out.println("Deleted docs: " + reader.numDeletedDocs());

                 // Print all documents
                 for (int i = 0; i < reader.maxDoc(); i++)
                     {
                     Document doc = searcher.storedFields().document(i);
                     BytesRef key = doc.getBinaryValue("key");
                     String content = doc.get(extractor.getCanonicalName());
                     System.out.println("Doc " + i + ": key=" + Arrays.toString(key.bytes) + ", content=" + content);
                     }
                 }
             finally
                 {
                 mapIndex.releaseSearcher(searcher);
                 }
             }
         catch (IOException e)
             {
             throw new RuntimeException(e);
             }
         }

     /**
      * Test document class for unit tests.
      */
     @SuppressWarnings("unused")
     public static class TestDocument
             implements PortableObject
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

     /**
      * Simple BinaryEntry implementation for testing.
      */
     static class SimpleBinaryEntry<K, V>
             implements BinaryEntry<K, V>
         {
         private final Binary binKey;
         private V value;
         private final SimplePofContext pofContext;

         SimpleBinaryEntry(K key, V value, SimplePofContext pofContext)
             {
             this.binKey = toBinary(key, pofContext);
             this.value = value;
             this.pofContext = pofContext;
             }

         @Override
         public Binary getBinaryKey()
             {
             return binKey;
             }

         @Override
         public Binary getBinaryValue()
             {
             return toBinary(value, pofContext);
             }

         public Serializer getSerializer()
             {
             return pofContext;
             }

         public BackingMapManagerContext getContext()
             {
             return null;
             }

         public void updateBinaryValue(Binary binValue)
             {
             }

         public void updateBinaryValue(Binary binValue, boolean fSynthetic)
             {
             }

         public V getOriginalValue()
             {
             return null;
             }

         public Binary getOriginalBinaryValue()
             {
             return null;
             }

         public ObservableMap<K, V> getBackingMap()
             {
             return null;
             }

         public BackingMapContext getBackingMapContext()
             {
             return null;
             }

         public void expire(long cMillis)
             {
             }

         public long getExpiry()
             {
             return 0;
             }

         public boolean isReadOnly()
             {
             return false;
             }

         public void setValue(V value, boolean fSynthetic)
             {
             }

         public <T> void update(ValueUpdater<V, T> updater, T value)
             {
             }

         public boolean isPresent()
             {
             return false;
             }

         public boolean isSynthetic()
             {
             return false;
             }

         public void remove(boolean fSynthetic)
             {
             }

         @Override
         public K getKey()
             {
             return fromBinary(binKey, pofContext);
             }

         @Override
         public V getValue()
             {
             return value;
             }

         public V setValue(V value)
             {
             V old = this.value;
             this.value = value;
             return old;
             }

         public <T, E> E extract(ValueExtractor<T, E> extractor)
             {
             return extractor.extract((T) value);
             }
         }
     }
