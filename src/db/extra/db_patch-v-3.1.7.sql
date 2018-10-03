SET echo ON
spool db_patch-v3.1.7.log

/* ==============================================================*/
/*  CR - 8956 table: CSERVICEATTRIBUTE                           */
/* ==============================================================*/
UPDATE 
    CSERVICEATTRIBUTE 
SET 
    VALUE = '4 hours' 
WHERE 
    SERVICEATTRIBUTENAME = 'Response Time' AND 
    VALUE = 'Response_Time_Under_50KM_4_HRS_ID';
COMMIT;

UPDATE 
CSERVICEATTRIBUTE 
SET 
    VALUE = '2 hours' 
WHERE 
    SERVICEATTRIBUTENAME = 'Response Time' AND 
    VALUE = 'Response_Time_50_100_KM_2_HRS_ID';
 COMMIT;

 /* ==============================================================*/
 /*  CR - 8895 table: CSERVICEELEMENT                             */
 /* ==============================================================*/

UPDATE CSERVICEELEMENT
	SET 
	SERVICEELEMENTCLASS = 'AccessConnection'
	WHERE
	USID IN ('AB000KFB86',
	'AB000LJA94',
	'AB000PHZC4',
	'AB000PLMD3',
	'AB000TSYDC',
	'AB000W7DCB',
	'AB000XFHE5',
	'AB000XFJE7',
	'AB00128Z88',
	'AB003RWRC3',
	'AB00LTCEC6',
	'XN002VCJFB',
	'XN002WEFF2',
	'XN0048K5E3',
	'XN009A2G87',
	'XN009EX6ED'
	);
COMMIT;

 /* ==============================================================*/
 /*  CR - 9124 table: CSERVICEATTRIBUTE                           */
 /* ==============================================================*/

UPDATE 
    CSERVICEATTRIBUTE 
SET 
    SERVICEATTRIBUTENAME = 'PE Router Name' 
WHERE 
	SERVICEATTRIBUTEID = 618557;
COMMIT;

spool OFF;

quit