set echo on
spool db_patch_updt_usid-v3.1.5a.log

/* ==============================================================*/
/*  CR - 8924 - Discrepancy in USID in Gold and Fileade		 */
/*  table: cserviceelement					 */
/* ==============================================================*/
update CSERVICEELEMENT
set USID = 'FI007ZL5D9'
where
USID = 'GO000Y5XAE';
commit;

update CSERVICEATTRIBUTE 
set value ='FI007ZL5D9'
where
SERVICEATTRIBUTENAME = 'Local Access Connection USID'
and
VALUE = 'GO000Y5XAE';
commit;

update CSERVICEELEMENT
set USID = 'FI007ZL2DE'
where
USID = 'GO000Y5WA1';
commit;

update CSERVICEELEMENT
set USID = 'FI0051SLDA'
where
USID = 'GO0005ALD2';
commit;

update CSERVICEATTRIBUTE 
set value ='FI0051SLDA'
where
SERVICEATTRIBUTENAME = 'Local Access Connection USID'
and
VALUE = 'GO0005ALD2';
commit;

update CSERVICEATTRIBUTE 
set value ='FI007VK0F4'
where
SERVICEATTRIBUTENAME = 'Local Access Connection USID'
and
VALUE = 'GO000WSR83';
commit;

update CSERVICEATTRIBUTE 
set value ='FI0088YZF9'
where
SERVICEATTRIBUTENAME = 'Local Access Connection USID'
and
VALUE = 'GO0010S0B3';
commit;

update CSERVICEATTRIBUTE 
set value ='FI008AYZEC'
where
SERVICEATTRIBUTENAME = 'Local Access Connection USID'
and
VALUE = 'GO0010LRD1';
commit;

update CSERVICEATTRIBUTE 
set value ='FI008LQPCB'
where
SERVICEATTRIBUTENAME = 'Local Access Connection USID'
and
VALUE = 'GO00158WCA';
commit;

update CSERVICEATTRIBUTE 
set value ='XN00B1N5D3'
where
SERVICEATTRIBUTENAME = 'Local Access Connection USID'
and
VALUE = 'GO000XWBB1';
commit;

Spool off;
set echo off
exit;