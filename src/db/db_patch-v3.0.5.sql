SET echo ON
spool db_patch-v3.0.5-ResponseTime.log

/* ==============================================================*/
/*  table: CSI_PROPERTIES                                        */
/* ==============================================================*/
UPDATE CSI_PROPERTIES SET VALUE = '3.0.5' WHERE NAME = 'CSI_VERSION';


/* ==============================================================*/
/*  table: CSERVICEATTRIBUTE                                     */
/* ==============================================================*/
UPDATE 
    CSERVICEATTRIBUTE 
SET 
    VALUE = '4 hours' 
WHERE 
    SERVICEATTRIBUTENAME = 'Response Time' AND 
    VALUE = 'Response_Time_Under_50KM_4_HRS_ID';

spool OFF;

quit
