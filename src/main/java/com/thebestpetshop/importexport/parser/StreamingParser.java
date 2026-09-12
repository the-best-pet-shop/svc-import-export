package com.thebestpetshop.importexport.parser;

import com.thebestpetshop.importexport.exception.ImportExportException;
import com.thebestpetshop.importexport.model.ColumnType;
import com.thebestpetshop.importexport.model.ImportManifest;
import com.thebestpetshop.importexport.model.ManifestColumn;
import com.thebestpetshop.importexport.model.RowIssue;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;

@ApplicationScoped
public class StreamingParser {
    public ParsedBatch parse(String content, ImportManifest manifest, long maxRows, long maxPreviewRows) {
        if (manifest.format().name().equals("XLSX")) return parseXlsx(content, manifest, maxRows, maxPreviewRows);
        try (var reader = new BufferedReader(new StringReader(content))) {
            var headerLine = reader.readLine();
            if (headerLine == null) throw ImportExportException.invalid("file is empty");
            var delimiter = detectDelimiter(headerLine); var table = new ArrayList<List<String>>(); table.add(split(headerLine, delimiter));
            String line;
            while ((line = reader.readLine()) != null) { if (table.size() > maxRows) throw ImportExportException.tooLarge("row limit exceeded"); table.add(split(line, delimiter)); }
            return validateTable(table, manifest, maxPreviewRows);
        } catch (IOException exception) { throw ImportExportException.invalid("unable to stream file"); }
    }

    private ParsedBatch parseXlsx(String content, ImportManifest manifest, long maxRows, long maxPreviewRows) {
        if (content == null || content.isBlank()) throw ImportExportException.invalid("XLSX content is empty");
        try {
            var bytes = Base64.getDecoder().decode(content); var shared = new ArrayList<String>(); var rows = new ArrayList<List<String>>();
            try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) { if (entry.getName().equals("xl/sharedStrings.xml")) readSharedStrings(zip, shared); if (entry.getName().equals("xl/worksheets/sheet1.xml")) readSheet(zip, shared, rows, maxRows); }
            }
            if (rows.isEmpty()) throw ImportExportException.invalid("XLSX contains no rows");
            return validateTable(rows, manifest, maxPreviewRows);
        } catch (ImportExportException e) { throw e; } catch (Exception e) { throw ImportExportException.invalid("XLSX is not a valid workbook"); }
    }

    private static void readSharedStrings(ZipInputStream zip, List<String> shared) throws Exception {
        var xml = XMLInputFactory.newFactory().createXMLStreamReader(zip); var value = new StringBuilder();
        while (xml.hasNext()) { var event = xml.next(); if (event == XMLStreamConstants.CHARACTERS) value.append(xml.getText()); if (event == XMLStreamConstants.END_ELEMENT && xml.getLocalName().equals("si")) { shared.add(value.toString()); value.setLength(0); } }
    }
    private static void readSheet(ZipInputStream zip, List<String> shared, List<List<String>> rows, long maxRows) throws Exception {
        var xml = XMLInputFactory.newFactory().createXMLStreamReader(zip); List<String> row = null; String type = null; var value = new StringBuilder(); var index = 0;
        while (xml.hasNext()) { var event = xml.next();
            if (event == XMLStreamConstants.START_ELEMENT && xml.getLocalName().equals("row")) { row = new ArrayList<>(); index = 0; }
            else if (event == XMLStreamConstants.START_ELEMENT && xml.getLocalName().equals("c")) { var ref = xml.getAttributeValue(null, "r"); type = xml.getAttributeValue(null, "t"); if (ref != null) index = columnIndex(ref); while (row != null && row.size() < index) row.add(""); }
            else if (event == XMLStreamConstants.CHARACTERS && row != null) value.append(xml.getText());
            else if (event == XMLStreamConstants.END_ELEMENT && xml.getLocalName().equals("v") && row != null) { var text = value.toString(); row.add(type != null && type.equals("s") ? shared.get(Integer.parseInt(text)) : text); value.setLength(0); index++; }
            else if (event == XMLStreamConstants.END_ELEMENT && xml.getLocalName().equals("row") && row != null) { rows.add(row); row = null; if (rows.size() > maxRows + 1) throw ImportExportException.tooLarge("row limit exceeded"); }
        }
    }
    private static int columnIndex(String ref) { var index = 0; for (var i = 0; i < ref.length() && Character.isLetter(ref.charAt(i)); i++) index = index * 26 + (Character.toUpperCase(ref.charAt(i)) - 'A' + 1); return Math.max(0, index - 1); }

    private static ParsedBatch validateTable(List<List<String>> table, ImportManifest manifest, long maxPreviewRows) {
        var expected = manifest.columns().stream().map(ManifestColumn::name).toList(); var headers = table.getFirst().stream().map(String::strip).toList();
        if (!headers.equals(expected)) throw ImportExportException.invalid("header does not match the versioned manifest columns");
        var issues = new ArrayList<RowIssue>(); var unique = new HashMap<String, Set<String>>(); manifest.columns().forEach(c -> { if (c.unique()) unique.put(c.name(), new HashSet<>()); }); long valid = 0, invalid = 0, conflicts = 0;
        for (var rowIndex = 1; rowIndex < table.size(); rowIndex++) { var values = table.get(rowIndex); var rowNumber = (long) rowIndex; var rowInvalid = false;
            if (values.size() != expected.size()) { add(issues, rowNumber, "INVALID", "_row", "column count does not match manifest", maxPreviewRows); invalid++; continue; }
            for (var i = 0; i < values.size(); i++) { var column = manifest.columns().get(i); var value = values.get(i); if (value.isBlank() && column.required()) { add(issues, rowNumber, "ERROR", column.name(), "required value is missing", maxPreviewRows); rowInvalid = true; continue; } if (!value.isBlank()) { var error = ValueValidator.validate(column.type(), value, manifest.timezone()); if (error != null) { add(issues, rowNumber, "ERROR", column.name(), error, maxPreviewRows); rowInvalid = true; } if (column.type() == ColumnType.UNIT_ID && manifest.unitId() != null && !manifest.unitId().toString().equals(value)) { add(issues, rowNumber, "ERROR", column.name(), "cross-unit row is rejected", maxPreviewRows); rowInvalid = true; } if (column.unique() && !unique.get(column.name()).add(value)) { add(issues, rowNumber, "CONFLICT", column.name(), "duplicate value in batch", maxPreviewRows); conflicts++; rowInvalid = true; } } }
            if (rowInvalid) invalid++; else valid++;
        }
        return new ParsedBatch(table.size() - 1L, valid, invalid, conflicts, List.copyOf(issues));
    }

    private static char detectDelimiter(String header) { return header.indexOf(';') >= 0 && header.indexOf(';') > header.indexOf(',') ? ';' : ','; }
    private static List<String> split(String line, char delimiter) { var text = line.startsWith("\uFEFF") ? line.substring(1) : line; var result = new ArrayList<String>(); var current = new StringBuilder(); var quoted = false; for (var i = 0; i < text.length(); i++) { var c = text.charAt(i); if (c == '"') quoted = !quoted; else if (c == delimiter && !quoted) { result.add(current.toString().strip()); current.setLength(0); } else current.append(c); } result.add(current.toString().strip()); return result; }
    private static void add(List<RowIssue> issues, long row, String severity, String column, String message, long max) { if (issues.size() < max) issues.add(new RowIssue(row + 1, severity, severity.equals("CONFLICT") ? "DUPLICATE" : "INVALID_VALUE", column, message)); }
}
