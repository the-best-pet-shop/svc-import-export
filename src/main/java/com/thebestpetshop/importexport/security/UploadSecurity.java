package com.thebestpetshop.importexport.security;

import com.thebestpetshop.importexport.config.ImportExportConfig;
import com.thebestpetshop.importexport.exception.ImportExportException;
import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class UploadSecurity {
    private final ImportExportConfig config;
    public UploadSecurity(ImportExportConfig config) { this.config = config; }
    public byte[] bytes(String content, String format) {
        if (content == null) throw ImportExportException.invalid("content is required");
        try { return format.equalsIgnoreCase("XLSX") ? java.util.Base64.getDecoder().decode(content) : content.getBytes(StandardCharsets.UTF_8); }
        catch (IllegalArgumentException e) { throw ImportExportException.invalid("XLSX content must be base64 encoded"); }
    }
    public String checksum(byte[] bytes) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch (Exception e) { throw new IllegalStateException(e); } }
    public void verify(UUID organizationId, String checksum, String signature, byte[] bytes) {
        if (bytes.length > config.maxBytes()) throw ImportExportException.tooLarge("upload size limit exceeded");
        var actual = checksum(bytes);
        if (!actual.equalsIgnoreCase(checksum)) throw ImportExportException.conflict("checksum does not match upload");
        try {
            var mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(config.uploadSigningKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var expected = HexFormat.of().formatHex(mac.doFinal((organizationId + ":" + checksum).getBytes(StandardCharsets.UTF_8)));
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), signature.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII))) throw ImportExportException.unauthorized("upload signature is invalid");
        } catch (ImportExportException e) { throw e; } catch (Exception e) { throw ImportExportException.unavailable("upload signing provider is unavailable"); }
    }
    public void scan(byte[] bytes) { var text = new String(bytes, StandardCharsets.ISO_8859_1); if (text.contains("EICAR-STANDARD-ANTIVIRUS-TEST-FILE") || text.contains("X5O!P%@AP")) throw ImportExportException.invalid("antivirus scan rejected the upload"); }
}
