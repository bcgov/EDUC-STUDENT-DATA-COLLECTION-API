INSERT INTO SDC_DISTRICT_COLLECTION_STATUS_CODE (SDC_DISTRICT_COLLECTION_STATUS_CODE, LABEL, DESCRIPTION,
                                               DISPLAY_ORDER, EFFECTIVE_DATE,
                                               EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                               UPDATE_DATE)
VALUES ('VERIFIED', 'Verified', 'District collection has been verified', '40',
        TO_DATE('20240415', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        CURRENT_TIMESTAMP,
        'API_STUDENT_DATA_COLLECTION', CURRENT_TIMESTAMP);

INSERT INTO SDC_DISTRICT_COLLECTION_STATUS_CODE (SDC_DISTRICT_COLLECTION_STATUS_CODE, LABEL, DESCRIPTION,
                                                 DISPLAY_ORDER, EFFECTIVE_DATE,
                                                 EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                 UPDATE_DATE)
VALUES ('D_DUP_VRFD', 'District duplicates verified', 'The in district duplicates have been verified', '50',
        TO_DATE('20240415', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        CURRENT_TIMESTAMP,
        'API_STUDENT_DATA_COLLECTION', CURRENT_TIMESTAMP);