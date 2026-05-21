package hello.liveclass_be_b.course.entity;

import hello.liveclass_be_b.creator.entity.Creator;
import jakarta.persistence.*;

@Entity
public class Course {

    @Id
    private String id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private Creator creator;

}
