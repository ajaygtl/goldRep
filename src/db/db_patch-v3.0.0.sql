set echo on
spool db_patch-v3.0.0.log

/* ==============================================================*/
/*  table: CSI_PROPERTIES                                        */
/* ==============================================================*/
UPDATE CSI_PROPERTIES SET VALUE = '3.0.0' WHERE NAME = 'CSI_VERSION';

/* ==============================================================*/
/*  table: CSERVICEELEMENT                                       */
/* ==============================================================*/
ALTER TABLE CSERVICEELEMENT ADD (
          SOURCESYSTEM        VARCHAR(5) DEFAULT 'GOLD' NOT NULL
);

ALTER TABLE CSERVICEELEMENT MODIFY (SOURCESYSTEM DEFAULT NULL);

CREATE INDEX IDX_SOURCESYSTEM ON CSERVICEELEMENT (SOURCESYSTEM);

CREATE INDEX IDX_ORDERHANDLE ON CVERSION (ORDHANDLE);

CREATE INDEX IDX_SERVICEHANDLE ON CVERSION (SERVICEHANDLE);

/* ==============================================================*/
/*  migtate Already In Place:                                    */
/*  Yes = Use Existing                                           */
/*  No  = No = Disconnect                                        */
/* ==============================================================*/
UPDATE CSERVICEATTRIBUTE SET VALUE = 'Use Existing' WHERE SERVICEATTRIBUTENAME = 'Already In Place';
UPDATE CSERVICEATTRIBUTE SET VALUE = 'Use Existing' WHERE SERVICEATTRIBUTENAME = 'In Place';

/* ==============================================================*/
/*  table: VIW_VERSION_ELEMENT_ATTRIBUTE                         */
/* ==============================================================*/
CREATE OR REPLACE VIEW VIW_VERSION_ELEMENT_ATTRIBUTE
(
  V_CUSTHANDLE,
  V_SITEHANDLE,
  V_SERVICEHANDLE,
  V_ORDHANDLE,
  V_ORDERSTATUS,
  V_ORDERTYPE,
  V_CTRHANDLE,
  V_SBAHANDLE,
  V_ENDUSERHANDLE,
  V_EXCHANGERATE,
  V_CREATEDATE,
  VSE_VERSIONSERVICEELEMENTID,
  VSE_CREATEDATE,
  VSE_LUPDDATE,
  VSES_STATUSTYPECODE,
  SE_SERVICEELEMENTID,
  SE_USID,
  SE_SERVICEELEMENTCLASS,
  SE_SERVICEELEMENTNAME,
  SE_DESCRIPTION,
  SE_CREATIONDATE,
  SE_GRANDFATHERDATE,
  SE_STARTBILLINGDATE,
  SE_ENDBILLINGDATE,
  SE_PRODID,
  U_SYSTEMUSERNAME,
  U_SYSTEMCODE,
  I_SERVICEATTRIBUTEID,
  I_LUPDDATE,
  I_CHANGETYPECODE,
  T_SERVICEATTRIBUTEID,
  T_SERVICEATTRIBUTENAME,
  T_VALUE,
  VSE_SOURCESYSTEM
)
AS SELECT
       v.custhandle                v_custhandle
     , v.sitehandle                v_sitehandle
     , v.servicehandle             v_servicehandle
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
         , se.sourcesystem                         vse_sourcesystem
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
/
spool off;

quit
