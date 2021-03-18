# Development Guidelines

This page provides information on how to successfully contribute to the Coherence product. Coding conventions are stylistic in nature and the Coherence style is different to many open source projects therefore we understand the raising of eyebrows. However, consistency is significantly more important than the adopted subjective style, therefore please conform to the following rules as it has a direct impact on review times.

## Contents
1. [Coding Guidelines](#intro)
    1. [Java File Layout and Structure](#1)
    2. [File Headers](#2)
    3. [Import Declarations](#3)
    4. [Indentation and Spacing](#4)
    5. [Wrapping and Alignment](#5)
    6. [Variable Declarations](#6)
    7. [Member Declarations](#7)
    8. [Enum Declarations](#8)
    9. [Conditional Statements and Expressions](#9)
    10. [Java Documentation and Comments](#10)
    11. [Formatting Casts](#11)
    12. [Use of Generics](#12)
    13. [Lambdas](#13)
    14. [Streams](#14)
    15. [Use of @Override](#15)
    16. [Do not (directly) use Java's interruptible methods](#16)
    17. [General Advice](#17)
1. [Tools](#tools)
1. [TDE](#tde)


# <a name="intro"></a>Coding Guidelines

The rules below must be adhered to with exceptions being specifically called out to the reviewer. Exceptions to the below rules are exactly that, therefore if an exception is to be presented it must have a compelling justification.

> Note: using tools such as [IntelliJ code formatter or JIndent](#tools) do significantly help in conforming, however we would strongly encourage embracing the conventions as you develop opposed to running the modifications through a formatter prior to committing (there is no `cohfmt` akin to `gofmt` ... currently).

> Note: If you are writing guides or tutorials to be included in `prj/examples` directory then please use standard
Java coding conventions as described [here](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html).

## <a name="1"></a>1. Java File Layout and Structure

1. A .java file contains three sections: A required package statement, an option set of imports, and the class/interface/enum definition. With the exception of Inner-Class definitions (see below) a .java file will contain only a single definition.

```java
package com.tangosol.util;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * ..
 */
public class FilterEnumerator
        extends Base
        implements Enumeration, Iterator
    {
    }
```

2. The first line contains the package declaration and there are one blank line between each section.

> **Note:** the previous style guidelines required a blank line at the top of a file. This is no longer a requirement for new code. Existing files may be left as-is, but if you're changing an existing file you should try to cleanup said empty lines.

3. Classes, interfaces and enums should be structured as follows with section separators to contain areas of declaration.  
(sections are not required if a class consists of a single section)

```java
public class Foo
        implements Bar
    {
(optional blank line)
    //----- constructors ---- (fill to 78 characters) ------
(blank line)
    public Foo(...)
        {

        }
(blank line)
    public Foo(...)
(blank line)
    //----- Foo methods ---- (fill to 78 characters) ------
(blank line)
    public foo1(...)
(blank line)
    public foo2(...)
(blank line)
    public foo3(...)
(blank line)
    //----- Bar interface ---- (fill to 78 characters) ------
(blank line)
    public bar(...)
(blank line)
    // ----- Object methods  ---- (fill to 78 characters) ------
(blank line)
    public String toString()...
(blank line)
    // ----- helpers  ---- (fill to 78 characters) ------
(blank line)
    // ----- constants ---- (fill to 78 characters) ------
(blank line)
    public static final String ...
(blank line)
    // ----- data members ---- (fill to 78 characters) ------
(blank line)
    protected Map m_map...
    }
```

4. There should be one blank line above and below a separator.

5. There should be one blank line between each method and/or constructor declared in the same section.

6. There should be one blank line between member and/or constant declaration.

> **Warning:** The previous style guidelines contained several different spacing rules. This guide adopts a simple rule: A single blank line is all that is required between declarations and sections.

## <a name="2"></a>2. File Headers

1. All files should have a header of the following format where `<year>` is replaced with the year the file was introduced.

```java
/*
 * Copyright (c) 2000, <year>, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
```

## <a name="3"></a>3. Import Declarations

1. All import statements should be sorted alphabetically.

2. All imports should clearly separate imports from different packages, except when past three levels deep. That is, there's no requirement to separate packages when the first three levels are the same unless it makes sense to do so.

3. Do not use the `.*` style of importing, unless you are importing more than 99 classes from a single package.

4. Import all necessary classes from packages that are different from the package that the class is declared with in.

5. Never import any unnecessary classes ("no unused imports").

Example import section from the "ConverterCollections" class in the "com.tangosol.util" package:

```java
import com.oracle.coherence.common.base.Holder;
import com.oracle.coherence.common.base.NaturalHasher;

import com.tangosol.internal.net.NamedCacheDeactivationListener;
import com.tangosol.internal.util.processor.CacheProcessors;

import com.tangosol.io.Serializer;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.CacheMap;
import com.tangosol.net.cache.ConfigurableCacheMap;

import com.tangosol.util.InvocableMap.EntryAggregator;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.MapListenerSupport.WrapperListener;

import com.tangosol.util.function.Remote;

import java.io.Serializable;

import java.lang.reflect.Array;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import java.util.function.BiFunction;
```

## <a name="4"></a>4. Indentation and Spacing

1. Block level indentation is 4 spaces.

2. Statements start at the same level as the braces in the block.
```java
    {
    super();
    }
```

3. All control structure statements **must** use blocks. That is, `if`, `for`, `while`, `do` and `switch` statements must use blocks. Single statement blocks are not permitted.

Good Example:
```java
    if (fCondition)
        {
        cSize++;
        }
    else
        {
        cSize--;
        }
```

Bad Example:
```java
    if (fCondition)
        cSize++;
    else
        cSize--;
```

4. A blank line should follow each logical section (ie: indicating an end of thought)

Example recommended post block space.
```java
    if (fCondition)
        {
        cSize++;
        }
    else
        {
        cSize--;
        }

    System.out.println("a message");
```

5. All structural statement keywords require a space after their name.

Example:

```java
    if (fCondition)
        {
        cSize++;
        }
```

Bad Example:

```java
    if(fCondition)
        {
        cSize++;
        }
```

6. Array initializations should have no spaces between brackets.

Example:
```java
Object[] aoParams = new Object[] {this, clz, sName, sXmlName, xml};
```

7. Trailing whitespaces are not permitted.

## <a name="5"></a>5. Wrapping and Alignment

1. Line wrapping must be done when the line exceeds 120 characters. Optionally, it is acceptable to wrap at 78 characters if the author prefers, as is the majority of the code base.

> Note: if editing an existing file, the existing standard should be followed throughout (either 78 or 120 characters)

2. Line wrapping may occur at less than 120 characters if one believes it will improve readability.

3. When wrapping a line, indent 4 spaces from the start of the previous line.

> Note: the previous style guidelines required indenting to occur 8 spaces from the previous line. This is still acceptable. No effort needs to be made to existing code, but effort should be made to ensure consistency with existing code.

4. On subsequent line wrappings of the same statement, use zero spaces to ensure wrapping line expressions are lined up.

> Note: the previous style guidelines required subsequent indenting to occur 4 spaces from the previous line. This is still acceptable. No effort needs to be made to existing code, but effort should be made to ensure consistency with existing code.

5. For class declarations, line wrapping (when required) must be 8 spaces from the start of the line. i.e. each **extends** and **implements** declaration must be indented 8 spaces under the class line.

```java
public class ClassScheme
        extends Scheme
        implements ReflectiveScheme
```

6. For method declarations, checked exceptions should be declared on the line following the method declaration, with subsequent exceptions added on the same line.
```java
    public void unregisterMBean(ObjectName name)
            throws InstanceNotFoundException, MBeanRegistrationException
```
7. When wrapping an assignment statement, leave the equals sign on the previous line and for other operators, including the . operator, place them on the start of the new indented line.  For string concatenation, put the "+" at the end of the line before wrapping to help inform the reader that the concatenation continues on the next line.

Example: Assignment Wrapping
```java
    sVariableToAssign =
        "this is a string";
```

Example: Operator Wrapping
```java
    sComposedString = "Log entry:" +
        message.toString +
        " time of entry:" +
        time.toString();
```

Example: dot Operator Wrapping
```java
    builderCacheServer = new CacheServerBuilder()
        .setCacheConfig("my-cache-config.xml")
        .setJMXEnabled(true)
        .setPOFConfig("my-pof-config.xml")
        .setLogLevel(5)
        .setSystemProperty("extend.port", 100);
```

8. When wrapping a conditional statement, end the line with boolean operator and align expressions according to their corresponding logical blocks  

Example: Boolean Operator Wrapping
```java
    if (memberTo.getMachineId() != memberFrom.getMachineId() &&
        (memberBackup == null ||
         memberTo.getMachineId() == memberBackup.getMachineId()))
```

Example: Unacceptable format with boolean operators at the beginning of the line
```java
    if (memberTo.getMachineId() != memberFrom.getMachineId()
       && (memberBackup == null
           || memberTo.getMachineId() == memberBackup.getMachineId()))
```

9. We expect assignment "=" operators to be aligned horizontally when multiple variables are assigned in a sequence.

Example: Assignment Operator Alignment
```java
int    iField  = 0;
int    cParams = 3;
String sData   = "foo";
```
Example: Assignment Operator Alignment
```java
Entry[] aeBucket = m_aeBuckets;
int     cBuckets = aeBuckets.length;
```
However, when mixing short and long type names, or fairly trivially with generics, this may worsen the appearance of the code. This can fairly trivially be overcome by starting a new declaration block.

## <a name="6"></a>6. Variable Declarations

Naming variables is one of the most intellectual tasks a developer undertakes. Please don't rely on your IDE to chose one for you. Accurately and consistently describing the intent of a variable is not the role of a development tool. It requires careful human consideration for the future reader of the code you develop.

1. Variable names should be short but descriptive. In general the length and descriptiveness of a variable name is directly proportional to the scope of the variable. That is a variable used in a single block that spans just few lines should be a short as possible (e.g. "e" instead of "classNotFoundException").

2. All variable names must start with an **intent prefix**, a common abbreviation indicating the main use of a variable. While often related to the underlying declared type, the purpose is not to specify type information but to provide contextual information to aid reading and comprehension of an implementation.

Commonly used Intent Abbreviations (for Java)

|Abbreviation|Intent|Discussion|
|------------|------|----------|
|`f`|A flag|Used for true/false values (but also TriStates)|
|`b`|A Byte|Used when 8-bit precision is required and needs to be respected|
|`n`|A Number|Used for general numeric values|
|`l`|A Long Number|Used when long precision is required and needs to be respected (not a date or time)|
|`ldt`|A Date/Time|Used to hold values returned by `System.currentTime()`|
|`date`|A Date/Time|Used for `Date` types|
|`c`|A Counter|Used for counter values, including those represented as ints/longs/shorts/AtomicIntegers/AtomicLongs|
|`of`|An offset|Used for offsets from a known position in a data-structure|
|`s`|A String|Used for all String objects|
|`sb`|A StringBuilder|Used for all StringBuilder instances|
|`fl`|A 32-bit Floating Pointer Number|For simple precision floating point numbers|
|`d`|A 64-bit Floating Pointer Number|For high precision floating point numbers|
|`i`|An Index|Used with in loops and indices in data-structures|
|`e` or `t`|An Exception|Used for all Exceptions (checked or unchecked)|
|`a`|An Array|Must be followed by another intent prefix indicating the intent of the array|
|`clz`|A Class|Used for java.lang.Class types|
|`col`|A Collection|Used for java.util.Collection types|
|`list`|A List|Used for java.util.List types|
|`map`|A Map|Used for java.util.Map types|
|`set`|A Set|Used for java.util.Set types|
|`entry`|An Entry|Used for java.util.Map.Entry types|
|`evt`|An Event|Used for \*Events|
|`iter`|An Iterator|Used for Iterator implementations|
|`itrb`|An Iterable|Used for Iterable implementations|
|`o`|An Object|**Only** to be used for java.lang.Object references where type information is unknown|

Commonly used Intent Abbreviations (for Coherence)

|Abbreviation|Intent|Discussion|
|------------|------|----------|
|`aggr`|An Aggregator|Used for *Aggregators|
|`atomic`|Atomic* types|Used for Atomic*|
|`bin`|A Binary|Used for Binaries|
|`binEntry`|A BinaryEntry|Used for BinaryEntries|
|`bldr`|A Builder|Used for *Builders|
|`cache`|A Cache|Used for *Caches|
|`ctx`|A Context|Used for *Contexts|
|`factory`|A Factory|Used for *Factorys|
|`incptr`|An EventInterceptor|Used for EventInterceptors|
|`mgr`|A Manager|Used for *Managers|
|`parts`|A PartitionSet|Used for PartitionSet|
|`proc`|A Processor|Used for *Processors|
|`scheme`|A Scheme|Used for *Schemes|
|`store`|A Store|Used for *Stores|
|`task`|A Task|Used for Runnables, Tasks, Invocables and Agents|

Example: Sensible variable names
```java
PartitionSet partsOwned = ...;
BinaryEntry  binEntry   = ...;
Binary       binKey     = ...;
byte[]       abKey      = ...;
int          cParts     = ...;
int          ofStart    = ...;
int          cMillis    = ...;
int          nMask      = ...;
```

Example: Unacceptable variable names
```java
byte[]       abBytes    = ...;  // "Bytes" is redundant and adds no information
int          nFileCount = ...;  // "n" is an inappropriate intent prefix; should be "cFiles"
int          nOffset    = ...;  // "n" is an inappropriate intent prefix; should be "of"
Object[]     aoArray    = ...;  // "Array" is redundant and adds no information
File[]       afileFiles = ...;  // "Files" is redundant; should be "aFiles"
```
3. All collection and array type names should be **plural**.

> Note: the previous style guidelines required array variable names to be singular. This is still acceptable for existing code. No effort needs to be made to existing code, but effort should be made to ensure consistency with the new rule moving forward.

4. Array declarations must occur on the type declaration, not on the variable name.

Example: Recommended array declaration
```java
int[] anSizes = new int[MAX_COUNT];
```

Example: Unacceptable array declaration
```java
int anSizes[] = new int[MAX_COUNT];
```

5. For complex (non-intrinsic) Java types, the intent prefix should reflect a core use of the reference in a shortest but recognizable manner.

6. Where a variable is used only once, abbreviates may not be required. However abbreviations are encouraged when a type is used more than once.

Example variable declarations:
```java
Object oHolder; // polymorphic object holding whatever

ArrayList listFrom, listRemotePublishers;

SafeHashMap mapIndexes;

DefaultCacheFactoryBuilder builderCustom, cfbCustom;

RemoteClusterPublisherScheme schemeRemote, rcps;

Map<String, RemoteClusterPublisherScheme> mapSchemes;

BackingMapManagerContext ctxService;
```

7. When using Generics there is no need prefix variable declarations with "o".

> Note: this is because the type is actually known where are "o" is required when runtime types are unknown.

Example: Recommended variable and parameter declaration using generics
```java
K key;
V value;
```

Example: Unacceptable variable and parameter declaration using generics
```java
K oKey;
V oValue;
```

8. When using Generics there is no need to add **further** intent prefixes.

Example: Recommended declaration with generics
```java
ArrayList<Integer> listAges;
Map<String, RemoteClusterPublisherScheme> mapSchemes;
```

Example: Unacceptable declaration with generics
```java
ArrayList<Integer> listnAges;
Map<String, RemoteClusterPublisherScheme> mapsschemesSchemes;
```

## <a name="7"></a>7. Member Declarations

1. Member mutable field declarations must be prefixed with `m_` and appear at the end of a class declaration.

2. Static mutable fields are prefixed with `s_` and appear before non-static fields at the end of a class declaration.

3. Member non-mutable (final) field declarations must be prefixed with `f_` and appear at the end of a class declaration.

4. Static final fields are all CAPS, with an underscore `_` used to separate words.

Example: Static declaration
```java
private static final int MAXIMUM_SIZE = 20;
```

## <a name="8"></a>8. Enum Declarations

1. Enum types are considered to be regular classes and should be treated as such.

2. Enum constants are considered to be static final fields and should be treated as such.

## <a name="9"></a>9. Conditional Statements and Expressions

1. Avoid using double-negative expressions first in conditional statements and expressions.

Example: Recommended conditional statement declaration.
```java
if (sName.isEmpty())
    {
    // so something when empty
    }
else
    {
    // do something when not empty
    }
```

Example: Unacceptable - using a double-negative conditional statement
```java
if (!sName.isEmpty())
    {
    // do something when not empty
    }
else
    {
    // so something when empty
    }
```

2. Use the ternary operator to be succinct for assignments

Example: Conditional expression declaration
```java
int nSize = parameter == null ? 0 : parameter.size();
```

3. If the ternary spans multiple lines, start a new line for each branch in the ternary

Example: Multi-line ternary
```java
return isBalanced()
            ? "BALANCED"
            : isVulnerable()
                ? "VULNERABLE"
                : "SAFE"
```

4. Within switch statements, each case statement must occur on its own line with associated code on subsequent lines.

5. There should be a newline following a break statement. Fall through is allowed but should be commented if not obvious.

Example: Recommended switch statement layout
```java
switch (nType)
    {
    case T_REFERENCE:
        af = (boolean[]) lookupIdentity(in.readPackedInt());
        break;

    case V_REFERENCE_NULL:
        break;

    case V_STRING_ZERO_LENGTH: // obvious fall-through
    case V_COLLECTION_EMPTY:
        af = BOOLEAN_ARRAY_EMPTY;
        break;

    default:
        throw new IOException("unable to convert type " +
            nType + " to an array type");
    }
```

## <a name="10"></a>10. Java Documentation and Comments

1. All classes, interfaces, enums, methods, statics and field declarations must contain a JavaDoc comment. An exception to this rule is made for anonymous inner-classes.

2. All method JavaDoc comments must commence with a verb.

3. Include a blank comment line between method summary and subsequent JavaDoc tags, i.e. parameter descriptions (`@param`).

4. Do not have a period at the end of the @param comment.

5. We follow the Sun Java style for JavaDoc comments.

Example: Required JavaDoc Style
```java
/**
 * We like our comments this way now.
 */
 ```

Example: Unacceptable JavaDoc Style
```java
/**
* Though the previous version used this style.
*/
```

> Note: the previous style guidelines required initial '`*`'s to be aligned under the /. We no longer require this as we're using the Java standard approach. No effort needs to be made to correct existing code, but effort should be made to ensure new code follows the new standard.

6. Always provide `@param`, `@return`, `@throws`, `@since` declarations in that order.

7. There must be a blank comment line between each JavaDoc tag (`@param`, `@return` et al) declaration except between `@author` and `@since`.

8. Align `@param` declaration descriptions.

Example: Aligned parameter documentation
```java
/**
 * ...
 * @param binKey    key to store the value under
 * @param binValue  value to be stored
 */
public void store(Binary binKey, Binary binValue);
```

9. Capitalization and punctuation are required. JavaDoc is generated for user consumption and thus must be readable.

10. Use of {`@inheritDoc`} is only required when supplementing inherited documentation. The javadoc tool will copy the documentation from the parent without the child specifying {`@inheritDoc`}, however we do ask for the `@Override` annotation to be present for clarity.

11. Validate comment readability in IDEA using `<CTRL-Q>` (IDEA on Windows), `<CTRL-J>` (IDEA on Mac) or `F2` with Eclipse.

12. If you need to break the comment in to paragraphs for readability, use the HTML `<p>` tag on an empty line. Do **not** use `<p/>` or `<p>` followed by `</p>`.

Example: Recommended JavaDoc
```java
/**
 * Return a set view of the keys contained in this map for entries that
 * satisfy the criteria expressed by the filter.
 * <p>
 * Unlike the {@link #keySet()} method, the set returned by this method
 * may not be backed by the map, so changes to the set may not reflected
 * in the map, and vice-versa.
 *
 * @param filter  the Filter object representing the criteria that
 *                the entries of this map should satisfy
 *
 * @return a set of keys for entries that satisfy the specified criteria
 */
public Set keySet(Filter filter);
```

13. It's unacceptable to leave auto-generated/default IDE comments in the code.

Example: Unacceptable JavaDoc
```java
/**
 * Created by IntelliJ IDEA. User: flastname
 */
```

Example: Unacceptable JavaDoc
```java
// TODO Auto-generated method stub
```

14. Single line comments should have a space between the `//` and the first word.

Example: Recommended single line comment
```java
// this is a correct single line comment
```
Example: Unacceptable single line comment
```java
//This is an incorrect single line comment
```

General guidelines for single line/in-line comments:

14.a. Unlike JavaDoc comments, single line comments are not full sentences (no periods at the end of the comment); they are short phrases intended to guide the reader of the source

14.b. Avoid documenting the obvious

Example: Acceptable in-line comment
```java
// no need to synchronize; it is acceptable that two threads would
// instantiate a key set
Set set = m_setKeys;
if (set == null)
    {
    m_setKeys = set = instantiateKeySet();
    }
```

Example: Unacceptable in-line comment
```java
// set flag to false
boolean fDone = false;
```

## <a name="11"></a>11. Formatting Casts

1. A space must be inserted after a cast.

Example: Acceptable cast
```java
String s = (String) obj;
```

Example: Unacceptable cast
```java
String s = (String)obj;
```

## <a name="12"></a>12. Use of Generics

1. It is only acceptable to generics if there is a clear benefit from their use to aid clarity, type-safety and usability of the resulting code.

2. It is **unacceptable** to use `<?>` to suppress compile-time warnings. When using Generics you should always attempt to identify/narrow the required type. Generally it's only acceptable to use `<?>` when dealing with reflection. For example, when using `Class<?>` where it may not be possible to specify or narrow a type.

3. Like regular parameters, all generic parameters must be clearly documented in javadoc.

4. Like regular parameters, there should be a space between the generic parameters when they are declared. eg: `<K, V>` not `<K,V>`

5. Methods which take generified types as parameters should apply the PECS rule, namely they should take `Set<? extends Dog>` or `Set<? super Dog>` rather then `Set<Dog>`

6. Methods returning generified types should not use "? extends" or "? super" if at all possible.

## <a name="13"></a>13. Lambdas

1. Single-statement lambdas should be written on a single line:

```java
Consumer<String> printer = s -> System.out.println(s);
```

2. Multi-statement lambdas should have a line break before an opening brace and the implementation should be indented 4 spaces:
```java
Consumer<String> charPrinter = s ->
    {
    for (Character ch : s.toCharArray())
        {
        System.out.println(ch);
        }
    }
```

3. The parentheses should be used around lambda arguments only if required by the Java compiler (ie. when there are two or more arguments).

4. The nesting of lambdas should be avoided, not only because it creates a [Pyramid of Doom](https://en.wikipedia.org/wiki/Pyramid_of_doom_(programming)), but more importantly because it makes inner lambdas non-remotable, thus breaking the support for remoting of outer lambdas as well.

Example: Don't do this
```java
Function<String, String> encoder = s ->
    {
    StringBuilder sb = new StringBuilder();

    BiFunction<Character, Integer, Character> charEncoder = (ch, nPos) -> (char) (ch + nPos);
    for (int i = 0; i < s.length(); i++)
        {
        sb.append(charEncoder.apply(s.charAt(i), i));
        }
    return sb.toString();
    };
```

Instead, each inner lambda should be captured into a local variable that is accessible by the outer lambda.

Example: Do this instead
```java
BiFunction<Character, Integer, Character> charEncoder = (ch, nPos) -> (char) (ch + nPos);

Function<String, String> encoder = s ->
    {
   StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++)
        {
        sb.append(charEncoder.apply(s.charAt(i), i));
        }
    return sb.toString();
    };
```

This way the `charEncoder` will become a captured argument of `encoder`, which will allow it to be properly remoted. It is important to note that method references are lambdas as well, which means that you should never use a method reference inside of another lambda directly either.

## <a name="14"></a>14. Streams

1. Simple uses of Stream API, where you call terminal operation immediately, can be written on a single line:
```java
InvocableMap.Entry<Long, Person> oldestPerson = people.stream().max(Person::getAge);
```

2. However, if there are any intermediate operations in the pipeline, each intermediate operation and the terminal operation should be on its own line and indented 8 spaces:
```java
Map<Long, Person> mapMinors = people.stream()
		.map(entry -> entry.getValue())
		.filter(person -> person.getAge() < 18)
		.collect(toMap(Person::getId, Function.identity());
```

## <a name="15"></a>15. Use of @Override

The annotation is recommended when:
* Overriding a method from a super class, especially when said method fails to make a call to the method being overridden. e.g.: the method fails to call `super.method(...)`
* Anytime it may not be obvious that a method is overriding another method (i.e. a protected method)

The annotation is not recommended when:

*   It is obvious that a method is being overridden (for example toString, equals, etc)
*   Implementing common interfaces (for example `Map.put`, `Map.get`, etc)
*   A class that is implementing a single interface
*   Anonymous classes

## <a name="16"></a>16. Do not (directly) use Java's interruptible methods

This includes:
```java
Object.wait
Thread.sleep
Thread.interrupted
Selector.select
Lock.lockInterruptibly
Lock.tryLock(time)
Condition.await
LockSupport.park/parkNanos
```

Nope, not kidding, don’t use them instead use the com.oracle.coherence.common.base.Blocking static helpers correspond to each of them.

i.e. `o.wait()` becomes `Blocking.wait(o)`, `o.wait(1000)`, becomes `Blocking.wait(o, 1000)`, you get the idea.

Why? Well because now we can override any thread’s behavior while blocked in any of our code, specifically while in at an interruptible point. For an example of a feature which is dependent on this behavior see `com.oracle.coherence.common.base.Timeout`.

## <a name="17"></a>17. General Advice

This section provides general advice about coding style and implementation semantics. Where possible they should be followed as we've been burnt by failing to do so in the past.

1. If at all possible, avoid private methods. Use protected as they allow sub-classing/overriding. You never know when you'll need to sub-class an implementation

2. If a field has both a getter and setter, the field must be private.

3. If a field doesn't have a setter, it should be protected.

4. Fields should never be public.

5. Methods that access non-final members (field read or "getter" method calls) multiple times should pull the result into a local.

Example: Unacceptable - repeated read of `m_cElems` field.
```java
if (m_cElems > 0)
    {
    return m_cElems;
    }
```

Example: Recommended - cache to a local
```java
int cElems = m_cElems;
if (cElems > 0)
    {
    return cElems;
    }
```

# <a name="tools"></a>Tools
* IntelliJ settings file: [here](tools/conf/coh-idea-codescheme.xml)
* JIndent style: [here](tools/conf/coh-jindent-codingstyle.xjs)

# <a name="tde"></a>TDE

TDE is an entire development environment, IDE + ([bootstrapping](https://en.wikipedia.org/wiki/Bootstrapping_(compilers))) compiler, that allows the editing and compilation of components (.cdb files). If you source one of the [cfg* scripts](./bin) to set up your local shell session you can launch TDE by executing `tde` or there is an [osx package](tools/tde/bin/TDE.app) if you prefer. This is far from an introduction to TDE, however does provide some useful info for those that are trained in the art thereof.

## IDE Keyboard shortcuts

| Key stroke | Descritpion |
|------------|-------------|
|Ctrl-A, Command-A|Select all.|
|Ctrl-B, Command-B|Go to a method declaration. works only within a scope of a single component.|
|Ctrl-C, Command-C|Copy text to the Clipboard.|
|Ctrl-X, Command-X|Cut text to the Clipboard.|
|Ctrl-V, Command-V|Paste text from the Clipboard.|
|Ctrl-Z, Command-Z|Undo the last change.|
|Ctrl-Y, Command-Y|Redo the last change.|
|Tab|Indent the current selection.|
|Shft-Tab|Outdent the current selection.|
|Ctrl-F, Command-F|Find text.|
|Ctrl-G, Command-G|Go to line.|
|Ctrl-F3, Command-F3|Use selection to find.|
|F3|Find next.|
|Shft-F3|Find previous.|
|F4|Find next error or search item or bookmark (depending on the Output panel tag).|
|Shft-F4|Find previous error or search item or bookmark (depending on the Output panel tag).|
|F5|Refresh the browser panel.|
|F7|Compile.|
|Ctrl-F2, Command-F2|Toggle the bookmark.|
|Ctrl-F4, Command-W|Close component.|
|Ctrl-Alt-Left, Command-Alt-Left|Go to the previous location within component (history).|
|Ctrl-Alt-Right, Command-Alt-Right|Go to the next location within component (history).|
|Ctrl-}, Command-}|Place the cursor on the matching brace.|

## Property values

* `[null]`
* `[instance]`
* `class-name`
* a line in the Doc of a property stating `@volatile` will make a property volatile (this allowed Gene to not enter GUI land)

## Custom font in the ScriptEditor

To use a custom font, before opening the TDE add any of the following properties to the $HOME/TDE.properties file:

```
UI.Font.Scripts.Size=12
UI.Font.Scripts.Name=Lucida Console
```

## Java debugger support

To allow Java debugging for TDE created code:

1. Add the following property to `$HOME/TDE.properties`:
```
Storage.Class.JavaLines=true

```
2. Recompile the TDE based code and build the Java source (listing)
```
cd $DEV_ROOT/prj
mvn install
```
> Note: uncompilable java sources based on the associated components, which can be used for debugging, are generated under `$DEV_ROOT/prj/coherence-core-components/target/artifact-sources`
