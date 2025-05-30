package algorithms.FutureWork.DPF;

import algorithms.Algorithm;
import algorithms.FutureWork.DPF.decoder.DoubleDPFDecoder;
import algorithms.FutureWork.DPF.encoder.DoubleDPFEncoder;
import algorithms.Decoder;
import algorithms.Encoder;
import enums.DataTypeEnums;

// todo
public class DPF extends Algorithm {
    public DPF(){
        // Encoder
        EncoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleDPFEncoder.class);
        // Decoder
        DecoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleDPFDecoder.class);
    }

    protected Encoder getEncoder(String data_type, String output_path) throws Exception {
        return super.getEncoder(data_type, output_path);
    }

    protected Decoder getDecoder(String data_type, String input_path) throws Exception {
        return super.getDecoder(data_type, input_path);
    }
}
