ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'YYYY-MM-DD HH24:MI:SS.FF6';
ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD HH24:MI:SS';

CREATE INDEX STUDENT_LINK_PEN_INDX
ON STUDENT_LINK(PEN);

CREATE INDEX STUDENT_PEN_REP_DATE_INDX
ON STUDENT(PEN,REPORT_DATE);

CREATE INDEX STUDENT_PROGRAMS_PEN_INDX
ON STUDENT_PROGRAMS(PEN);

CREATE INDEX STUDENT_PROGRAMS_REPORT_DATE_INDX
ON STUDENT_PROGRAMS(REPORT_DATE);

CREATE INDEX STUDENT_PROGRAMS_CAREER_PROGRAM_INDX
ON STUDENT_PROGRAMS(CAREER_PROGRAM);

CREATE TABLE COLLECTION_TYPE_CODE
(
    COLLECTION_TYPE_CODE VARCHAR(10)                         NOT NULL,
    LABEL           VARCHAR(30)                         NOT NULL,
    DESCRIPTION     VARCHAR(255)                        NOT NULL,
    DISPLAY_ORDER   NUMERIC   DEFAULT 1                 NOT NULL,
    EFFECTIVE_DATE  TIMESTAMP                           NOT NULL,
    EXPIRY_DATE     TIMESTAMP                           NOT NULL,
    OPEN_DATE       TIMESTAMP                           NOT NULL,
    CLOSE_DATE      TIMESTAMP                           NOT NULL,
    SNAPSHOT_DATE   DATE                                NOT NULL,
    CREATE_USER     VARCHAR(32)                         NOT NULL,
    CREATE_DATE     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_USER     VARCHAR(32)                         NOT NULL,
    UPDATE_DATE     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT COLLECTION_TYPE_CODE_PK PRIMARY KEY (COLLECTION_TYPE_CODE)
);

INSERT INTO COLLECTION_TYPE_CODE (COLLECTION_TYPE_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                             EXPIRY_DATE, OPEN_DATE, CLOSE_DATE, SNAPSHOT_DATE, CREATE_USER, CREATE_DATE,
                             UPDATE_USER, UPDATE_DATE)
VALUES ('SEPTEMBER', 'September', 'September collection', '10', TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), TO_DATE('20230929', 'YYYYMMDD'),
        TO_DATE('20231115', 'YYYYMMDD'), TO_DATE('99990929', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        TO_DATE('20230210', 'YYYYMMDD'),
        'API_STUDENT_DATA_COLLECTION', TO_DATE('20230210', 'YYYYMMDD'));

INSERT INTO COLLECTION_TYPE_CODE (COLLECTION_TYPE_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                             EXPIRY_DATE, OPEN_DATE, CLOSE_DATE, SNAPSHOT_DATE, CREATE_USER, CREATE_DATE,
                             UPDATE_USER, UPDATE_DATE)
VALUES ('FEBRUARY', 'February', 'February collection', '10', TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), TO_DATE('20230201', 'YYYYMMDD'),
        TO_DATE('20230307', 'YYYYMMDD'), TO_DATE('99990210', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        TO_DATE('20230210', 'YYYYMMDD'),
        'API_STUDENT_DATA_COLLECTION', TO_DATE('20230210', 'YYYYMMDD'));

INSERT INTO COLLECTION_TYPE_CODE (COLLECTION_TYPE_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                             EXPIRY_DATE, OPEN_DATE, CLOSE_DATE, SNAPSHOT_DATE, CREATE_USER, CREATE_DATE,
                             UPDATE_USER, UPDATE_DATE)
VALUES ('MAY', 'May', 'May collection', '10', TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), TO_DATE('20230501', 'YYYYMMDD'),
        TO_DATE('20230530', 'YYYYMMDD'), TO_DATE('99990505', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        TO_DATE('20230210', 'YYYYMMDD'),
        'API_STUDENT_DATA_COLLECTION', TO_DATE('20230210', 'YYYYMMDD'));

INSERT INTO COLLECTION_TYPE_CODE (COLLECTION_TYPE_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                             EXPIRY_DATE, OPEN_DATE, CLOSE_DATE, SNAPSHOT_DATE, CREATE_USER, CREATE_DATE,
                             UPDATE_USER, UPDATE_DATE)
VALUES ('JUNE', 'June', 'June collection', '10', TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), TO_DATE('20230601', 'YYYYMMDD'),
        TO_DATE('20230630', 'YYYYMMDD'), TO_DATE('99990605', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        TO_DATE('20230210', 'YYYYMMDD'),
        'API_STUDENT_DATA_COLLECTION', TO_DATE('20230210', 'YYYYMMDD'));

INSERT INTO COLLECTION_TYPE_CODE (COLLECTION_TYPE_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                             EXPIRY_DATE, OPEN_DATE, CLOSE_DATE, SNAPSHOT_DATE, CREATE_USER, CREATE_DATE,
                             UPDATE_USER, UPDATE_DATE)
VALUES ('JULY', 'July', 'July Collection', '10', TO_DATE('20230210', 'YYYYMMDD'),
        TO_DATE('99991231', 'YYYYMMDD'), TO_DATE('20230707', 'YYYYMMDD'),
        TO_DATE('20230730', 'YYYYMMDD'), TO_DATE('99990707', 'YYYYMMDD'), 'API_STUDENT_DATA_COLLECTION',
        TO_DATE('20230210', 'YYYYMMDD'),
        'API_STUDENT_DATA_COLLECTION', TO_DATE('20230210', 'YYYYMMDD'));

CREATE TABLE COLLECTION
as
SELECT
    LOWER(REGEXP_REPLACE(dbms_crypto.randombytes(16), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5')) as COLLECTION_ID,
    (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
     WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
               BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
               AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) AS COLLECTION_TYPE_CODE,
    TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD') as OPEN_DATE,
    (SELECT TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'),'YYYYMMDD') FROM COLLECTION_TYPE_CODE ctc
     WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
               BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
               AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) as CLOSE_DATE,
    (SELECT TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || LPAD(EXTRACT(MONTH FROM ctc.SNAPSHOT_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.SNAPSHOT_DATE),2,'0'),'YYYYMMDD') FROM COLLECTION_TYPE_CODE ctc
     WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
               BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
               AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) as SNAPSHOT_DATE,
    CASE
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'SEPTEMBER' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-10-06', 'YYYY-MM-DD')
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'FEBRUARY' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-02-17', 'YYYY-MM-DD')
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'MAY' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-05-12', 'YYYY-MM-DD')
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'JULY' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-07-14', 'YYYY-MM-DD')
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'JUNE' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-06-12', 'YYYY-MM-DD')
        END as SUBMISSION_DUE_DATE,
    CASE
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'SEPTEMBER' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-10-20', 'YYYY-MM-DD')
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'FEBRUARY' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-03-01', 'YYYY-MM-DD')
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'MAY' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-05-26', 'YYYY-MM-DD')
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'JULY' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-07-28', 'YYYY-MM-DD')
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'JUNE' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-06-26', 'YYYY-MM-DD')
        END as DUPLICATION_RESOLUTION_DUE_DATE,
    CASE
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'SEPTEMBER' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-10-27', 'YYYY-MM-DD')
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'FEBRUARY' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-03-08', 'YYYY-MM-DD')
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'MAY' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-06-04', 'YYYY-MM-DD')
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'JULY' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-08-04', 'YYYY-MM-DD')
        WHEN (SELECT COLLECTION_TYPE_CODE FROM COLLECTION_TYPE_CODE ctc
              WHERE TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0') || LPAD(EXTRACT(DAY FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')),2,'0'), 'YYYYMMDD')
                        BETWEEN TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.OPEN_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.OPEN_DATE),2,'0'), 'YYYYMMDD')
                        AND TO_DATE('1900' || LPAD(EXTRACT(MONTH FROM ctc.CLOSE_DATE),2,'0') || LPAD(EXTRACT(DAY FROM ctc.CLOSE_DATE),2,'0'), 'YYYYMMDD')) = 'JUNE' THEN TO_DATE(EXTRACT(YEAR FROM TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD')) || '-07-03', 'YYYY-MM-DD')
        END as SIGN_OFF_DUE_DATE,
    'COMPLETED' as COLLECTION_STATUS_CODE,
    'SLD_MIGRATION' as CREATE_USER,
    sysdate as CREATE_DATE,
    'SLD_MIGRATION' as UPDATE_USER,
    sysdate as UPDATE_DATE
FROM STUDENT sld_student
GROUP BY REPORT_DATE;

CREATE TABLE SDC_DISTRICT_COLLECTION
as
SELECT
    LOWER(REGEXP_REPLACE(dbms_crypto.randombytes(16), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5')) as SDC_DISTRICT_COLLECTION_ID,
    (SELECT COLLECTION_ID FROM COLLECTION coll WHERE TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD') BETWEEN coll.OPEN_DATE AND coll.CLOSE_DATE) AS COLLECTION_ID,
    (SELECT dist.DISTRICT_ID from DISTRICT dist WHERE dist.DISTRICT_NUMBER = sld_student.DISTNO) as DISTRICT_ID,
    'COMPLETED' AS SDC_DISTRICT_COLLECTION_STATUS_CODE,
    'SLD_MIGRATION' as CREATE_USER,
    sysdate as CREATE_DATE,
    'SLD_MIGRATION' as UPDATE_USER,
    sysdate as UPDATE_DATE
FROM STUDENT sld_student
GROUP BY sld_student.DISTNO, sld_student.REPORT_DATE;

CREATE TABLE SDC_SCHOOL_COLLECTION
as
SELECT
    LOWER(REGEXP_REPLACE(dbms_crypto.randombytes(16), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5')) as SDC_SCHOOL_COLLECTION_ID,
    CASE
        WHEN (SELECT schl.SCHOOL_CATEGORY_CODE from SCHOOL schl WHERE SUBSTR( DISTNO||'' ||SCHLNO, 4) = schl.SCHOOL_NUMBER AND SUBSTR( sld_student.DISTNO||'' ||SCHLNO, 0 , 3) = (SELECT dist.DISTRICT_NUMBER from DISTRICT dist WHERE schl.DISTRICT_ID = dist.DISTRICT_ID)) in ('INDEPEND', 'INDP_FNS', 'OFFSHORE') THEN null
        ELSE (SELECT SDC_DISTRICT_COLLECTION_ID FROM SDC_DISTRICT_COLLECTION WHERE DISTRICT_ID = (SELECT dist.DISTRICT_ID from DISTRICT dist WHERE dist.DISTRICT_NUMBER = SUBSTR( sld_student.DISTNO||'' ||SCHLNO, 0 , 3)) AND COLLECTION_ID = (SELECT COLLECTION_ID FROM COLLECTION coll WHERE TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD') BETWEEN coll.OPEN_DATE AND coll.CLOSE_DATE))
    END as SDC_DISTRICT_COLLECTION_ID,
    (SELECT COLLECTION_ID FROM COLLECTION coll WHERE TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD') BETWEEN coll.OPEN_DATE AND coll.CLOSE_DATE) AS COLLECTION_ID,
    (SELECT schl.SCHOOL_ID from SCHOOL schl WHERE SUBSTR( DISTNO||'' ||SCHLNO, 4) = schl.SCHOOL_NUMBER AND SUBSTR( sld_student.DISTNO||'' ||SCHLNO, 0 , 3) = (SELECT dist.DISTRICT_NUMBER from DISTRICT dist WHERE schl.DISTRICT_ID = dist.DISTRICT_ID)) as SCHOOL_ID,
	'COMPLETED' AS SDC_SCHOOL_COLLECTION_STATUS_CODE,
	TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD') AS UPLOAD_DATE,
	'NOT_AVAILABLE' AS UPLOAD_FILE_NAME,
    'SLD_MIGRATION' as CREATE_USER,
    sysdate as CREATE_DATE,
    'SLD_MIGRATION' as UPDATE_USER,
    sysdate as UPDATE_DATE
FROM STUDENT sld_student
GROUP BY DISTNO||'' ||SCHLNO, REPORT_DATE;

CREATE TABLE SDC_SCHOOL_COLLECTION_HISTORY
as
SELECT
    LOWER(REGEXP_REPLACE(dbms_crypto.randombytes(16), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5')) as SDC_SCHOOL_COLLECTION_HISTORY_ID,
    sdc.*
FROM SDC_SCHOOL_COLLECTION sdc;

CREATE INDEX SDC_SCHOOL_COLLECTION_COLLECTION_ID_INDX
ON SDC_SCHOOL_COLLECTION(COLLECTION_ID);

CREATE INDEX SDC_SCHOOL_COLLECTION_SCHOOL_ID_INDX
ON SDC_SCHOOL_COLLECTION(SCHOOL_ID);

CREATE TABLE SDC_STUDENT_ELL
AS
SELECT
    LOWER(REGEXP_REPLACE(dbms_crypto.randombytes(16), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5')) as SDC_STUDENT_ELL_ID,
    (SELECT LOWER(REGEXP_REPLACE(STUDENT_ID, '(.{8})(.{4})(.{4})(.{4})(.*)', '\1-\2-\3-\4-\5'))
	 FROM STUDENT_LINK stud_link
	 WHERE stud_link.PEN = TRIM(sld_student.PEN)) AS STUDENT_ID,
    sld_student.ESL_YRS AS YEARS_IN_ELL,
    'SLD_MIGRATION' as CREATE_USER,
    sysdate as CREATE_DATE,
    'SLD_MIGRATION' as UPDATE_USER,
    sysdate as UPDATE_DATE
from STUDENT sld_student
WHERE sld_student.ESL_YRS IS NOT NULL
AND sld_student.ESL_YRS != 0
order by row_number() over (partition by sld_student.PEN order by ESL_YRS desc)
fetch first 1 row with ties;

CREATE TABLE SDC_SCHOOL_COLLECTION_STUDENT
as
SELECT
    LOWER(REGEXP_REPLACE(dbms_crypto.randombytes(16), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5')) as SDC_SCHOOL_COLLECTION_STUDENT_ID,
    (SELECT sdc_coll.SDC_SCHOOL_COLLECTION_ID
	 FROM SDC_SCHOOL_COLLECTION sdc_coll, COLLECTION coll
	 WHERE sdc_coll.COLLECTION_ID = coll.COLLECTION_ID
	 AND sdc_coll.SCHOOL_ID = (SELECT schl.SCHOOL_ID from SCHOOL schl WHERE SUBSTR( DISTNO||'' ||SCHLNO, 4) = schl.SCHOOL_NUMBER AND SUBSTR( sld_student.DISTNO||'' ||SCHLNO, 0 , 3) = (SELECT dist.DISTRICT_NUMBER from DISTRICT dist WHERE schl.DISTRICT_ID = dist.DISTRICT_ID))
	 AND TO_DATE(sld_student.REPORT_DATE,'YYYYMMDD') BETWEEN coll.OPEN_DATE AND coll.CLOSE_DATE) AS SDC_SCHOOL_COLLECTION_ID,
    'VERIFIED' AS SDC_SCHOOL_COLLECTION_STUDENT_STATUS_CODE,
	TRIM(sld_student.LOCAL_STUDENT_ID) AS LOCAL_ID,
    TRIM(sld_student.PEN) AS STUDENT_PEN,
    TRIM(sld_student.LEGAL_GIVEN_NAME) AS LEGAL_FIRST_NAME,
    TRIM(sld_student.LEGAL_MIDDLE_NAME) AS LEGAL_MIDDLE_NAMES,
    TRIM(sld_student.LEGAL_SURNAME) AS LEGAL_LAST_NAME,
    TRIM(sld_student.USUAL_GIVEN_NAME) AS USUAL_FIRST_NAME,
    TRIM(sld_student.USUAL_MIDDLE_NAME) AS USUAL_MIDDLE_NAMES,
    TRIM(sld_student.USUAL_SURNAME) AS USUAL_LAST_NAME,
    TRIM(sld_student.BIRTH_DATE) AS DOB,
    TRIM(sld_student.SEX) AS GENDER_CODE,
    TRIM(sld_student.SPED_CAT) AS SPECIAL_EDUCATION_CATEGORY_CODE,
    TRIM(sld_student.SCHOOL_FUNDING_CODE) AS SCHOOL_FUNDING_CODE,
    TRIM(sld_student.NATIVE_ANCESTRY_IND) AS NATIVE_ANCESTRY_IND,
    CAST(sld_student.ESL_YRS as integer) AS YEARS_IN_ELL,
    TRIM(sld_student.HOME_LANGUAGE_SPOKEN) AS HOME_LANGUAGE_SPOKEN_CODE,
    TRIM(sld_student.OTHER_COURSES) AS OTHER_COURSES,
    TRIM(sld_student.NUMBER_OF_SUPPORT_BLOCKS) AS SUPPORT_BLOCKS,
    TRIM(sld_student.ENROLLED_GRADE_CODE) AS ENROLLED_GRADE_CODE,
    TO_NUMBER(TO_CHAR(sld_student.STUDENT_FTE_VALUE / 10000,'99999.9999')) AS FTE,
    (SELECT LISTAGG(stud_prog.ENROLLED_PROGRAM_CODE, '') WITHIN GROUP (ORDER BY 1)
	 FROM STUDENT_PROGRAMS stud_prog
	 WHERE stud_prog.REPORT_DATE = sld_student.REPORT_DATE
	 AND stud_prog.PEN = sld_student.PEN
	 AND sld_student.DISTNO = STUD_PROG.DISTNO
	 AND sld_student.SCHLNO = STUD_PROG.SCHLNO
	 GROUP BY PEN) AS ENROLLED_PROGRAM_CODES,
	(SELECT CAREER_PROGRAM
	 FROM STUDENT_PROGRAMS stud_prog
	 WHERE stud_prog.REPORT_DATE = sld_student.REPORT_DATE
	 AND stud_prog.PEN = sld_student.PEN
	 AND sld_student.DISTNO = STUD_PROG.DISTNO
	 AND sld_student.SCHLNO = STUD_PROG.SCHLNO
	 AND CAREER_PROGRAM IS NOT NULL
	 AND CAREER_PROGRAM != ' ') AS CAREER_PROGRAM_CODE,
    TRIM(sld_student.NUMBER_OF_COURSES) AS NUMBER_OF_COURSES,
    TRIM(sld_student.BAND_CODE) AS BAND_CODE,
    TRIM(sld_student.POSTAL) AS POSTAL_CODE,
    TRIM(sld_student.PEN) AS ASSIGNED_PEN,
    (SELECT LOWER(REGEXP_REPLACE(STUDENT_ID, '(.{8})(.{4})(.{4})(.{4})(.*)', '\1-\2-\3-\4-\5'))
	 FROM STUDENT_LINK stud_link
	 WHERE stud_link.PEN = TRIM(sld_student.PEN)) AS ASSIGNED_STUDENT_ID,
    'SLD_MIGRATION' as CREATE_USER,
    sysdate as CREATE_DATE,
    'SLD_MIGRATION' as UPDATE_USER,
    sysdate as UPDATE_DATE
FROM STUDENT sld_student;

UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET LOCAL_ID = replace(LOCAL_ID, unistr('\0000')) WHERE instr(LOCAL_ID, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET STUDENT_PEN = replace(STUDENT_PEN, unistr('\0000')) WHERE instr(STUDENT_PEN, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET LEGAL_FIRST_NAME = replace(LEGAL_FIRST_NAME, unistr('\0000')) WHERE instr(LEGAL_FIRST_NAME, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET LEGAL_MIDDLE_NAMES = replace(LEGAL_MIDDLE_NAMES, unistr('\0000')) WHERE instr(LEGAL_MIDDLE_NAMES, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET LEGAL_LAST_NAME = replace(LEGAL_LAST_NAME, unistr('\0000')) WHERE instr(LEGAL_LAST_NAME, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET USUAL_FIRST_NAME = replace(USUAL_FIRST_NAME, unistr('\0000')) WHERE instr(USUAL_FIRST_NAME, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET USUAL_MIDDLE_NAMES = replace(USUAL_MIDDLE_NAMES, unistr('\0000')) WHERE instr(USUAL_MIDDLE_NAMES, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET USUAL_LAST_NAME = replace(USUAL_LAST_NAME, unistr('\0000')) WHERE instr(USUAL_LAST_NAME, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET DOB = replace(DOB, unistr('\0000')) WHERE instr(DOB, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET GENDER_CODE = replace(GENDER_CODE, unistr('\0000')) WHERE instr(GENDER_CODE, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET SPECIAL_EDUCATION_CATEGORY_CODE = replace(SPECIAL_EDUCATION_CATEGORY_CODE, unistr('\0000')) WHERE instr(SPECIAL_EDUCATION_CATEGORY_CODE, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET SCHOOL_FUNDING_CODE = replace(SCHOOL_FUNDING_CODE, unistr('\0000')) WHERE instr(SCHOOL_FUNDING_CODE, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET NATIVE_ANCESTRY_IND = replace(NATIVE_ANCESTRY_IND, unistr('\0000')) WHERE instr(NATIVE_ANCESTRY_IND, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET HOME_LANGUAGE_SPOKEN_CODE = replace(HOME_LANGUAGE_SPOKEN_CODE, unistr('\0000')) WHERE instr(HOME_LANGUAGE_SPOKEN_CODE, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET OTHER_COURSES = replace(OTHER_COURSES, unistr('\0000')) WHERE instr(OTHER_COURSES, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET SUPPORT_BLOCKS = replace(SUPPORT_BLOCKS, unistr('\0000')) WHERE instr(SUPPORT_BLOCKS, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET ENROLLED_GRADE_CODE = replace(ENROLLED_GRADE_CODE, unistr('\0000')) WHERE instr(ENROLLED_GRADE_CODE, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET ENROLLED_PROGRAM_CODES = replace(ENROLLED_PROGRAM_CODES, unistr('\0000')) WHERE instr(ENROLLED_PROGRAM_CODES, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET CAREER_PROGRAM_CODE = replace(CAREER_PROGRAM_CODE, unistr('\0000')) WHERE instr(CAREER_PROGRAM_CODE, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET NUMBER_OF_COURSES = replace(NUMBER_OF_COURSES, unistr('\0000')) WHERE instr(NUMBER_OF_COURSES, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET BAND_CODE = replace(BAND_CODE, unistr('\0000')) WHERE instr(BAND_CODE, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET POSTAL_CODE = replace(POSTAL_CODE, unistr('\0000')) WHERE instr(POSTAL_CODE, unistr('\0000')) > 0;
UPDATE SDC_SCHOOL_COLLECTION_STUDENT SET ASSIGNED_PEN = replace(ASSIGNED_PEN, unistr('\0000')) WHERE instr(ASSIGNED_PEN, unistr('\0000')) > 0;

