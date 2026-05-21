package hello.liveclass_be_b.settlement.controller;

import hello.liveclass_be_b.global.response.ApiResponse;
import hello.liveclass_be_b.settlement.dto.MonthlySettlementResponse;
import hello.liveclass_be_b.settlement.dto.SettlementCreateRequest;
import hello.liveclass_be_b.settlement.dto.SettlementResponse;
import hello.liveclass_be_b.settlement.dto.TotalSettlementResponse;
import hello.liveclass_be_b.settlement.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/settlements/creators/{creatorId}/monthly")
    public ResponseEntity<ApiResponse<MonthlySettlementResponse>> getMonthlySettlementByCreator(
            @PathVariable String creatorId,
            @RequestParam String date
    ) {
        MonthlySettlementResponse response = settlementService.getMonthlySettlementByCreator(
                creatorId,
                date
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/admin/settlements")
    public ResponseEntity<ApiResponse<TotalSettlementResponse>> getTotalSettlement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        TotalSettlementResponse response = settlementService.getTotalSettlement(
                startDate,
                endDate
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/admin/settlements/monthly")
    public ResponseEntity<ApiResponse<SettlementResponse>> createMonthlySettlement(
            @Valid @RequestBody SettlementCreateRequest request
    ) {
        SettlementResponse response = settlementService.createMonthlySettlement(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/admin/settlements/{settlementId}/confirm")
    public ResponseEntity<ApiResponse<SettlementResponse>> confirmSettlement(
            @PathVariable Long settlementId
    ) {
        SettlementResponse response = settlementService.confirmSettlement(settlementId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/admin/settlements/{settlementId}/pay")
    public ResponseEntity<ApiResponse<SettlementResponse>> paySettlement(
            @PathVariable Long settlementId
    ) {
        SettlementResponse response = settlementService.paySettlement(settlementId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
