set echo on
spool db_patch_CR_8949-v3.1.5a.log

/* ==============================================================*/
/*  CR - 8949 -Change Order references				 */
/*	New Order with incorrect IC01				 */
/*  table: cversion					 */
/* ==============================================================*/

update cversion
set CUSTHANDLE = 28112
where
ORDHANDLE = 27499 ;
commit;

update cversion
set ENDUSERHANDLE = 28112
where
ORDHANDLE = 27499 ;
commit;

update cversion
set SITEHANDLE ='87947;03;H003 87947;03;I003'
where
ORDHANDLE = 27499 ;
commit;

Spool off;
set echo off
exit;