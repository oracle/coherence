/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.vectors;

import com.oracle.coherence.ai.Float32Vector;
import com.oracle.coherence.ai.QueryResult;
import com.oracle.coherence.ai.Vector;

import com.oracle.coherence.ai.hnsw.HnswIndex;

import com.oracle.coherence.ai.search.SimilaritySearch;

import com.oracle.coherence.ai.util.Vectors;

import com.oracle.coherence.io.json.JsonObject;
import com.oracle.coherence.io.json.JsonSerializer;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.util.Extractors;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.Resources;
import com.tangosol.util.ValueExtractor;

import dev.langchain4j.data.embedding.Embedding;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;

import dev.langchain4j.model.output.Response;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.List;
import java.util.zip.GZIPInputStream;

public class MovieRepository
    {
    /**
     * The name of the field in the json containing the embeddings.
     */
    public static final String VECTOR_FIELD = "embeddings";

    /**
     * The {@link NamedMap} to store document chunks and vectors.
     */
    private final NamedMap<String, JsonObject> movies;

    /**
     * The embedding model to use to create vectors.
     */
    private final EmbeddingModel model;

    /**
     * The {@link ValueExtractor} to extract the embeddings vector from the json.
     */
    private final ValueExtractor<JsonObject, Vector<float[]>> extractor = Extractors.extract(VECTOR_FIELD);

    /**
     * Create a {@link MovieRepository} that uses the {@link AllMiniLmL6V2EmbeddingModel}
     * embedding model.
     *
     * @param movies  the {@link NamedMap} to store movie data and vectors
     */
    public MovieRepository(NamedMap<String, JsonObject> movies) {
        this(movies, new AllMiniLmL6V2EmbeddingModel());
    }

    /**
     * Create a {@link MovieRepository}.
     *
     * @param movies  the {@link NamedMap} to store movie data and vectors
     * @param model   the embedding model to use
     */
    public MovieRepository(NamedMap<String, JsonObject> movies, EmbeddingModel model) {
        this.movies = movies;
        this.model  = model;
        // Add the HNSW Index
        this.movies.addIndex(new HnswIndex<>(extractor, model.dimension()));
    }

    /**
     * Load the movie data from the specified input stream into the cache.
     *
     * @param in an input stream containing JSON data to load
     *
     * @throws IOException if there is an error reading the data
     */
    @SuppressWarnings("unchecked")
    public void load(InputStream in) throws IOException {
        JsonSerializer serializer = new JsonSerializer();
        List<JsonObject> list = serializer.deserialize(in.readAllBytes(), List.class);

        for (JsonObject jsonObject : list) {
            String plot = (String) jsonObject.get("fullplot");
            Vector<float[]> vector = vectorize(plot);
            jsonObject.put(VECTOR_FIELD, vector);
            movies.put(jsonObject.getString("title"), jsonObject);
        }
    }

    /**
     * Perform a Knn search on the movie data.
     *
     * @param text    the text to match on the movie plot
     * @param count   the count of the nearest matches to return
     *
     * @return the list of search results
     */
    public List<QueryResult<String, JsonObject>> search(String text, int count) {
        return search(text, Filters.always(), count);
    }

    /**
     * Perform a Knn search on the movie data.
     *
     * @param text    the text to match on the movie plot
     * @param filter  a {@link Filter} to use to further reduce the movies to be queried
     * @param count   the count of the nearest matches to return
     *
     * @return the list of search results
     */
    public List<QueryResult<String, JsonObject>> search(String text, Filter<JsonObject> filter, int count) {
        Vector<float[]> vector = vectorize(text);
        SimilaritySearch<String, JsonObject, float[]> search = new SimilaritySearch<>(extractor, vector, count);
        return movies.aggregate(filter, search);
    }

    /**
     * Return the vector embedding for the specified text.
     *
     * @param s  the text to obtain the embeddings for
     *
     * @return the embeddings for the specified text
     */
    public Vector<float[]> vectorize(String s) {
        Response<Embedding> response  = model.embed(s);
        Embedding embedding = response.content();
        return new Float32Vector(Vectors.normalize(embedding.vector()));
    }

    /**
     * Load the movie embeddings into the cache.
     *
     * @param args  ignored
     */
    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception {
        try (Coherence coherence = Coherence.client().start().join();
            Session session = coherence.getSession()) {
            NamedMap<String, JsonObject> movies = session.getMap("movies");

            MovieRepository loader = new MovieRepository(movies);

            URL url = Resources.findFileOrResource("movies.json.gzip", null);
            try (GZIPInputStream in = new GZIPInputStream(url.openStream())) {
                loader.load(in);
            }
        }
    }
}
