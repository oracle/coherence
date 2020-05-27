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

package com.oracle.coherence.io.json.genson;


import java.io.*;

/**
 * This is an internal class that might evolve in the future into a JsonReader Factory and be moved
 * to the stream package.
 */
public final class EncodingAwareReaderFactory {

  static enum UTFEncoding {
    UTF_32BE(4), UTF_32LE(4), UTF_16BE(2), UTF_16LE(2), UTF_8(1), UNKNOWN(-1);

    final int bytes;

    private UTFEncoding(int bytes) {
      this.bytes = bytes;
    }

    public String encoding() {
      return name().replace('_', '-');
    }
  }

  /**
   * Creates java.io.Reader instances with detected encoding from the input stream
   * using BOM if present or JSON spec.
   *
   * Some links:
   * http://www.herongyang.com/Unicode/
   * http://www.ietf.org/rfc/rfc4627.txt
   *
   * @throws IOException
   * @throws UnsupportedEncodingException
   */
  public Reader createReader(InputStream is) throws IOException {
    byte[] bytes = new byte[4];
    int len = fetchBytes(bytes, is);

    if (len < 1) return new InputStreamReader(is);

    // read first 4 bytes if available
    int bits_32 = (bytes[0] & 0xFF) << 24
      | (bytes[1] & 0xFF) << 16
      | (bytes[2] & 0xFF) << 8
      | (bytes[3] & 0xFF);

    UTFEncoding encoding = UTFEncoding.UNKNOWN;
    boolean hasBOM = false;

    // try to detect the encoding from those 4 bytes if BOM is used
    if (len == 4) encoding = detectEncodingFromBOM(bits_32);

    // no BOM then fall back to JSON spec
    if (encoding == UTFEncoding.UNKNOWN) {
      encoding = detectEncodingUsingJSONSpec(bits_32);
    } else hasBOM = true;

    // should not happen as we default to UTF-8
    if (encoding == UTFEncoding.UNKNOWN) {
      throw new UnsupportedEncodingException("The encoding could not be detected from the stream.");
    }

    int usedBOMBytes = hasBOM ? len - (4 - encoding.bytes) : 0;
    int bytesToUnread = len - usedBOMBytes;

    // small optimization to avoid encapsulation when there is nothing to unread
    if (bytesToUnread == 0) {
      return new InputStreamReader(is, encoding.encoding());
    } else {
      PushbackInputStream pis = new PushbackInputStream(is, bytesToUnread);
      pis.unread(bytes, usedBOMBytes, bytesToUnread);
      return new InputStreamReader(pis, encoding.encoding());
    }
  }

  private UTFEncoding detectEncodingFromBOM(int bits_32) {
    int bits_16  = bits_32 >>> 16;

    if (bits_32 == 0x0000FEFF) return UTFEncoding.UTF_32BE;
    else if (bits_32 == 0xFFFE0000) return UTFEncoding.UTF_32LE;
    else if (bits_16 == 0xFEFF) return UTFEncoding.UTF_16BE;
    else if (bits_16 == 0xFFFE) return UTFEncoding.UTF_16LE;
    else if (bits_32 >>> 8 == 0xEFBBBF) return UTFEncoding.UTF_8;
    else return UTFEncoding.UNKNOWN;
  }

  private UTFEncoding detectEncodingUsingJSONSpec(int bits_32) {
    int bits_16  = bits_32 >>> 16;

    if (bits_32 >>> 8 == 0) return UTFEncoding.UTF_32BE;
    else if ((bits_32 & 0x00FFFFFF) == 0) return UTFEncoding.UTF_32LE;
    else if ((bits_16 & 0xFF00) == 0) return UTFEncoding.UTF_16BE;
    else if ((bits_16 & 0x00FF) == 0) return UTFEncoding.UTF_16LE;
    else return UTFEncoding.UTF_8;
  }

  private int fetchBytes(byte[] bytes, InputStream is) throws IOException {
    int start = 0;
    int bytesRead;

    while(start < bytes.length-1 && (bytesRead = is.read(bytes, start, bytes.length-start)) > -1) {
      start += bytesRead;
    }

    return start;
  }
}
