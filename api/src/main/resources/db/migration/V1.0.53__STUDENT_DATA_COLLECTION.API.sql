CREATE TABLE duplicate_resolution_code
(
    duplicate_resolution_code   varchar(10) NOT NULL,
    label                       varchar(30) NOT NULL,
    description                 varchar(255) NOT NULL,
    display_order               numeric NOT NULL,
    effective_date              timestamp NOT NULL,
    expiry_date                 timestamp NOT NULL,
    create_user                 varchar(100) NOT NULL,
    create_date                 timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_user                 varchar(100) NOT NULL,
    update_date                 timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL
);

INSERT INTO duplicate_resolution_code (duplicate_resolution_code, label, description, display_order, effective_date,
                                       expiry_date, create_user, update_user)
VALUES ('RELEASED', 'Released', 'Student released from school.', 10, to_date('2023-02-10 00:00:00', 'YYYY-MM-DD HH24:MI:SS'),
        to_date('9999-12-31 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'API_STUDENT_DATA_COLLECTION', 'API_STUDENT_DATA_COLLECTION');

INSERT INTO duplicate_resolution_code (duplicate_resolution_code, label, description, display_order, effective_date,
                                       expiry_date, create_user, update_user)
VALUES ('GRADE_CHNG', 'Grade Change', 'The students grade was changed.', 10, to_date('2023-02-10 00:00:00', 'YYYY-MM-DD HH24:MI:SS'),
        to_date('9999-12-31 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'API_STUDENT_DATA_COLLECTION', 'API_STUDENT_DATA_COLLECTION');

INSERT INTO duplicate_resolution_code (duplicate_resolution_code, label, description, display_order, effective_date,
                                       expiry_date, create_user, update_user)
VALUES ('RESOLVED', 'Resolved', 'Student removed from conflicting program.', 10, to_date('2023-02-10 00:00:00', 'YYYY-MM-DD HH24:MI:SS'),
        to_date('9999-12-31 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'API_STUDENT_DATA_COLLECTION', 'API_STUDENT_DATA_COLLECTION');