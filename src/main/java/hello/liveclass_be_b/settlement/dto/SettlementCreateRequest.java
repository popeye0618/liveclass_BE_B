package hello.liveclass_be_b.settlement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SettlementCreateRequest(
        @NotBlank(message = "크리에이터 ID는 필수입니다.")
        String creatorId,

        @NotBlank(message = "정산 연월은 필수입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "정산 연월은 yyyy-MM 형식이어야 합니다.")
        String settlementMonth
) {
}
