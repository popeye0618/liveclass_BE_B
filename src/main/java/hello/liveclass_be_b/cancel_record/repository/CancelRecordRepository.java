package hello.liveclass_be_b.cancel_record.repository;

import hello.liveclass_be_b.cancel_record.entity.CancelRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CancelRecordRepository extends JpaRepository<CancelRecord, String> {

    @Query("""
        select coalesce(sum(cr.amount), 0)
        from CancelRecord cr
        where cr.saleRecord.id = :saleRecordId
    """)
    Long sumRefundAmountBySaleRecordId(String saleRecordId);
}
