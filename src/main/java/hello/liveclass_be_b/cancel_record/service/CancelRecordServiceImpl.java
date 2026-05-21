package hello.liveclass_be_b.cancel_record.service;

import hello.liveclass_be_b.cancel_record.dto.CancelRecordCreateRequest;
import hello.liveclass_be_b.cancel_record.dto.CancelRecordResponse;
import hello.liveclass_be_b.cancel_record.entity.CancelRecord;
import hello.liveclass_be_b.cancel_record.error.CancelRecordErrorCode;
import hello.liveclass_be_b.cancel_record.repository.CancelRecordRepository;
import hello.liveclass_be_b.global.error.BusinessException;
import hello.liveclass_be_b.sale_record.entity.SaleRecord;
import hello.liveclass_be_b.sale_record.repository.SaleRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CancelRecordServiceImpl implements CancelRecordService {

    private final CancelRecordRepository cancelRecordRepository;
    private final SaleRecordRepository saleRecordRepository;

    @Override
    @Transactional
    public CancelRecordResponse createCancelRecord(CancelRecordCreateRequest request) {
        SaleRecord saleRecord = saleRecordRepository.findById(request.saleRecordId())
                .orElseThrow(() -> new BusinessException(CancelRecordErrorCode.SALE_RECORD_NOT_FOUND));

        Long alreadyRefundedAmount = cancelRecordRepository.sumRefundAmountBySaleRecordId(saleRecord.getId());
        long totalRefundAmount = alreadyRefundedAmount + request.refundAmount();

        if (totalRefundAmount > saleRecord.getAmount()) {
            throw new BusinessException(CancelRecordErrorCode.REFUND_AMOUNT_EXCEEDED);
        }

        CancelRecord cancelRecord = CancelRecord.builder()
                .id(request.id())
                .saleRecord(saleRecord)
                .amount(request.refundAmount())
                .canceledAt(request.canceledAt())
                .build();

        cancelRecordRepository.save(cancelRecord);

        return CancelRecordResponse.from(cancelRecord);
    }
}
