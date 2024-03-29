--In SDC Schema
drop index sdc_coll_student_sdc_school_collection_student_status_code_idx;
drop index sdc_coll_student_sdc_school_collection_id_idx;
CREATE INDEX sdc_coll_student_sdc_school_collection_student_status_code_idx ON api_student_data_collection.sdc_school_collection_student USING btree (sdc_school_collection_student_status_code);
CREATE INDEX sdc_coll_student_sdc_school_collection_id_idx ON api_student_data_collection.sdc_school_collection_student USING btree (sdc_school_collection_id);

