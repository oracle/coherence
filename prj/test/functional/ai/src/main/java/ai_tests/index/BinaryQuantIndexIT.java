/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests.index;

import com.oracle.coherence.ai.Float32Vector;
import com.oracle.coherence.ai.Vector;
import com.oracle.coherence.ai.index.BinaryQuantIndex;
import com.oracle.coherence.ai.search.SimilaritySearch;
import com.oracle.coherence.ai.util.Vectors;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.filter.InFilter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class BinaryQuantIndexIT
    {
    @BeforeAll
    @SuppressWarnings("resource")
    static void setup() throws Exception
        {
        String sAddress = "127.0.0.1";
        System.setProperty("coherence.wka", sAddress);
        System.setProperty("coherence.localhost", sAddress);
        System.setProperty("test.unicast.address", sAddress);
        System.setProperty("test.unicast.port", "0");
        System.setProperty("coherence.ttl", "0");

        System.setProperty("coherence.distributed.partitioncount", "13");

        Coherence coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        m_session = coherence.getSession();

        NamedMap<Integer, ValueWithVector> vectors = m_session.getMap("vectors");
        vectors.addIndex(new BinaryQuantIndex<>(ValueExtractor.of(ValueWithVector::getVector)));
        m_valueZero = populateVectors(vectors);
        }

    @AfterAll
    static void cleanup()
        {
        Coherence.closeAll();
        }

    @Test
    public void shouldSearch()
        {
        ValueExtractor<ValueWithVector, Vector<float[]>> extractor = ValueExtractor.of(ValueWithVector::getVector);

        NamedMap<Integer, ValueWithVector> vectors = m_session.getMap("vectors");

        Vector<float[]> vector = m_valueZero.getVector();
        int             k      = 10;

        SimilaritySearch<Integer, ValueWithVector, float[]> similaritySearch = new SimilaritySearch<>(extractor, vector, k);

        long startTimeHnsw = System.nanoTime();
        var  resultsHnsw = vectors.aggregate(similaritySearch);
        long endTimeHnsw = System.nanoTime();
        System.out.println("******* BinaryQuant ********");
        resultsHnsw.forEach(System.out::println);
        System.out.println("BinaryQuant took " + (endTimeHnsw - startTimeHnsw) + " ns");

        assertThat(resultsHnsw.size(), is(k));

        long startTimeBruteForce = System.nanoTime();
        var  results = vectors.aggregate(similaritySearch.bruteForce());
        long endTimeBruteForce = System.nanoTime();
        System.out.println("******* Brute Force ********");
        results.forEach(System.out::println);
        System.out.println("Brute Force took " + (endTimeBruteForce - startTimeBruteForce) + " ns");

        assertThat(results.size(), is(k));
        }

    @Test
    public void shouldSearchWithFilter()
        {
        ValueExtractor<ValueWithVector, Vector<float[]>> extractor = ValueExtractor.of(ValueWithVector::getVector);
        ValueExtractor<ValueWithVector, Integer> extractorFilter = ValueExtractor.of(ValueWithVector::getNumber);

        NamedMap<Integer, ValueWithVector> vectors = m_session.getMap("vectors");

        Set<Integer>    setMatch = Set.of(0, 1, 2, 3);
        Filter<?>       filter   = new InFilter<>(extractorFilter, setMatch);
        Vector<float[]> vector   = m_valueZero.getVector();
        int             k        = 5;

        SimilaritySearch<Integer, ValueWithVector, float[]> similaritySearch = new SimilaritySearch<>(extractor, vector, k);

        long startTimeHnsw = System.nanoTime();
        var  resultsHnsw = vectors.aggregate(similaritySearch.filter(filter));
        long endTimeHnsw = System.nanoTime();
        System.out.println("******* BinaryQuant ********");
        resultsHnsw.forEach(System.out::println);
        System.out.println("BinaryQuant took " + (endTimeHnsw - startTimeHnsw) + " ns");

        assertThat(resultsHnsw.size(), is(setMatch.size()));

        long startTimeBruteForce = System.nanoTime();
        var results = vectors.aggregate(similaritySearch.filter(filter).bruteForce());;
        long endTimeBruteForce = System.nanoTime();
        System.out.println("******* Brute Force ********");
        results.forEach(System.out::println);
        System.out.println("Brute Force took " + (endTimeBruteForce - startTimeBruteForce) + " ns");

        assertThat(results.size(), is(setMatch.size()));
        }

    public static ValueWithVector populateVectors(NamedMap<Integer, ValueWithVector> vectors)
        {
        float[][] matches = new float[5][];
        matches[0] = randomFloats(DIMENSIONS);
        matches[1] = Arrays.copyOf(matches[0], matches[0].length);
        matches[2] = Arrays.copyOf(matches[0], matches[0].length);
        matches[3] = Arrays.copyOf(matches[0], matches[0].length);
        matches[4] = Arrays.copyOf(matches[0], matches[0].length);
        matches[1][0] = matches[1][0] + 1.0f;
        matches[2][0] = matches[2][0] + 1.0f;
        matches[3][0] = matches[3][0] + 1.0f;
        matches[4][0] = matches[4][0] + 1.0f;

        ValueWithVector[] values = new ValueWithVector[10000];

        for (int i = 0; i < matches.length; i++)
            {
            values[i] = new ValueWithVector(new Float32Vector(Vectors.normalize(matches[i])), String.valueOf(i), i);
            vectors.put(i, values[i]);
            }
        for (int i = matches.length; i < values.length; i++)
            {
            values[i] = new ValueWithVector(new Float32Vector(Vectors.normalize(randomFloats(DIMENSIONS))), String.valueOf(i), i);
            vectors.put(i, values[i]);
            }

        return values[0];
        }

    public static float[] randomFloats(int n)
        {
        float[] floats = new float[n];
        for (int i = 0; i < n; i++)
            {
            floats[i] = m_random.nextFloat(-50, 50);
            }
        return floats;
        }

    // ----- inner class: ValueWithVector -----------------------------------

    /**
     * A simple test holder for a vector and a text value.
     */
    public static class ValueWithVector
            implements ExternalizableLite, PortableObject
        {
        public ValueWithVector()
            {
            }

        public ValueWithVector(Vector<float[]> vector, String text, int n)
            {
            this.vector = vector;
            this.text   = text;
            this.number = n;
            }

        public Vector<float[]> getVector()
            {
            return vector;
            }

        public String getText()
            {
            return text;
            }

        public int getNumber()
            {
            return number;
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            vector = in.readObject(0);
            text   = in.readString(1);
            number = in.readInt(2);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, vector);
            out.writeString(1, text);
            out.writeInt(2, number);
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            vector = ExternalizableHelper.readObject(in);
            text   = ExternalizableHelper.readSafeUTF(in);
            number = ExternalizableHelper.readInt(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, vector);
            ExternalizableHelper.writeSafeUTF(out, text);
            ExternalizableHelper.writeInt(out, number);
            }

        @Override
        public String toString()
            {
            return "ValueWithVector{" +
                    "vector=" + vector +
                    ", text='" + text + '\'' +
                    ", number=" + number +
                    '}';
            }

        // ----- data members ---------------------------------------------------

        private Vector<float[]> vector;

        private String text;

        private int number;
        }

    // ----- data members ---------------------------------------------------

    public static final int DIMENSIONS = 384;

    private static Session m_session;

    private static ValueWithVector m_valueZero;

    private static final Random m_random = new Random(System.currentTimeMillis());
    }
