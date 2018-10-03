SET echo ON
spool db_patch-v-3.1.7A.log

/* ==============================================================*/
/*  CR - 8829 table: CSERVICEATTRIBUTE                           */
/* ==============================================================*/

UPDATE 
CSERVICEATTRIBUTE 
SET 
    VALUE = '4 hours' 
WHERE 
    SERVICEATTRIBUTENAME = 'Response Time' AND 
    VALUE = 'Response_Time_50_100_KM_4_HRS_ID';
 COMMIT;

/* ==============================================================*/
/*  CR - 9220 table: CSERVICEATTRIBUTE                           */
/* ==============================================================*/

update CSERVICEATTRIBUTE
set value = '384 Kbps'
where
SERVICEATTRIBUTEID in(70092,70094);
commit;

update CSERVICEATTRIBUTE
set value = '128 Kbps'
where
SERVICEATTRIBUTEID in(1522762,9789937,9789938);
commit;

update CSERVICEATTRIBUTE
set value = '256 Kbps'
where
SERVICEATTRIBUTEID in(9664390,9664393,879594,879595);
commit;

update CSERVICEATTRIBUTE
set value = '512 Kbps'
where
SERVICEATTRIBUTEID in(9819989);

commit;

spool off;
quit

