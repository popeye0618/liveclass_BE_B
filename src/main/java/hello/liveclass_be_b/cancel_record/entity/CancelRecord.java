package hello.liveclass_be_b.cancel_record.entity;

import hello.liveclass_be_b.sale_record.entity.SaleRecord;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CancelRecord {

    @Id
    private String id;

    private Long amount;
    private OffsetDateTime canceledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_record_id")
    private SaleRecord saleRecord;

    @Builder
    public CancelRecord(String id, Long amount, OffsetDateTime canceledAt, SaleRecord saleRecord) {
        this.id = id;
        this.amount = amount;
        this.canceledAt = canceledAt;
        this.saleRecord = saleRecord;
    }
}
