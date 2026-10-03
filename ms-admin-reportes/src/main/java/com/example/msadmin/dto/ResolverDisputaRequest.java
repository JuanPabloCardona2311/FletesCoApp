package com.example.msadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolverDisputaRequest(
        @NotBlank @Size(max = 500) String resolucion) {
}
