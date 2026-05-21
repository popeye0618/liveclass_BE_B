package hello.liveclass_be_b.cancel_record.repository;

import hello.liveclass_be_b.cancel_record.entity.CancelRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface CancelRecordRepository extends JpaRepository<CancelRecord, String> {

    @Query("""
        select coalesce(sum(cr.amount), 0)
        from CancelRecord cr
        where cr.saleRecord.id = :saleRecordId
    """)
    Long sumRefundAmountBySaleRecordId(@Param("saleRecordId") String saleRecordId);

    @Query("""
    select cr
    from CancelRecord cr
    join fetch cr.saleRecord sr
    join fetch sr.course c
    join fetch c.creator creator
    where creator.id = :creatorId
      and cr.canceledAt >= :start
      and cr.canceledAt < :end
""")
    List<CancelRecord> findByCreatorIdAndCanceledAtRange(
            @Param("creatorId") String creatorId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    @Query("""
    select cr
    from CancelRecord cr
    join fetch cr.saleRecord sr
    join fetch sr.course c
    join fetch c.creator creator
    where cr.canceledAt >= :start
      and cr.canceledAt < :end
""")
    List<CancelRecord> findAllByCanceledAtRange(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );
}
