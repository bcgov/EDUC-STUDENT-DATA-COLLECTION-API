INSERT INTO SDC_SCHOOL_COLLECTION_STATUS_CODE (SDC_SCHOOL_COLLECTION_STATUS_CODE, LABEL, DESCRIPTION,
                                               DISPLAY_ORDER, EFFECTIVE_DATE,
                                               EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                               UPDATE_DATE)
VALUES ('DUP_VRFD', 'In School Duplicates', 'School has resolved duplicates', '10',
        TO_DATE('20240415', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        CURRENT_TIMESTAMP,
        'API_STUDENT_DATA_COLLECTION', CURRENT_TIMESTAMP);

