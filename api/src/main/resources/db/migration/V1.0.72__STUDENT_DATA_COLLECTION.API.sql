DROP INDEX SDC_DUPLICATE_COLLECTION_ID_IDX;

CREATE INDEX SDC_DUPLICATE_COLLECTION_DUPLICATE_CODE_ID_IDX ON SDC_DUPLICATE (COLLECTION_ID, DUPLICATE_LEVEL_CODE);