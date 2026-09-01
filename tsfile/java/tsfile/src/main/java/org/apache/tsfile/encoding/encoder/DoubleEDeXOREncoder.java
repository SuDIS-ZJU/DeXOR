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
package org.apache.tsfile.encoding.encoder;

import org.apache.tsfile.encoding.DeXORTools;
import org.apache.tsfile.file.metadata.enums.TSEncoding;

import java.io.ByteArrayOutputStream;

/** Error-bounded DeXOR using the preceding reconstructed value as its predictor. */
public class DoubleEDeXOREncoder extends GorillaEncoderV2 {
  private static final double ENDING = 998274353.66;
  private static final int MAX_DELTA = 16;
  private static final int DELTA_BITS = 4;
  private static final int Q_BITS = 4;

  private final int decimalPlaces;
  private final int epsilon;
  private boolean headerWritten;
  private double previousValue;
  private int previousQ;
  private int previousO;
  private int previousDelta;

  public DoubleEDeXOREncoder() {
    this(2);
  }

  public DoubleEDeXOREncoder(int decimalPlaces) {
    if (decimalPlaces < 0 || decimalPlaces > 15) {
      throw new IllegalArgumentException("decimalPlaces must be between 0 and 15");
    }
    this.decimalPlaces = decimalPlaces;
    this.epsilon = -decimalPlaces;
    setType(TSEncoding.EDEXOR);
  }

  @Override
  public int getOneItemMaxSize() {
    return 12;
  }

  @Override
  public void encode(double value, ByteArrayOutputStream out) {
    if (!headerWritten) {
      writeBits(decimalPlaces, 4, out);
      headerWritten = true;
    }
    encodeValue(value, out);
  }

  private void encodeValue(double value, ByteArrayOutputStream out) {
    int q = DeXORTools.getEndWithEpsilon(value, epsilon);
    int o = Math.max(epsilon, q);
    int delta = 0;
    double alpha = 0;
    while (delta < MAX_DELTA) {
      double prefixPow = DeXORTools.getP10(o);
      long currentPrefix = DeXORTools.truncate(value / prefixPow);
      long predictedPrefix = DeXORTools.truncate(previousValue / prefixPow);
      if (currentPrefix == predictedPrefix) {
        alpha = currentPrefix * prefixPow;
        break;
      }
      delta++;
      o++;
    }
    if (delta >= MAX_DELTA) {
      throw new IllegalArgumentException("EDeXOR value exceeds the 4-bit decimal-prefix range: " + value);
    }

    double residual = value - alpha;
    long beta;
    double betaStar;
    if (q <= epsilon) {
      writeBit(out);
      double pow = DeXORTools.getP10(epsilon);
      beta = DeXORTools.truncate(residual / pow);
      betaStar = beta * pow;
      delta = o - epsilon;
      if (o == previousO) {
        writeBit(out);
      } else {
        skipBit(out);
        previousO = o;
        writeBits(delta, DELTA_BITS, out);
      }
    } else {
      skipBit(out);
      double pow = DeXORTools.getP10(q);
      beta = DeXORTools.truncate(residual / pow);
      betaStar = beta * pow;
      delta = o - q;
      if (q == previousQ) {
        writeBit(out);
      } else {
        skipBit(out);
        int encodedQ = q - epsilon - 1;
        if (encodedQ < 0 || encodedQ >= (1 << Q_BITS)) {
          throw new IllegalArgumentException("EDeXOR q exceeds the 4-bit range: " + q);
        }
        previousQ = q;
        writeBits(encodedQ, Q_BITS, out);
      }
      if (delta == previousDelta) {
        writeBit(out);
      } else {
        skipBit(out);
        previousDelta = delta;
        writeBits(delta, DELTA_BITS, out);
      }
    }

    long magnitude = beta == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(beta);
    int betaBits = DeXORTools.decimalBits(delta);
    if ((magnitude >> betaBits) != 0) {
      throw new IllegalArgumentException("EDeXOR beta exceeds its payload width: " + value);
    }
    if (DeXORTools.comp(alpha, 0) == 0) {
      if (value > 0) {
        writeBit(out);
      } else {
        skipBit(out);
      }
    }
    writeBits(magnitude, betaBits, out);
    previousValue = alpha + betaStar;
  }

  @Override
  public void flush(ByteArrayOutputStream out) {
    encode(ENDING, out);
    bitsLeft = 0;
    flipByte(out);
    reset();
  }

  @Override
  protected void reset() {
    super.reset();
    headerWritten = false;
    previousValue = 0;
    previousQ = 0;
    previousO = 0;
    previousDelta = 0;
  }
}
