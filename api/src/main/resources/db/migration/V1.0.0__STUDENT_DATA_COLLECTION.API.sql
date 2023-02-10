CREATE TABLE COLLECTION
(
    COLLECTION_ID        UUID                                NOT NULL,
    COLLECTION_CODE VARCHAR(10)                         NOT NULL,
    OPEN_DATE            TIMESTAMP                           NOT NULL,
    CLOSE_DATE           TIMESTAMP                           NOT NULL,
    CREATE_USER          VARCHAR(32)                         NOT NULL,
    CREATE_DATE          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER          VARCHAR(32)                         NOT NULL,
    UPDATE_DATE          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT COLLECTION_ID_PK PRIMARY KEY (COLLECTION_ID)
);

CREATE TABLE COLLECTION_CODE
(
    COLLECTION_CODE VARCHAR(10)                         NOT NULL,
    LABEL                       VARCHAR(30)                         NOT NULL,
    DESCRIPTION                 VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER               NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE              TIMESTAMP                           NOT NULL,
    EXPIRY_DATE                 TIMESTAMP                           NOT NULL,
    OPEN_DATE TIMESTAMP NOT NULL,
    CLOSE_DATE TIMESTAMP NOT NULL,
    CREATE_USER                 VARCHAR(32)                         NOT NULL,
    CREATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                 VARCHAR(32)                         NOT NULL,
    UPDATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT COLLECTION_CODE_PK PRIMARY KEY (COLLECTION_CODE)
);

CREATE TABLE COLLECTION_CODE_CRITERIA
(
    COLLECTION_CODE_CRITERIA_ID UUID                         NOT NULL,
    COLLECTION_CODE                       VARCHAR(10)                         NOT NULL,
    SCHOOL_CATEGORY_CODE VARCHAR(10),
    FACILITY_TYPE_CODE                 VARCHAR(10)                        ,
    REPORTING_REQUIREMENT_CODE VARCHAR(10) NOT NULL,
    CREATE_USER                 VARCHAR(32)                         NOT NULL,
    CREATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                 VARCHAR(32)                         NOT NULL,
    UPDATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT COLLECTION_CODE_CRITERIA_PK PRIMARY KEY (COLLECTION_CODE_CRITERIA_ID)
);

CREATE TABLE STUDENT_DATA_COLLECTION_SCHOOL
(
    SDC_SCHOOL_ID               UUID                                NOT NULL,
    COLLECTION_ID               UUID                                NOT NULL,
    SCHOOL_ID                   UUID                                NOT NULL,
    UPLOAD_DATE                 TIMESTAMP,
    COLLECTION_STATUS_TYPE_CODE VARCHAR(10)                         NOT NULL,
    CREATE_USER                 VARCHAR(32)                         NOT NULL,
    CREATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                 VARCHAR(32)                         NOT NULL,
    UPDATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT SDC_SCHOOL_ID_PK PRIMARY KEY (SDC_SCHOOL_ID)
);

CREATE TABLE STUDENT_DATA_COLLECTION_SCHOOL_HISTORY
(
    SDC_SCHOOL_HISTORY_ID       UUID                                NOT NULL,
    SDC_SCHOOL_ID               UUID                                NOT NULL,
    COLLECTION_ID               UUID                                NOT NULL,
    SCHOOL_ID                   UUID                                NOT NULL,
    UPLOAD_DATE                 TIMESTAMP,
    COLLECTION_STATUS_TYPE_CODE VARCHAR(10)                         NOT NULL,
    CREATE_USER                 VARCHAR(32)                         NOT NULL,
    CREATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                 VARCHAR(32)                         NOT NULL,
    UPDATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT SDC_SCHOOL_HISTORY_ID_PK PRIMARY KEY (SDC_SCHOOL_HISTORY_ID)
);

CREATE TABLE COLLECTION_STATUS_TYPE_CODE
(
    COLLECTION_STATUS_TYPE_CODE VARCHAR(10)                         NOT NULL,
    LABEL                       VARCHAR(30)                         NOT NULL,
    DESCRIPTION                 VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER               NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE              TIMESTAMP                           NOT NULL,
    EXPIRY_DATE                 TIMESTAMP                           NOT NULL,
    CREATE_USER                 VARCHAR(32)                         NOT NULL,
    CREATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                 VARCHAR(32)                         NOT NULL,
    UPDATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT COLLECTION_STATUS_TYPE_CODE_PK PRIMARY KEY (COLLECTION_STATUS_TYPE_CODE)
);

CREATE TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT
(
    SDC_SCHOOL_STUDENT_ID           UUID                                NOT NULL,
    SDC_SCHOOL_ID                   UUID                                NOT NULL,
    LOCAL_ID                        VARCHAR(12),
    STUDENT_PEN                     VARCHAR(10),
    LEGAL_FIRST_NAME                VARCHAR(255),
    LEGAL_MIDDLE_NAMES              VARCHAR(255),
    LEGAL_LAST_NAME                 VARCHAR(255),
    USUAL_FIRST_NAME                VARCHAR(255),
    USUAL_MIDDLE_NAMES              VARCHAR(255),
    USUAL_LAST_NAME                 VARCHAR(255),
    DOB                             VARCHAR(8),
    GENDER_TYPE_CODE                     VARCHAR(1),
    GRADE_TYPE_CODE                      VARCHAR(10),
    SPECIAL_EDUCATION_CATEGORY_TYPE_CODE VARCHAR(10),
    SCHOOL_FUNDING_TYPE_CODE             VARCHAR(10),
    NATIVE_INDIAN_ANCESTRY_IND      BOOLEAN,
    HOME_LANGUAGE_SPOKEN_TYPE_CODE       VARCHAR(10),
    OTHER_COURSES                   NUMERIC,
    SUPPORT_BLOCKS                  NUMERIC,
    ENROLLED_GRADE_TYPE_CODE             VARCHAR(10),
    ENROLLED_PROGRAM_TYPE_CODE           VARCHAR(10),
    CAREER_PROGRAM_TYPE_CODE VARCHAR(10),
    NUMBER_OF_COURSES               NUMERIC,
    BAND_TYPE_CODE                       VARCHAR(4),
    POSTAL_CODE                     VARCHAR(6),
    STATUS_TYPE_CODE                     VARCHAR(10),
    CREATE_USER                     VARCHAR(32)                         NOT NULL,
    CREATE_DATE                     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                     VARCHAR(32)                         NOT NULL,
    UPDATE_DATE                     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT SDC_SCHOOL_STUDENT_ID_PK PRIMARY KEY (SDC_SCHOOL_STUDENT_ID)
);

CREATE TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT_HISTORY
(
    SDC_SCHOOL_STUDENT_HISTORY_ID   UUID                                NOT NULL,
    SDC_SCHOOL_STUDENT_ID           UUID                                NOT NULL,
    SDC_SCHOOL_ID                   UUID                                NOT NULL,
    LOCAL_ID                        VARCHAR(12),
    STUDENT_PEN                     VARCHAR(10),
    LEGAL_FIRST_NAME                VARCHAR(255),
    LEGAL_MIDDLE_NAMES              VARCHAR(255),
    LEGAL_LAST_NAME                 VARCHAR(255),
    USUAL_FIRST_NAME                VARCHAR(255),
    USUAL_MIDDLE_NAMES              VARCHAR(255),
    USUAL_LAST_NAME                 VARCHAR(255),
    DOB                             VARCHAR(8),
    GENDER_TYPE_CODE                     VARCHAR(1),
    GRADE_TYPE_CODE                      VARCHAR(10),
    SPECIAL_EDUCATION_CATEGORY_TYPE_CODE VARCHAR(10),
    SCHOOL_FUNDING_TYPE_CODE             VARCHAR(10),
    clockNATIVE_INDIAN_ANCESTRY_IND      BOOLEAN,
    HOME_LANGUAGE_SPOKEN_TYPE_CODE       VARCHAR(10),
    OTHER_COURSES                   NUMERIC,
    SUPPORT_BLOCKS                  NUMERIC,
    ENROLLED_GRADE_TYPE_CODE             VARCHAR(10),
    ENROLLED_PROGRAM_TYPE_CODE           VARCHAR(10),
    CAREER_PROGRAM_TYPE_CODE VARCHAR(10),
    NUMBER_OF_COURSES               NUMERIC,
    BAND_TYPE_CODE                       VARCHAR(4),
    POSTAL_CODE                     VARCHAR(6),
    STATUS_TYPE_CODE                     VARCHAR(10),
    CREATE_USER                     VARCHAR(32)                         NOT NULL,
    CREATE_DATE                     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                     VARCHAR(32)                         NOT NULL,
    UPDATE_DATE                     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT SDC_SCHOOL_STUDENT_HISTORY_ID_PK PRIMARY KEY (SDC_SCHOOL_STUDENT_HISTORY_ID)
);

CREATE TABLE STUDENT_DATA_COLLECTION_STUDENT_VALIDATION_ISSUE
(
    SDC_STUDENT_VALIDATION_ISSUE_ID     UUID                                NOT NULL,
    SDC_SCHOOL_STUDENT_ID               UUID                                NOT NULL,
    VALIDATION_ISSUE_SEVERITY_CODE VARCHAR(10)                         NOT NULL,
    VALIDATION_ISSUE_CODE          VARCHAR(10)                         NOT NULL,
    VALIDATION_ISSUE_FIELD_CODE    VARCHAR(10)                         NOT NULL,
    CREATE_USER                         VARCHAR(32)                         NOT NULL,
    CREATE_DATE                         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                         VARCHAR(32)                         NOT NULL,
    UPDATE_DATE                         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT SDC_STUDENT_VALIDATION_ISSUE_ID_PK PRIMARY KEY (SDC_STUDENT_VALIDATION_ISSUE_ID)
);

CREATE TABLE GENDER_TYPE_CODE
(
    GENDER_TYPE_CODE VARCHAR(10)                         NOT NULL,
    LABEL            VARCHAR(30)                         NOT NULL,
    DESCRIPTION      VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER    NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE   TIMESTAMP                           NOT NULL,
    EXPIRY_DATE      TIMESTAMP                           NOT NULL,
    CREATE_USER      VARCHAR(32)                         NOT NULL,
    CREATE_DATE      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER      VARCHAR(32)                         NOT NULL,
    UPDATE_DATE      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT GENDER_TYPE_CODE_PK PRIMARY KEY (GENDER_TYPE_CODE)
);

CREATE TABLE SPECIAL_EDUCATION_CATEGORY_TYPE_CODE
(
    SPECIAL_EDUCATION_CATEGORY_TYPE_CODE VARCHAR(10)                         NOT NULL,
    LABEL                                VARCHAR(30)                         NOT NULL,
    DESCRIPTION                          VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER                        NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE                       TIMESTAMP                           NOT NULL,
    EXPIRY_DATE                          TIMESTAMP                           NOT NULL,
    CREATE_USER                          VARCHAR(32)                         NOT NULL,
    CREATE_DATE                          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                          VARCHAR(32)                         NOT NULL,
    UPDATE_DATE                          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT SPECIAL_EDUCATION_CATEGORY_TYPE_CODE_PK PRIMARY KEY (SPECIAL_EDUCATION_CATEGORY_TYPE_CODE)
);

CREATE TABLE SCHOOL_FUNDING_TYPE_CODE
(
    SCHOOL_FUNDING_TYPE_CODE VARCHAR(10)                         NOT NULL,
    LABEL                    VARCHAR(30)                         NOT NULL,
    DESCRIPTION              VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER            NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE           TIMESTAMP                           NOT NULL,
    EXPIRY_DATE              TIMESTAMP                           NOT NULL,
    CREATE_USER              VARCHAR(32)                         NOT NULL,
    CREATE_DATE              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER              VARCHAR(32)                         NOT NULL,
    UPDATE_DATE              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT SCHOOL_FUNDING_TYPE_CODE_PK PRIMARY KEY (SCHOOL_FUNDING_TYPE_CODE)
);

CREATE TABLE HOME_LANGUAGE_SPOKEN_TYPE_CODE
(
    HOME_LANGUAGE_SPOKEN_TYPE_CODE VARCHAR(10)                         NOT NULL,
    LABEL                    VARCHAR(30)                         NOT NULL,
    DESCRIPTION              VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER            NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE           TIMESTAMP                           NOT NULL,
    EXPIRY_DATE              TIMESTAMP                           NOT NULL,
    CREATE_USER              VARCHAR(32)                         NOT NULL,
    CREATE_DATE              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER              VARCHAR(32)                         NOT NULL,
    UPDATE_DATE              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT HOME_LANGUAGE_SPOKEN_TYPE_CODE_PK PRIMARY KEY (HOME_LANGUAGE_SPOKEN_TYPE_CODE)
);

CREATE TABLE ENROLLED_GRADE_TYPE_CODE
(
    ENROLLED_GRADE_TYPE_CODE VARCHAR(10)                         NOT NULL,
    LABEL                    VARCHAR(30)                         NOT NULL,
    DESCRIPTION              VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER            NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE           TIMESTAMP                           NOT NULL,
    EXPIRY_DATE              TIMESTAMP                           NOT NULL,
    CREATE_USER              VARCHAR(32)                         NOT NULL,
    CREATE_DATE              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER              VARCHAR(32)                         NOT NULL,
    UPDATE_DATE              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ENROLLED_GRADE_TYPE_CODE_PK PRIMARY KEY (ENROLLED_GRADE_TYPE_CODE)
);

CREATE TABLE ENROLLED_PROGRAM_TYPE_CODE
(
    ENROLLED_PROGRAM_TYPE_CODE VARCHAR(10)                         NOT NULL,
    LABEL                      VARCHAR(30)                         NOT NULL,
    DESCRIPTION                VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER              NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE             TIMESTAMP                           NOT NULL,
    EXPIRY_DATE                TIMESTAMP                           NOT NULL,
    CREATE_USER                VARCHAR(32)                         NOT NULL,
    CREATE_DATE                TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                VARCHAR(32)                         NOT NULL,
    UPDATE_DATE                TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ENROLLED_PROGRAM_TYPE_CODE_PK PRIMARY KEY (ENROLLED_PROGRAM_TYPE_CODE)
);

CREATE TABLE CAREER_PROGRAM_TYPE_CODE
(
    CAREER_PROGRAM_TYPE_CODE VARCHAR(10)                         NOT NULL,
    LABEL                    VARCHAR(30)                         NOT NULL,
    DESCRIPTION              VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER            NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE           TIMESTAMP                           NOT NULL,
    EXPIRY_DATE              TIMESTAMP                           NOT NULL,
    CREATE_USER              VARCHAR(32)                         NOT NULL,
    CREATE_DATE              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER              VARCHAR(32)                         NOT NULL,
    UPDATE_DATE              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT CAREER_PROGRAM_TYPE_CODE_PK PRIMARY KEY (CAREER_PROGRAM_TYPE_CODE)
);

CREATE TABLE BAND_TYPE_CODE
(
    BAND_TYPE_CODE VARCHAR(10)                         NOT NULL,
    LABEL                    VARCHAR(30)                         NOT NULL,
    DESCRIPTION              VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER            NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE           TIMESTAMP                           NOT NULL,
    EXPIRY_DATE              TIMESTAMP                           NOT NULL,
    CREATE_USER              VARCHAR(32)                         NOT NULL,
    CREATE_DATE              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER              VARCHAR(32)                         NOT NULL,
    UPDATE_DATE              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT BAND_TYPE_CODE_PK PRIMARY KEY (BAND_TYPE_CODE)
);

CREATE TABLE STATUS_TYPE_CODE
(
    STATUS_TYPE_CODE VARCHAR(10)                         NOT NULL,
    LABEL            VARCHAR(30)                         NOT NULL,
    DESCRIPTION      VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER    NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE   TIMESTAMP                           NOT NULL,
    EXPIRY_DATE      TIMESTAMP                           NOT NULL,
    CREATE_USER      VARCHAR(32)                         NOT NULL,
    CREATE_DATE      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER      VARCHAR(32)                         NOT NULL,
    UPDATE_DATE      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT STATUS_TYPE_CODE_PK PRIMARY KEY (STATUS_TYPE_CODE)
);

CREATE TABLE RECIPROCAL_TUITION_COLLECTION_SCHOOL
(
    RTC_SCHOOL_ID               UUID                                NOT NULL,
    COLLECTION_ID               UUID                                NOT NULL,
    SCHOOL_ID                   UUID                                NOT NULL,
    UPLOAD_DATE                 TIMESTAMP,
    COLLECTION_STATUS_TYPE_CODE VARCHAR(10)                         NOT NULL,
    CREATE_USER                 VARCHAR(32)                         NOT NULL,
    CREATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                 VARCHAR(32)                         NOT NULL,
    UPDATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT RTC_SCHOOL_ID_PK PRIMARY KEY (RTC_SCHOOL_ID)
);

CREATE TABLE RECIPROCAL_TUITION_COLLECTION_SCHOOL_HISTORY
(
    RTC_SCHOOL_HISTORY_ID       UUID                                NOT NULL,
    RTC_SCHOOL_ID               UUID                                NOT NULL,
    COLLECTION_ID               UUID                                NOT NULL,
    SCHOOL_ID                   UUID                                NOT NULL,
    UPLOAD_DATE                 TIMESTAMP,
    COLLECTION_STATUS_TYPE_CODE VARCHAR(10)                         NOT NULL,
    CREATE_USER                 VARCHAR(32)                         NOT NULL,
    CREATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                 VARCHAR(32)                         NOT NULL,
    UPDATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT RTC_SCHOOL_HISTORY_ID_PK PRIMARY KEY (RTC_SCHOOL_HISTORY_ID)
);

CREATE TABLE RECIPROCAL_TUITION_COLLECTION_SCHOOL_STUDENT
(
    RTC_SCHOOL_STUDENT_ID UUID                                NOT NULL,
    RTC_SCHOOL_ID         UUID                                NOT NULL,
    LOCAL_ID              VARCHAR(12),
    STUDENT_PEN           VARCHAR(10),
    LEGAL_FIRST_NAME      VARCHAR(255),
    LEGAL_MIDDLE_NAMES    VARCHAR(255),
    LEGAL_LAST_NAME       VARCHAR(255),
    USUAL_FIRST_NAME      VARCHAR(255),
    USUAL_MIDDLE_NAMES    VARCHAR(255),
    USUAL_LAST_NAME       VARCHAR(255),
    DOB                   VARCHAR(8),
    GENDER_TYPE_CODE           VARCHAR(1),
    POSTAL_CODE           VARCHAR(6),
    STATUS_TYPE_CODE           VARCHAR(10),
    CREATE_USER           VARCHAR(32)                         NOT NULL,
    CREATE_DATE           TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER           VARCHAR(32)                         NOT NULL,
    UPDATE_DATE           TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT RTC_SCHOOL_STUDENT_ID_PK PRIMARY KEY (RTC_SCHOOL_STUDENT_ID)
);

CREATE TABLE RECIPROCAL_TUITION_COLLECTION_SCHOOL_STUDENT_HISTORY
(
    RTC_SCHOOL_STUDENT_HISTORY_ID UUID                                NOT NULL,
    RTC_SCHOOL_STUDENT_ID         UUID                                NOT NULL,
    RTC_SCHOOL_ID                 UUID                                NOT NULL,
    LOCAL_ID                      VARCHAR(12),
    STUDENT_PEN                   VARCHAR(10),
    LEGAL_FIRST_NAME              VARCHAR(255),
    LEGAL_MIDDLE_NAMES            VARCHAR(255),
    LEGAL_LAST_NAME               VARCHAR(255),
    USUAL_FIRST_NAME              VARCHAR(255),
    USUAL_MIDDLE_NAMES            VARCHAR(255),
    USUAL_LAST_NAME               VARCHAR(255),
    DOB                           VARCHAR(8),
    GENDER_TYPE_CODE                   VARCHAR(1),
    POSTAL_CODE                   VARCHAR(6),
    STATUS_TYPE_CODE                   VARCHAR(10),
    CREATE_USER                   VARCHAR(32)                         NOT NULL,
    CREATE_DATE                   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                   VARCHAR(32)                         NOT NULL,
    UPDATE_DATE                   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT RTC_SCHOOL_STUDENT_HISTORY_ID_PK PRIMARY KEY (RTC_SCHOOL_STUDENT_HISTORY_ID)
);

CREATE TABLE RECIPROCAL_TUITION_COLLECTION_STUDENT_VALIDATION_ISSUE
(
    RTC_STUDENT_VALIDATION_ISSUE_ID     UUID                                NOT NULL,
    RTC_SCHOOL_STUDENT_ID               UUID                                NOT NULL,
    VALIDATION_ISSUE_SEVERITY_CODE VARCHAR(10)                         NOT NULL,
    VALIDATION_ISSUE_CODE          VARCHAR(10)                         NOT NULL,
    VALIDATION_ISSUE_FIELD_CODE    VARCHAR(10)                         NOT NULL,
    CREATE_USER                         VARCHAR(32)                         NOT NULL,
    CREATE_DATE                         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                         VARCHAR(32)                         NOT NULL,
    UPDATE_DATE                         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT RTC_STUDENT_VALIDATION_ISSUE_ID_PK PRIMARY KEY (RTC_STUDENT_VALIDATION_ISSUE_ID)
);

-- Collection foreign keys
ALTER TABLE COLLECTION
    ADD CONSTRAINT FK_COLLECTION__COLLECTION_CODE FOREIGN KEY (COLLECTION_CODE)
        REFERENCES COLLECTION_CODE (COLLECTION_CODE);

-- Collection Code Criteria foreign keys
ALTER TABLE COLLECTION_CODE_CRITERIA
    ADD CONSTRAINT FK_COLLECTION_CODE_CRITERIA_COLLECTION_CODE FOREIGN KEY (COLLECTION_CODE)
        REFERENCES COLLECTION_CODE (COLLECTION_CODE);

-- SDC school and school history foreign keys
ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL
    ADD CONSTRAINT FK_SDC_SCHOOL_COLLECTION_ID FOREIGN KEY (COLLECTION_ID)
        REFERENCES COLLECTION (COLLECTION_ID);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL
    ADD CONSTRAINT FK_SDC_SCHOOL_COLLECTION_STATUS_TYPE_CODE FOREIGN KEY (COLLECTION_STATUS_TYPE_CODE)
        REFERENCES COLLECTION_STATUS_TYPE_CODE (COLLECTION_STATUS_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_HISTORY
    ADD CONSTRAINT FK_SDC_SCHOOL_HISTORY_SDC_SCHOOL_ID FOREIGN KEY (SDC_SCHOOL_ID)
        REFERENCES STUDENT_DATA_COLLECTION_SCHOOL (SDC_SCHOOL_ID);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_HISTORY
    ADD CONSTRAINT FK_SDC_SCHOOL_HISTORY_COLLECTION_ID FOREIGN KEY (COLLECTION_ID)
        REFERENCES COLLECTION (COLLECTION_ID);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_HISTORY
    ADD CONSTRAINT FK_SDC_SCHOOL_HISTORY_COLLECTION_STATUS_TYPE_CODE FOREIGN KEY (COLLECTION_STATUS_TYPE_CODE)
        REFERENCES COLLECTION_STATUS_TYPE_CODE (COLLECTION_STATUS_TYPE_CODE);

-- SDC school student and school student history foreign keys

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_SDC_SCHOOL_ID FOREIGN KEY (SDC_SCHOOL_ID)
        REFERENCES STUDENT_DATA_COLLECTION_SCHOOL (SDC_SCHOOL_ID);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_GENDER_TYPE_CODE FOREIGN KEY (GENDER_TYPE_CODE)
        REFERENCES GENDER_TYPE_CODE (GENDER_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_SPECIAL_EDUCATION_CATEGORY_TYPE_CODE FOREIGN KEY (SPECIAL_EDUCATION_CATEGORY_TYPE_CODE)
        REFERENCES SPECIAL_EDUCATION_CATEGORY_TYPE_CODE (SPECIAL_EDUCATION_CATEGORY_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_SCHOOL_FUNDING_TYPE_CODE FOREIGN KEY (SCHOOL_FUNDING_TYPE_CODE)
        REFERENCES SCHOOL_FUNDING_TYPE_CODE (SCHOOL_FUNDING_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_HOME_LANGUAGE_SPOKEN_TYPE_CODE FOREIGN KEY (HOME_LANGUAGE_SPOKEN_TYPE_CODE)
        REFERENCES HOME_LANGUAGE_SPOKEN_TYPE_CODE (HOME_LANGUAGE_SPOKEN_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_ENROLLED_GRADE_TYPE_CODE FOREIGN KEY (ENROLLED_GRADE_TYPE_CODE)
        REFERENCES ENROLLED_GRADE_TYPE_CODE (ENROLLED_GRADE_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_ENROLLED_PROGRAM_TYPE_CODE FOREIGN KEY (ENROLLED_PROGRAM_TYPE_CODE)
        REFERENCES ENROLLED_PROGRAM_TYPE_CODE (ENROLLED_PROGRAM_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_CAREER_PROGRAM_TYPE_CODE FOREIGN KEY (CAREER_PROGRAM_TYPE_CODE)
        REFERENCES CAREER_PROGRAM_TYPE_CODE (CAREER_PROGRAM_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_BAND_TYPE_CODE FOREIGN KEY (BAND_TYPE_CODE)
        REFERENCES BAND_TYPE_CODE (BAND_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_STATUS_TYPE_CODE FOREIGN KEY (STATUS_TYPE_CODE)
        REFERENCES STATUS_TYPE_CODE (STATUS_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT_HISTORY
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_HISTORY_SDC_SCHOOL_STUDENT_ID FOREIGN KEY (SDC_SCHOOL_STUDENT_ID)
        REFERENCES STUDENT_DATA_COLLECTION_SCHOOL_STUDENT (SDC_SCHOOL_STUDENT_ID);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT_HISTORY
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_HISTORY_SDC_SCHOOL_ID FOREIGN KEY (SDC_SCHOOL_ID)
        REFERENCES STUDENT_DATA_COLLECTION_SCHOOL (SDC_SCHOOL_ID);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT_HISTORY
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_HISTORY_GENDER_TYPE_CODE FOREIGN KEY (GENDER_TYPE_CODE)
        REFERENCES GENDER_TYPE_CODE (GENDER_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT_HISTORY
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_HISTORY_SPECIAL_EDUCATION_CATEGORY_TYPE_CODE FOREIGN KEY (SPECIAL_EDUCATION_CATEGORY_TYPE_CODE)
        REFERENCES SPECIAL_EDUCATION_CATEGORY_TYPE_CODE (SPECIAL_EDUCATION_CATEGORY_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT_HISTORY
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_HISTORY_SCHOOL_FUNDING_TYPE_CODE FOREIGN KEY (SCHOOL_FUNDING_TYPE_CODE)
        REFERENCES SCHOOL_FUNDING_TYPE_CODE (SCHOOL_FUNDING_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT_HISTORY
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_HISTORY_ENROLLED_GRADE_TYPE_CODE FOREIGN KEY (ENROLLED_GRADE_TYPE_CODE)
        REFERENCES ENROLLED_GRADE_TYPE_CODE (ENROLLED_GRADE_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT_HISTORY
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_HISTORY_ENROLLED_PROGRAM_TYPE_CODE FOREIGN KEY (ENROLLED_PROGRAM_TYPE_CODE)
        REFERENCES ENROLLED_PROGRAM_TYPE_CODE (ENROLLED_PROGRAM_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT_HISTORY
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_HISTORY_CAREER_PROGRAM_TYPE_CODE FOREIGN KEY (CAREER_PROGRAM_TYPE_CODE)
        REFERENCES CAREER_PROGRAM_TYPE_CODE (CAREER_PROGRAM_TYPE_CODE);

ALTER TABLE STUDENT_DATA_COLLECTION_SCHOOL_STUDENT_HISTORY
    ADD CONSTRAINT FK_SDC_SCHOOL_STUDENT_HISTORY_STATUS_TYPE_CODE FOREIGN KEY (STATUS_TYPE_CODE)
        REFERENCES STATUS_TYPE_CODE (STATUS_TYPE_CODE);

-- SDC student validation issue foreign keys

ALTER TABLE STUDENT_DATA_COLLECTION_STUDENT_VALIDATION_ISSUE
    ADD CONSTRAINT FK_SDC_STUDENT_VALIDATION_ISSUE_SDC_SCHOOL_STUDENT_ID FOREIGN KEY (SDC_SCHOOL_STUDENT_ID)
        REFERENCES STUDENT_DATA_COLLECTION_SCHOOL_STUDENT (SDC_SCHOOL_STUDENT_ID);

-- RTC school and school history foreign keys
ALTER TABLE RECIPROCAL_TUITION_COLLECTION_SCHOOL
    ADD CONSTRAINT FK_RTC_SCHOOL_COLLECTION_ID FOREIGN KEY (COLLECTION_ID)
        REFERENCES COLLECTION (COLLECTION_ID);

ALTER TABLE RECIPROCAL_TUITION_COLLECTION_SCHOOL
    ADD CONSTRAINT FK_RTC_SCHOOL_COLLECTION_STATUS_TYPE_CODE FOREIGN KEY (COLLECTION_STATUS_TYPE_CODE)
        REFERENCES COLLECTION_STATUS_TYPE_CODE (COLLECTION_STATUS_TYPE_CODE);

ALTER TABLE RECIPROCAL_TUITION_COLLECTION_SCHOOL_HISTORY
    ADD CONSTRAINT FK_RTC_SCHOOL_HISTORY_RTC_SCHOOL_ID FOREIGN KEY (RTC_SCHOOL_ID)
        REFERENCES RECIPROCAL_TUITION_COLLECTION_SCHOOL (RTC_SCHOOL_ID);

ALTER TABLE RECIPROCAL_TUITION_COLLECTION_SCHOOL_HISTORY
    ADD CONSTRAINT FK_RTC_SCHOOL_HISTORY_COLLECTION_ID FOREIGN KEY (COLLECTION_ID)
        REFERENCES COLLECTION (COLLECTION_ID);

ALTER TABLE RECIPROCAL_TUITION_COLLECTION_SCHOOL_HISTORY
    ADD CONSTRAINT FK_RTC_SCHOOL_HISTORY_COLLECTION_STATUS_TYPE_CODE FOREIGN KEY (COLLECTION_STATUS_TYPE_CODE)
        REFERENCES COLLECTION_STATUS_TYPE_CODE (COLLECTION_STATUS_TYPE_CODE);

-- RTC school student and school student history foreign keys

ALTER TABLE RECIPROCAL_TUITION_COLLECTION_SCHOOL_STUDENT
    ADD CONSTRAINT FK_RTC_SCHOOL_STUDENT_GENDER_TYPE_CODE FOREIGN KEY (GENDER_TYPE_CODE)
        REFERENCES GENDER_TYPE_CODE (GENDER_TYPE_CODE);

ALTER TABLE RECIPROCAL_TUITION_COLLECTION_SCHOOL_STUDENT
    ADD CONSTRAINT FK_RTC_SCHOOL_STUDENT_STATUS_TYPE_CODE FOREIGN KEY (STATUS_TYPE_CODE)
        REFERENCES STATUS_TYPE_CODE (STATUS_TYPE_CODE);

ALTER TABLE RECIPROCAL_TUITION_COLLECTION_SCHOOL_STUDENT_HISTORY
    ADD CONSTRAINT FK_RTC_SCHOOL_STUDENT_HISTORY_RTC_SCHOOL_STUDENT_ID FOREIGN KEY (RTC_SCHOOL_STUDENT_ID)
        REFERENCES RECIPROCAL_TUITION_COLLECTION_SCHOOL_STUDENT (RTC_SCHOOL_STUDENT_ID);

ALTER TABLE RECIPROCAL_TUITION_COLLECTION_SCHOOL_STUDENT_HISTORY
    ADD CONSTRAINT FK_RTC_SCHOOL_STUDENT_HISTORY_GENDER_TYPE_CODE FOREIGN KEY (GENDER_TYPE_CODE)
        REFERENCES GENDER_TYPE_CODE (GENDER_TYPE_CODE);

ALTER TABLE RECIPROCAL_TUITION_COLLECTION_SCHOOL_STUDENT_HISTORY
    ADD CONSTRAINT FK_RTC_SCHOOL_STUDENT_HISTORY_STATUS_TYPE_CODE FOREIGN KEY (STATUS_TYPE_CODE)
        REFERENCES STATUS_TYPE_CODE (STATUS_TYPE_CODE);

-- RTC student validation issue foreign keys

ALTER TABLE RECIPROCAL_TUITION_COLLECTION_STUDENT_VALIDATION_ISSUE
    ADD CONSTRAINT FK_RTC_STUDENT_VALIDATION_ISSUE_RTC_SCHOOL_STUDENT_ID FOREIGN KEY (RTC_SCHOOL_STUDENT_ID)
        REFERENCES RECIPROCAL_TUITION_COLLECTION_SCHOOL_STUDENT (RTC_SCHOOL_STUDENT_ID);

