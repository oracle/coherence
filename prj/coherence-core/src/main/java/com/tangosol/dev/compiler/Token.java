/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


/**
* Represents a language token.  This interface is implemented for each
* specific language, and potentially for each token or category of
* tokens within each language.  This interface is intended only as the
* basis for a token object's actual interface; other methods will be
* necessary to describe tokens that have values of a particular type,
* for example.
*
* @version 1.00, 11/21/96
* @author 	Cameron Purdy
*/
public interface Token
    {
    /**
    * Determines the unique identifier of a predefined token.  For example,
    * operators are usually pre-defined by a language and can therefore be
    * referenced as enumerated constants by a compiler.
    *
    * @return the token identifier, or 0 if the token does not have an ID
    */
    public int getID();

    /**
    * Determines the category that a token belongs to.  Most languages, for
    * example, define categories for literals, operators, and identifiers.
    *
    * @return the category of the token
    */
    public int getCategory();

    /**
    * Determines the sub-category that a token belongs to.  Some categories
    * are further divided into sub-categories; most languages, for example,
    * define sub-categories of literals, like numeric literals and string
    * literals.
    *
    * @return the sub-category of the token or 0 if sub-categorization is
    *         not applicable to this token
    */
    public int getSubCategory();

    /**
    * Provides the value of the token if applicable.  This is most useful
    * for literal tokens.
    *
    * @return the value of the token or null if the token has no value
    */
    public Object getValue();

    /**
    * Determines the line of the script which contains the token.  The
    * line number is zero-based and corresponds to the line in which the
    * first character of the token was encountered.
    *
    * @return the token's line number
    */
    public int getLine();

    /**
    * Determines the offset of the token within the line of the script.
    * The offset is zero-based and specifies the location of the first
    * character of the token.
    *
    * @return the token's offset within the line
    */
    public int getOffset();

    /**
    * Determines the length, in characters, of the token.  The length of the
    * token is calculated as the difference between the offset of the first
    * character following the token and the offset of the first character of
    * the token itself.
    *
    * @return the length of the token
    */
    public int getLength();

    /**
    * Adjusts the location (line/offset) of the token relative to it current
    * location.
    */
    public void adjust(int dLine, int dOffset);

    /**
    * Provides a string representation of the token as it would appear in
    * a script.  In other words, this method reverse-engineers the token.
    *
    * @return the token as it would appear in a script
    */
    public String getText();

    /**
    * Provides a human-readable description of the token, including any
    * category and sub-category information.
    *
    * @return a human-readable description of the token
    */
    public String toString();
    }
