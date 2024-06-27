ALTER TABLE sdc_duplicate_student ALTER COLUMN sdc_district_collection_id DROP NOT NULL;

ALTER TABLE sdc_duplicate
    ADD COLUMN collection_id UUID,
ADD CONSTRAINT FK_COLLECTION_ID FOREIGN KEY (collection_id) REFERENCES COLLECTION (COLLECTION_ID);

ALTER TABLE sdc_duplicate ALTER COLUMN collection_id SET NOT NULL;
