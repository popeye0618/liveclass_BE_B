package hello.liveclass_be_b.creator.entity;

import hello.liveclass_be_b.course.entity.Course;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Creator {

    @Id
    private String id;

    private String name;

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Course> courseList = new ArrayList<>();
}
