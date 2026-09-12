package com.thebestpetshop.importexport.parser;

import com.thebestpetshop.importexport.model.ColumnType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ValueValidator {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private ValueValidator() {}

    public static String validate(ColumnType type, String raw, String timezone) {
        var value = raw == null ? "" : raw.strip();
        if (value.isEmpty()) return "value is required";
        try {
            switch (type) {
                case STRING -> { if (value.length() > 1000) return "value exceeds 1000 characters"; }
                case EMAIL -> { if (!EMAIL.matcher(value).matches()) return "invalid email"; }
                case PHONE -> { if (!value.replaceAll("[ +()\\-]", "").matches("\\d{10,15}")) return "invalid phone"; }
                case CPF -> { if (!validDocument(value, 11)) return "invalid CPF"; }
                case CNPJ -> { if (!validDocument(value, 14)) return "invalid CNPJ"; }
                case EAN -> { if (!validEan(value)) return "invalid EAN"; }
                case CURRENCY -> { var normalized = value.replace(',', '.'); if (!normalized.matches("-?\\d+(?:\\.\\d{1,2})?")) return "invalid currency"; new BigDecimal(normalized); }
                case DATE -> LocalDate.parse(value);
                case TIMEZONE -> ZoneId.of(value);
                case UUID, UNIT_ID -> UUID.fromString(value);
            }
            if (type == ColumnType.TIMEZONE && !value.equals(ZoneId.of(value).getId())) return "timezone must be IANA";
        } catch (RuntimeException exception) { return "invalid " + type.name().toLowerCase(); }
        return null;
    }

    public static boolean validDocument(String raw, int length) {
        var digits = raw.replaceAll("\\D", "");
        if (digits.length() != length || digits.chars().distinct().count() == 1) return false;
        var weights = length == 11 ? new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2} : new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        for (var check = 0; check < 2; check++) {
            var sum = 0;
            for (var i = 0; i < weights.length; i++) sum += (digits.charAt(i) - '0') * weights[i];
            var digit = (sum * 10) % 11;
            if (digit == 10) digit = 0;
            if (digit != digits.charAt(weights.length) - '0') return false;
            if (check == 0) { var next = new int[weights.length + 1]; System.arraycopy(weights, 0, next, 1, weights.length); next[0] = length == 11 ? 11 : 6; weights = next; }
        }
        return true;
    }

    public static boolean validEan(String raw) {
        var digits = raw.replaceAll("\\D", "");
        if (!(digits.length() == 8 || digits.length() == 12 || digits.length() == 13 || digits.length() == 14)) return false;
        var sum = 0;
        var position = 0;
        for (var i = digits.length() - 2; i >= 0; i--, position++) sum += (digits.charAt(i) - '0') * (position % 2 == 0 ? 3 : 1);
        return (10 - (sum % 10)) % 10 == digits.charAt(digits.length() - 1) - '0';
    }
}
