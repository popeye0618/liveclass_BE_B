package hello.liveclass_be_b.cancel_record.service;

import hello.liveclass_be_b.cancel_record.dto.CancelRecordCreateRequest;
import hello.liveclass_be_b.cancel_record.dto.CancelRecordResponse;

public interface CancelRecordService {

    CancelRecordResponse createCancelRecord(CancelRecordCreateRequest request);
}
