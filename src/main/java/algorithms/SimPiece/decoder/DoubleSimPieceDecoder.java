package algorithms.SimPiece.decoder;

import algorithms.Decoder;
import algorithms.SimPiece.Point;
import algorithms.SimPiece.SimPieceCore;
import utils.StreamReader;

import java.util.List;

public class DoubleSimPieceDecoder extends Decoder {
    private List<Point> decompressedPoints;
    private int currentIndex = 0;
    private boolean initialized = false;

    public DoubleSimPieceDecoder(String inputPath) {
        super(inputPath);
    }

    public DoubleSimPieceDecoder(String inputPath, String config) {
        super(inputPath, config);
    }

    private void initialize() {
        if (initialized) {
            return;
        }
        try {
            byte[] compressed = in.readAllBytes();
            decompressedPoints = SimPieceCore.decompress(compressed);
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
            decompressedPoints = new java.util.ArrayList<>();
            initialized = true;
        }
    }

    @Override
    public double decodeDouble() {
        initialize();
        
        if (currentIndex >= decompressedPoints.size()) {
            return 0;
        }
        
        double value = decompressedPoints.get(currentIndex).getValue();
        currentIndex++;
        return value;
    }
}
