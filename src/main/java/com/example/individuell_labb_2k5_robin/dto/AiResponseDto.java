package com.example.individuell_labb_2k5_robin.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiResponseDto {

    @NotNull
    @Pattern(regexp = "POSITIVE|NEGATIVE|NEUTRAL")
    private String sentiment;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double score;

    @NotNull
    private String summary;

    public static AiResponseDto fallback() {
        return new AiResponseDto("NEUTRAL", 0.0, "Could not analyze sentiment.");
    }
}
