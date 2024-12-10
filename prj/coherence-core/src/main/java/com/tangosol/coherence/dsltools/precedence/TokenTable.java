/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import java.util.HashMap;
import java.util.HashSet;


/**
* TokenTable is a helper class used by Scanners to convert BaseTokens to
* to OPTokens.  TokenTables have protocol to add tokens under a name, alias
* tokens, and control the enabeleded state of tokens.
*
* @author djl  2009.03.14
*/
public class TokenTable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new OPToken.
    */
     public TokenTable()
        {
        }

    /**
    * Construct a new TokenTable with the given parameters to set the
    * ast names for identifiers and literals.
    *
    * @param sIdentifierASTName  identifiers ast names
    * @param sLiteralASTName     literals ast name
    */
    public TokenTable(String sIdentifierASTName, String sLiteralASTName)
        {
        m_sIdentifierASTName = sIdentifierASTName;
        m_sIiteralASTName    = sLiteralASTName;
        }

    /**
    * Use given boolean to decide whether to ignore case for keyword
    * operations
    *
    * @param fIgnore  the boolean to use to control case interest
    *
    */
    public void setIgnoreCase(boolean fIgnore)
        {
        m_fIgnoreCase = fIgnore;
        }

    /**
    * Answer wheather the receiver is ignoringCase
    *
    * @return the ignoreCase flag
    */
    public boolean isIgnoringCase()
        {
        return m_fIgnoreCase;
        }

    /**
    * Lookup an OPToken by name
    *
    * @param sName  the name of the token to lookup
    *
    * @return the token found under name or null if absent
    */
    public OPToken lookup(String sName)
        {
        String nm = m_fIgnoreCase ? sName.toLowerCase() : sName;
        if (m_disabled.contains(nm))
            {
            return null;
            }
        Object o = m_table.get(nm);
        if (o == null)
            {
            return null;
            }
        else
            {
            return (OPToken) o;
            }
        }

    /**
    * Add a token t under the given name
    *
    * @param sName  string identifier for this token
    * @param token  the token to add
    *
    * @return the given token
    */
    public OPToken addToken(String sName, OPToken token)
        {
        m_table.put(sName, token);
        return token;
        }

    /**
    * Add a token t under the given name with given ast name
    *
    * @param sName     string identifier for this token
    * @param token     the token to add
    * @param sASTName  the token's led symbol
    *
    * @return the given token
    */
    public OPToken addToken(String sName, OPToken token, String sASTName)
        {
        m_table.put(sName, token);
        token.setLedASTName(sASTName);
        return token;
        }

    /**
    * Add a token t under the given name with given led and nud names
    *
    * @param sName     string identifier for this token
    * @param token     the token to add
    * @param sLedName  the token's led symbol
    * @param sNudName  the token's nud symbol
    *
    * @return the given token
    */
    public OPToken addToken(String sName, OPToken token, String sLedName,
            String sNudName)
        {
        m_table.put(sName, token);
        token.setLedASTName(sLedName);
        token.setNudASTName(sNudName);
        return token;
        }


    /**
    * Add a token t under the id stored in the token
    *
    * @param token  the token to add
    *
    * @return the given token
    */
    public OPToken addToken(OPToken token)
        {
        m_table.put(token.getId(), token);
        return token;
        }

    /**
    * Add a token t  with given ast name
    *
    * @param token    the token to add
    * @param sASTName  the token to add
    *
    * @return the given token
    */
    public OPToken addToken(OPToken token, String sASTName)
        {
        m_table.put(token.getId(), token);
        token.setLedASTName(sASTName);
        return token;
        }

    /**
    * Add a token alias under the given name for a token already installed
    *
    * @param sName       string identifier for token alias
    * @param sInstalled  the name of a token already in the table
    */
    public void alias(String sName, String sInstalled)
        {
        Object o = m_table.get(sInstalled);
        if (o != null)
            {
            m_table.put(sName, o);
            }
        }

    /**
    * Disable an installed token stored under the given name
    *
    * @param sName  string identifier for token to disable
    */
    public void disable(String sName)
        {
        m_disabled.add(sName);
        }

    /**
    * Enable an installed token stored under the given name
    *
    * @param sName  string identifier for token to enable
    */
    public void enable(String sName)
        {
        m_disabled.remove(sName);
        }


    // ----- token factory --------------------------------------------------

    /**
    * Construct a new literal OPToken instance with the given parameters.
    *
    * @param sValue     string representation of the literal
    * @param nTypeCode  the type code for this literal token
    *
    * @return the token of some appropriate class
    */
    public OPToken newLiteral(String sValue, int nTypeCode )
        {
        return new LiteralOPToken(sValue, nTypeCode, m_sIiteralASTName);
        }

    /**
    * Construct a new identifier OPToken instance with the given id.
    *
    * @param sValue  string representation of the literal
    *
    * @return the token of some appropriate class
    */
    public OPToken newIdentifier(String sValue)
        {
        return new IdentifierOPToken(sValue, m_sIdentifierASTName);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The Map that holds the name to token mappings
    */
    HashMap m_table = new HashMap();

    /**
    * A set of token names that are currently disabled
    */
    HashSet m_disabled = new HashSet();

    /**
    * A ast name for literals
    */
    boolean m_fIgnoreCase = false;

    /**
    * A  ast name for identifiers
    */
    String m_sIdentifierASTName = null;

    /**
    * A ast name for literals
    */
    String m_sIiteralASTName = null;
    }