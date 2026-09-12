package com.thebestpetshop.importexport.parser;

import com.thebestpetshop.importexport.model.RowIssue;

import java.util.List;

public record ParsedBatch(long rows, long validRows, long invalidRows, long conflicts, List<RowIssue> issues) {}
