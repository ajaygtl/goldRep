--create new tables for cis-csi interface
spool csi_cis_new_tables.log
set echo on

DROP TABLE Ccisextractlog;
DROP TABLE Ccismapping;
DROP TABLE Ccisvaluemapping;
DROP SEQUENCE SEQCcisextractlog;
DROP SEQUENCE SEQCcismapping;

CREATE TABLE Ccismapping (
    ccismappingid       number,
    cistablename        varchar2(50),
    ciscolumnname       varchar2(50),
    cisservice          varchar2(50),
    csiservice          varchar2(50),
    csisrvelmclass      varchar2(100),
    csisrvelmpropname   varchar2(100),
    csisrvelmattrname   varchar2(100),
    description         varchar2(200),
    status              number,
    createdate          date,
    createdby           varchar2(255),
    lupddate            date,
    lupdby              varchar2(255))
TABLESPACE CSI_DATA
PCTUSED    60
PCTFREE    20
STORAGE    (
            INITIAL          256 K
            NEXT             256 K
            PCTINCREASE      0
           );

ALTER TABLE Ccismapping ADD (
  CONSTRAINT ccismapping_pk PRIMARY KEY (ccismappingid)
    USING INDEX
    TABLESPACE CSI_INDEX
    PCTFREE    10
    STORAGE    (
                INITIAL          256K
                NEXT             256K
               ));

CREATE TABLE Ccisvaluemapping (
    ccisvaluemappingid       number,
    csiservice          varchar2(50),
    csisrvelmclass      varchar2(100),
    csisrvelmattrname   varchar2(100),
    cisvalue		varchar2(1000),
    csivalue		varchar2(1000),
    description         varchar2(200),
    status              number,
    createdate          date,
    createdby           varchar2(255),
    lupddate            date,
    lupdby              varchar2(255))
TABLESPACE CSI_DATA
PCTUSED    60
PCTFREE    20
STORAGE    (
            INITIAL          256 K
            NEXT             256 K
            PCTINCREASE      0
           );

ALTER TABLE Ccisvaluemapping ADD (
  CONSTRAINT ccisvaluemapping_pk PRIMARY KEY (ccisvaluemappingid)
    USING INDEX
    TABLESPACE CSI_INDEX
    PCTFREE    10
    STORAGE    (
                INITIAL          256K
                NEXT             256K
               ));


CREATE TABLE Ccisextractlog (
    ccisextractlogid    number,
    extracttype         number,
    starttime           date,
    endtime             date,
    status              number,
    note                varchar2(1000),
    createdate          date,
    createdby           varchar2(255),
    lupddate            date,
    lupdby              varchar2(255))
TABLESPACE CSI_DATA
PCTUSED    60
PCTFREE    20
STORAGE    (
            INITIAL          256 K
            NEXT             256 K
            PCTINCREASE      0
           );

ALTER TABLE Ccisextractlog ADD (
  CONSTRAINT ccisextractlog_pk PRIMARY KEY (ccisextractlogid)
    USING INDEX
    TABLESPACE CSI_INDEX
    PCTFREE    10
    STORAGE    (
                INITIAL          256K
                NEXT             256K
               ));

CREATE SEQUENCE SEQCcisextractlog start with 1;
CREATE SEQUENCE SEQCcismapping start with 1;
CREATE SEQUENCE SEQCcisvaluemapping start with 1;

set echo off
spool off