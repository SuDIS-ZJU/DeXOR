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
package org.apache.tsfile.encoding.decoder;

import org.apache.tsfile.encoding.DeXORTools;
import org.apache.tsfile.file.metadata.enums.TSEncoding;

import java.nio.ByteBuffer;

/** Decoder counterpart of {@link org.apache.tsfile.encoding.encoder.DoubleEDeXOREncoder}. */
public class DoubleEDeXORDecoder extends GorillaDecoderV2 {
  private static final double ENDING = 998274353.66;
  private int epsilon;
  private double previousValue;
  private double cachedValue;
  private int previousQ;
  private int previousO;
  private int previousDelta;

  public DoubleEDeXORDecoder() {
    setType(TSEncoding.EDEXOR);
  }

  @Override
  public double readDouble(ByteBuffer in) {
    if (!firstValueWasRead) {
      flipByte(in);
      epsilon = -(int) readLong(4, in);
      firstValueWasRead = true;
      cachedValue = cacheNext(in);
    }
    double result = cachedValue;
    if (hasNext) {
      cachedValue = cacheNext(in);
    }
    return result;
  }

  private double cacheNext(ByteBuffer in) {
    double value = readNextValue(in);
    if (Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(ENDING)) {
      hasNext = false;
    }
    return value;
  }

  private double readNextValue(ByteBuffer in) {
    int q;
    int o;
    int delta;
    double alpha;
    if (readBit(in)) {
      q = epsilon;
      if (readBit(in)) {
        o = previousO;
        delta = o - q;
      } else {
        delta = (int) readLong(4, in);
        o = q + delta;
      }
      double pow = DeXORTools.getP10(o);
      alpha = DeXORTools.truncate(previousValue / pow) * pow;
      previousO = o;
    } else {
      if (readBit(in)) {
        q = previousQ;
      } else {
        q = (int) readLong(4, in) + epsilon + 1;
      }
      if (readBit(in)) {
        delta = previousDelta;
      } else {
        delta = (int) readLong(4, in);
      }
      o = q + delta;
      double pow = DeXORTools.getP10(o);
      alpha = DeXORTools.truncate(previousValue / pow) * pow;
      previousQ = q;
      previousDelta = delta;
    }

    long sign = alpha > 0 ? 1 : -1;
    if (DeXORTools.comp(alpha, 0) == 0) {
      sign = readBit(in) ? 1 : -1;
    }
    long beta = sign * readLong(DeXORTools.decimalBits(delta), in);
    previousValue = alpha + beta * DeXORTools.getP10(q);
    return previousValue;
  }

  @Override
  public void reset() {
    super.reset();
    epsilon = 0;
    previousValue = 0;
    cachedValue = 0;
    previousQ = 0;
    previousO = 0;
    previousDelta = 0;
  }
}
