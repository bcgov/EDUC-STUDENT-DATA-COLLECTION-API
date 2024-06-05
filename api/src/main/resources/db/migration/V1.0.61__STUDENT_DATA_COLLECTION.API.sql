UPDATE SDC_SCHOOL_COLLECTION_STATUS_CODE
SET LABEL = 'Submitted to District',
    DESCRIPTION = 'School collection has been submitted to the district'
WHERE SDC_SCHOOL_COLLECTION_STATUS_CODE = 'SUBMITTED';

