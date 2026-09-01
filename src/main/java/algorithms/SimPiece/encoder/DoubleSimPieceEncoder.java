package algorithms.SimPiece.encoder;

import algorithms.Encoder;
import algorithms.SimPiece.Point;
import algorithms.SimPiece.SimPieceCore;
import utils.StreamWriter;

import java.util.ArrayList;
import java.util.List;

public class DoubleSimPieceEncoder extends Encoder {
    private List<Point> points;
    private boolean closed = false;
    private int bitCount = 0;

    public DoubleSimPieceEncoder(String outputPath) {
        super(outputPath);
        this.points = new ArrayList<>();
    }

    public DoubleSimPieceEncoder(String outputPath, String config) {
        super(outputPath, config);
        this.points = new ArrayList<>();
    }

    @Override
    public int encode(double value) {
        if (closed) {
            return bitCount;
        }
        long timestamp = points.size();
        points.add(new Point(timestamp, value));
        return bitCount;
    }

    @Override
    public int close() {
        if (closed) {
            return bitCount;
        }
        closed = true;
        
        if (points.isEmpty()) {
            return 0;
        }

        try {
            int decimalPlaces = -1;
            if (config.containsKey("decimal_places")) {
                decimalPlaces = Integer.parseInt(config.get("decimal_places"));
            }
            if (decimalPlaces < 0) {
                String failedDp = config.get("failed_decimal_places");
                decimalPlaces = (failedDp != null) ? Integer.parseInt(failedDp) : 2;
            }
            double epsilon = Math.pow(10, -decimalPlaces);

            byte[] compressed = SimPieceCore.compress(points, epsilon);
            out.writeBytes(compressed);
            bitCount = out.track_bits();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return bitCount;
    }

    @Override
    public void flush() {
        if (!closed) {
            close();
        }
        super.flush();
    }
}
