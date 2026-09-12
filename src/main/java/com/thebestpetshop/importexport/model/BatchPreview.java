package com.thebestpetshop.importexport.model;

import java.util.List;

public record BatchPreview(long rows, long validRows, long invalidRows, long conflicts, List<RowIssue> issues, String checksum) {}
