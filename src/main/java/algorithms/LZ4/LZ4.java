package algorithms.LZ4;

import algorithms.Algorithm;
import algorithms.Decoder;
import algorithms.Encoder;
import algorithms.LZ4.decoder.DoubleLZ4Decoder;
import algorithms.LZ4.encoder.DoubleLZ4Encoder;
import enums.DataTypeEnums;

public class LZ4 extends Algorithm {
    public LZ4(){
        EncoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleLZ4Encoder.class);
        DecoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleLZ4Decoder.class);
    }

    protected Encoder getEncoder(String data_type, String output_path) throws Exception {
        return super.getEncoder(data_type, output_path);
    }

    protected Decoder getDecoder(String data_type, String input_path) throws Exception {
        return super.getDecoder(data_type, input_path);
    }
}
