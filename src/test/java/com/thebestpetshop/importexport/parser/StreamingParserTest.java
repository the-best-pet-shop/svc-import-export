package com.thebestpetshop.importexport.parser;

import com.thebestpetshop.importexport.model.ColumnType;
import com.thebestpetshop.importexport.model.FileFormat;
import com.thebestpetshop.importexport.model.ImportManifest;
import com.thebestpetshop.importexport.model.ManifestColumn;
import com.thebestpetshop.importexport.model.ScopePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StreamingParserTest {
    private static final UUID ORG = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID UNIT = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static ImportManifest manifest() { return new ImportManifest("pet.v1", 1, "pets", FileFormat.CSV, "UTF-8", "America/Sao_Paulo", ScopePolicy.UNIT_LINKED, ORG, UNIT, List.of(new ManifestColumn("email", ColumnType.EMAIL, true, true), new ManifestColumn("unitId", ColumnType.UNIT_ID, true, false))); }

    @Test void streamsRowsAndReportsDuplicateAndCrossUnit() {
        var parser = new StreamingParser(); var result = parser.parse("email,unitId\na@x.com," + UNIT + "\na@x.com,33333333-3333-3333-3333-333333333333\n", manifest(), 100, 20);
        assertEquals(2, result.rows()); assertEquals(1, result.validRows()); assertEquals(1, result.invalidRows()); assertEquals(1, result.conflicts()); assertEquals(2, result.issues().size());
    }

    @Test void enforcesRowLimit() {
        var parser = new StreamingParser(); assertThrows(RuntimeException.class, () -> parser.parse("email,unitId\na@x.com," + UNIT + "\n", manifest(), 0, 20));
    }
}
