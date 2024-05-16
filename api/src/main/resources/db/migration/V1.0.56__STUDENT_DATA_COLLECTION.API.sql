INSERT INTO SDC_SCHOOL_COLLECTION_STATUS_CODE (SDC_SCHOOL_COLLECTION_STATUS_CODE, LABEL, DESCRIPTION,
                                               DISPLAY_ORDER, EFFECTIVE_DATE,
                                               EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                               UPDATE_DATE)
VALUES ('DIS_UPLOAD', 'District Upload', 'File uploaded by district', '10',
        TO_DATE('20240516', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        TO_DATE('20240516', 'YYYYMMDD'),
        'API_STUDENT_DATA_COLLECTION', TO_DATE('20240516', 'YYYYMMDD'));