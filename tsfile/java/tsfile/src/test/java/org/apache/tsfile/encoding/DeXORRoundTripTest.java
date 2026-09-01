/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tsfile.encoding;

import org.apache.tsfile.encoding.decoder.DoubleDeXORDecoder;
import org.apache.tsfile.encoding.decoder.DoubleEDeXORDecoder;
import org.apache.tsfile.encoding.encoder.DoubleDeXOREncoder;
import org.apache.tsfile.encoding.encoder.DoubleEDeXOREncoder;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeXORRoundTripTest {

  @Test
  public void losslessDecoderPreservesRawBitsIncludingExceptions() {
    double[] values = {
      0.0,
      -0.0,
      0.1,
      -123.45678901234567,
      1.2345678901234567E20,
      4.9E-324,
      3.141592653589793,
      -987654321.125
    };
    DoubleDeXOREncoder encoder = new DoubleDeXOREncoder();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    for (double value : values) {
      try {
        encoder.encode(value, output);
      } catch (RuntimeException error) {
        throw new AssertionError("Failed to encode " + value, error);
      }
    }
    encoder.flush(output);

    DoubleDeXORDecoder decoder = new DoubleDeXORDecoder();
    ByteBuffer input = ByteBuffer.wrap(output.toByteArray());
    int index = 0;
    while (decoder.hasNext(input)) {
      double decoded = decoder.readDouble(input);
      assertEquals(
          Double.doubleToRawLongBits(values[index]), Double.doubleToRawLongBits(decoded));
      index++;
    }
    assertEquals(values.length, index);
  }

  @Test
  public void errorBoundedDecoderStaysWithinTwoDecimalPlaces() {
    double[] values = {0.0, -0.0, 0.1, -123.4567, 3.1415926, 99999.9999, -42.4242};
    DoubleEDeXOREncoder encoder = new DoubleEDeXOREncoder(2);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    for (double value : values) {
      encoder.encode(value, output);
    }
    encoder.flush(output);

    DoubleEDeXORDecoder decoder = new DoubleEDeXORDecoder();
    ByteBuffer input = ByteBuffer.wrap(output.toByteArray());
    int index = 0;
    while (decoder.hasNext(input)) {
      double decoded = decoder.readDouble(input);
      assertTrue(Math.abs(values[index] - decoded) < 0.01 + 1E-12);
      index++;
    }
    assertEquals(values.length, index);
  }
}
