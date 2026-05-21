package hello.liveclass_be_b.course.entity;

import hello.liveclass_be_b.creator.entity.Creator;
import hello.liveclass_be_b.sale_record.entity.SaleRecord;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

    @Id
    private String id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private Creator creator;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleRecord> SaleRecordList = new ArrayList<>();

    @Builder
    public Course(String id, String title, Creator creator, List<SaleRecord> saleRecordList) {
        this.id = id;
        this.title = title;
        this.creator = creator;
        SaleRecordList = saleRecordList;
    }
}
