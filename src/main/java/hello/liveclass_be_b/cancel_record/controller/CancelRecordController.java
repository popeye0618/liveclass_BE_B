package hello.liveclass_be_b.cancel_record.controller;

import hello.liveclass_be_b.cancel_record.dto.CancelRecordCreateRequest;
import hello.liveclass_be_b.cancel_record.dto.CancelRecordResponse;
import hello.liveclass_be_b.cancel_record.service.CancelRecordService;
import hello.liveclass_be_b.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cancel-records")
public class CancelRecordController {

    private final CancelRecordService cancelRecordService;

    @PostMapping
    public ResponseEntity<ApiResponse<CancelRecordResponse>> createCancelRecord(
            @Valid @RequestBody CancelRecordCreateRequest request
            ) {
        CancelRecordResponse response = cancelRecordService.createCancelRecord(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
