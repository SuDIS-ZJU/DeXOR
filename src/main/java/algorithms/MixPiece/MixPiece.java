package algorithms.MixPiece;

import algorithms.Algorithm;
import algorithms.Decoder;
import algorithms.Encoder;
import algorithms.MixPiece.decoder.DoubleMixPieceDecoder;
import algorithms.MixPiece.encoder.DoubleMixPieceEncoder;
import enums.DataTypeEnums;

public class MixPiece extends Algorithm {
    public MixPiece(){
        EncoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleMixPieceEncoder.class);
        DecoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleMixPieceDecoder.class);
    }

    protected Encoder getEncoder(String data_type, String output_path) throws Exception {
        return super.getEncoder(data_type, output_path);
    }

    protected Decoder getDecoder(String data_type, String input_path) throws Exception {
        return super.getDecoder(data_type, input_path);
    }
}
