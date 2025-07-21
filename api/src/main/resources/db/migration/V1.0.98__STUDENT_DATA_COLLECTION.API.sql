UPDATE HOME_LANGUAGE_SPOKEN_CODE
SET
    LABEL = 'Swahili',
    DESCRIPTION = 'Swahili',
    UPDATE_USER = 'API_STUDENT_DATA_COLLECTION',
    UPDATE_DATE = TO_DATE('20250721', 'YYYYMMDD')
WHERE
    HOME_LANGUAGE_SPOKEN_CODE = '088';

ALTER TABLE sdc_school_collection_student_enrolled_program
    DROP CONSTRAINT unique_enrolled_program_student;
