package com.thebestpetshop.importexport.security;

import com.thebestpetshop.importexport.config.ImportExportConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UploadSecurityTest {
    private final ImportExportConfig config = new ImportExportConfig() {
        public long maxBytes() { return 1024; } public long maxRows() { return 10; } public long maxPreviewRows() { return 10; }
        public String uploadSigningKey() { return "test-key"; } public Duration downloadTtl() { return Duration.ofMinutes(30); } public boolean requireAntivirus() { return true; }
    };

    @Test void rejectsDeterministicEicarFixture() { var security = new UploadSecurity(config); assertThrows(RuntimeException.class, () -> security.scan("X5O!P%@AP".getBytes())); }
    @Test void verifiesChecksumAndSignature() throws Exception {
        var security = new UploadSecurity(config); var org = UUID.fromString("11111111-1111-1111-1111-111111111111"); var bytes = "a".getBytes(); var checksum = security.checksum(bytes);
        var mac = javax.crypto.Mac.getInstance("HmacSHA256"); mac.init(new javax.crypto.spec.SecretKeySpec("test-key".getBytes(), "HmacSHA256")); var sig = java.util.HexFormat.of().formatHex(mac.doFinal((org + ":" + checksum).getBytes())); security.verify(org, checksum, sig, bytes);
    }
}
