CREATE TABLE STUDENT_DATA_COLLECTION_EVENT
(
    EVENT_ID      UUID                               NOT NULL,
    EVENT_PAYLOAD BYTEA                              NOT NULL,
    EVENT_STATUS  VARCHAR(50)                        NOT NULL,
    EVENT_TYPE    VARCHAR(100)                       NOT NULL,
    SAGA_ID       UUID,
    EVENT_OUTCOME VARCHAR(100)                       NOT NULL,
    REPLY_CHANNEL VARCHAR(100),
    CREATE_USER   VARCHAR(32)                         NOT NULL,
    CREATE_DATE   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER   VARCHAR(32)                         NOT NULL,
    UPDATE_DATE   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT NOMINAL_ROLL_EVENT_EVENT_PK PRIMARY KEY (EVENT_ID)
);

CREATE INDEX STUDENT_DATA_COLLECTION_EVENT_STATUS_IDX ON STUDENT_DATA_COLLECTION_EVENT (EVENT_STATUS);
CREATE INDEX STUDENT_DATA_COLLECTION_EVENT_TYPE_IDX ON STUDENT_DATA_COLLECTION_EVENT (EVENT_TYPE);

CREATE TABLE STUDENT_DATA_COLLECTION_SAGA
(
    SAGA_ID            UUID                                NOT NULL,
    STUDENT_DATA_COLLECTION_STUDENT_ID       UUID,
    STUDENT_DATA_COLLECTION_SCHOOL_ID        UUID,
    SAGA_NAME          VARCHAR(50)                         NOT NULL,
    SAGA_STATE         VARCHAR(100)                        NOT NULL,
    PAYLOAD            VARCHAR                             NOT NULL,
    RETRY_COUNT        INTEGER,
    STATUS             VARCHAR(20)                         NOT NULL,
    CREATE_USER        VARCHAR(32)                         NOT NULL,
    CREATE_DATE        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER        VARCHAR(32)                         NOT NULL,
    UPDATE_DATE        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT STUDENT_DATA_COLLECTION_SAGA_PK PRIMARY KEY (SAGA_ID)
);

CREATE INDEX SDC_SAGA_STATUS_IDX ON STUDENT_DATA_COLLECTION_SAGA (STATUS);

CREATE TABLE STUDENT_DATA_COLLECTION_SAGA_EVENT_STATES
(
    SAGA_EVENT_ID       UUID                                NOT NULL,
    SAGA_ID             UUID                                NOT NULL,
    SAGA_EVENT_STATE    VARCHAR(100)                        NOT NULL,
    SAGA_EVENT_OUTCOME  VARCHAR(100)                        NOT NULL,
    SAGA_STEP_NUMBER    INTEGER                             NOT NULL,
    SAGA_EVENT_RESPONSE VARCHAR                             NOT NULL,
    CREATE_USER         VARCHAR(32)                         NOT NULL,
    CREATE_DATE         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER         VARCHAR(32)                         NOT NULL,
    UPDATE_DATE         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT STUDENT_DATA_COLLECTION_SAGA_EVENT_STATES_PK PRIMARY KEY (SAGA_EVENT_ID)
);

ALTER TABLE STUDENT_DATA_COLLECTION_SAGA_EVENT_STATES
    ADD CONSTRAINT SDC_SAGA_EVENT_STATES_SAGA_ID_FK FOREIGN KEY (SAGA_ID) REFERENCES STUDENT_DATA_COLLECTION_SAGA (SAGA_ID);
CREATE INDEX SDC_SAGA_EVENT_STATES_SID_SES_SEO_SSN_IDX ON STUDENT_DATA_COLLECTION_SAGA_EVENT_STATES (SAGA_ID, SAGA_EVENT_STATE, SAGA_EVENT_OUTCOME, SAGA_STEP_NUMBER);
