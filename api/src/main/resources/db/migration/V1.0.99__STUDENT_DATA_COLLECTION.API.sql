CREATE INDEX idx_student_collection_grade_fte
    ON sdc_school_collection_student (
                                      sdc_school_collection_id,
                                      sdc_school_collection_student_status_code,
                                      enrolled_grade_code,
                                      is_adult,
                                      fte,
                                      sdc_school_collection_student_id
        ) WHERE sdc_school_collection_student_status_code NOT IN ('ERROR', 'DELETED');

CREATE INDEX idx_enrolled_program_student_code
    ON sdc_school_collection_student_enrolled_program (
                                                       sdc_school_collection_student_id,
                                                       enrolled_program_code
        );