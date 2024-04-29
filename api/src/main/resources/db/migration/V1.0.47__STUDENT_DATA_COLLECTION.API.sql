CREATE TABLE DUPLICATE_SEVERITY_CODE
(
    DUPLICATE_SEVERITY_CODE     VARCHAR(10)                         NOT NULL,
    LABEL                       VARCHAR(30)                         NOT NULL,
    DESCRIPTION                 VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER               NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE              TIMESTAMP                           NOT NULL,
    EXPIRY_DATE                 TIMESTAMP                           NOT NULL,
    CREATE_USER                 VARCHAR(100)                        NOT NULL,
    CREATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                 VARCHAR(100)                        NOT NULL,
    UPDATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT DUPLICATE_SEVERITY_CODE_PK PRIMARY KEY (DUPLICATE_SEVERITY_CODE)
);

INSERT INTO DUPLICATE_SEVERITY_CODE (DUPLICATE_SEVERITY_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                     EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('ALLOWABLE', 'Allowable', 'Allowable duplicate record.', '10',
        TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        CURRENT_TIMESTAMP,
        'API_STUDENT_DATA_COLLECTION', CURRENT_TIMESTAMP);

INSERT INTO DUPLICATE_SEVERITY_CODE (DUPLICATE_SEVERITY_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                     EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('NON_ALLOW', 'Non-Allowable', 'Non-allowable duplicate record.', '20',
        TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        CURRENT_TIMESTAMP,
        'API_STUDENT_DATA_COLLECTION', CURRENT_TIMESTAMP);

CREATE TABLE DUPLICATE_LEVEL_CODE
(
    DUPLICATE_LEVEL_CODE        VARCHAR(10)                         NOT NULL,
    LABEL                       VARCHAR(30)                         NOT NULL,
    DESCRIPTION                 VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER               NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE              TIMESTAMP                           NOT NULL,
    EXPIRY_DATE                 TIMESTAMP                           NOT NULL,
    CREATE_USER                 VARCHAR(100)                        NOT NULL,
    CREATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                 VARCHAR(100)                        NOT NULL,
    UPDATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT DUPLICATE_LEVEL_CODE_PK PRIMARY KEY (DUPLICATE_LEVEL_CODE)
);

INSERT INTO DUPLICATE_LEVEL_CODE (DUPLICATE_LEVEL_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                  EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('IN_DIST', 'In-District', 'In-District duplicate record.', '10',
        TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        CURRENT_TIMESTAMP,
        'API_STUDENT_DATA_COLLECTION', CURRENT_TIMESTAMP);

INSERT INTO DUPLICATE_LEVEL_CODE (DUPLICATE_LEVEL_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                  EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('PROVINCIAL', 'Provincial', 'Provincial duplicate record.', '20',
        TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        CURRENT_TIMESTAMP,
        'API_STUDENT_DATA_COLLECTION', CURRENT_TIMESTAMP);

CREATE TABLE DUPLICATE_TYPE_CODE
(
    DUPLICATE_TYPE_CODE     VARCHAR(10)                         NOT NULL,
    LABEL                       VARCHAR(30)                         NOT NULL,
    DESCRIPTION                 VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER               NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE              TIMESTAMP                           NOT NULL,
    EXPIRY_DATE                 TIMESTAMP                           NOT NULL,
    CREATE_USER                 VARCHAR(100)                        NOT NULL,
    CREATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                 VARCHAR(100)                        NOT NULL,
    UPDATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT DUPLICATE_TYPE_CODE_PK PRIMARY KEY (DUPLICATE_TYPE_CODE)
);

INSERT INTO DUPLICATE_TYPE_CODE (DUPLICATE_TYPE_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                 EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('ENROLLMENT', 'Enrollment', 'Enrollment duplicate record.', '10',
        TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        CURRENT_TIMESTAMP,
        'API_STUDENT_DATA_COLLECTION', CURRENT_TIMESTAMP);

INSERT INTO DUPLICATE_TYPE_CODE (DUPLICATE_TYPE_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                 EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('PROGRAM', 'Program', 'Program duplicate record.', '20',
        TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        CURRENT_TIMESTAMP,
        'API_STUDENT_DATA_COLLECTION', CURRENT_TIMESTAMP);

CREATE TABLE PROGRAM_DUPLICATE_TYPE_CODE
(
    PROGRAM_DUPLICATE_TYPE_CODE     VARCHAR(10)                         NOT NULL,
    LABEL                       VARCHAR(30)                         NOT NULL,
    DESCRIPTION                 VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER               NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE              TIMESTAMP                           NOT NULL,
    EXPIRY_DATE                 TIMESTAMP                           NOT NULL,
    CREATE_USER                 VARCHAR(100)                        NOT NULL,
    CREATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                 VARCHAR(100)                        NOT NULL,
    UPDATE_DATE                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PROGRAM_DUPLICATE_TYPE_CODE_PK PRIMARY KEY (PROGRAM_DUPLICATE_TYPE_CODE)
);

INSERT INTO PROGRAM_DUPLICATE_TYPE_CODE (PROGRAM_DUPLICATE_TYPE_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                         EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('SPECIAL_ED', 'Special Education', 'Special education program duplicate record.', '10',
        TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        CURRENT_TIMESTAMP,
        'API_STUDENT_DATA_COLLECTION', CURRENT_TIMESTAMP);

INSERT INTO PROGRAM_DUPLICATE_TYPE_CODE (PROGRAM_DUPLICATE_TYPE_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                         EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('CAREER', 'Career', 'Career program duplicate record.', '20',
        TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        CURRENT_TIMESTAMP,
        'API_STUDENT_DATA_COLLECTION', CURRENT_TIMESTAMP);

INSERT INTO PROGRAM_DUPLICATE_TYPE_CODE (PROGRAM_DUPLICATE_TYPE_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                         EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('INDIGENOUS', 'Indigenous Support', 'Indigenous support program duplicate record.', '30',
        TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        CURRENT_TIMESTAMP,
        'API_STUDENT_DATA_COLLECTION', CURRENT_TIMESTAMP);

INSERT INTO PROGRAM_DUPLICATE_TYPE_CODE (PROGRAM_DUPLICATE_TYPE_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                         EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('LANGUAGE', 'Language', 'Language program duplicate record.', '40',
        TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        CURRENT_TIMESTAMP,
        'API_STUDENT_DATA_COLLECTION', CURRENT_TIMESTAMP);

CREATE TABLE SDC_DUPLICATE
(
    SDC_DUPLICATE_ID                           UUID                                NOT NULL,
    RETAINED_SDC_SCHOOL_COLLECTION_STUDENT_ID  UUID                                NOT NULL,
    DUPLICATE_SEVERITY_CODE                    VARCHAR(10)                         NOT NULL,
    DUPLICATE_LEVEL_CODE                       VARCHAR(10)                         NOT NULL,
    DUPLICATE_TYPE_CODE                        VARCHAR(10)                         NOT NULL,
    PROGRAM_DUPLICATE_TYPE_CODE                VARCHAR(10)                         NOT NULL,
    CREATE_USER                         VARCHAR(100)                        NOT NULL,
    CREATE_DATE                         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                         VARCHAR(100)                        NOT NULL,
    UPDATE_DATE                         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT SDC_DUPLICATE_ID_PK PRIMARY KEY (SDC_DUPLICATE_ID)
);

ALTER TABLE SDC_DUPLICATE
    ADD CONSTRAINT FK_RETAINED_SDC_SCHOOL_COLLECTION_STUDENT_ID FOREIGN KEY (RETAINED_SDC_SCHOOL_COLLECTION_STUDENT_ID)
        REFERENCES SDC_SCHOOL_COLLECTION_STUDENT (SDC_SCHOOL_COLLECTION_STUDENT_ID);

CREATE TABLE SDC_DUPLICATE_STUDENT
(
    SDC_DUPLICATE_STUDENT_ID                  UUID                          NOT NULL,
    SDC_SCHOOL_COLLECTION_STUDENT_ID          UUID                          NOT NULL,
    SDC_DISTRICT_COLLECTION_ID                UUID                          NOT NULL,
    SDC_SCHOOL_COLLECTION_ID                  UUID                          NOT NULL,
    SDC_DUPLICATE_ID                          UUID                          NOT NULL,
    CREATE_USER                         VARCHAR(100)                        NOT NULL,
    CREATE_DATE                         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER                         VARCHAR(100)                        NOT NULL,
    UPDATE_DATE                         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT SDC_DUPLICATE_STUDENT_ID_PK PRIMARY KEY (SDC_DUPLICATE_STUDENT_ID)
);

ALTER TABLE SDC_DUPLICATE_STUDENT
    ADD CONSTRAINT FK_SDC_SCHOOL_COLLECTION_STUDENT_ID FOREIGN KEY (SDC_SCHOOL_COLLECTION_STUDENT_ID)
        REFERENCES SDC_SCHOOL_COLLECTION_STUDENT (SDC_SCHOOL_COLLECTION_STUDENT_ID);

ALTER TABLE SDC_DUPLICATE_STUDENT
    ADD CONSTRAINT FK_SDC_DISTRICT_COLLECTION_ID FOREIGN KEY (SDC_DISTRICT_COLLECTION_ID)
        REFERENCES SDC_DISTRICT_COLLECTION (SDC_DISTRICT_COLLECTION_ID);

ALTER TABLE SDC_DUPLICATE_STUDENT
    ADD CONSTRAINT FK_SDC_SCHOOL_COLLECTION_ID FOREIGN KEY (SDC_SCHOOL_COLLECTION_ID)
        REFERENCES SDC_SCHOOL_COLLECTION (SDC_SCHOOL_COLLECTION_ID);

ALTER TABLE SDC_DUPLICATE_STUDENT
    ADD CONSTRAINT FK_SDC_DUPLICATE_ID FOREIGN KEY (SDC_DUPLICATE_ID)
        REFERENCES SDC_DUPLICATE (SDC_DUPLICATE_ID);

CREATE INDEX SDC_DUPLICATE_STUDENT_SDC_DISTRICT_COLLECTION_ID_IDX ON SDC_DUPLICATE_STUDENT (SDC_DISTRICT_COLLECTION_ID);
CREATE INDEX SDC_DUPLICATE_STUDENT_SDC_SCHOOL_COLLECTION_ID_IDX ON SDC_DUPLICATE_STUDENT (SDC_SCHOOL_COLLECTION_ID);