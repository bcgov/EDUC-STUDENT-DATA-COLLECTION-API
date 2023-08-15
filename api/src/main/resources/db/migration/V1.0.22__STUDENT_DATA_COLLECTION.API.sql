INSERT INTO SDC_SCHOOL_COLLECTION_STUDENT_STATUS_CODE (SDC_SCHOOL_COLLECTION_STUDENT_STATUS_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                       EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('DELETED', 'Deleted', 'Student record has been removed from the Student Data Collection.', '10',
        TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        TO_DATE('20230210', 'YYYYMMDD'),
        'API_STUDENT_DATA_COLLECTION', TO_DATE('20230210', 'YYYYMMDD'));
