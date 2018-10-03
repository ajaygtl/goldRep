--========================================
-- For release 3.1.5
--========================================
DROP INDEX IDX_CVERSION_CUSTSERV;

CREATE INDEX IDX_CVERSION_CUSTSERV ON CVERSION
(CUSTHANDLE, SERVICEHANDLE)
TABLESPACE CSI_INDEX
STORAGE    (
            INITIAL          512K
            NEXT             512K
            PCTINCREASE      0
           );