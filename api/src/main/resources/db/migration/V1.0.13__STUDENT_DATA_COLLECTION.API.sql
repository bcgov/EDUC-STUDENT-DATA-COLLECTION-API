ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT
ADD COLUMN FTE NUMERIC(5,4);

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT
ADD COLUMN FTE_ZERO_REASON_CODE VARCHAR(10);

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT
ADD COLUMN FRENCH_PROGRAM_NON_ELIG_REASON_CODE VARCHAR(10);

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT
ADD COLUMN ELL_NON_ELIG_REASON_CODE VARCHAR(10);

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT
ADD COLUMN INDIGENOUS_SUPPORT_PROGRAM_NON_ELIG_REASON_CODE VARCHAR(10);

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT
ADD COLUMN CAREER_PROGRAM_NON_ELIG_REASON_CODE VARCHAR(10);

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT
ADD COLUMN SPECIAL_EDUCATION_NON_ELIG_REASON_CODE VARCHAR(10);

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT
ADD COLUMN IS_GRADUATED BOOLEAN;

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT_HISTORY
ADD COLUMN FTE NUMERIC(5,4);

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT_HISTORY
ADD COLUMN FTE_ZERO_REASON_CODE VARCHAR(10);

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT_HISTORY
ADD COLUMN FRENCH_PROGRAM_NON_ELIG_REASON_CODE VARCHAR(10);

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT_HISTORY
ADD COLUMN ELL_NON_ELIG_REASON_CODE VARCHAR(10);

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT_HISTORY
ADD COLUMN INDIGENOUS_SUPPORT_PROGRAM_NON_ELIG_REASON_CODE VARCHAR(10);

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT_HISTORY
ADD COLUMN CAREER_PROGRAM_NON_ELIG_REASON_CODE VARCHAR(10);

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT_HISTORY
ADD COLUMN SPECIAL_EDUCATION_NON_ELIG_REASON_CODE VARCHAR(10);

ALTER TABLE SDC_SCHOOL_COLLECTION_STUDENT_HISTORY
ADD COLUMN IS_GRADUATED BOOLEAN;
