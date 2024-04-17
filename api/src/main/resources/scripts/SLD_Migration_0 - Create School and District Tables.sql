CREATE TABLE DISTRICT
(
    district_id          VARCHAR(40)  NOT NULL,
    district_number      varchar(3)   NOT NULL,
    fax_number           varchar(10)  NULL,
    phone_number         varchar(10)  NULL,
    email                varchar(255) NULL,
    website              varchar(255) NULL,
    display_name         varchar(255) NOT NULL,
    district_region_code varchar(10)  NOT NULL,
    district_status_code varchar(10)  NOT NULL,
    create_user          varchar(100) NOT NULL,
    create_date          timestamp    NOT NULL,
    update_user          varchar(100) NOT NULL,
    update_date          timestamp    NOT NULL,
    CONSTRAINT district_id_pk PRIMARY KEY (district_id)
);

CREATE TABLE SCHOOL
(
    school_id                         VARCHAR(40)         NOT NULL,
    district_id                       VARCHAR(40)         NOT NULL,
    independent_authority_id          VARCHAR(40)         NULL,
    school_number                     varchar(5)   NOT NULL,
    fax_number                        varchar(10)  NULL,
    phone_number                      varchar(10)  NULL,
    email                             varchar(255) NULL,
    website                           varchar(255) NULL,
    display_name                      varchar(255) NOT NULL,
    school_organization_code          varchar(10)  NOT NULL,
    school_category_code              varchar(10)  NOT NULL,
    facility_type_code                varchar(10)  NOT NULL,
    school_reporting_requirement_code varchar(10)  NOT NULL,
    opened_date                       timestamp    NULL,
    closed_date                       timestamp    NULL,
    create_user                       varchar(100) NOT NULL,
    create_date                       timestamp    NOT NULL,
    update_user                       varchar(100) NOT NULL,
    update_date                       timestamp    NOT NULL,
    display_name_no_spec_chars        varchar(255) NULL,
    CONSTRAINT school_id_pk PRIMARY KEY (school_id)
);
