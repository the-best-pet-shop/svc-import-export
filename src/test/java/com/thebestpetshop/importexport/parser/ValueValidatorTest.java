package com.thebestpetshop.importexport.parser;

import com.thebestpetshop.importexport.model.ColumnType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValueValidatorTest {
    @Test void validatesBrazilianFieldsAndCurrency() {
        assertNull(ValueValidator.validate(ColumnType.EMAIL, "ana@example.com", "America/Sao_Paulo"));
        assertNull(ValueValidator.validate(ColumnType.CPF, "529.982.247-25", "America/Sao_Paulo"));
        assertNull(ValueValidator.validate(ColumnType.CNPJ, "04.252.011/0001-10", "America/Sao_Paulo"));
        assertNull(ValueValidator.validate(ColumnType.EAN, "7894900011517", "America/Sao_Paulo"));
        assertNull(ValueValidator.validate(ColumnType.CURRENCY, "19,90", "America/Sao_Paulo"));
        assertNotNull(ValueValidator.validate(ColumnType.EMAIL, "invalid", "America/Sao_Paulo"));
        assertNotNull(ValueValidator.validate(ColumnType.TIMEZONE, "not-a-timezone", "America/Sao_Paulo"));
    }
}
