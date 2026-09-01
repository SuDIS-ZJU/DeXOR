package algorithms.SimPiece;

import algorithms.Algorithm;
import algorithms.Decoder;
import algorithms.Encoder;
import algorithms.SimPiece.decoder.DoubleSimPieceDecoder;
import algorithms.SimPiece.encoder.DoubleSimPieceEncoder;
import enums.DataTypeEnums;

public class SimPiece extends Algorithm {
    public SimPiece(){
        EncoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleSimPieceEncoder.class);
        DecoderClassMap.put(DataTypeEnums.DOUBLE.getType(), DoubleSimPieceDecoder.class);
    }

    protected Encoder getEncoder(String data_type, String output_path) throws Exception {
        return super.getEncoder(data_type, output_path);
    }

    protected Decoder getDecoder(String data_type, String input_path) throws Exception {
        return super.getDecoder(data_type, input_path);
    }
}
