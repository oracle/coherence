/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.lucene.pof;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

/**
 * A collection of POF (Portable Object Format) serializers for Lucene query
 * types. This class provides serializers that enable Coherence to efficiently
 * serialize and deserialize various Lucene query objects across the network.
 *
 * @author Aleks Seovic  2025.05.16
 * @since 25.09
 */
public class LucenePofSerializers
    {
    /**
     * POF serializer for Lucene {@link Term}. Serializes a Term's field name
     * and text value.
     */
    public static class TermSerializer
            implements PofSerializer<Term>
        {
        @Override
        public void serialize(PofWriter out, Term term) throws IOException
            {
            out.writeString(0, term.field());
            out.writeString(1, term.text());
            out.writeRemainder(null);
            }

        @Override
        public Term deserialize(PofReader in) throws IOException
            {
            String field = in.readString(0);
            String text = in.readString(1);
            in.readRemainder();
            return new Term(field, text);
            }
        }

    /**
     * POF serializer for Lucene {@link TermQuery}. Serializes a query that
     * matches documents containing a specific term.
     */
    public static class TermQuerySerializer
            implements PofSerializer<TermQuery>
        {
        @Override
        public void serialize(PofWriter out, TermQuery query) throws IOException
            {
            out.writeObject(0, query.getTerm());
            out.writeRemainder(null);
            }

        @Override
        public TermQuery deserialize(PofReader in) throws IOException
            {
            Term term = in.readObject(0);
            in.readRemainder();
            return new TermQuery(term);
            }
        }

    /**
     * POF serializer for Lucene {@link BooleanClause}. Serializes a clause in a
     * boolean query, including its query and occurrence type (MUST, SHOULD, or
     * MUST_NOT).
     */
    public static class BooleanClauseSerializer
            implements PofSerializer<BooleanClause>
        {
        @Override
        public void serialize(PofWriter out, BooleanClause clause)
                throws IOException
            {
            out.writeObject(0, clause.query());
            out.writeString(1, clause.occur().name());
            out.writeRemainder(null);
            }

        @Override
        public BooleanClause deserialize(PofReader in) throws IOException
            {
            Query query = in.readObject(0);
            BooleanClause.Occur occur = BooleanClause.Occur.valueOf(in.readString(1));
            in.readRemainder();
            return new BooleanClause(query, occur);
            }
        }

    /**
     * POF serializer for Lucene {@link BooleanQuery}. Serializes a query that
     * matches documents based on boolean combinations of other queries.
     */
    public static class BooleanQuerySerializer
            implements PofSerializer<BooleanQuery>
        {
        @Override
        public void serialize(PofWriter out, BooleanQuery query)
                throws IOException
            {
            out.writeCollection(0, query.clauses());
            out.writeRemainder(null);
            }

        @Override
        public BooleanQuery deserialize(PofReader in) throws IOException
            {
            List<BooleanClause> clauses = new ArrayList<>();
            in.readCollection(0, clauses);
            in.readRemainder();

            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            for (BooleanClause clause : clauses)
                {
                builder.add(clause);
                }

            return builder.build();
            }
        }

    /**
     * POF serializer for Lucene {@link MatchAllDocsQuery}. Serializes a query
     * that matches all documents with a constant score of 1.0.
     */
    public static class MatchAllDocsQuerySerializer
            implements PofSerializer<MatchAllDocsQuery>
        {
        @Override
        public void serialize(PofWriter out, MatchAllDocsQuery query)
                throws IOException
            {
            out.writeRemainder(null);
            }

        @Override
        public MatchAllDocsQuery deserialize(PofReader in) throws IOException
            {
            in.readRemainder();
            return new MatchAllDocsQuery();
            }
        }

    /**
     * POF serializer for Lucene {@link PhraseQuery}. Serializes a query that
     * matches documents containing a particular sequence of terms in a specific
     * order.
     */
    public static class PhraseQuerySerializer
            implements PofSerializer<PhraseQuery>
        {
        @Override
        public void serialize(PofWriter out, PhraseQuery query)
                throws IOException
            {
            Term[] terms = query.getTerms();
            int[] positions = query.getPositions();

            out.writeObjectArray(0, terms);
            out.writeIntArray(1, positions);
            out.writeRemainder(null);
            }

        @Override
        public PhraseQuery deserialize(PofReader in) throws IOException
            {
            Term[] terms = in.readArray(0, Term[]::new);
            int[] positions = in.readIntArray(1);
            in.readRemainder();

            PhraseQuery.Builder builder = new PhraseQuery.Builder();
            for (int i = 0; i < terms.length; i++)
                {
                builder.add(terms[i], positions[i]);
                }

            return builder.build();
            }
        }

    /**
     * POF serializer for Lucene {@link WildcardQuery}. Serializes a query that
     * matches documents containing terms similar to a pattern, where the
     * pattern can include the wildcards * (any sequence of characters) and ?
     * (any single character).
     */
    public static class WildcardQuerySerializer
            implements PofSerializer<WildcardQuery>
        {
        @Override
        public void serialize(PofWriter out, WildcardQuery query)
                throws IOException
            {
            out.writeObject(0, query.getTerm());
            out.writeRemainder(null);
            }

        @Override
        public WildcardQuery deserialize(PofReader in) throws IOException
            {
            Term term = in.readObject(0);
            in.readRemainder();
            return new WildcardQuery(term);
            }
        }

    /**
     * POF serializer for Lucene {@link PrefixQuery}. Serializes a query that
     * matches documents containing terms beginning with a specified prefix.
     */
    public static class PrefixQuerySerializer
            implements PofSerializer<PrefixQuery>
        {
        @Override
        public void serialize(PofWriter out, PrefixQuery query)
                throws IOException
            {
            out.writeObject(0, query.getPrefix());
            out.writeRemainder(null);
            }

        @Override
        public PrefixQuery deserialize(PofReader in) throws IOException
            {
            Term term = in.readObject(0);
            in.readRemainder();
            return new PrefixQuery(term);
            }
        }

    /**
     * POF serializer for Lucene {@link FuzzyQuery}. Serializes a query that
     * implements fuzzy string matching based on Levenshtein (edit distance)
     * similarity.
     */
    public static class FuzzyQuerySerializer
            implements PofSerializer<FuzzyQuery>
        {
        @Override
        public void serialize(PofWriter out, FuzzyQuery query)
                throws IOException
            {
            out.writeObject(0, query.getTerm());
            out.writeInt(1, query.getMaxEdits());
            out.writeRemainder(null);
            }

        @Override
        public FuzzyQuery deserialize(PofReader in) throws IOException
            {
            Term term = in.readObject(0);
            int maxEdits = in.readInt(1);
            in.readRemainder();
            return new FuzzyQuery(term, maxEdits);
            }
        }

    /**
     * POF serializer for Lucene {@link RegexpQuery}. Serializes a query that
     * matches documents containing terms matching a regular expression
     * pattern.
     */
    public static class RegexpQuerySerializer
            implements PofSerializer<RegexpQuery>
        {
        @Override
        public void serialize(PofWriter out, RegexpQuery query)
                throws IOException
            {
            out.writeObject(0, query.getRegexp());
            out.writeRemainder(null);
            }

        @Override
        public RegexpQuery deserialize(PofReader in) throws IOException
            {
            Term term = in.readObject(0);
            in.readRemainder();
            return new RegexpQuery(term);
            }
        }

    /**
     * POF serializer for Lucene {@link BoostQuery}. Serializes a query that
     * wraps another query and boosts (multiplies) its score by a specified
     * boost factor.
     */
    public static class BoostQuerySerializer
            implements PofSerializer<BoostQuery>
        {
        @Override
        public void serialize(PofWriter out, BoostQuery query)
                throws IOException
            {
            out.writeObject(0, query.getQuery());
            out.writeFloat(1, query.getBoost());
            out.writeRemainder(null);
            }

        @Override
        public BoostQuery deserialize(PofReader in) throws IOException
            {
            Query query = in.readObject(0);
            float boost = in.readFloat(1);
            in.readRemainder();
            return new BoostQuery(query, boost);
            }
        }
    }
