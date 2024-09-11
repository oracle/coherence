/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.dslquery.function.FunctionBuilders;

import com.tangosol.coherence.dslquery.statement.BackupStatementBuilder;
import com.tangosol.coherence.dslquery.statement.CreateCacheStatementBuilder;
import com.tangosol.coherence.dslquery.statement.CreateIndexStatementBuilder;
import com.tangosol.coherence.dslquery.statement.DeleteStatementBuilder;
import com.tangosol.coherence.dslquery.statement.DropCacheStatementBuilder;
import com.tangosol.coherence.dslquery.statement.TruncateCacheStatementBuilder;
import com.tangosol.coherence.dslquery.statement.DropIndexStatementBuilder;
import com.tangosol.coherence.dslquery.statement.InsertStatementBuilder;
import com.tangosol.coherence.dslquery.statement.QueryRecorderStatementBuilder;
import com.tangosol.coherence.dslquery.statement.RestoreStatementBuilder;
import com.tangosol.coherence.dslquery.statement.SelectStatementBuilder;
import com.tangosol.coherence.dslquery.statement.SourceStatementBuilder;
import com.tangosol.coherence.dslquery.statement.UpdateStatementBuilder;

import com.tangosol.coherence.dslquery.statement.persistence.ArchiveSnapshotStatementBuilder;
import com.tangosol.coherence.dslquery.statement.persistence.CreateSnapshotStatementBuilder;
import com.tangosol.coherence.dslquery.statement.persistence.ForceRecoveryStatementBuilder;
import com.tangosol.coherence.dslquery.statement.persistence.ListArchiverStatementBuilder;
import com.tangosol.coherence.dslquery.statement.persistence.ListServicesStatementBuilder;
import com.tangosol.coherence.dslquery.statement.persistence.ListSnapshotsStatementBuilder;
import com.tangosol.coherence.dslquery.statement.persistence.RecoverSnapshotStatementBuilder;
import com.tangosol.coherence.dslquery.statement.persistence.RemoveSnapshotStatementBuilder;
import com.tangosol.coherence.dslquery.statement.persistence.ResumeServiceStatementBuilder;
import com.tangosol.coherence.dslquery.statement.persistence.RetrieveSnapshotStatementBuilder;
import com.tangosol.coherence.dslquery.statement.persistence.SuspendServiceStatementBuilder;
import com.tangosol.coherence.dslquery.statement.persistence.ValidateSnapshotStatementBuilder;

import com.tangosol.coherence.dslquery.operator.AdditionOperator;
import com.tangosol.coherence.dslquery.operator.AndOperator;
import com.tangosol.coherence.dslquery.operator.BaseOperator;
import com.tangosol.coherence.dslquery.operator.BetweenOperator;
import com.tangosol.coherence.dslquery.operator.ContainsAllOperator;
import com.tangosol.coherence.dslquery.operator.ContainsAnyOperator;
import com.tangosol.coherence.dslquery.operator.ContainsOperator;
import com.tangosol.coherence.dslquery.operator.DivisionOperator;
import com.tangosol.coherence.dslquery.operator.EqualsOperator;
import com.tangosol.coherence.dslquery.operator.GreaterEqualsOperator;
import com.tangosol.coherence.dslquery.operator.GreaterOperator;
import com.tangosol.coherence.dslquery.operator.ILikeOperator;
import com.tangosol.coherence.dslquery.operator.InOperator;
import com.tangosol.coherence.dslquery.operator.LessEqualsOperator;
import com.tangosol.coherence.dslquery.operator.LessOperator;
import com.tangosol.coherence.dslquery.operator.LikeOperator;
import com.tangosol.coherence.dslquery.operator.MultiplicationOperator;
import com.tangosol.coherence.dslquery.operator.NotEqualsOperator;
import com.tangosol.coherence.dslquery.operator.OrOperator;
import com.tangosol.coherence.dslquery.operator.SubtractionOperator;
import com.tangosol.coherence.dslquery.operator.XorOperator;

import com.tangosol.coherence.dslquery.token.SQLBackupOPToken;
import com.tangosol.coherence.dslquery.token.SQLCreateCacheOPToken;
import com.tangosol.coherence.dslquery.token.SQLCreateIndexOPToken;
import com.tangosol.coherence.dslquery.token.SQLDeleteOPToken;
import com.tangosol.coherence.dslquery.token.SQLDropCacheOPToken;
import com.tangosol.coherence.dslquery.token.SQLDropIndexOPToken;
import com.tangosol.coherence.dslquery.token.SQLExplainOPToken;
import com.tangosol.coherence.dslquery.token.SQLInsertOPToken;
import com.tangosol.coherence.dslquery.token.SQLPeekOPToken;
import com.tangosol.coherence.dslquery.token.SQLRestoreOPToken;
import com.tangosol.coherence.dslquery.token.SQLSelectOPToken;
import com.tangosol.coherence.dslquery.token.SQLSourceOPToken;
import com.tangosol.coherence.dslquery.token.SQLTraceOPToken;
import com.tangosol.coherence.dslquery.token.SQLTruncateCacheOPToken;
import com.tangosol.coherence.dslquery.token.SQLUpdateOPToken;

import com.tangosol.coherence.dslquery.token.persistence.SQLArchiveSnapshotOPToken;
import com.tangosol.coherence.dslquery.token.persistence.SQLCreateSnapshotOPToken;
import com.tangosol.coherence.dslquery.token.persistence.SQLForceRecoveryOPToken;
import com.tangosol.coherence.dslquery.token.persistence.SQLListArchivedSnapshotsOPToken;
import com.tangosol.coherence.dslquery.token.persistence.SQLListArchiverOPToken;
import com.tangosol.coherence.dslquery.token.persistence.SQLListServicesOPToken;
import com.tangosol.coherence.dslquery.token.persistence.SQLListSnapshotsOPToken;
import com.tangosol.coherence.dslquery.token.persistence.SQLRecoverSnapshotOPToken;
import com.tangosol.coherence.dslquery.token.persistence.SQLRemoveSnapshotOPToken;
import com.tangosol.coherence.dslquery.token.persistence.SQLResumeServiceOPToken;
import com.tangosol.coherence.dslquery.token.persistence.SQLRetrieveSnapshotOPToken;
import com.tangosol.coherence.dslquery.token.persistence.SQLSuspendServiceOPToken;
import com.tangosol.coherence.dslquery.token.persistence.SQLValidateSnapshotOPToken;

import com.tangosol.coherence.dsltools.base.LiteralBaseToken;

import com.tangosol.coherence.dsltools.precedence.EndOfStatementOPToken;
import com.tangosol.coherence.dsltools.precedence.InfixRightOPToken;
import com.tangosol.coherence.dsltools.precedence.KeywordOPToken;
import com.tangosol.coherence.dsltools.precedence.ListOpToken;
import com.tangosol.coherence.dsltools.precedence.LiteralOPToken;
import com.tangosol.coherence.dsltools.precedence.NotOPToken;
import com.tangosol.coherence.dsltools.precedence.ParenOPToken;
import com.tangosol.coherence.dsltools.precedence.PathOPToken;
import com.tangosol.coherence.dsltools.precedence.PrefixOPToken;
import com.tangosol.coherence.dsltools.precedence.PunctuationOPToken;
import com.tangosol.coherence.dsltools.precedence.TokenTable;

import com.tangosol.coherence.dsltools.termlanguage.ColonToken;
import com.tangosol.coherence.dsltools.termlanguage.CurlyToken;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.config.expression.ParameterResolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import static com.tangosol.coherence.dsltools.precedence.OPToken.BINARY_OPERATOR_NODE;
import static com.tangosol.coherence.dsltools.precedence.OPToken.BINDING_NODE;
import static com.tangosol.coherence.dsltools.precedence.OPToken.CALL_NODE;
import static com.tangosol.coherence.dsltools.precedence.OPToken.DEREF_NODE;
import static com.tangosol.coherence.dsltools.precedence.OPToken.IDENTIFIER_NODE;
import static com.tangosol.coherence.dsltools.precedence.OPToken.LIST_NODE;
import static com.tangosol.coherence.dsltools.precedence.OPToken.LITERAL_NODE;
import static com.tangosol.coherence.dsltools.precedence.OPToken.PRECEDENCE_EXPONENT;
import static com.tangosol.coherence.dsltools.precedence.OPToken.PRECEDENCE_PARENTHESES;
import static com.tangosol.coherence.dsltools.precedence.OPToken.PRECEDENCE_UNARY;
import static com.tangosol.coherence.dsltools.precedence.OPToken.UNARY_OPERATOR_NODE;

/**
 * CoherenceQueryLanguage is a simple language for building
 * Filters and doing simple queries that are similar in style to SQL.
 *
 * This class configures CohQL and contains various attributes
 * that control CohQL execution.
 *
 * @author djl  2009.08.31
 * @author jk   2013.12.02
 */
public class CoherenceQueryLanguage
    {
    // ----- Language table API ---------------------------------------------

    /**
     * Construct a CoherenceQueryLanguage instance.
     */
    public CoherenceQueryLanguage()
        {
        m_tokenTableSQL         = getSqlTokenTable(false);
        m_tokenTableExtendedSQL = getSqlTokenTable(true);
        m_tokenTableForFilter   = createCommonTokens();
        m_mapOperators          = initializeOperatorMap();
        m_mapFunctions          = initializeFunctionMap();
        m_mapStatementBuilders  = createStatements();
        }

    // ----- Language table API ---------------------------------------------

    /**
     * Return an initialized standard {@link TokenTable} known by this
     * CoherenceQueryLanguage.
     *
     * @return an initialized standard TokenTable known by this
     *         CoherenceQueryLanguage
     */
    public TokenTable filtersTokenTable()
        {
        return m_tokenTableForFilter;
        }

    /**
     * Return an initialized extended {@link TokenTable} known by this
     * CoherenceQueryLanguage.
     *
     * @return an initialized extended TokenTable known by this
     *         CoherenceQueryLanguage
     */
    public TokenTable sqlTokenTable()
        {
        return m_tokenTableSQL;
        }

    /**
     * Return an initialized TokenTable for the full CoherenceQueryLanguage.
     *
     * @return a TokenTable for the CoherenceQueryLanguage
     */
    public TokenTable extendedSqlTokenTable()
        {
        return m_tokenTableExtendedSQL;
        }

    /**
     * Return an initialized TokenTable for the full CoherenceQueryLanguage.
     *
     * @param fExtended  flag that enables extended and experimental features
     *
     * @return a TokenTable for the CoherenceQueryLanguage
     */
    protected TokenTable getSqlTokenTable(boolean fExtended)
        {
        // Create tokens with their respective ids and precedences
        TokenTable tokens = createCommonTokens();

        // CohQL Language Elements with custom OPToken implementations
        tokens.addToken(new SQLPeekOPToken("alter"));
        tokens.addToken(new SQLBackupOPToken("backup"));
        tokens.addToken(new SQLPeekOPToken("create", new SQLCreateCacheOPToken(), new SQLCreateIndexOPToken(),
                                           new SQLCreateSnapshotOPToken()));
        tokens.alias("ensure", "create");
        tokens.addToken(new SQLDeleteOPToken("delete"));
        tokens.addToken(new SQLPeekOPToken("drop", new SQLDropCacheOPToken(), new SQLDropIndexOPToken()));
        tokens.addToken(new SQLTruncateCacheOPToken("truncate"));
        tokens.addToken(new SQLExplainOPToken("explain"));
        tokens.addToken(new SQLInsertOPToken("insert"));
        tokens.addToken(new SQLRestoreOPToken("restore"));
        tokens.addToken(new SQLSelectOPToken("select"));
        tokens.addToken(new SQLSourceOPToken("@"));
        tokens.alias("source", "@");
        tokens.addToken(new SQLTraceOPToken("trace"));
        tokens.addToken(new SQLUpdateOPToken("update"));

        // persistence related commands
        tokens.addToken(new SQLPeekOPToken("list", new SQLListServicesOPToken(), new SQLListSnapshotsOPToken(),
                new SQLListArchivedSnapshotsOPToken(), new SQLListArchiverOPToken()));
        tokens.addToken(new SQLRemoveSnapshotOPToken("remove"));
        tokens.addToken(new SQLRecoverSnapshotOPToken("recover"));
        tokens.addToken(new SQLValidateSnapshotOPToken("validate"));
        tokens.addToken(new SQLArchiveSnapshotOPToken("archive"));
        tokens.addToken(new SQLRetrieveSnapshotOPToken("retrieve"));
        tokens.addToken(new SQLResumeServiceOPToken("resume"));
        tokens.addToken(new SQLSuspendServiceOPToken("suspend"));
        tokens.addToken(new SQLForceRecoveryOPToken("force"));

        // Keywords
        tokens.addToken(new KeywordOPToken("by"));
        tokens.addToken(new KeywordOPToken("cache"));
        tokens.addToken(new KeywordOPToken("check"));
        tokens.addToken(new KeywordOPToken("distinct"));
        tokens.addToken(new KeywordOPToken("escape"));
        tokens.addToken(new KeywordOPToken("file"));
        tokens.addToken(new KeywordOPToken("from"));
        tokens.addToken(new KeywordOPToken("group"));
        tokens.addToken(new KeywordOPToken("having"));
        tokens.addToken(new KeywordOPToken("index"));
        tokens.addToken(new KeywordOPToken("into"));
        tokens.addToken(new KeywordOPToken("key"));
        tokens.addToken(new KeywordOPToken("off"));
        tokens.addToken(new KeywordOPToken("on"));
        tokens.addToken(new KeywordOPToken("order"));
        tokens.addToken(new KeywordOPToken("plan"));
        tokens.addToken(new KeywordOPToken("service"));
        tokens.addToken(new KeywordOPToken("set"));
        tokens.addToken(new KeywordOPToken("show"));
        tokens.addToken(new KeywordOPToken("to"));
        tokens.addToken(new KeywordOPToken("value"));
        tokens.addToken(new KeywordOPToken("where"));

        if (fExtended)
            {
            tokens.addToken(new ListOpToken("[", PRECEDENCE_PARENTHESES, ".list."));
            tokens.addToken(new CurlyToken("{", PRECEDENCE_PARENTHESES));
            tokens.addToken(new ColonToken(":", PRECEDENCE_PARENTHESES));
            }

        return tokens;
        }

    /**
     * There are three token tables in CohQL, the one used to build {@link com.tangosol.util.Filter}
     * instances, the standard CohQL table and the extended CohQL table. All three share a set
     * of common tokens which are initialised by this method. Examples of common tokens would
     * be operators such as '+', '-', '/', literals such as 'true', 'false' etc.
     *
     * @return a {@link TokenTable} initialised with the common CohQL tokens
     */
    private TokenTable createCommonTokens()
        {
        TokenTable tokenTable = new TokenTable(IDENTIFIER_NODE, LITERAL_NODE);

        tokenTable.setIgnoreCase(true);

        tokenTable.addToken(new InfixRightOPToken("**", PRECEDENCE_EXPONENT, BINARY_OPERATOR_NODE));

        tokenTable.addToken(new NotOPToken("!", PRECEDENCE_UNARY, UNARY_OPERATOR_NODE, UNARY_OPERATOR_NODE));
        tokenTable.addToken(new PrefixOPToken("new", PRECEDENCE_UNARY, UNARY_OPERATOR_NODE));
        tokenTable.addToken(new PrefixOPToken("~", PRECEDENCE_UNARY, UNARY_OPERATOR_NODE));
        tokenTable.addToken(new PrefixOPToken("?", PRECEDENCE_UNARY, BINDING_NODE));
        tokenTable.addToken(new PrefixOPToken(":", PRECEDENCE_UNARY, BINDING_NODE));
        tokenTable.addToken(new ParenOPToken("(", PRECEDENCE_PARENTHESES, CALL_NODE, LIST_NODE));
        tokenTable.addToken(new PathOPToken(".", PRECEDENCE_PARENTHESES, DEREF_NODE));
        tokenTable.addToken(new PunctuationOPToken(","));
        tokenTable.addToken(EndOfStatementOPToken.INSTANCE);

        tokenTable.addToken("true", new LiteralOPToken(LiteralBaseToken.createBoolean("true")), null, LITERAL_NODE);
        tokenTable.addToken("false", new LiteralOPToken(LiteralBaseToken.createBoolean("false")), null, LITERAL_NODE);
        tokenTable.addToken("null", new LiteralOPToken(LiteralBaseToken.createNull("null")), null, LITERAL_NODE);
        tokenTable.addToken("nan", new LiteralOPToken(LiteralBaseToken.createDouble("NaN")), null, LITERAL_NODE);
        tokenTable.addToken("infinity",
                            new LiteralOPToken(LiteralBaseToken.createDouble("Infinity")), null, LITERAL_NODE);

        tokenTable.alias("not", "!");

        if (m_mapOperators != null)
            {
            for (BaseOperator op : m_mapOperators.values())
                {
                op.addToTokenTable(tokenTable);
                }
            }

        return tokenTable;
        }

    /**
     * Creates the Map holding CohQL functions and populates it
     * with the default functions available in CohQL.
     * CohQL functions are elements such as sum(), avg(), max() etc.
     *
     * @return the function Map populated with the default functions
     */
    protected Map<String, ParameterizedBuilder<?>> initializeFunctionMap()
        {
        Map<String, ParameterizedBuilder<?>> map = new ConcurrentHashMap<>();

        map.put("max",      FunctionBuilders.DOUBLE_MAX_FUNCTION_BUILDER);
        map.put("min",      FunctionBuilders.DOUBLE_MIN_FUNCTION_BUILDER);
        map.put("sum",      FunctionBuilders.DOUBLE_SUM_FUNCTION_BUILDER);
        map.put("avg",      FunctionBuilders.DOUBLE_AVERAGE_FUNCTION_BUILDER);
        map.put("bd_max",   FunctionBuilders.BIG_DECIMAL_MAX_FUNCTION_BUILDER);
        map.put("bd_min",   FunctionBuilders.BIG_DECIMAL_MIN_FUNCTION_BUILDER);
        map.put("bd_sum",   FunctionBuilders.BIG_DECIMAL_SUM_FUNCTION_BUILDER);
        map.put("bd_avg",   FunctionBuilders.BIG_DECIMAL_AVERAGE_FUNCTION_BUILDER);
        map.put("long_max", FunctionBuilders.LONG_MAX_FUNCTION_BUILDER);
        map.put("long_min", FunctionBuilders.LONG_MIN_FUNCTION_BUILDER);
        map.put("long_sum", FunctionBuilders.LONG_SUM_FUNCTION_BUILDER);
        map.put("count",    FunctionBuilders.COUNT_FUNCTION_BUILDER);
        map.put("value",    FunctionBuilders.VALUE_FUNCTION_BUILDER);
        map.put("key",      FunctionBuilders.KEY_FUNCTION_BUILDER);
        map.put("concat",   FunctionBuilders.CONCAT_FUNCTION_BUILDER);

        return map;
        }

    /**
     * Return the function mapped to the given function name or
     * null if no function mapping exists.
     *
     * @param sName  the name of the function to return
     *
     * @return the function mapped to the given function name or
     *         null if no function mapping exists
     */
    public ParameterizedBuilder getFunction(String sName)
        {
        return m_mapFunctions.get(getFunctionKey(sName));
        }

    /**
     * Map the specified CohQL {@link ParameterizedBuilder} to
     * the specified function name. If either the name of the implementation
     * is null then no mapping will occur.
     *
     * @param sName         the name of the function
     * @param bldrFunction  the implementation of the function
     */
    public void addFunction(String sName, ParameterizedBuilder bldrFunction)
        {
        if (sName == null || bldrFunction == null)
             {
             throw new IllegalArgumentException(
                     "Both name and function must be supplied to add a function");
             }
         m_mapFunctions.put(getFunctionKey(sName), bldrFunction);
        }

    /**
     * Remove the CohQL function mapping for the specified function name.
     * The removed {@link ParameterizedBuilder} will be returned, or null if there
     * was no mapping for the specified name.
     *
     * @param sName  the name of the function to remove
     *
     * @return the removed ParameterizedBuilder will be returned, or null if there
     *         was no mapping for the specified name.
     */
    public ParameterizedBuilder<?> removeFunction(String sName)
        {
        return m_mapFunctions.remove(getFunctionKey(sName));
        }

    /**
     * Remove all custom function mappings that have been registered.
     */
    public synchronized void clearCustomFunctions()
        {
        m_mapFunctions = null;
        initializeFunctionMap();
        }

    /**
     * Initialize the CohQL operators Map with all of the built-in CohQL
     * operators and return the Map.
     *
     * @return the Map of CohQL operators
     */
    protected Map<CharSequence, BaseOperator> initializeOperatorMap()
        {
        Map<CharSequence, BaseOperator> map = m_mapOperators = new HashMap<>();

        addOperatorInternal(map, AndOperator.INSTANCE);               // && and
        addOperatorInternal(map, BetweenOperator.INSTANCE);           // between
        addOperatorInternal(map, ContainsAllOperator.INSTANCE);       // contains all
        addOperatorInternal(map, ContainsAnyOperator.INSTANCE);       // contains any
        addOperatorInternal(map, ContainsOperator.INSTANCE);          // contains
        addOperatorInternal(map, EqualsOperator.INSTANCE);            // ==
        addOperatorInternal(map, GreaterEqualsOperator.INSTANCE);     // >=
        addOperatorInternal(map, GreaterOperator.INSTANCE);           // >
        addOperatorInternal(map, InOperator.INSTANCE);                // in
        addOperatorInternal(map, LessEqualsOperator.INSTANCE);        // <=
        addOperatorInternal(map, LessOperator.INSTANCE);              // <
        addOperatorInternal(map, LikeOperator.INSTANCE);              // like
        addOperatorInternal(map, ILikeOperator.INSTANCE);             // ilike
        addOperatorInternal(map, NotEqualsOperator.INSTANCE);         // !=
        addOperatorInternal(map, OrOperator.INSTANCE);                // ||
        addOperatorInternal(map, XorOperator.INSTANCE);               // xor
        addOperatorInternal(map, AdditionOperator.INSTANCE);          // -
        addOperatorInternal(map, SubtractionOperator.INSTANCE);       // -
        addOperatorInternal(map, MultiplicationOperator.INSTANCE);    // *
        addOperatorInternal(map, DivisionOperator.INSTANCE);          // /

        return map;
        }

    /**
     * Add an operator to the specified operator Map and to all of the
     * internal {@link TokenTable}s.
     *
     * @param map  the operator map to add the operator to
     * @param op   the operator to add
     */
    private void addOperatorInternal(Map<CharSequence, BaseOperator> map, BaseOperator op)
        {
        map.put(op.getSymbol(), op);

        TokenTable tokenTable = m_tokenTableForFilter;
        if (tokenTable != null)
            {
            op.addToTokenTable(tokenTable);
            }

        tokenTable = m_tokenTableSQL;
        if (tokenTable != null)
            {
            op.addToTokenTable(tokenTable);
            }

        tokenTable = m_tokenTableExtendedSQL;
        if (tokenTable != null)
            {
            op.addToTokenTable(tokenTable);
            }
        }

    /**
     * Add the custom operator to the CohQL language.
     *
     * @param operator  the operator to add
     */
    public void addOperator(BaseOperator operator)
        {
        addOperatorInternal(m_mapOperators, operator);
        }

    /**
     * Return the set of CohQL Operators characters.
     *
     * @return the set of CohQL operators characters
     */
    public Set<CharSequence> getOperators()
        {
        return Collections.unmodifiableSet(m_mapOperators.keySet());
        }

    /**
     * Return the {@link BaseOperator} mapped to the given symbol.
     *
     * @param sSymbol  the symbol of the BaseOperator to get
     *
     * @return the BaseOperator mapped to the specified symbol or
     *         null if there i sno mapping for that symbol
     */
    public BaseOperator getOperator(String sSymbol)
        {
        return m_mapOperators.get(sSymbol);
        }

    /**
     * Return the map of {@link StatementBuilder} instances.
     * The map is keyed on the functor used to represent a particular
     * query type in a CohQL AST.
     *
     * @return the map of StatementBuilder instances
     */
    public Map<String, StatementBuilder<?>> getStatementBuilders()
        {
        return Collections.unmodifiableMap(m_mapStatementBuilders);
        }

    /**
     * Create the CohQL {@link StatementBuilder} map and initialise
     * it with the standard CohQL statements.
     *
     * @return the standard CohQL statement map
     */
    protected Map<String, StatementBuilder<?>> createStatements()
        {
        Map<String, StatementBuilder<?>> map = new LinkedHashMap<>();

        map.put(SQLCreateCacheOPToken.FUNCTOR, CreateCacheStatementBuilder.INSTANCE);
        map.put(SQLCreateIndexOPToken.FUNCTOR, CreateIndexStatementBuilder.INSTANCE);
        map.put(SQLDropCacheOPToken.FUNCTOR, DropCacheStatementBuilder.INSTANCE);
        map.put(SQLTruncateCacheOPToken.FUNCTOR, TruncateCacheStatementBuilder.INSTANCE);
        map.put(SQLDropIndexOPToken.FUNCTOR, DropIndexStatementBuilder.INSTANCE);
        map.put(SQLBackupOPToken.FUNCTOR, BackupStatementBuilder.INSTANCE);
        map.put(SQLRestoreOPToken.FUNCTOR, RestoreStatementBuilder.INSTANCE);
        map.put(SQLInsertOPToken.FUNCTOR, InsertStatementBuilder.INSTANCE);
        map.put(SQLDeleteOPToken.FUNCTOR, DeleteStatementBuilder.INSTANCE);
        map.put(SQLUpdateOPToken.FUNCTOR, UpdateStatementBuilder.INSTANCE);
        map.put(SQLSelectOPToken.FUNCTOR, SelectStatementBuilder.INSTANCE);
        map.put(SQLSourceOPToken.FUNCTOR, SourceStatementBuilder.INSTANCE);
        map.put(SQLExplainOPToken.FUNCTOR, QueryRecorderStatementBuilder.EXPLAIN_INSTANCE);
        map.put(SQLTraceOPToken.FUNCTOR, QueryRecorderStatementBuilder.TRACE_INSTANCE);

        // persistence commands
        map.put(SQLListServicesOPToken.FUNCTOR, ListServicesStatementBuilder.INSTANCE);
        map.put(SQLListSnapshotsOPToken.FUNCTOR, ListSnapshotsStatementBuilder.INSTANCE);
        map.put(SQLListArchiverOPToken.FUNCTOR, ListArchiverStatementBuilder.INSTANCE);
        map.put(SQLCreateSnapshotOPToken.FUNCTOR, CreateSnapshotStatementBuilder.INSTANCE);
        map.put(SQLRecoverSnapshotOPToken.FUNCTOR, RecoverSnapshotStatementBuilder.INSTANCE);
        map.put(SQLRemoveSnapshotOPToken.FUNCTOR, RemoveSnapshotStatementBuilder.INSTANCE);
        map.put(SQLValidateSnapshotOPToken.FUNCTOR, ValidateSnapshotStatementBuilder.INSTANCE);
        map.put(SQLArchiveSnapshotOPToken.FUNCTOR, ArchiveSnapshotStatementBuilder.INSTANCE);
        map.put(SQLRetrieveSnapshotOPToken.FUNCTOR, RetrieveSnapshotStatementBuilder.INSTANCE);
        map.put(SQLResumeServiceOPToken.FUNCTOR, ResumeServiceStatementBuilder.INSTANCE);
        map.put(SQLSuspendServiceOPToken.FUNCTOR, SuspendServiceStatementBuilder.INSTANCE);
        map.put(SQLForceRecoveryOPToken.FUNCTOR, ForceRecoveryStatementBuilder.INSTANCE);

        return map;
        }

    /**
     * Return the {@link StatementBuilder} for a given CohQL AST functor.
     * The returned value may be null if no builder has been registered
     * for a given functor.
     *
     * @param sFunctor  the functor representing the statement who's builder
     *                  should be returned
     *
     * @return the StatementBuilder for a given CohQL AST functor
     */
    public StatementBuilder<?> getStatementBuilder(String sFunctor)
        {
        return m_mapStatementBuilders.get(sFunctor);
        }

    /**
     * Realize an instance of the {@link Statement} that will execute the CohQL statement
     * represented by the AST node.
     *
     * @param term           the parsed AST node representing the CohQL statement
     * @param context        the {@link ExecutionContext} to use to realize the command
     * @param listBindVars   the indexed bind variables to use for the query
     * @param namedBindVars  the named bind variables to use for the query
     *
     * @return the Statement to execute the CohQL statement
     *
     * @throws CohQLException if there is an error building the statement
     *         or there are no registered builders for the statement
     */
    public Statement prepareStatement(NodeTerm term, ExecutionContext context,
                                      List listBindVars, ParameterResolver namedBindVars)
        {
        StatementBuilder<?> bldrStatement = getStatementBuilder(term.getFunctor());

        if (bldrStatement == null)
            {
            throw new CohQLException("Unknown translation tree: " + term.getFunctor());
            }

        return bldrStatement.realize(context, term, listBindVars, namedBindVars);
        }

    /**
     * Register the given {@link StatementBuilder} to the specified functor name.
     * The specified StatementBuilder will then be used to build any
     * {@link Statement} instances when the given functor is present in
     * a query AST.
     *
     * @param sFunctor  the functor to map the StatementBuilder to
     * @param builder   the StatementBuilder to be mapped
     */
    public void addStatement(String sFunctor, StatementBuilder<?> builder)
        {
        m_mapStatementBuilders.put(sFunctor, builder);
        }

    /**
     * Remove the {@link StatementBuilder} associated with the given functor.
     * <p>
     * Note: removal of a functor may cause errors if subsequent queries
     *       refer to the functor.
     *
     * @param sFunctor  the functor mapping to remove
     *
     * @return the removed StatementBuilder or null
     */
    public StatementBuilder<?> removeStatementBuilder(String sFunctor)
        {
        return m_mapStatementBuilders.remove(sFunctor);
        }

    /**
     * Remove all customisations that have been added to the CohQL language.
     */
    public void clearCustomOperators()
        {
        m_tokenTableSQL = null;
        m_tokenTableExtendedSQL = null;
        m_tokenTableForFilter = null;
        m_mapOperators = null;
        m_mapStatementBuilders = null;
        }

    /**
     * Set the {@link ExtractorBuilder} that will be used by CohQL queries to
     * build {@link com.tangosol.util.ValueExtractor}s.
     *
     * @param builder  the ExtractorBuilder to use
     */
    public void setExtractorBuilder(ExtractorBuilder builder)
        {
        m_bldrExtractor = builder == null ? new UniversalExtractorBuilder() : builder;
        }

    /**
     * Return the {@link ExtractorBuilder} to use to build
     * {@link com.tangosol.util.ValueExtractor}s.
     *
     * @return the ExtractorBuilder to use to build ValueExtractors
     */
    public ExtractorBuilder getExtractorBuilder()
        {
        return m_bldrExtractor;
        }

    /**
     * Returns a key value based on case-insensitivity of the token table.
     *
     * @param sFunctionName the function name to create a key for
     *
     * @return a key value based on case-insensitivity of the token table
     *
     * @since 21.06
     */
    protected String getFunctionKey(String sFunctionName)
        {
        Objects.requireNonNull(sFunctionName);

        return getSqlTokenTable(false).isIgnoringCase() ? sFunctionName.toLowerCase() : sFunctionName;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The TokenTable for Filters.
     */
    protected TokenTable m_tokenTableForFilter;

    /**
     * The TokenTable for the full language that resembles SQL.
     */
    protected TokenTable m_tokenTableSQL;

    /**
     * The extended TokenTable for the full language that resembles SQL.
     */
    protected TokenTable m_tokenTableExtendedSQL;

    /**
     * The map of CohQL functions. The key is the function name
     * and the value is the function implementation.
     */
    protected Map<String, ParameterizedBuilder<?>> m_mapFunctions;

    /**
     * The map of CohQL operators. The key is the operator name
     * and the value is the {@link BaseOperator} implementation.
     */
    protected Map<CharSequence, BaseOperator> m_mapOperators;

    /**
     * The map of CohQL query builders. The key is the CohQL token name
     * that the parser produces to represent a particular query.
     */
    protected Map<String, StatementBuilder<?>> m_mapStatementBuilders;

    /**
     * The {@link ExtractorBuilder} that will be used to realize
     * {@link com.tangosol.util.ValueExtractor}s to be used by CohQL.
     */
    protected ExtractorBuilder m_bldrExtractor = new UniversalExtractorBuilder();
    }
