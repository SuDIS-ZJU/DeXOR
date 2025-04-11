package algorithms.ATDP;

import algorithms.Algorithm;
import algorithms.Decoder;
import algorithms.Encoder;
import algorithms.ATDP.decoder.DoubleATDPDecoder;
import algorithms.ATDP.encoder.DoubleATDPEncoder;
import enums.DataTypeEnums;

public class ATDP extends Algorithm {
    public ATDP(){
        // Encoder
        EncoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleATDPEncoder.class);
        // Decoder
        DecoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleATDPDecoder.class);
    }

    protected Encoder getEncoder(String data_type, String output_path) throws Exception {
        return super.getEncoder(data_type, output_path);
    }

    protected Decoder getDecoder(String data_type, String input_path) throws Exception {
        return super.getDecoder(data_type, input_path);
    }
}
