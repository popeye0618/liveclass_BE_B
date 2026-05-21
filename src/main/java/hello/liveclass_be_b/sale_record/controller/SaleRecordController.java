package hello.liveclass_be_b.sale_record.controller;

import hello.liveclass_be_b.global.response.ApiResponse;
import hello.liveclass_be_b.sale_record.dto.CoursePurchaseRequest;
import hello.liveclass_be_b.sale_record.dto.CoursePurchaseResponse;
import hello.liveclass_be_b.sale_record.dto.SaleRecordResponse;
import hello.liveclass_be_b.sale_record.service.SaleRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SaleRecordController {

    private final SaleRecordService saleRecordService;

    @PostMapping("/sale-record/purchase")
    public ResponseEntity<ApiResponse<CoursePurchaseResponse>> purchaseCourse(
            @Valid @RequestBody CoursePurchaseRequest request
            ) {
        CoursePurchaseResponse response = saleRecordService.purchaseCourse(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/creators/{creatorId}/sale-records")
    public ResponseEntity<ApiResponse<List<SaleRecordResponse>>> findSaleRecordsByCreator(
            @PathVariable String creatorId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        List<SaleRecordResponse> response = saleRecordService.findSaleRecordsByCreator(
                creatorId,
                startDate,
                endDate
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
