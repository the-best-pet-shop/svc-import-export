package com.thebestpetshop.importexport.model;

public record RowIssue(long rowNumber, String severity, String code, String column, String message) {}
