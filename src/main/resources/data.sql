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

INSERT INTO sale_record (id, course_id, student_id, amount, paid_at)
VALUES
    ('sale-1', 'course-1', 'student-1', 50000, '2025-03-05T10:00:00+09:00'),
    ('sale-2', 'course-1', 'student-2', 50000, '2025-03-15T14:30:00+09:00'),
    ('sale-3', 'course-2', 'student-3', 80000, '2025-03-20T09:00:00+09:00'),
    ('sale-4', 'course-2', 'student-4', 80000, '2025-03-22T11:00:00+09:00'),
    ('sale-5', 'course-3', 'student-5', 60000, '2025-01-31T23:30:00+09:00'),
    ('sale-6', 'course-3', 'student-6', 60000, '2025-03-10T16:00:00+09:00'),
    ('sale-7', 'course-4', 'student-7', 120000, '2025-02-14T10:00:00+09:00');

INSERT INTO cancel_record (id, sale_record_id, amount, canceled_at)
VALUES
    ('cancel-1', 'sale-3', 80000, '2025-03-25T10:00:00+09:00'),
    ('cancel-2', 'sale-4', 30000, '2025-03-28T10:00:00+09:00'),
    ('cancel-3', 'sale-5', 60000, '2025-02-01T09:00:00+09:00');