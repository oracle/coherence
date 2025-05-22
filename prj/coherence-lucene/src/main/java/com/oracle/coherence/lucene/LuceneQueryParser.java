/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.lucene;

import com.tangosol.util.ValueExtractor;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.synonym.SolrSynonymParser;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.CharsRef;

/**
 * A thread-safe, reusable, and highly configurable Lucene query parser
 * builder.
 * <p>
 * This class allows you to fluently configure and construct a Lucene
 * {@link QueryParser} or {@link MultiFieldQueryParser} for use in full-text
 * search scenarios. It supports:
 * <ul>
 *   <li>Multiple fields with optional boosts</li>
 *   <li>Custom analyzers</li>
 *   <li>Stop word removal</li>
 *   <li>Synonym expansion (from map, Solr, or WordNet files)</li>
 *   <li>Custom query preprocessing</li>
 *   <li>Custom parser configuration and custom parser injection</li>
 * </ul>
 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * // Basic usage with a single field
 * LuceneQueryParser parser = LuceneQueryParser.create(Document::getBody);
 * Query             query  = parser.parse("search terms");
 *
 * // Advanced usage with boosts, stop words, synonyms, and parser config
 * LuceneQueryParser parser = LuceneQueryParser.builder()
 *     .field(Document::getTitle, 2.0f)
 *     .field(Document::getBody)
 *     .stopWords(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET)
 *     .synonyms(Map.of("car", List.of("automobile", "vehicle")))
 *     .configureParser(qp -> qp.setDefaultOperator(QueryParser.Operator.AND))
 *     .build();
 *
 * // Using synonyms from a Solr-format file
 * LuceneQueryParser parser = LuceneQueryParser.builder()
 *     .field(myFieldExtractor)
 *     .synonyms(LuceneQueryParser.solrSynonyms(Paths.get("synonyms.txt"), true))
 *     .build();
 *
 * // Using a custom QueryParser instance
 * QueryParser custom = new MyCustomQueryParser("field", new StandardAnalyzer());
 * LuceneQueryParser parser = LuceneQueryParser.builder()
 *     .parser(custom)
 *     .build();
 * }</pre>
 *
 * @author Aleks Seovic  2025.05.20
 * @since 25.09
 */
public class LuceneQueryParser
    {
    /**
     * Constructs a new LuceneQueryParser.
     *
     * @param parser       the Lucene QueryParser to use
     * @param preprocessor the preprocessor function to apply to queries
     */
    private LuceneQueryParser(QueryParser parser, Function<String, String> preprocessor)
        {
        f_parser = parser;
        f_preprocessor = preprocessor;
        }

    /**
     * Parses the given user query string into a Lucene {@link Query}. This
     * method is thread-safe.
     *
     * @param userQuery the user query string
     *
     * @return the Lucene Query
     */
    public synchronized Query parse(String userQuery)
        {
        try
            {
            return f_parser.parse(f_preprocessor.apply(userQuery));
            }
        catch (ParseException e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Returns a new {@link Builder} for LuceneQueryParser.
     *
     * @return a new Builder instance
     */
    public static Builder builder()
        {
        return new Builder();
        }

    /**
     * Creates a parser with default configuration (StandardAnalyzer, no stop
     * words, no synonyms). Requires at least one field extractor.
     *
     * @param field the field extractor to use
     *
     * @return a LuceneQueryParser instance
     */
    public static LuceneQueryParser create(ValueExtractor<?, String> field)
        {
        return builder().field(field).build();
        }

    // ---- inner class: Builder --------------------------------------------

    /**
     * Fluent builder for {@link LuceneQueryParser}.
     * <p>
     * Allows configuration of fields, boosts, analyzers, stop words, synonyms,
     * preprocessing, parser configuration, and custom parser injection.
     */
    public static class Builder
        {
        /**
         * Adds a field extractor with default boost (1.0).
         *
         * @param field the field extractor
         *
         * @return this builder
         */
        public Builder field(ValueExtractor<?, String> field)
            {
            return field(field, 1.0f);
            }

        /**
         * Adds a field extractor with a specific boost.
         *
         * @param field the field extractor
         * @param boost the boost value
         *
         * @return this builder
         */
        public Builder field(ValueExtractor<?, String> field, float boost)
            {
            f_fieldBoosts.put(Objects.requireNonNull(field), boost);
            return this;
            }

        /**
         * Sets the Lucene analyzer to use.
         *
         * @param analyzer the analyzer
         *
         * @return this builder
         */
        public Builder analyzer(Analyzer analyzer)
            {
            m_analyzer = Objects.requireNonNull(analyzer);
            return this;
            }

        /**
         * Sets the stop words set to use.
         *
         * @param stopWords the stop words set
         *
         * @return this builder
         */
        public Builder stopWords(Set<?> stopWords)
            {
            m_stopWords = stopWords instanceof CharArraySet
                             ? (CharArraySet) stopWords
                             : CharArraySet.copy(stopWords);
            return this;
            }

        /**
         * Sets a static synonym map to use for synonym expansion.
         *
         * @param synonymMap the synonym map
         *
         * @return this builder
         */
        public Builder synonyms(SynonymMap synonymMap)
            {
            m_synonymMap = synonymMap;
            return this;
            }

        /**
         * Sets synonyms from a map of terms to their synonyms.
         *
         * @param synonyms the map of terms to synonyms
         *
         * @return this builder
         *
         * @throws IllegalArgumentException if the synonym map cannot be built
         */
        public Builder synonyms(Map<String, List<String>> synonyms)
            {
            try
                {
                SynonymMap.Builder builder = new SynonymMap.Builder(true);
                for (Map.Entry<String, List<String>> entry : synonyms.entrySet())
                    {
                    String from = entry.getKey();
                    for (String to : entry.getValue())
                        {
                        builder.add(new CharsRef(from), new CharsRef(to), true);
                        }
                    }
                SynonymMap map = builder.build();
                return synonyms(map);
                }
            catch (IOException e)
                {
                throw new IllegalArgumentException("Failed to build SynonymMap", e);
                }
            }

        /**
         * Sets a function to build a synonym map from the configured analyzer.
         * Use with {@link LuceneQueryParser#solrSynonyms(Path, boolean)} or
         * {@link LuceneQueryParser#wordNetSynonyms(Path)}.
         *
         * @param function the function to build a SynonymMap
         *
         * @return this builder
         */
        public Builder synonyms(Function<Analyzer, SynonymMap> function)
            {
            m_synonymFunction = Objects.requireNonNull(function);
            return this;
            }

        /**
         * Sets a query preprocessor function to apply to user queries before
         * parsing.
         *
         * @param preprocessor the preprocessor function
         *
         * @return this builder
         */
        public Builder preprocessor(Function<String, String> preprocessor)
            {
            m_preprocessor = Objects.requireNonNull(preprocessor);
            return this;
            }

        /**
         * Allows custom configuration of the QueryParser after it is
         * constructed.
         *
         * @param configurer a consumer to configure the QueryParser
         *
         * @return this builder
         */
        public Builder configureParser(Consumer<QueryParser> configurer)
            {
            m_parserConfigurer = Objects.requireNonNull(configurer);
            return this;
            }

        /**
         * Allows use of a custom QueryParser instance. If set, this will be
         * used instead of the default parser construction logic.
         *
         * @param parser the custom QueryParser instance
         *
         * @return this builder
         */
        public Builder parser(QueryParser parser)
            {
            m_customParser = Objects.requireNonNull(parser);
            return this;
            }

        /**
         * Builds the configured {@link LuceneQueryParser} instance.
         *
         * @return a new LuceneQueryParser
         *
         * @throws IllegalStateException if required configuration is missing or
         *                               invalid
         */
        public LuceneQueryParser build()
            {
            if (m_synonymMap != null && m_synonymFunction != null)
                {
                throw new IllegalStateException("Cannot set both a static SynonymMap and a synonym function. Please use only one synonyms source.");
                }
            QueryParser parser;
            if (m_customParser != null)
                {
                parser = m_customParser;
                }
            else
                {
                if (f_fieldBoosts.isEmpty())
                    {
                    throw new IllegalStateException("At least one field must be specified to build a query.");
                    }

                Analyzer effectiveAnalyzer = buildAnalyzer();
                String[] fields = f_fieldBoosts.keySet().stream()
                        .map(ValueExtractor::getCanonicalName)
                        .toArray(String[]::new);
                Map<String, Float> boosts = f_fieldBoosts.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().getCanonicalName(), Map.Entry::getValue));
                if (fields.length == 1)
                    {
                    parser = new QueryParser(fields[0], effectiveAnalyzer);
                    }
                else
                    {
                    parser = new MultiFieldQueryParser(fields, effectiveAnalyzer, boosts);
                    }
                }
            if (m_parserConfigurer != null)
                {
                m_parserConfigurer.accept(parser);
                }
            return new LuceneQueryParser(parser, m_preprocessor);
            }

        /**
         * Builds the effective analyzer, applying stop words and synonyms as
         * configured.
         *
         * @return the configured Analyzer
         */
        private Analyzer buildAnalyzer()
            {
            SynonymMap mergedSynonymMap = null;
            if (m_synonymFunction != null)
                {
                mergedSynonymMap = m_synonymFunction.apply(m_analyzer);
                }
            else if (m_synonymMap != null)
                {
                mergedSynonymMap = m_synonymMap;
                }
            if (m_stopWords == null && mergedSynonymMap == null)
                {
                return m_analyzer;
                }
            return new WrappingAnalyzer(m_analyzer, m_stopWords, mergedSynonymMap);
            }
        
        // ---- data members ------------------------------------------------

        /**
         * Field extractors and their boosts.
         */
        private final Map<ValueExtractor<?, String>, Float> f_fieldBoosts = new LinkedHashMap<>();

        /**
         * The Lucene analyzer to use.
         */
        private Analyzer m_analyzer = new StandardAnalyzer();

        /**
         * The stop words set to use, or null for none.
         */
        private CharArraySet m_stopWords = null;

        /**
         * The static synonym map to use, or null.
         */
        private SynonymMap m_synonymMap = null;

        /**
         * Query preprocessor function.
         */
        private Function<String, String> m_preprocessor = Function.identity();

        /**
         * Function to build a synonym map from the configured analyzer.
         */
        private Function<Analyzer, SynonymMap> m_synonymFunction = null;

        /**
         * Optional configuration for the QueryParser after construction.
         */
        private Consumer<QueryParser> m_parserConfigurer = null;

        /**
         * Optional custom QueryParser instance to use.
         */
        private QueryParser m_customParser = null;
        }

    // ---- inner class: WrappingAnalyzer -----------------------------------
    
    /**
     * Analyzer wrapper that applies stop word and synonym filters to the token
     * stream.
     */
    private static class WrappingAnalyzer
            extends AnalyzerWrapper
        {
        /**
         * Constructs a new WrappingAnalyzer.
         *
         * @param delegate   the base analyzer
         * @param stopWords  the stop words set, or null
         * @param synonymMap the synonym map, or null
         */
        protected WrappingAnalyzer(Analyzer delegate, CharArraySet stopWords, SynonymMap synonymMap)
            {
            super(PER_FIELD_REUSE_STRATEGY);
            this.f_delegate = delegate;
            this.f_stopWords = stopWords;
            this.f_synonymMap = synonymMap;
            }

        @Override
        protected Analyzer getWrappedAnalyzer(String fieldName)
            {
            return f_delegate;
            }

        @Override
        protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components)
            {
            TokenStream tokenStream = components.getTokenStream();
            if (f_stopWords != null)
                {
                tokenStream = new StopFilter(tokenStream, f_stopWords);
                }
            if (f_synonymMap != null)
                {
                tokenStream = new SynonymGraphFilter(tokenStream, f_synonymMap, true);
                }
            return new TokenStreamComponents(components.getSource(), tokenStream);
            }
        
        // ---- data members ------------------------------------------------
        
        private final Analyzer f_delegate;
        private final CharArraySet f_stopWords;
        private final SynonymMap f_synonymMap;
        }

    // ---- static helpers --------------------------------------------------

    /**
     * Returns a function that loads synonyms from a Solr-format file using the
     * given analyzer.
     *
     * @param file   the path to the Solr synonym file
     * @param expand whether to expand synonyms bi-directionally
     *
     * @return a function that produces a SynonymMap from an Analyzer
     */
    public static Function<Analyzer, SynonymMap> solrSynonyms(Path file, boolean expand)
        {
        return (analyzer) ->
            {
            try (Reader reader = Files.newBufferedReader(file))
                {
                SolrSynonymParser parser = new SolrSynonymParser(true, expand, analyzer);
                parser.parse(reader);
                return parser.build();
                }
            catch (Exception e)
                {
                throw new IllegalArgumentException("Failed to load Solr synonyms from file: " + file, e);
                }
            };
        }

    /**
     * Returns a function that loads synonyms from a WordNet-format file using
     * the given analyzer.
     *
     * @param file the path to the WordNet synonym file
     *
     * @return a function that produces a SynonymMap from an Analyzer
     */
    public static Function<Analyzer, SynonymMap> wordNetSynonyms(Path file)
        {
        return (analyzer) ->
            {
            try (Reader reader = Files.newBufferedReader(file))
                {
                WordnetSynonymParser parser = new WordnetSynonymParser(true, true, analyzer);
                parser.parse(reader);
                return parser.build();
                }
            catch (Exception e)
                {
                throw new IllegalArgumentException("Failed to load WordNet synonyms from file: " + file, e);
                }
            };
        }

    // ---- data members ----------------------------------------------------

    /**
     * The underlying Lucene QueryParser or MultiFieldQueryParser.
     */
    private final QueryParser f_parser;

    /**
     * The preprocessor function applied to the user query string before
     * parsing.
     */
    private final Function<String, String> f_preprocessor;
    }
