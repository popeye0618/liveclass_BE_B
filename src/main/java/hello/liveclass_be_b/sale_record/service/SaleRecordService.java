package hello.liveclass_be_b.sale_record.service;

import hello.liveclass_be_b.sale_record.dto.CoursePurchaseRequest;
import hello.liveclass_be_b.sale_record.dto.CoursePurchaseResponse;
import hello.liveclass_be_b.sale_record.dto.SaleRecordResponse;

import java.time.LocalDate;
import java.util.List;

public interface SaleRecordService {

    CoursePurchaseResponse purchaseCourse(CoursePurchaseRequest request);

    List<SaleRecordResponse> findSaleRecordsByCreator(
            String creatorId,
            LocalDate startDate,
            LocalDate endDate
    );
}
