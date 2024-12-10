/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Copyright 2011-2014 Genson - Cepoi Eugen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.oracle.coherence.io.json.genson.stream;


import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import static com.oracle.coherence.io.json.genson.stream.ValueType.*;

public class JsonReader implements ObjectReader {

  protected final static int[] SKIPPED_TOKENS;

  static {
    SKIPPED_TOKENS = new int[128];
    SKIPPED_TOKENS['\t'] = 1;
    SKIPPED_TOKENS['\b'] = 1;
    SKIPPED_TOKENS['\n'] = 1;
    SKIPPED_TOKENS['\r'] = 1;
    SKIPPED_TOKENS['\f'] = 1;
    SKIPPED_TOKENS[' '] = 1;
  }

  private final static boolean[] _NEXT_TOKEN = new boolean[128];

  static {
    _NEXT_TOKEN[','] = true;
    _NEXT_TOKEN['"'] = true;
    _NEXT_TOKEN['{'] = true;
    _NEXT_TOKEN['['] = true;
    _NEXT_TOKEN['n'] = true;
    _NEXT_TOKEN['N'] = true;
    _NEXT_TOKEN['-'] = true;
    _NEXT_TOKEN['t'] = true;
    _NEXT_TOKEN['f'] = true;
    _NEXT_TOKEN['T'] = true;
    _NEXT_TOKEN['F'] = true;

    for (int i = 48; i < 58; i++)
      _NEXT_TOKEN[i] = true;
  }

  private final static char[] _END_OF_LINE = new char[]{'\n'};
  private final static char[] _END_OF_BLOCK_COMMENT = new char[]{'*', '/'};

  /*
   * Recupere dans Jackson
   */
  private final static int[] sHexValues = new int[128];

  static {
    Arrays.fill(sHexValues, -1);
    for (int i = 0; i < 10; ++i) {
      sHexValues['0' + i] = i;
    }
    for (int i = 0; i < 6; ++i) {
      sHexValues['a' + i] = 10 + i;
      sHexValues['A' + i] = 10 + i;
    }
  }

  private final static double[] _POWS = new double[309];

  static {
    for (int i = 0; i < _POWS.length; i++)
      _POWS[i] = Math.pow(10, i);
  }

  private final Reader reader;
  private final boolean strictDoubleParse;
  private final boolean readMetadata;
  private final char[] _buffer = new char[2048];
  private int _col;
  private int _row;
  private int _cursor;
  private int _buflen;

  private char[] _stringBuffer = new char[16];
  private int _stringBufferTail = 0;
  private int _stringBufferLength = _stringBuffer.length;

  private String currentName;
  private String _stringValue;
  protected long _intValue;
  protected double _doubleValue;
  private int _numberLen = 0;
  private Boolean _booleanValue;
  private ValueType valueType;
  private boolean _first = true;
  private boolean _metadata_readen = false;
  private Map<String, Object> _metadata = new HashMap<>(5);

  private final Deque<JsonType> _ctx = new ArrayDeque<JsonType>(10);

  {
    _ctx.push(JsonType.EMPTY);
  }

  public JsonReader(String source) {
    this(new StringReader(source), false, false);
  }

  public JsonReader(Reader reader, boolean strictDoubleParse, boolean readMetadata) {
    this.reader = reader;
    this.strictDoubleParse = strictDoubleParse;
    this.readMetadata = readMetadata;

    char token = (char) readNextToken(false);
    if ('[' == token) valueType = ARRAY;
    else if ('{' == token) valueType = OBJECT;
    else {
      // ok lets try to read next
      if (_buflen > 0) {
        try {
          valueType = consumeValue();
        } catch (JsonStreamException jse) {
          try {
            // we must cheat because consumeString attends the current token to be "
            // and will increment the cursor
            _cursor = -1;
            _col = -1;
            _stringValue = consumeString('"');
            valueType = STRING;
          } catch (RuntimeException re) {
            throw re;
          }
        }
        if (valueOf(valueType.name()) == null)
          throw new JsonStreamException(
            "Failed to instanciate reader, first character was " + (char) token
              + " when possible characters are [ and {");
      } else valueType = NULL;
    }
  }

  public void close() {
    try {
      reader.close();
    } catch (IOException e) {
      throw new JsonStreamException(e);
    }
  }

  public ObjectReader beginArray() {
    begin('[', JsonType.ARRAY);
    valueType = ARRAY;
    if (_metadata_readen) _metadata.clear();
    return this;
  }

  public ObjectReader beginObject() {
    if (!_metadata_readen) {
      begin('{', JsonType.OBJECT);
      valueType = OBJECT;
      if (readMetadata) {
        _metadata.clear();
        readMetadata();
      }
    }
    return this;
  }

  public ObjectReader nextObjectMetadata() {
    return beginObject();
  }

  public ObjectReader endArray() {
    end(']', JsonType.ARRAY);
    return this;
  }

  public ObjectReader endObject() {
    end('}', JsonType.OBJECT);
    _metadata.clear();
    _metadata_readen = false;
    return this;
  }

  public String name() {
    if (enclosingType() != JsonType.OBJECT)
      throw new JsonStreamException("Only json objects have names, actual type is "
        + valueType);
    return currentName;
  }

  public String valueAsString() {
    if (STRING == valueType || CHAR == valueType) return _stringValue;
    if (INTEGER == valueType) return "" + _intValue;
    if (DOUBLE == valueType) return "" + _doubleValue;
    if (NULL == valueType) return null;
    if (BOOLEAN == valueType) {
      return _booleanValue.toString();
    }
    throw new JsonStreamException("Readen value can not be converted to String");
  }

  public int valueAsInt() {
    if (INTEGER == valueType) {
      int value = (int) _intValue;
      if (value != _intValue) throwNumberFormatException("an int", "overflowing long value " + _intValue);
      return value;
    } else if (DOUBLE == valueType) {
      int value = (int) _doubleValue;
      long longValue = (long) _doubleValue;
      // lets accept only if the integer part is the same and ignore the decimals
      if (value != longValue) {
        throwNumberFormatException("an int", "overflowing double value " + _doubleValue);
      }
      return value;
    } else if (STRING == valueType) return Integer.parseInt(_stringValue);

    throw new JsonStreamException("Expected a int but value is of type " + valueType);
  }

  public long valueAsLong() {
    if (INTEGER == valueType) {
      return _intValue;
    } else if (DOUBLE == valueType) {
      if (Long.MIN_VALUE > _doubleValue || _doubleValue > Long.MAX_VALUE) {
        throwNumberFormatException("a long", "overflowing double value " + _doubleValue);
      }
      return (long) _doubleValue;
    } else if (STRING == valueType) return Long.parseLong(_stringValue);

    throw new JsonStreamException("Expected a long but value is of type " + valueType);
  }

  public double valueAsDouble() {
    if (DOUBLE == valueType) {
      return _doubleValue;
    } else if (INTEGER == valueType) {
      // for the moment lets do that even if there is some precision loss...
      return Long.valueOf(_intValue).doubleValue();
    } else if (STRING == valueType) return Double.parseDouble(_stringValue);

    throw new JsonStreamException("Expected a double but value is of type " + valueType);
  }

  public short valueAsShort() {
    if (INTEGER == valueType) {
      short value = (short) _intValue;
      if (value != _intValue) throwNumberFormatException("a short", "overflowing long value " + _intValue);
      return value;
    } else if (DOUBLE == valueType) {
      short value = (short) _doubleValue;
      long longValue = (long) _doubleValue;
      // lets accept only if the integer part is the same and ignore the decimals
      if (value != longValue) {
        throwNumberFormatException("a short", "overflowing double value " + _doubleValue);
      }
      return value;
    } else if (STRING == valueType) return Short.parseShort(_stringValue);

    throw new JsonStreamException("Expected a short but value is of type " + valueType);
  }

  public float valueAsFloat() {
    if (DOUBLE == valueType) {
      return (float) _doubleValue;
    } else if (INTEGER == valueType) {
      // same as for doubles, for the moment lets do that even if there is some precision
      // loss...
      return Long.valueOf(_intValue).floatValue();
    } else if (STRING == valueType) return Float.parseFloat(_stringValue);

    throw new JsonStreamException("Expected a float but value is of type " + valueType);
  }

  public boolean valueAsBoolean() {
    if (BOOLEAN == valueType) {
      return _booleanValue;
    }
    if (STRING == valueType) return Boolean.parseBoolean(_stringValue);

    throw new JsonStreamException("Readen value is not of type boolean");
  }

  public byte[] valueAsByteArray() {
    if (STRING == valueType) return Base64.decodeFast(_stringValue);
    if (NULL == valueType) return null;
    throw new JsonStreamException("Expected a String to convert to byte array found "
      + valueType);
  }

  public Map<String, Object> metadata() {
    if (!_metadata_readen) nextObjectMetadata();
    return Collections.unmodifiableMap(_metadata);
  }

  public String metadata(String name) {
    if (!_metadata_readen) nextObjectMetadata();
    return (String) _metadata.get(name);
  }

  public Long metadataAsLong(String name) {
    if (!_metadata_readen) nextObjectMetadata();
    return (Long) _metadata.get(name);
  }

  public Boolean metadataAsBoolean(String name) {
    if (!_metadata_readen) nextObjectMetadata();
    return (Boolean) _metadata.get(name);
  }

  public ValueType getValueType() {
    return valueType;
  }

  public ObjectReader skipValue() {

    if (ARRAY == valueType || OBJECT == valueType) {
      int balance = 0;
      do {
        if (ARRAY == valueType) {
          beginArray();
          balance++;
        } else if (OBJECT == valueType) {
          beginObject();
          balance++;
        }

        while (hasNext()) {
          next();
          skipValue();
        }

        JsonType type = _ctx.peek();
        if (JsonType.ARRAY == type) {
          endArray();
          balance--;
        } else if (JsonType.OBJECT == type) {
          endObject();
          balance--;
        }
      } while (balance > 0);
    }

    return this;
  }

  public boolean hasNext() {
    int token = readNextToken(false);
    if (token == -1) return false;
    if (token < 128) {
      if (_first || _ctx.size() == 1) return _NEXT_TOKEN[token];
      else if (token == ',') return true;
    }

    return false;
  }

  public ValueType next() {
    _metadata_readen = false;
    _first = false;
    valueType = null;

    char ctoken = (char) readNextToken(false);

    if (ctoken == ',') {
      _cursor++;
      ctoken = (char) readNextToken(false);
    } else if (JsonType.ARRAY == _ctx.peek()) {
      if (ctoken == '[') {
        valueType = ARRAY;
        return valueType;
      }
      if (ctoken == '{') {
        valueType = OBJECT;
        return valueType;
      }
    }

    if (JsonType.OBJECT == _ctx.peek()) {
      currentName = consumeString(ctoken);
      if (readNextToken(true) != ':') newWrongTokenException(":", _cursor - 1);
    }

    valueType = consumeValue();
    return valueType;
  }

  @Override
  public JsonType enclosingType() {
    return _ctx.peek();
  }

  protected final ValueType consumeValue() {
    char ctoken = (char) readNextToken(false);
    if (ctoken == '"') {
      _stringValue = consumeString(ctoken);
      return valueType == CHAR && _stringValue.length() == 1 ? CHAR : STRING;
    } else if (ctoken == '[') return ARRAY;
    else if (ctoken == '{') return OBJECT;
    else return consumeLiteral();
  }

  protected final void readMetadata() {
    _metadata_readen = true;
    while (true) {
      char ctoken = (char) readNextToken(false);
      if ('"' != ctoken) return;
      ensureBufferHas(2, true);

      if ('@' == _buffer[_cursor + 1]) {
        _cursor++;
        // we cheat here...
        String key = consumeString(ctoken);

        if (readNextToken(true) != ':') newWrongTokenException(":", _cursor - 1);

        ctoken = (char) readNextToken(false);
        if (ctoken == '"') {
          String value = consumeString((char) readNextToken(false));
          _metadata.put(key, value);
        } else {
          ValueType valueType = consumeLiteral();
          if (valueType == ValueType.BOOLEAN) {
            _metadata.put(key, _booleanValue);
          } else if (valueType == ValueType.INTEGER)
            _metadata.put(key, _intValue);
        }
        if (readNextToken(false) == ',') {
          _cursor++;
        }
      } else return;
    }
  }

  protected final void begin(int character, JsonType type) {
    int token = readNextToken(true);
    // seems unecessary as it is done in readNextToken
    // checkIllegalEnd(token);
    if (character == token) {
      _ctx.push(type);
    } else newWrongTokenException("" + (char) character, _cursor - 1);
    _first = true;
  }

  protected final void end(int character, JsonType type) {
    int token = readNextToken(true);
    // seems unecessary as it is done in readNextToken
    // checkIllegalEnd(token);
    if (character == token && type == _ctx.peek()) {
      _ctx.pop();
    } else newWrongTokenException("" + (char) character, _cursor - 1);
    _first = false;
  }

  protected final String consumeString(int token) {
    if (token != '"') newMisplacedTokenException(_cursor);
    _cursor++;
    boolean buffered = false;
    while (true) {
      if (fillBuffer(true) < 0) {
        // TODO ugly to copy, by the way ensure we don't have the same problem elsewhere
        String name = new String(_stringBuffer, 0, _stringBufferTail);
        _stringBufferTail = 0;
        return name;
      }

      int i = _cursor;
      for (; i < _buflen; ) {
        if (_buffer[i] == '"') {
          if (buffered) {
            writeToStringBuffer(_buffer, _cursor, i - _cursor);
            _cursor = i + 1;
            String name = new String(_stringBuffer, 0, _stringBufferTail);
            _stringBufferTail = 0;
            return name;
          } else {
            String name = new String(_buffer, _cursor, i - _cursor);
            _cursor = i + 1;
            return name;
          }
        } else if (_buffer[i] == '\\') {
          buffered = true;
          writeToStringBuffer(_buffer, _cursor, i - _cursor);
          _cursor = i + 1;
          if (_stringBufferLength <= (_stringBufferTail + 1)) expandStringBuffer(16);
          _stringBuffer[_stringBufferTail++] = readEscaped();
          i = _cursor;
        } else i++;
      }

      buffered = true;
      writeToStringBuffer(_buffer, _cursor, i - _cursor);
      _cursor = i + 1;
    }
  }

  /**
   * Reads the next literal value into _booleanValue, _doubleValue or _intValue and returns the
   * type of the readed literal, possible values are : INTEGER, DOUBLE, BOOLEAN, NULL. When
   * calling this method the _cursor must be positioned on the first character of the value in the
   * _buffer, you can ensure that by calling {@link #readNextToken(boolean)}.
   */
  protected final ValueType consumeLiteral() {
    int token = _buffer[_cursor];

    if ((token > 47 && token < 58) || token == 45) {
      return consumeNumber();
    } else {
      ensureBufferHas(4, true);

      if ((_buffer[_cursor] == 'N' || _buffer[_cursor] == 'n')
        && (_buffer[_cursor + 1] == 'U' || _buffer[_cursor + 1] == 'u')
        && (_buffer[_cursor + 2] == 'L' || _buffer[_cursor + 2] == 'l')
        && (_buffer[_cursor + 3] == 'L' || _buffer[_cursor + 3] == 'l')) {
        _cursor += 4;
        return NULL;
      }

      if ((_buffer[_cursor] == 'T' || _buffer[_cursor] == 't')
        && (_buffer[_cursor + 1] == 'R' || _buffer[_cursor + 1] == 'r')
        && (_buffer[_cursor + 2] == 'U' || _buffer[_cursor + 2] == 'u')
        && (_buffer[_cursor + 3] == 'E' || _buffer[_cursor + 3] == 'e')) {
        _booleanValue = true;
        _cursor += 4;
        return BOOLEAN;
      }
      ensureBufferHas(5, true);

      if ((_buffer[_cursor] == 'F' || _buffer[_cursor] == 'f')
        && (_buffer[_cursor + 1] == 'A' || _buffer[_cursor + 1] == 'a')
        && (_buffer[_cursor + 2] == 'L' || _buffer[_cursor + 2] == 'l')
        && (_buffer[_cursor + 3] == 'S' || _buffer[_cursor + 3] == 's')
        && (_buffer[_cursor + 4] == 'E' || _buffer[_cursor + 4] == 'e')) {
        _booleanValue = false;
        _cursor += 5;
        return BOOLEAN;
      } else {
        throw new JsonStreamException.Builder().message(
          "Illegal character around row " + _row + " and column " + (_cursor - _col)
            + " awaited for literal (number, boolean or null) but read '"
            + _buffer[_cursor] + "'!").create();
      }
    }
  }

  private ValueType consumeNumber() {
    // lets fill the buffer and handle differently overflowing values
    if ((_buflen - _cursor) < 378) ensureBufferHas(_buflen, false);

    int begin = _cursor;
    int cur;
    boolean negative;
    // check the sign
    if (_buffer[_cursor] == 45) {
      negative = true;
      _cursor++;
      cur = _cursor;
    } else {
      negative = false;
      cur = _cursor;
    }
    // just to handle invalid leading 0000
    for (; cur < _buflen && _buffer[cur] == 48; cur++) ;
    // Careful we consume the '-' here, but also all the leading 0, even if it is of form 0.xxx
    _cursor = cur;

    int len = Math.min(_buflen, cur + 18);
    int token;

    long longValue = 0;
    for (; cur < len; cur++) {
      token = _buffer[cur];
      if (token < 48 || token > 57) {
        break;
      }
      longValue = 10L * longValue + (token - 48);
    }

    if (cur < _buflen) {
      // read the maximum we can to fill the long capacity, at max we can read 1 additional
      // digit
      token = _buffer[cur];
      if (token > 47 && token < 58) {
        long newLongValue = 10L * longValue + (token - 48);
        if (newLongValue > longValue) {
          longValue = newLongValue;
          cur++;
        }
        /* TODO we parse here Long.MIN_VALUE as a double, we had also pb in the writer
        * the solution => instead of doing operations on positive numbers, do the inverse, use negative numbers
        * as the min negative long holds the - max positive long.
        */
        // else we exceed long capacity, just continue and parse it as a double
      }

      if (cur < _buflen
        && ((token = _buffer[cur]) == 46 || token == 101 || token == 69 || (token > 47 && token < 58))) {

        if (strictDoubleParse) {
          _cursor = begin;
          return consumeStrictNumber(cur);
        } else return consumeDouble(cur, longValue, negative);
      }
    }

    _intValue = negative ? -longValue : longValue;
    _numberLen = cur - _cursor;
    _cursor = cur;
    return INTEGER;
  }

  // a custom implementation based on fast path principles
  private ValueType consumeDouble(int cur, long longValue, boolean negative) {
    int token;

    // TODO a verifier : on ne veut compter dans les intDigit que ce qui n'est pas pris dans le
    // longValue, idem pour les decimales??
    int intDigits = cur - _cursor;
    int valueDigits = longValue > 0 ? cur - _cursor : 0;

    // ok we have readen as many characters as a long can contain
    // the next readen characters will serve for the digit count for large integer numbers
    if (intDigits > 17) {
      for (; cur < _buflen; cur++) {
        if (_buffer[cur] < 48 || _buffer[cur] > 57) {
          break;
        }
      }
      // only if we advanced
      if (intDigits != (cur - _cursor)) intDigits = (cur - _cursor) - intDigits;
    } else
      // TODO check ça m'a l'air bizarre comme cas...
      intDigits = 0;

    int decimalDigits = 0;

    // next possible case is a dot
    if (cur < _buflen && _buffer[cur] == 46) {
      cur++;
      int start = cur;
      // if integer part value is zero we could use scientific notation and win in precision
      if (longValue == 0) {
        // reset the counter as we don't care of the leading zeros
        intDigits = 0;
        // now lets try to read as many consecutive zeros as available
        for (; cur < _buflen && _buffer[cur] == 48; cur++) ;
      }
      // ok now we must read again into the longValue
      int len = Math.min(_buflen, cur + (18 - valueDigits));

      for (; cur < len; cur++) {
        token = _buffer[cur];
        if (token < 48 || token > 57) {
          break;
        }
        longValue = 10L * longValue + (token - 48);
      }
      decimalDigits = cur - start;

      start = cur;
      // no need to count digits after the precision we support for decimals
      // continue reading digits and just ignore the values, we will truncate
      for (; cur < _buflen; cur++) {
        if (_buffer[cur] < 48 || _buffer[cur] > 57) {
          break;
        }
      }
    }

    // now try to read exponent E/e
    if ((cur + 1) < _buflen && (_buffer[cur] == 101 || _buffer[cur] == 69)) {
      token = _buffer[++cur];
      boolean negativeExp;
      // check the sign
      if (token == 45) {
        negativeExp = true;
        cur++;
      } else {
        if (token == 43) cur++;
        negativeExp = false;
      }
      // read the power of ten
      int powValue = 0;
      for (; cur < _buflen; cur++) {
        token = _buffer[cur];
        if (token < 48 || token > 57) {
          break;
        }
        powValue = 10 * powValue + (token - 48);
      }

      // depending on the sign put it in the decimal digit counter or integer digit counter
      if (negativeExp) decimalDigits += powValue;
      else intDigits += powValue;
    }

    // and now make the difference so it balances well
    decimalDigits = intDigits - decimalDigits;

    if (decimalDigits < 0) {
            /*
             * This allows to handle also Double.MIN_VALUE and Double.MIN_NORMAL as
             * Double.MIN_NORMAL has 16 decimal digits, 308+16=324, but 10^324 overflows the double
             * capacity, its Infinity. So we use this little trick.
             */
      if (decimalDigits < -308) {
        if (decimalDigits < -325) {
          _doubleValue = 0;
        } else {
          _doubleValue = longValue / _POWS[-decimalDigits - 308];
          _doubleValue = _doubleValue / _POWS[308];
        }
      } else {
        // better precision than multiplication
        _doubleValue = longValue / _POWS[-decimalDigits];
      }
    } else {
      if (decimalDigits > 308) {
        _doubleValue = Double.POSITIVE_INFINITY;
      } else {
        // we don't need to apply same technique as before as
        // 308-16=292 so it's okay :)
        _doubleValue = longValue * _POWS[decimalDigits];
      }
    }

    _doubleValue = negative ? -_doubleValue : _doubleValue;
    _numberLen = cur - _cursor;
    _cursor = cur;
    return DOUBLE;
  }

  private final ValueType consumeStrictNumber(int localCursor) {
    if (localCursor < _buflen) {
      // consider all the remaining integer values as part of the double
      for (; localCursor < _buflen; localCursor++) {
        if (_buffer[localCursor] < 48 || _buffer[localCursor] > 57) {
          break;
        }
      }
    }

    if (localCursor < _buflen) {
      if (_buffer[localCursor] == '.') {
        localCursor = advanceWhileNumeric(++localCursor);
      }
    }

    if (localCursor + 1 < _buflen) {
      char ctoken = _buffer[localCursor];
      if (ctoken == 'e' || ctoken == 'E') {
        ctoken = _buffer[++localCursor];
        if (ctoken == '-' || ctoken == '+' || (ctoken > 47 && ctoken < 58)) {
          localCursor = advanceWhileNumeric(++localCursor);
        } else newWrongTokenException("'-' or '+' or '' (same as +)");
      }
    }

    // it may overflow of the max numeric value we accept to read, it should
    if (localCursor >= _buflen) {

    }

    _numberLen = localCursor - _cursor;
    _stringValue = new String(_buffer, _cursor, _numberLen);
    _doubleValue = Double.parseDouble(_stringValue);
    _cursor = localCursor;
    return DOUBLE;
  }

  private int advanceWhileNumeric(int cursor) {
    for (; cursor < _buflen; cursor++) {
      if ((_buffer[cursor] < 48 || _buffer[cursor] > 57)) {
        return cursor;
      }
    }
    return cursor;
  }

  protected final int readNextToken(boolean consume) {
    while (true) {
      if (_cursor >= _buflen) fillBuffer(true);

      for (; _cursor < _buflen; _cursor++) {
        int token = _buffer[_cursor];
        if (token < 128 && SKIPPED_TOKENS[token] == 0) {
          if (token == '/') {
            ensureBufferHas(2, true);
            if (_buffer[_cursor + 1] == '*') {
              _cursor += 2;
              advanceAfter(_END_OF_BLOCK_COMMENT);
            } else if (_buffer[_cursor + 1] == '/') {
              _cursor += 2;
              advanceAfter(_END_OF_LINE);
              _row++;
              _col = _cursor;
            } else newWrongTokenException("start comment // or /*", _cursor);
            // don't consume the token
            _cursor--;
          } else if (consume) {
            return _buffer[_cursor++];
          } else return token;
        } else if (_buffer[_cursor] == '\n') {
          _row++;
          _col = _cursor;
        }
      }

      if (_buflen == -1) break;
    }

    return _cursor < _buflen ? _buffer[_cursor] : -1;
  }

  private final void advanceAfter(char[] str) {
    int strPos = 0;
    while (true) {
      if (_cursor >= _buflen) fillBuffer(true);

      for (; _cursor < _buflen && strPos < str.length; _cursor++) {
        if (_buffer[_cursor] == str[strPos]) {
          strPos++;
        } else strPos = 0;
      }

      if (strPos == str.length) {
        return;
      }
      if (_buflen == -1) break;
    }
  }

  protected final char readEscaped() {
    fillBuffer(true);

    char token = _buffer[_cursor++];
    switch (token) {
      case 'b':
        return '\b';
      case 't':
        return '\t';
      case 'n':
        return '\n';
      case 'f':
        return '\f';
      case 'r':
        return '\r';
      case '"':
      case '/':
      case '\\':
        return token;

      case 'u':
        valueType = CHAR;
        break;

      default:
        newMisplacedTokenException(_cursor - 1);
    }

    int value = 0;
    if (ensureBufferHas(4, false) < 0) {
      throw new JsonStreamException("Expected 4 hex-digit for character escape sequence!");
      // System.arraycopy(buffer, cursor, buffer, 0, buflen-cursor);
      // buflen = buflen - cursor + reader.read(buffer, buflen-cursor, cursor);
      // cursor = 0;
    }
    for (int i = 0; i < 4; ++i) {
      int ch = _buffer[_cursor++];
      int digit = (ch > 127) ? -1 : sHexValues[ch];
      if (digit < 0) {
        throw new JsonStreamException("Wrong character '" + ch
          + "' expected a hex-digit for character escape sequence!");
      }
      value = (value << 4) | digit;
    }

    return (char) value;
  }

  private final void writeToStringBuffer(final char[] data, final int offset, final int length) {
    if (_stringBufferLength <= (_stringBufferTail + length)) {
      expandStringBuffer(length);
    }
    System.arraycopy(data, offset, _stringBuffer, _stringBufferTail, length);
    _stringBufferTail += length;
  }

  private final void expandStringBuffer(int length) {
    char[] extendedStringBuffer = new char[_stringBufferLength * 2 + length];
    System.arraycopy(_stringBuffer, 0, extendedStringBuffer, 0, _stringBufferTail);
    _stringBuffer = extendedStringBuffer;
    _stringBufferLength = extendedStringBuffer.length;
  }

  private final int fillBuffer(boolean doThrow) {
    if (_cursor < _buflen) return _buflen;
    try {
      _buflen = reader.read(_buffer);
    } catch (IOException ioe) {
      throw new JsonStreamException(ioe);
    }
    checkIllegalEnd(_buflen);
    _cursor = 0;
    _col = 0;
    return _buflen;
  }

  private final int ensureBufferHas(int minLength, boolean doThrow) {
    try {
      int actualLen = _buflen - _cursor;
      if (actualLen >= minLength) {
        return actualLen;
      }

      System.arraycopy(_buffer, _cursor, _buffer, 0, actualLen);
      for (; actualLen < minLength; ) {
        int len = reader.read(_buffer, actualLen, _buffer.length - actualLen);
        if (len < 0) {
          if (doThrow) throw new JsonStreamException(
            "Encountered end of stream, incomplete json!");
          else {
            _buflen = actualLen;
            _col = 0;
            _cursor = 0;
            return len;
          }
        }
        actualLen += len;
      }
      _buflen = actualLen;
      _col = 0;
      _cursor = 0;
      return actualLen;
    } catch (IOException ioe) {
      throw new JsonStreamException(ioe);
    }
  }

  protected final boolean isEOF() {
    return _buflen < 0 || fillBuffer(false) < 0;
  }

  private final void newWrongTokenException(String awaited) {
    newWrongTokenException(awaited, _cursor);
  }

  public int column() {
    int col = _cursor - _col;
    return col < 0 ? 0 : col;
  }

  public int row() {
    return _row;
  }

  private final void newWrongTokenException(String awaited, int cursor) {
    // otherwise it fails when an error occurs on first character
    if (cursor < 0) cursor = 0;
    int pos = cursor - _col;
    if (pos < 0) pos = 0;

    if (_buflen < 0) throw new JsonStreamException(
      "Incomplete data or malformed json : encoutered end of stream but expected "
        + awaited).niceTrace();
    else throw new JsonStreamException.Builder()
      .message(
        "Illegal character at row " + _row + " and column " + pos + " expected "
          + awaited + " but read '" + _buffer[cursor] + "' !")
      .locate(_row, pos).create().niceTrace();
  }

  private final void newMisplacedTokenException(int cursor) {
    if (_buflen < 0)
      throw JsonStreamException.niceTrace(new JsonStreamException(
        "Incomplete data or malformed json : encoutered end of stream."));

    if (cursor < 0) cursor = 0;
    int pos = cursor - _col;
    if (pos < 0) pos = 0;

    throw new JsonStreamException.Builder()
      .message(
        "Encountred misplaced character '" + _buffer[cursor] + "' around row "
          + _row + " and column " + pos).locate(_row, pos).create().niceTrace();
  }

  private final void checkIllegalEnd(int token) {
    if (token == -1 && JsonType.EMPTY != _ctx.peek())
      throw new JsonStreamException(
        "Incomplete data or malformed json : encoutered end of stream!").niceTrace();
  }

  private void throwNumberFormatException(String expected, String encoutered) {
    int pos = _cursor - _col - _numberLen;
    throw JsonStreamException.niceTrace(new NumberFormatException("Wrong numeric type at row " + _row + " and column " + pos
      + ", expected " + expected + " but encoutered " + encoutered));
  }

}
