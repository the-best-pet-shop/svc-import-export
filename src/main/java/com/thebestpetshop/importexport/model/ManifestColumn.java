package com.thebestpetshop.importexport.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ManifestColumn(@NotBlank String name, @NotNull ColumnType type, boolean required, boolean unique) {}
