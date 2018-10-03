SET SCAN ON VERIFY OFF

WHENEVER SQLERROR EXIT SUCCESS
CONNECT &&USER_NAME/&&USER_PASS@&&SERVICE_NAME
WHENEVER SQLERROR EXIT SUCCESS

/***********************************************************************
** Create Tables, Constraints, Indexes. (Generated with ObjectFronfier)
*/
CREATE TABLE object_id_generator      (
            attribute_name            VARCHAR2(50)                  NOT NULL,
                class_name            VARCHAR2(50)                  NOT NULL,
                  id_value              NUMBER(38)                  NOT NULL)
/
CREATE TABLE TestTable                (
               TestTableId              NUMBER(10)                  NOT NULL,
                      Name            VARCHAR2(50)                      NULL,
                     Place            VARCHAR2(50)                      NULL, primary key (TestTableId))
/
CREATE TABLE CUsageCharge             (
             usageChargeId              NUMBER(38)                  NOT NULL,
                  chgCatId            VARCHAR2(255)                     NULL,
                createDate                DATE                          NULL,
                 createdBy            VARCHAR2(255)                 NOT NULL,
                  lupdDate                DATE                          NULL,
                    lupdBy            VARCHAR2(255)                     NULL, primary key (usageChargeId))
/
CREATE TABLE CSystemUser              (
              systemUserId              NUMBER(38)                  NOT NULL,
            systemUserName            VARCHAR2(255)                 NOT NULL,
                systemCode            VARCHAR2(255)                     NULL,
                createDate                DATE                          NULL,
                  lupdDate                DATE                          NULL,
                    lupdBy            VARCHAR2(255)                     NULL,
                 createdBy            VARCHAR2(255)                 NOT NULL, primary key (systemUserId))
/
CREATE TABLE COnceOffCharge           (
           onceOffChargeId              NUMBER(38)                  NOT NULL,
                    amount              NUMBER(10,1)                    NULL,
                createDate                DATE                          NULL,
                  lupdDate                DATE                          NULL,
                    lupdBy            VARCHAR2(255)                     NULL,
                  chgCatId            VARCHAR2(255)                     NULL,
                 createdBy            VARCHAR2(255)                 NOT NULL,
                  discCode            VARCHAR2(255)                     NULL, primary key (onceOffChargeId))
/
CREATE TABLE CVersion                 (
                 versionId              NUMBER(38)                  NOT NULL,
               description            VARCHAR2(255)                     NULL,
                 sbaHandle              NUMBER(38)                  NOT NULL,
                 ordHandle            VARCHAR2(255)                     NULL,
                 ctrHandle            VARCHAR2(255)                     NULL,
                siteHandle            VARCHAR2(255)                     NULL,
                 orderType            VARCHAR2(50)                      NULL,
                custHandle            VARCHAR2(255)                     NULL,
             endUserHandle            VARCHAR2(255)                     NULL,
             serviceHandle            VARCHAR2(255)                     NULL,
            ratingCurrency            VARCHAR2(255)                     NULL,
              exchangeRate              NUMBER(10,1)                    NULL,
                createDate                DATE                          NULL,
                 createdBy            VARCHAR2(255)                 NOT NULL,
                  lupdDate                DATE                          NULL,
                    lupdBy            VARCHAR2(255)                     NULL,
               orderStatus            VARCHAR2(50)                      NULL,
                 sysUserId              NUMBER(38)                      NULL,
              systemUserId              NUMBER(38)                  NOT NULL,
			 LOCALCURRENCY			  VARCHAR2(30)						NULL, 
			LOCALCURRENCYEXCHANGERATE NUMBER(10,4)						NULL, 
				CoreSiteID            VARCHAR2(40) 	                    NULL,
				AddressID             VARCHAR2(40)                      NULL,primary key (versionId))
/
CREATE TABLE CVersionServiceElementSts(
versionServiceElementStsId              NUMBER(38)                  NOT NULL,
            statusTypeCode            VARCHAR2(50)                  NOT NULL,
                statusDate                DATE                          NULL,
                createDate                DATE                          NULL,
                  lupdDate                DATE                          NULL,
                 createdBy            VARCHAR2(255)                 NOT NULL,
                    lupdBy            VARCHAR2(255)                     NULL,
   versionServiceElementId              NUMBER(38)                  NOT NULL, primary key (versionServiceElementStsId))
/
CREATE TABLE CServiceElement          (
          serviceElementId              NUMBER(38)                  NOT NULL,
                      usid            VARCHAR2(255)                 NOT NULL,
       serviceElementClass            VARCHAR2(50)                  NOT NULL,
        serviceElementName            VARCHAR2(255)                     NULL,
               description            VARCHAR2(255)                     NULL,
              creationDate                DATE                          NULL,
           grandFatherDate                DATE                          NULL,
          startBillingDate                DATE                          NULL,
            endBillingDate                DATE                          NULL,
                    prodId            VARCHAR2(255)                     NULL,
                createDate                DATE                          NULL,
                  lupdDate                DATE                          NULL,
                    lupdBy            VARCHAR2(255)                     NULL,
                 createdBy            VARCHAR2(255)                 NOT NULL, primary key (serviceElementId))
/
CREATE TABLE CServiceChangeItem       (
       serviceChangeItemId              NUMBER(38)                  NOT NULL,
                createDate                DATE                          NULL,
                 createdBy            VARCHAR2(255)                 NOT NULL,
                  lupdDate                DATE                          NULL,
                    lupdBy            VARCHAR2(255)                     NULL,
            changeTypeCode            VARCHAR2(255)                     NULL,
        serviceAttributeId              NUMBER(38)                      NULL,
   versionServiceElementId              NUMBER(38)                  NOT NULL,
          serviceElementId              NUMBER(38)                      NULL,
        serviceElementId_1              NUMBER(38)                      NULL, primary key (serviceChangeItemId))
/
CREATE TABLE CRecurringCharge         (
         recurringChargeId              NUMBER(38)                  NOT NULL,
                    amount              NUMBER(10,1)                    NULL,
                  discCode            VARCHAR2(255)                     NULL,
                  chgCatId            VARCHAR2(255)                     NULL,
                createDate                DATE                          NULL,
                  lupdDate                DATE                          NULL,
                 createdBy            VARCHAR2(255)                 NOT NULL,
                    lupdBy            VARCHAR2(255)                     NULL, primary key (recurringChargeId))
/
CREATE TABLE CServiceAttribute        (
        serviceAttributeId              NUMBER(38)                  NOT NULL,
      serviceAttributeName            VARCHAR2(255)                     NULL,
                     value            VARCHAR2(255)                     NULL,
                prodAttrId            VARCHAR2(50)                      NULL,
                createDate                DATE                          NULL,
                 createdBy            VARCHAR2(255)                 NOT NULL,
                  lupdDate                DATE                          NULL,
                    lupdBy            VARCHAR2(255)                     NULL, 
			 customerlabel		      VARCHAR2(78)						NULL, 
			 localcurrency		      NUMBER(1,0)						NULL, primary key (serviceAttributeId))
/
CREATE TABLE CRefType                 (
                 refTypeId              NUMBER(38)                  NOT NULL,
                      code            VARCHAR2(50)                  NOT NULL,
                domainName            VARCHAR2(50)                  NOT NULL,
                domainType            VARCHAR2(50)                  NOT NULL,
               description            VARCHAR2(50)                      NULL,
                createDate                DATE                          NULL,
                 createdBy            VARCHAR2(255)                 NOT NULL,
                  lupdDate                DATE                          NULL,
                    lupdBy            VARCHAR2(255)                     NULL, primary key (refTypeId))
/
CREATE TABLE CChargeChangeItem        (
        chargeChangeItemId              NUMBER(38)                  NOT NULL,
            changeTypeCode            VARCHAR2(255)                     NULL,
               chargeLevel            VARCHAR2(50)                      NULL,
                createDate                DATE                          NULL,
                 createdBy            VARCHAR2(255)                 NOT NULL,
                    lupdBy            VARCHAR2(255)                     NULL,
                  discCode            VARCHAR2(255)                     NULL,
                  lupdDate                DATE                          NULL,
           onceOffChargeId              NUMBER(38)                      NULL,
         recurringChargeId              NUMBER(38)                      NULL,
        serviceAttributeId              NUMBER(38)                      NULL,
             usageChargeId              NUMBER(38)                      NULL,
   versionServiceElementId              NUMBER(38)                  NOT NULL, primary key (chargeChangeItemId))
/
CREATE TABLE CVersionServiceElement   (
   versionServiceElementId              NUMBER(38)                  NOT NULL,
                createDate                DATE                          NULL,
                 createdBy            VARCHAR2(255)                 NOT NULL,
                  lupdDate                DATE                          NULL,
                    lupdBy            VARCHAR2(255)                     NULL,
                    newElm            VARCHAR2(50)                      NULL,
                 versionId              NUMBER(38)                  NOT NULL,
          serviceElementId              NUMBER(38)                  NOT NULL, primary key (versionServiceElementId))
/

ALTER TABLE CServiceChangeItem ADD CONSTRAINT CServChngItem_CServElem
    FOREIGN KEY  (serviceElementId)  REFERENCES CServiceElement (serviceElementId)
/
ALTER TABLE CVersionServiceElement ADD CONSTRAINT CVerServElem_CVersion
    FOREIGN KEY  (versionId)  REFERENCES CVersion (versionId)
/
ALTER TABLE CServiceChangeItem ADD CONSTRAINT CServChangeItem_CVerServElem
    FOREIGN KEY  (versionServiceElementId)  REFERENCES CVersionServiceElement (versionServiceElementId)
/
ALTER TABLE CChargeChangeItem ADD CONSTRAINT CChrgChngItem_CRecurCharge
    FOREIGN KEY  (recurringChargeId)  REFERENCES CRecurringCharge (recurringChargeId)
/
ALTER TABLE CVersion ADD CONSTRAINT CVersion_CSystemUser
    FOREIGN KEY (systemUserId) REFERENCES CSystemUser (systemUserId)
/
ALTER TABLE CVersionServiceElementSts ADD CONSTRAINT CVerServElem_CVerServElemSts
    FOREIGN KEY  (versionServiceElementId)  REFERENCES CVersionServiceElement (versionServiceElementId)
/
ALTER TABLE CChargeChangeItem ADD CONSTRAINT CChrgChngItem_COnceOffCharge
    FOREIGN KEY  (onceOffChargeId)  REFERENCES COnceOffCharge (onceOffChargeId)
/
ALTER TABLE CChargeChangeItem ADD CONSTRAINT CChrgChngItem_CVerServElem
    FOREIGN KEY  (versionServiceElementId)  REFERENCES CVersionServiceElement (versionServiceElementId)
/
ALTER TABLE CServiceChangeItem ADD CONSTRAINT CServiceChangeItem_C_2
    FOREIGN KEY  (serviceElementId_1)  REFERENCES CServiceElement (serviceElementId)
/
ALTER TABLE CServiceChangeItem ADD CONSTRAINT CServChngItem_CServAttr
    FOREIGN KEY  (serviceAttributeId)  REFERENCES CServiceAttribute (serviceAttributeId)
/
ALTER TABLE CVersionServiceElement ADD CONSTRAINT CVerServElem_CServElem
    FOREIGN KEY  (serviceElementId)  REFERENCES CServiceElement (serviceElementId)
/
ALTER TABLE CChargeChangeItem ADD CONSTRAINT CChrgChngItem_CUsageChrg
    FOREIGN KEY  (usageChargeId)  REFERENCES CUsageCharge (usageChargeId)
/
ALTER TABLE CChargeChangeItem ADD CONSTRAINT CChrgChngItem_CServAttr
    FOREIGN KEY  (serviceAttributeId)  REFERENCES CServiceAttribute (serviceAttributeId)
/

CREATE INDEX FK_CVersionServiceElement_01 on CVersionServiceElement (versionId) TABLESPACE &&INDEX_TBLSPACE;
CREATE INDEX FK_CVersionServiceElement_02 on CVersionServiceElement (serviceElementId) TABLESPACE &&INDEX_TBLSPACE;
CREATE INDEX FK_CChargeChangeItem_01 on CChargeChangeItem (onceOffChargeId) TABLESPACE &&INDEX_TBLSPACE;
CREATE INDEX FK_CChargeChangeItem_02 on CChargeChangeItem (recurringChargeId) TABLESPACE &&INDEX_TBLSPACE;
CREATE INDEX FK_CChargeChangeItem_03 on CChargeChangeItem (serviceAttributeId) TABLESPACE &&INDEX_TBLSPACE;
CREATE INDEX FK_CChargeChangeItem_04 on CChargeChangeItem (usageChargeId) TABLESPACE &&INDEX_TBLSPACE;
CREATE INDEX FK_CChargeChangeItem_05 on CChargeChangeItem (versionServiceElementId) TABLESPACE &&INDEX_TBLSPACE;
CREATE INDEX FK_CServiceChangeItem_01 on CServiceChangeItem (serviceAttributeId) TABLESPACE &&INDEX_TBLSPACE;
CREATE INDEX FK_CServiceChangeItem_02 on CServiceChangeItem (versionServiceElementId) TABLESPACE &&INDEX_TBLSPACE;
CREATE INDEX FK_CServiceChangeItem_03 on CServiceChangeItem (serviceElementId) TABLESPACE &&INDEX_TBLSPACE;
CREATE INDEX FK_CServiceChangeItem_04 on CServiceChangeItem (serviceElementId_1) TABLESPACE &&INDEX_TBLSPACE;
CREATE UNIQUE INDEX UK_CServiceElement_usid on CServiceElement (usid) TABLESPACE &&INDEX_TBLSPACE;
CREATE INDEX FK_CVersionServiceElemSts_01 on CVersionServiceElementSts (versionServiceElementId) TABLESPACE &&INDEX_TBLSPACE;
CREATE INDEX FK_CVersion_01 on CVersion (systemUserId) TABLESPACE &&INDEX_TBLSPACE;

INSERT INTO object_id_generator(class_name,attribute_name,id_value) VALUES ('com.equant.csi.jdo.CVersionServiceElementSts','versionServiceElementStsId',1);
INSERT INTO object_id_generator(class_name,attribute_name,id_value) VALUES ('com.equant.csi.jdo.CServiceAttribute','serviceAttributeId',1);
INSERT INTO object_id_generator(class_name,attribute_name,id_value) VALUES ('com.equant.csi.jdo.CServiceChangeItem','serviceChangeItemId',1);
INSERT INTO object_id_generator(class_name,attribute_name,id_value) VALUES ('com.equant.csi.jdo.CRefType','refTypeId',1);
INSERT INTO object_id_generator(class_name,attribute_name,id_value) VALUES ('com.equant.csi.jdo.CVersionServiceElement','versionServiceElementId',1);
INSERT INTO object_id_generator(class_name,attribute_name,id_value) VALUES ('com.equant.csi.jdo.CChargeChangeItem','chargeChangeItemId',1);
INSERT INTO object_id_generator(class_name,attribute_name,id_value) VALUES ('com.equant.csi.jdo.COnceOffCharge','onceOffChargeId',1);
INSERT INTO object_id_generator(class_name,attribute_name,id_value) VALUES ('com.equant.csi.jdo.CSystemUser','systemUserId',1);
INSERT INTO object_id_generator(class_name,attribute_name,id_value) VALUES ('com.equant.csi.jdo.CUsageCharge','usageChargeId',1);
INSERT INTO object_id_generator(class_name,attribute_name,id_value) VALUES ('com.equant.csi.jdo.TestTable','TestTableId',1);
INSERT INTO object_id_generator(class_name,attribute_name,id_value) VALUES ('com.equant.csi.jdo.CRecurringCharge','recurringChargeId',1);
INSERT INTO object_id_generator(class_name,attribute_name,id_value) VALUES ('com.equant.csi.jdo.CServiceElement','serviceElementId',1);
INSERT INTO object_id_generator(class_name,attribute_name,id_value) VALUES ('com.equant.csi.jdo.CVersion','versionId',1);

/***********************************************************************
** Apply patch
*/
CREATE INDEX IDX_CVersion_01 ON CVersion (sitehandle, custhandle);
CREATE INDEX IDX_CVersion_02 ON CVersion (sitehandle, custhandle, servicehandle);
CREATE INDEX IDX_CVersion_03 ON CVersion (coresiteid, addressid, custhandle);
CREATE INDEX IDX_CVersion_04 ON CVersion (coresiteid, addressid, custhandle, servicehandle);
CREATE INDEX IDX_CVersionServiceElement_01 ON CVersionServiceElement (createdate);
CREATE INDEX IDX_CVersionServiceElement_02 ON CVersionServiceElement (lupddate);
CREATE INDEX IDX_CVersionServiceElemSts_01 ON CVersionServiceElementSts (statustypecode);
CREATE INDEX IDX_CVersionServiceElemSts_02 ON CVersionServiceElementSts (statusdate);
CREATE INDEX IDX_CChargeChangeItem_01 ON CChargeChangeItem (chargelevel);

/***********************************************************************
** Create Views
*/

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
