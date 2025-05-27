package algorithms.BDeXOR;

import algorithms.Algorithm;
import algorithms.BDeXOR.decoder.DoubleBDeXORDecoder;
import algorithms.BDeXOR.encoder.DoubleBDeXOREncoder;
import algorithms.Decoder;
import algorithms.Encoder;
import enums.DataTypeEnums;

public class BDeXOR extends Algorithm {
    public BDeXOR(){
        // Encoder
        EncoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleBDeXOREncoder.class);
        // Decoder
        DecoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleBDeXORDecoder.class);
    }

    protected Encoder getEncoder(String data_type, String output_path) throws Exception {
        return super.getEncoder(data_type, output_path);
    }

    protected Decoder getDecoder(String data_type, String input_path) throws Exception {
        return super.getDecoder(data_type, input_path);
    }
}
