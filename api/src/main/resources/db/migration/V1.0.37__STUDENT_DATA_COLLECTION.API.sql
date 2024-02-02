INSERT INTO EDX_ROLE (EDX_ROLE_CODE, LABEL, IS_DISTRICT_ROLE, DESCRIPTION, CREATE_USER, UPDATE_USER)
VALUES ('SCHOOL_SDC', 'Student Data Collection', TRUE, 'Student Data Collection (1701) role for School.', 'IDIR/CMCDERMI', 'IDIR/CMCDERMI')

INSERT INTO EDX_PERMISSION (EDX_PERMISSION_CODE, LABEL, DESCRIPTION, CREATE_USER, UPDATE_USER)
VALUES ('SCHOOL_SDC', 'Student Data Collection', 'Student Data Collection (1701) permission for School.', 'IDIR/CMCDERMI', 'IDIR/CMCDERMI')

UPDATE EDX_USER_SCHOOL_ROLE
SET EDX_ROLE_CODE = 'SCHOOL_SDC'
WHERE EDX_ROLE_CODE = 'STUDENT_DATA_COLLECTION';

UPDATE EDX_ROLE_PERMISSION
SET EDX_ROLE_CODE = 'SCHOOL_SDC',
    EDX_PERMISSION_CODE = 'SCHOOL_SDC',
    UPDATE_USER = 'IDIR/CMCDERMI'
WHERE EDX_ROLE_CODE = 'STUDENT_DATA_COLLECTION' AND EDX_PERMISSION_CODE = 'STUDENT_DATA_COLLECTION';

UPDATE EDX_ROLE_PERMISSION
SET EDX_ROLE_CODE = 'SCHOOL_SDC',
    UPDATE_USER = 'IDIR/CMCDERMI'
WHERE EDX_ROLE_CODE = 'STUDENT_DATA_COLLECTION' AND EDX_PERMISSION_CODE = 'EDX_USER_SCHOOL_ADMIN';

DELETE FROM EDX_ROLE
WHERE EDX_ROLE_CODE = 'STUDENT_DATA_COLLECTION';

DELETE FROM EDX_PERMISSION
WHERE EDX_PERMISSION_CODE = 'STUDENT_DATA_COLLECTION';

INSERT INTO EDX_ROLE (EDX_ROLE_CODE, LABEL, IS_DISTRICT_ROLE, DESCRIPTION, CREATE_USER, UPDATE_USER)
VALUES ('DISTRICT_SDC', 'Student Data Collection', TRUE, 'Student Data Collection (1701) role for District.', 'IDIR/CMCDERMI', 'IDIR/CMCDERMI')

INSERT INTO EDX_PERMISSION (EDX_PERMISSION_CODE, LABEL, DESCRIPTION, CREATE_USER, UPDATE_USER)
VALUES ('DISTRICT_SDC', 'Student Data Collection', 'Student Data Collection (1701) permission for District.', 'IDIR/CMCDERMI', 'IDIR/CMCDERMI')

INSERT INTO EDX_ROLE_PERMISSION (EDX_ROLE_PERMISSION_ID, EDX_ROLE_CODE, EDX_PERMISSION_CODE, CREATE_USER, UPDATE_USER)
VALUES (gen_random_uuid(), 'DISTRICT_SDC', 'DISTRICT_SDC', 'IDIR/CMCDERMI', 'IDIR/CMCDERMI')