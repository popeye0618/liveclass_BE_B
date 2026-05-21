package hello.liveclass_be_b.settlement.repository;

import hello.liveclass_be_b.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("""
        select s
        from Settlement s
        join fetch s.creator c
        where c.id = :creatorId
          and s.settlementMonth = :settlementMonth
    """)
    Optional<Settlement> findByCreatorAndMonth(
            String creatorId,
            String settlementMonth
    );

    @Query("""
        select count(s) > 0
        from Settlement s
        where s.creator.id = :creatorId
          and s.settlementMonth = :settlementMonth
    """)
    boolean existsByCreatorAndMonth(
            String creatorId,
            String settlementMonth
    );

    @Query("""
        select s
        from Settlement s
        join fetch s.creator c
        where c.id = :creatorId
        order by s.settlementMonth desc
    """)
    List<Settlement> findAllByCreatorId(
            String creatorId
    );

    @Query("""
        select s
        from Settlement s
        join fetch s.creator c
        where s.settlementMonth between :startMonth and :endMonth
        order by s.settlementMonth asc, c.id asc
    """)
    List<Settlement> findAllByMonthRange(
            String startMonth,
            String endMonth
    );
}