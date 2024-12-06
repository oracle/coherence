/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests.search;

import com.oracle.coherence.ai.DocumentChunk;
import com.oracle.coherence.ai.Float32Vector;
import com.oracle.coherence.ai.Vector;
import com.oracle.coherence.ai.search.SimilaritySearch;
import com.oracle.coherence.ai.util.Vectors;

import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.util.ValueExtractor;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class BaseSimilaritySearchIT
    {

    @Test
    public void shouldSearch()
        {
        ValueExtractor<DocumentChunk, Vector<float[]>> extractor = ValueExtractor.of(DocumentChunk::vector);

        NamedMap<Integer, DocumentChunk> vectors = m_session.getMap("vectors");

        Vector<float[]> vector = m_valueZero.vector();

        var results = vectors.aggregate(new SimilaritySearch<>(extractor, vector, 10));
        assertThat(results.size(), is(10));
        }

    @Test
    public void shouldSearchAsync()
        {
        ValueExtractor<DocumentChunk, Vector<float[]>> extractor = ValueExtractor.of(DocumentChunk::vector);

        NamedMap<Integer, DocumentChunk> vectors = m_session.getMap("vectors");

        Vector<float[]> vector = m_valueZero.vector();

        var results = vectors.async().aggregate(new SimilaritySearch<>(extractor, vector, 10));
        assertThat(results.join().size(), is(10));
        }

    public static DocumentChunk populateVectors(NamedMap<Integer, DocumentChunk> vectors)
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

        DocumentChunk[] values = new DocumentChunk[10000];

        for (int i = 0; i < matches.length; i++)
            {
            values[i] = new DocumentChunk(String.valueOf(i), new Float32Vector(Vectors.normalize(matches[i])));
            vectors.put(i, values[i]);
            }
        for (int i = matches.length; i < values.length; i++)
            {
            values[i] = new DocumentChunk(String.valueOf(i), new Float32Vector(Vectors.normalize(randomFloats(DIMENSIONS))));
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

    // ----- data members ---------------------------------------------------

    public static final int DIMENSIONS = 384;

    protected static Session m_session;

    protected static DocumentChunk m_valueZero;

    protected static final Random m_random = new Random(System.currentTimeMillis());
    }
