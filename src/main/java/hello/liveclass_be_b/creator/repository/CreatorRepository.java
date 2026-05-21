package hello.liveclass_be_b.creator.repository;

import hello.liveclass_be_b.creator.entity.Creator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorRepository extends JpaRepository<Creator, String> {
}
