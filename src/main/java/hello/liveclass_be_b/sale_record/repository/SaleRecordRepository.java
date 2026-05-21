package hello.liveclass_be_b.sale_record.repository;

import hello.liveclass_be_b.sale_record.entity.SaleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;

public interface SaleRecordRepository extends JpaRepository<SaleRecord, String> {

    List<SaleRecord> findByCourse_Creator_Id(String creatorId);

    @Query("""
    select sr
    from SaleRecord sr
    join sr.course c
    join c.creator cr
    where cr.id = :creatorId
      and sr.paidAt >= :startInclusive
      and sr.paidAt < :endExclusive
""")
    List<SaleRecord> findByCreatorIdAndPaidAtRange(
            @Param("creatorId") String creatorId,
            @Param("startInclusive") OffsetDateTime start,
            @Param("endExclusive") OffsetDateTime end
    );

    @Query("""
    select sr
    from SaleRecord sr
    join fetch sr.course c
    join fetch c.creator creator
    where sr.paidAt >= :start
      and sr.paidAt < :end
""")
    List<SaleRecord> findAllByPaidAtRange(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );
}
