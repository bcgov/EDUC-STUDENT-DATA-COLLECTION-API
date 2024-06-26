ALTER TABLE sdc_duplicate_student ALTER COLUMN sdc_district_collection_id DROP NOT NULL;
DELETE from sdc_duplicate_student;
DELETE from sdc_duplicate;
ALTER TABLE sdc_duplicate add collection_id UUID NOT NULL CONSTRAINT FK_COLLECTION_ID foreign key (COLLECTION_ID) references COLLECTION (COLLECTION_ID);