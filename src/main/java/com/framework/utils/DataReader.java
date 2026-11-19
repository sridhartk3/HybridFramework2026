package com.framework.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.framework.config.ConfigReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ONE entry point for all test data:
 *   source = local file OR http(s) URL (cloud)
 *   format = Excel, CSV or JSON (auto-detected)
 * Always returns the same thing: a list of rows, each row = Map(column -> value).
 */
public final class DataReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DataReader() { }

    public static List<Map<String, String>> read(String source, String sheet, String format) {
        String fmt = (format == null || format.isBlank()) ? detect(source) : format.toLowerCase();
        try (InputStream in = open(source)) {
            return switch (fmt) {
                case "excel", "xlsx" -> readExcel(in, sheet);
                case "csv"           -> readCsv(in);
                case "json"          -> readJson(in, sheet);
                default -> throw new IllegalArgumentException("Unsupported format: " + fmt);
            };
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read test data: " + source, e);
        }
    }

    // ---------- where is the data? (local file or cloud URL) ----------
    private static InputStream open(String source) throws IOException {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(source)).GET();
            String auth = ConfigReader.get("testdata.auth");
            if (auth != null && !auth.isBlank()) req.header("Authorization", auth);
            try {
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS).build();
                HttpResponse<InputStream> res =
                        client.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
                if (res.statusCode() != 200) {
                    throw new IOException("HTTP " + res.statusCode() + " for " + source);
                }
                return res.body();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
        }
        return new FileInputStream(source);
    }

    // ---------- which format? ----------
    private static String detect(String source) {
        String s = source.toLowerCase();
        if (s.contains(".xlsx") || s.contains("format=xlsx")) return "excel";
        if (s.contains(".csv")  || s.contains("format=csv"))  return "csv";
        if (s.contains(".json")) return "json";
        throw new IllegalArgumentException(
                "Cannot detect format of '" + source + "'. Use @DataSource(format = \"csv|json|excel\")");
    }

    // ---------- readers ----------
    private static List<Map<String, String>> readExcel(InputStream in, String sheetName) throws IOException {
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) throw new IllegalArgumentException("Excel sheet not found: " + sheetName);

            DataFormatter fmt = new DataFormatter();
            Row header = sheet.getRow(0);
            int cols = header.getLastCellNum();
            List<Map<String, String>> rows = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> data = new LinkedHashMap<>();
                boolean empty = true;
                for (int c = 0; c < cols; c++) {
                    String key = fmt.formatCellValue(header.getCell(c)).trim();
                    String val = fmt.formatCellValue(row.getCell(c)).trim();
                    if (!val.isEmpty()) empty = false;
                    data.put(key, val);
                }
                if (!empty) rows.add(data);
            }
            return rows;
        }
    }

    private static List<Map<String, String>> readCsv(InputStream in) throws IOException {
        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) text = text.substring(1);       // Excel's BOM

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true).setTrim(true).build();

        List<Map<String, String>> rows = new ArrayList<>();
        try (CSVParser parser = format.parse(new StringReader(text))) {
            for (CSVRecord rec : parser) rows.add(new LinkedHashMap<>(rec.toMap()));
        }
        return rows;
    }

    /** JSON can be an array [ {...}, {...} ]  OR  { "testName": [ {...} ], "other": [ ... ] } */
    private static List<Map<String, String>> readJson(InputStream in, String key) throws IOException {
        JsonNode root = MAPPER.readTree(in);
        JsonNode array = root.isArray() ? root : root.get(key);
        if (array == null || !array.isArray()) {
            throw new IllegalArgumentException("JSON must be an array, or an object with array '" + key + "'");
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (JsonNode item : array) {
            Map<String, String> row = new LinkedHashMap<>();
            item.fields().forEachRemaining(e -> row.put(e.getKey(),
                    e.getValue().isValueNode() ? e.getValue().asText() : e.getValue().toString()));
            rows.add(row);
        }
        return rows;
    }
}
