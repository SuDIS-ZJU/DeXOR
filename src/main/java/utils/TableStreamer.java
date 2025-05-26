package utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.Reader;
import java.util.Iterator;

public class TableStreamer {
    private String path = "";
    private CSVParser parser;
    Iterator<CSVRecord> it;
    private CSVRecord column;
    private CSVRecord current_row;

    private Serie[] series;


    public TableStreamer(String path) throws Exception {
        this.path = path;
        this.reload(path);
    }

    public void reload(String path) throws Exception {
        Reader in = new FileReader(path);
        this.parser = new CSVParser(in, CSVFormat.DEFAULT);
        it = parser.iterator();
        column = it.next();
        series = new Serie[column.size()];
        for (int i = 0; i < column.size(); i++) {
            series[i] = new Serie(column.get(i));
        }
        next();
    }

    public void next() throws Exception {
        if (it.hasNext()) {
            current_row = it.next();
            for (int i = 0; i < column.size(); i++) {
                series[i].add(current_row.get(i));
            }
        } else {
            throw new Exception("EOF");
        }
    }

    public String get(int id) {
        String res = current_row.get(id);
//        fill null
//        if(res.isEmpty()) res = "0";
        return res;
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

}
