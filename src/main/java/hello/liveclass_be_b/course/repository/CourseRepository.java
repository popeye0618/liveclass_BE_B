package hello.liveclass_be_b.course.repository;

import hello.liveclass_be_b.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, String> {
}
