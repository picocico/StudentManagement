CREATE TABLE IF NOT EXISTS students(
　　 student_id BINARY(16) NOT NULL,
    full_name  VARCHAR(100) NOT NULL,
    furigana   VARCHAR(100) NOT NULL,
    nickname   VARCHAR(50),
    email      VARCHAR(255) NOT NULL,
    location   VARCHAR(100),
    age        INT,
    gender     VARCHAR(20) NOT NULL,
    remarks    TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_students PRIMARY KEY (student_id),
    CONSTRAINT uq_students_email UNIQUE (email),
    CONSTRAINT students_chk_1 CHECK (age >= 0)
    );

CREATE TABLE IF NOT EXISTS student_courses (
    course_id    BINARY(16)    NOT NULL,
    student_id   BINARY(16),
    course_name  VARCHAR(255)  NOT NULL,
    start_date   DATE          NOT NULL,
    end_date     DATE,
    created_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_student_courses PRIMARY KEY (course_id),
    CONSTRAINT fk_student_courses_student_id FOREIGN KEY (student_id)
    REFERENCES students(student_id),
    CONSTRAINT uq_student_course_name UNIQUE (student_id, course_name)
    );

CREATE TABLE IF NOT EXISTS student_courses_application_status (
    application_status_id BINARY(16) NOT NULL,
    course_id             BINARY(16) NOT NULL,
    status                VARCHAR(20) NOT NULL,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (application_status_id),
    CONSTRAINT uq_course_id UNIQUE (course_id),
    CONSTRAINT fk_status_course
    FOREIGN KEY (course_id)
    REFERENCES student_courses (course_id)
    ON DELETE CASCADE
    );
