/***********************************************************************
** Name:       viw_version_element_status
** Purpose:    Provides view on CVersion, CVersionServiceElement,
**             and CVersionServiceElementSts, with CSI status for
**             every CVersionServiceElement. GOLD statuses are omitted.
** Comments:   Used in Query to filter out Disconnected and Deleted
**             elements, and all their predecessors.
*/
CREATE OR REPLACE VIEW viw_version_element_status AS
SELECT v.custhandle                v_custhandle
     , v.sitehandle                v_sitehandle
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
   AND vses.statustypecode in ('InProgress', 'Current', 'Disconnect', 'Delete')
/

/***********************************************************************
** Name:       viw_version_element_attribute
** Purpose:    Provides view on CVersion, CVersionServiceElement,
**             CVersionServiceElementSts, CServiceChangeItem,
**             and CServiceAttribute. Allows to obtain Version from the
**             database with a signle select.
** Comments:   Used in Query to select a Version.
*/
CREATE OR REPLACE VIEW viw_version_element_attribute AS
SELECT v.custhandle                v_custhandle
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
   AND i.serviceattributeid = t.serviceattributeid(+)
/

/***********************************************************************
** Name:       viw_status
** Purpose:    Provides a view for statuces.
** Comments:   Additional selection logic.
*/
CREATE OR REPLACE VIEW viw_status
AS
SELECT base.versionserviceelementid            vse_id
     , base.statustypecode                     status_code
     , base.statusdate                         status_date
  FROM cversionserviceelementsts               base
 WHERE base.statustypecode = 'ReadyForService'
    OR base.statustypecode = 'ReadyForBilling'
    OR base.statustypecode = 'OrderDisconnect'
 ORDER BY
       base.versionserviceelementid
/

/***********************************************************************
** Name:       viw_charge
** Purpose:    Provides a view for all charges. All charges are in one line.
** Comments:   Simplifies select statements.
*/
CREATE OR REPLACE VIEW viw_charge
AS
SELECT cci.versionserviceelementid             vse_id
     , cci.serviceattributeid                  sa_id
     , cci.chargelevel                         cci_charge_level
     , cci.changetypecode                      cci_change_code
     , ooc.amount                              ooc_amount
     , ooc.chgcatid                            ooc_chgcatid
     , ooc.disccode                            ooc_disccode
     , rc.amount                               rc_amount
     , rc.chgcatid                             rc_chgcatid
     , rc.disccode                             rc_disccode
     , uc.chgcatid                             uc_chgcatid
  FROM cchargechangeitem                       cci
     , conceoffcharge                          ooc
     , crecurringcharge                        rc
     , cusagecharge                            uc
 WHERE cci.onceoffchargeid = ooc.onceoffchargeid(+)
   AND cci.recurringchargeid = rc.recurringchargeid(+)
   AND cci.usagechargeid = uc.usagechargeid(+)
/
