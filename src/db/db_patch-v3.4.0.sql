SET echo ON
spool db_patch-v3.4.0.LOG

-- create new cis_temp_site table
DROP TABLE CIS_TEMP_SITE;
CREATE TABLE CIS_TEMP_SITE
(
  CUSTCODE	VARCHAR2(20), -- corresponding to sc_organization custcode
  SITECODE	VARCHAR2(40), -- corresponding to eq_site sitecode
  ADDRESS1	VARCHAR2(50), -- corresponding to sc_address address1
  ADDRESS2	VARCHAR2(50), -- corresponding to sc_address address2
  ADDRESS3	VARCHAR2(50), -- corresponding to sc_address address3
  CITYCODE	VARCHAR2(3),  -- corresponding to eq_site citycode
  COUNTRYCODE   VARCHAR2(50), -- corresponding to sc_address country
  POSTALCODE	VARCHAR2(9),  -- corresponding to sc_address zipcode
  STATECODE	VARCHAR2(40), -- correstponding to sc_address state
  CITYNAME	VARCHAR2(50), -- correstponding to sc_address city
  CONTACTNAME	VARCHAR2(100), -- correstponding to sc_person lastname + firstname
  TELEPHONENUMBER	VARCHAR2(20), -- correstponding to sc_person phone1
  FAXNUMBER	VARCHAR2(20), -- correstponding to sc_person fax
  EMAILADDRESS	VARCHAR2(80) -- correstponding to sc_person email 
)
TABLESPACE csi_data
STORAGE    (
            INITIAL          512K
            NEXT             512K
            PCTINCREASE      0
           )
;

DROP INDEX CIS_TEMP_SITE_CUSTSITE_IDX;
CREATE INDEX CIS_TEMP_SITE_CUSTSITE_IDX ON CIS_TEMP_SITE (custcode, sitecode) 
TABLESPACE CSI_INDEX
STORAGE    (
            INITIAL          256K
            NEXT             256K
);

-- change column length
ALTER TABLE CCISEXTRACTLOG MODIFY (
NOTE	VARCHAR2(4000) );

-- add sequence for sitecode
DROP SEQUENCE SEQSITECODE;
CREATE SEQUENCE SEQSITECODE start with 25000;

spool OFF;

quit
