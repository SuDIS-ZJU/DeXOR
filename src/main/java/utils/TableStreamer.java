package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TableStreamer {
    private String path = "";
    private BufferedReader reader;
    private String[] currentRowValues;
    private String[] columnNames;
    private int columnCount;

    private Serie[] series;
    private boolean[] numericColumns;
    private String[] detectRecords;


    public TableStreamer(String path) throws Exception {
        this.path = path;
        this.reload(path);
    }

    public void reload(String path) throws Exception {
        this.path = path;
        reader = new BufferedReader(new FileReader(path));
        
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new Exception("Empty CSV file");
        }
        columnNames = parseLine(headerLine);
        columnCount = columnNames.length;
        
        series = new Serie[columnCount];
        numericColumns = new boolean[columnCount];
        
        for (int i = 0; i < columnCount; i++) {
            series[i] = new Serie(columnNames[i]);
        }
        
        detectNumericColumns();
        
        reader.close();
        
        reader = new BufferedReader(new FileReader(path));
        reader.readLine();
        
        readNextRow();
    }
    
    private String[] parseLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        
        return tokens.toArray(new String[0]);
    }
    
    private void detectNumericColumns() throws IOException {
        for (int i = 0; i < columnCount; i++) {
            if (i == 0) {
                numericColumns[i] = false;
                continue;
            }
            String colName = columnNames[i].toLowerCase();
            if (colName.contains("timestamp") || colName.contains("time") || colName.contains("date")) {
                numericColumns[i] = false;
                continue;
            }
            numericColumns[i] = true;
        }
        
        detectRecords = new String[101];
        for (int i = 0; i < 100; i++) {
            String line = reader.readLine();
            if (line == null) break;
            detectRecords[i] = line;
        }
        
        for (int row = 0; row < detectRecords.length && detectRecords[row] != null; row++) {
            String[] values = parseLine(detectRecords[row]);
            for (int i = 0; i < columnCount; i++) {
                if (numericColumns[i] && i < values.length && !isNumeric(values[i])) {
                    numericColumns[i] = false;
                }
            }
        }
    }
    
    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        try {
            Double.parseDouble(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public boolean isNumericColumn(int colIndex) {
        if (colIndex >= 0 && colIndex < numericColumns.length) {
            return numericColumns[colIndex];
        }
        return false;
    }
    
    public int[] getNumericColumnIndices() {
        int count = 0;
        for (boolean isNumeric : numericColumns) {
            if (isNumeric) count++;
        }
        int[] indices = new int[count];
        int idx = 0;
        for (int i = 0; i < numericColumns.length; i++) {
            if (numericColumns[i]) {
                indices[idx++] = i;
            }
        }
        return indices;
    }

    private void readNextRow() throws Exception {
        String line = reader.readLine();
        if (line == null) {
            throw new Exception("EOF");
        }
        currentRowValues = parseLine(line);
    }
    
    public void next() throws Exception {
        readNextRow();
    }
    
    public void reset() throws Exception {
        reader.close();
        reader = new BufferedReader(new FileReader(path));
        reader.readLine();
        readNextRow();
    }

    public String get(int id) {
        if (id >= 0 && id < currentRowValues.length) {
            return currentRowValues[id];
        }
        return "";
    }

    public int getInt(int j) {
        return Integer.parseInt(get(j));
    }

    public long getLong(int j) {
        return Long.parseLong(get(j));
    }

    public float getFloat(int j) {
        return Float.parseFloat(get(j));
    }

    public double getDouble(int j) {
        return Double.parseDouble(get(j));
    }

    public Serie[] getSeries() {
        return series;
    }
    
    public String getColumnName(int colIndex) {
        if (colIndex >= 0 && colIndex < series.length) {
            return series[colIndex].name;
        }
        return "";
    }

    public int column(){
        return series.length;
    }

}
