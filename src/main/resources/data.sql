INSERT INTO creator (id, name)
VALUES
    ('creator-1', '김강사'),
    ('creator-2', '이강사'),
    ('creator-3', '박강사');

INSERT INTO course (id, creator_id, title)
VALUES
    ('course-1', 'creator-1', 'Spring Boot 입문'),
    ('course-2', 'creator-1', 'JPA 실전'),
    ('course-3', 'creator-2', 'Kotlin 기초'),
    ('course-4', 'creator-3', 'MSA 설계');
