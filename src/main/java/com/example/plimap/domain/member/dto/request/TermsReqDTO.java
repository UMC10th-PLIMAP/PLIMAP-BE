package com.example.plimap.domain.member.dto.request;

import com.example.plimap.domain.member.enums.TermsType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class TermsReqDTO {

    @Schema(example = """
            {
              "agreements": [
                { "type": "SERVICE", "agreed": true },
                { "type": "PRIVACY", "agreed": true },
                { "type": "LOCATION", "agreed": true },
                { "type": "MARKETING", "agreed": false }
              ]
            }
            """)
    public record Agree(
            @NotEmpty
            @Valid
            List<Item> agreements
    ) {
        public record Item(
                @NotNull
                @Schema(example = "SERVICE")
                TermsType type,

                @NotNull
                @Schema(example = "true")
                Boolean agreed
        ) {
        }
    }
}
