package algorithms.Axe;

import algorithms.Algorithm;
import algorithms.Decoder;
import algorithms.Encoder;
import algorithms.Axe.decoder.DoubleAxeDecoder;
import algorithms.Axe.encoder.DoubleAxeEncoder;
import enums.DataTypeEnums;

public class Axe extends Algorithm {
    public Axe(){
        EncoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleAxeEncoder.class);
        DecoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleAxeDecoder.class);
    }

    protected Encoder getEncoder(String data_type, String output_path) throws Exception {
        return super.getEncoder(data_type, output_path);
    }

    protected Decoder getDecoder(String data_type, String input_path) throws Exception {
        return super.getDecoder(data_type, input_path);
    }
}