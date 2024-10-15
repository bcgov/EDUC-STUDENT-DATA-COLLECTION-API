INSERT INTO GENDER_CODE (GENDER_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                         EXPIRY_DATE,
                         CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('X', 'Gender Diverse',
        'Persons whose current gender is not exclusively as male or female. It includes people who do not have one gender, have no gender, are non-binary, or are Two-Spirit.',
        3, to_date('2020-01-01', 'YYYY-MM-DD'), to_date('2099-12-31', 'YYYY-MM-DD'), 'IDIR/DSO',
        to_date('2019-11-07', 'YYYY-MM-DD'), 'IDIR/DSO', to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO GENDER_CODE (GENDER_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                         EXPIRY_DATE,
                         CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('U', 'Unknown',
        'Persons whose gender is not known at the time of data collection. It may or may not get updated at a later point in time. X is different than U.',
        4, to_date('2020-01-01', 'YYYY-MM-DD'), to_date('2099-12-31', 'YYYY-MM-DD'), 'IDIR/DSO',
        to_date('2019-11-07', 'YYYY-MM-DD'), 'IDIR/DSO', to_date('2019-11-07', 'YYYY-MM-DD'));

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT
    ADD COLUMN UNDER_REVIEW_ASSIGNED_STUDENT_ID UUID;

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT
    ADD COLUMN UNDER_REVIEW_ASSIGNED_PEN VARCHAR(10);

