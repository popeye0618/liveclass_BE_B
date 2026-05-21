package hello.liveclass_be_b.sale_record.entity;

import hello.liveclass_be_b.cancel_record.entity.CancelRecord;
import hello.liveclass_be_b.course.entity.Course;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleRecord {

    @Id
    private String id;

    private String studentId;
    private Long amount;
    private OffsetDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @OneToMany(mappedBy = "saleRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CancelRecord> cancelRecordList = new ArrayList<>();

    @Builder
    public SaleRecord(String id, String studentId, Long amount, OffsetDateTime paidAt, Course course) {
        this.id = id;
        this.studentId = studentId;
        this.amount = amount;
        this.paidAt = paidAt;
        this.course = course;
    }
}
