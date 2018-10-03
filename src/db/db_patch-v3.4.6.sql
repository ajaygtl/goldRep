SET echo ON
spool db_patch-v3.4.6.log

ALTER TABLE cversion ADD CoreSiteID VARCHAR2(40) NULL;
ALTER TABLE cversion ADD AddressID VARCHAR2(40) NULL;

CREATE OR REPLACE VIEW viw_version_element_attribute AS
SELECT v.custhandle                v_custhandle
     , v.sitehandle                v_sitehandle
     , v.servicehandle             v_servicehandle
	 , v.coresiteid                v_coresiteid
	 , v.addressid                 v_addressid
     , v.ordhandle                 v_ordhandle
     , v.orderstatus               v_orderstatus
     , v.ordertype                 v_ordertype
     , v.ctrhandle                 v_ctrhandle
     , v.sbahandle                 v_sbahandle
     , v.enduserhandle             v_enduserhandle
     , v.exchangerate              v_exchangerate
     , v.createdate                v_createdate
     , vse.versionserviceelementid vse_versionserviceelementid
     , vse.createdate              vse_createdate
     , vse.lupddate                vse_lupddate
     , vses.statustypecode         vses_statustypecode
     , se.serviceelementid         se_serviceelementid
     , se.usid                     se_usid
     , se.serviceelementclass      se_serviceelementclass
     , se.serviceelementname       se_serviceelementname
     , se.description              se_description
     , se.creationdate             se_creationdate
     , se.grandfatherdate          se_grandfatherdate
     , se.startbillingdate         se_startbillingdate
     , se.endbillingdate           se_endbillingdate
     , se.prodid                   se_prodid
     , su.systemusername           u_systemusername
     , su.systemcode               u_systemcode
     , i.serviceattributeid        i_serviceattributeid
     , i.lupddate                  i_lupddate
     , i.changetypecode            i_changetypecode
     , t.serviceattributeid        t_serviceattributeid
     , t.serviceattributename      t_serviceattributename
     , t.value                     t_value
	 , se.sourcesystem			   vse_sourcesystem 
	 , t.customerlabel			   t_customerlabel 
	 , t.localcurrency			   t_localcurrency 
	 , v.localcurrency			   v_localcurrency 
	 , v.localcurrencyexchangerate v_localcurrencyexchangerate
  FROM cversion                  v
     , cserviceelement           se
     , cversionserviceelement    vse
     , cversionserviceelementsts vses
     , csystemuser               su
     , cservicechangeitem        i
     , cserviceattribute         t
 WHERE v.versionid = vse.versionid
   AND v.systemuserid = su.systemuserid
   AND vse.serviceelementid = se.serviceelementid
   AND vse.versionserviceelementid = vses.versionserviceelementid
   AND vses.statustypecode in ('InProgress', 'Current', 'Disconnect', 'Delete')
   AND vse.versionserviceelementid = i.versionserviceelementid(+)
   AND i.serviceattributeid = t.serviceattributeid(+);
   
CREATE OR REPLACE VIEW viw_version_element_status AS
SELECT v.custhandle                v_custhandle
     , v.sitehandle                v_sitehandle
	 , v.coresiteid                v_coresiteid
	 , v.addressid                 v_addressid
     , v.servicehandle             v_servicehandle
     , v.ordhandle                 v_ordhandle
     , vse.serviceelementid        se_serviceelementid
     , vse.lupddate                vse_lupddate
     , vses.statustypecode         vses_statustypecode
  FROM cversion                  v
     , cversionserviceelement    vse
     , cversionserviceelementsts vses
 WHERE v.versionid = vse.versionid
   AND vse.versionserviceelementid = vses.versionserviceelementid
   AND vses.statustypecode in ('InProgress', 'Current', 'Disconnect', 'Delete');
   

CREATE INDEX IDX_CVersion_03 ON CVersion (coresiteid, addressid, custhandle);
CREATE INDEX IDX_CVersion_04 ON CVersion (coresiteid, addressid, custhandle, servicehandle);   

COMMIT;

spool off;
quit
