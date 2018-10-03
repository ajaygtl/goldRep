set echo on
spool db_patch_site_del-v3.1.5a.log
/* ==============================================================*/
/*  CR - 8344 - CURRUPTED SITE HANDLE DATA - DELETED IN		 */
/*  table: cversion						 */
/*  table: cversionserviceelement				 */
/*  table: cchargechangeitem					 */
/*  table: cservicechangeitem					 */
/*  table: cversionserviceelementsts				 */
/* ==============================================================*/

/* ==============================================================*/
/*  table: cversionserviceelementsts				 */
/* ==============================================================*/

Delete from cversionserviceelementsts
where
versionserviceelementid in (select versionserviceelementid from cversionserviceelement 
where versionid in ( select versionid from cversion where sitehandle like '%?%' ));
commit;

/* ==============================================================*/
/*  table: cservicechangeitem					 */
/* ==============================================================*/

Delete from cservicechangeitem
where
versionserviceelementid in (select versionserviceelementid from cversionserviceelement 
where versionid in ( select versionid from cversion where sitehandle like '%?%' ));
commit;

/* ==============================================================*/
/*  table: cchargechangeitem					 */
/* ==============================================================*/

Delete from cchargechangeitem
where
versionserviceelementid in (select versionserviceelementid from cversionserviceelement 
where versionid in ( select versionid from cversion where sitehandle like '%?%'));
commit;


/* ==============================================================*/
/*  table: cversionserviceelement				 */
/* ==============================================================*/

Delete from cversionserviceelement
where
versionid in 
(select versionid from cversion where sitehandle like '%?%');
commit;

/* ==============================================================*/
/*  table: cversion						 */
/* ==============================================================*/

Delete from cversion
where
sitehandle like '%?%';
commit;

Spool off;
exit;
