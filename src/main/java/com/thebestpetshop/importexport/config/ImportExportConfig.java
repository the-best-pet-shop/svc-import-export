package com.thebestpetshop.importexport.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.time.Duration;

@ConfigMapping(prefix = "import-export")
public interface ImportExportConfig {
    @WithDefault("10485760") long maxBytes();
    @WithDefault("100000") long maxRows();
    @WithDefault("1000") long maxPreviewRows();
    @WithDefault("local-e06-test-key") String uploadSigningKey();
    @WithDefault("PT30M") Duration downloadTtl();
    @WithDefault("true") boolean requireAntivirus();
}
