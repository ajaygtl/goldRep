 create table  csi2.TestTable                (
	                   TestTableId              NUMBER(10)                  NOT NULL,
	                          Name            VARCHAR2(50)                      NULL,
	                         Place            VARCHAR2(50)                      NULL, primary key (TestTableId)) 
/
 create table  csi2.object_id_generator      (
	                attribute_name            VARCHAR2(50)                  NOT NULL,
	                    class_name            VARCHAR2(50)                  NOT NULL,
	                      id_value              NUMBER(38)                  NOT NULL) 
/
 create table  csi2.COnceOffCharge           (
	               onceOffChargeId              NUMBER(38)                  NOT NULL,
	                        amount              NUMBER(10,1)                    NULL,
	                    createDate                DATE                          NULL,
	                      lupdDate                DATE                          NULL,
	                        lupdBy            VARCHAR2(255)                     NULL,
	                      chgCatId            VARCHAR2(255)                     NULL,
	                     createdBy            VARCHAR2(255)                 NOT NULL,
	                      discCode            VARCHAR2(255)                     NULL, primary key (onceOffChargeId)) 
/
 create table  csi2.CSystemUser              (
	                  systemUserId              NUMBER(38)                  NOT NULL,
	                systemUserName            VARCHAR2(255)                 NOT NULL,
	                    systemCode            VARCHAR2(255)                     NULL,
	                    createDate                DATE                          NULL,
	                      lupdDate                DATE                          NULL,
	                        lupdBy            VARCHAR2(255)                     NULL,
	                     createdBy            VARCHAR2(255)                 NOT NULL, primary key (systemUserId)) 
/
 create table  csi2.CUsageCharge             (
	                 usageChargeId              NUMBER(38)                  NOT NULL,
	                      chgCatId            VARCHAR2(255)                     NULL,
	                    createDate                DATE                          NULL,
	                     createdBy            VARCHAR2(255)                 NOT NULL,
	                      lupdDate                DATE                          NULL,
	                        lupdBy            VARCHAR2(255)                     NULL, primary key (usageChargeId)) 
/
 create table  csi2.CVersion                 (
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
	                  systemUserId              NUMBER(38)                  NOT NULL, primary key (versionId)) 
/
 create table  csi2.CVersionServiceElementSts(
	    versionServiceElementStsId              NUMBER(38)                  NOT NULL,
	                statusTypeCode            VARCHAR2(50)                  NOT NULL,
	                    statusDate                DATE                          NULL,
	                    createDate                DATE                          NULL,
	                      lupdDate                DATE                          NULL,
	                     createdBy            VARCHAR2(255)                 NOT NULL,
	                        lupdBy            VARCHAR2(255)                     NULL,
	       versionServiceElementId              NUMBER(38)                  NOT NULL, primary key (versionServiceElementStsId)) 
/
 create table  csi2.CServiceElement          (
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
 create table  csi2.CServiceChangeItem       (
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
 create table  csi2.CRecurringCharge         (
	             recurringChargeId              NUMBER(38)                  NOT NULL,
	                        amount              NUMBER(10,1)                    NULL,
	                      discCode            VARCHAR2(255)                     NULL,
	                      chgCatId            VARCHAR2(255)                     NULL,
	                    createDate                DATE                          NULL,
	                      lupdDate                DATE                          NULL,
	                     createdBy            VARCHAR2(255)                 NOT NULL,
	                        lupdBy            VARCHAR2(255)                     NULL, primary key (recurringChargeId)) 
/
 create table  csi2.CServiceAttribute        (
	            serviceAttributeId              NUMBER(38)                  NOT NULL,
	          serviceAttributeName            VARCHAR2(255)                     NULL,
	                         value            VARCHAR2(255)                     NULL,
	                    prodAttrId            VARCHAR2(50)                      NULL,
	                    createDate                DATE                          NULL,
	                     createdBy            VARCHAR2(255)                 NOT NULL,
	                      lupdDate                DATE                          NULL,
	                        lupdBy            VARCHAR2(255)                     NULL, primary key (serviceAttributeId)) 
/
 create table  csi2.CRefType                 (
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
 create table  csi2.CChargeChangeItem        (
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
 create table  csi2.CVersionServiceElement   (
	       versionServiceElementId              NUMBER(38)                  NOT NULL,
	                    createDate                DATE                          NULL,
	                     createdBy            VARCHAR2(255)                 NOT NULL,
	                      lupdDate                DATE                          NULL,
	                        lupdBy            VARCHAR2(255)                     NULL,
	                        newElm            VARCHAR2(50)                      NULL,
	                     versionId              NUMBER(38)                  NOT NULL,
	              serviceElementId              NUMBER(38)                  NOT NULL, primary key (versionServiceElementId)) 
/
ALTER TABLE csi2.CServiceChangeItem ADD CONSTRAINT CServiceChangeItem_CServiceElement FOREIGN KEY  (serviceElementId)  REFERENCES csi2.CServiceElement (serviceElementId)
/
ALTER TABLE csi2.CVersionServiceElement ADD CONSTRAINT CVersionServiceElement_CVersion FOREIGN KEY  (versionId)  REFERENCES csi2.CVersion (versionId)
/
ALTER TABLE csi2.CServiceChangeItem ADD CONSTRAINT CServiceChangeItem_CVersionServiceElement FOREIGN KEY  (versionServiceElementId)  REFERENCES csi2.CVersionServiceElement (versionServiceElementId)
/
ALTER TABLE csi2.CChargeChangeItem ADD CONSTRAINT CChargeChangeItem_CRecurringCharge FOREIGN KEY  (recurringChargeId)  REFERENCES csi2.CRecurringCharge (recurringChargeId)
/
ALTER TABLE csi2.CVersionServiceElementSts ADD CONSTRAINT CVersionServiceElement_CVersionServiceElementSts FOREIGN KEY  (versionServiceElementId)  REFERENCES csi2.CVersionServiceElement (versionServiceElementId)
/
ALTER TABLE csi2.CVersion ADD CONSTRAINT CVersion_CSystemUser FOREIGN KEY  (systemUserId)  REFERENCES csi2.CSystemUser (systemUserId)
/
ALTER TABLE csi2.CChargeChangeItem ADD CONSTRAINT CChargeChangeItem_COnceOffCharge FOREIGN KEY  (onceOffChargeId)  REFERENCES csi2.COnceOffCharge (onceOffChargeId)
/
ALTER TABLE csi2.CServiceChangeItem ADD CONSTRAINT CServiceChangeItem_C_2 FOREIGN KEY  (serviceElementId_1)  REFERENCES csi2.CServiceElement (serviceElementId)
/
ALTER TABLE csi2.CChargeChangeItem ADD CONSTRAINT CChargeChangeItem_CVersionServiceElement FOREIGN KEY  (versionServiceElementId)  REFERENCES csi2.CVersionServiceElement (versionServiceElementId)
/
ALTER TABLE csi2.CVersionServiceElement ADD CONSTRAINT CVersionServiceElement_CServiceElement FOREIGN KEY  (serviceElementId)  REFERENCES csi2.CServiceElement (serviceElementId)
/
ALTER TABLE csi2.CServiceChangeItem ADD CONSTRAINT CServiceChangeItem_CServiceAttribute FOREIGN KEY  (serviceAttributeId)  REFERENCES csi2.CServiceAttribute (serviceAttributeId)
/
ALTER TABLE csi2.CChargeChangeItem ADD CONSTRAINT CChargeChangeItem_CUsageCharge FOREIGN KEY  (usageChargeId)  REFERENCES csi2.CUsageCharge (usageChargeId)
/
ALTER TABLE csi2.CChargeChangeItem ADD CONSTRAINT CChargeChangeItem_CServiceAttribute FOREIGN KEY  (serviceAttributeId)  REFERENCES csi2.CServiceAttribute (serviceAttributeId)
/
create         index csi2.CVersionServiceEleme_2    on csi2.CVersionServiceElement   (
                                         versionId)
/
create         index csi2.CVersionServiceElement_idx_f on csi2.CVersionServiceElement   (
                                  serviceElementId)
/
create         index csi2.CChargeChangeItem_idx_f   on csi2.CChargeChangeItem        (
                                   onceOffChargeId)
/
create         index csi2.CChargeChangeItem_id_2    on csi2.CChargeChangeItem        (
                                 recurringChargeId)
/
create         index csi2.CChargeChangeItem_id_5    on csi2.CChargeChangeItem        (
                                serviceAttributeId)
/
create         index csi2.CChargeChangeItem_id_3    on csi2.CChargeChangeItem        (
                                     usageChargeId)
/
create         index csi2.CChargeChangeItem_id_4    on csi2.CChargeChangeItem        (
                           versionServiceElementId)
/
create         index csi2.CServiceChangeItem_i_4    on csi2.CServiceChangeItem       (
                                serviceAttributeId)
/
create         index csi2.CServiceChangeItem_i_3    on csi2.CServiceChangeItem       (
                           versionServiceElementId)
/
create         index csi2.CServiceChangeItem_idx_f  on csi2.CServiceChangeItem       (
                                  serviceElementId)
/
create         index csi2.CServiceChangeItem_i_2    on csi2.CServiceChangeItem       (
                                serviceElementId_1)
/
create  UNIQUE index csi2.CServiceElement_USID_u_idx on csi2.CServiceElement          (
                                              usid)
/
create         index csi2.CVersionServiceElementSts_F on csi2.CVersionServiceElementSts(
                           versionServiceElementId)
/
create         index csi2.CVersion_idx_f            on csi2.CVersion                 (
                                      systemUserId)
/
insert into csi2.object_id_generator(class_name,attribute_name,id_value) values ('com.equant.csi.jdo.CVersionServiceElementSts','versionServiceElementStsId',1)
/
insert into csi2.object_id_generator(class_name,attribute_name,id_value) values ('com.equant.csi.jdo.CServiceAttribute','serviceAttributeId',1)
/
insert into csi2.object_id_generator(class_name,attribute_name,id_value) values ('com.equant.csi.jdo.CServiceChangeItem','serviceChangeItemId',1)
/
insert into csi2.object_id_generator(class_name,attribute_name,id_value) values ('com.equant.csi.jdo.CRefType','refTypeId',1)
/
insert into csi2.object_id_generator(class_name,attribute_name,id_value) values ('com.equant.csi.jdo.CVersionServiceElement','versionServiceElementId',1)
/
insert into csi2.object_id_generator(class_name,attribute_name,id_value) values ('com.equant.csi.jdo.CChargeChangeItem','chargeChangeItemId',1)
/
insert into csi2.object_id_generator(class_name,attribute_name,id_value) values ('com.equant.csi.jdo.COnceOffCharge','onceOffChargeId',1)
/
insert into csi2.object_id_generator(class_name,attribute_name,id_value) values ('com.equant.csi.jdo.CSystemUser','systemUserId',1)
/
insert into csi2.object_id_generator(class_name,attribute_name,id_value) values ('com.equant.csi.jdo.CUsageCharge','usageChargeId',1)
/
insert into csi2.object_id_generator(class_name,attribute_name,id_value) values ('com.equant.csi.jdo.TestTable','TestTableId',1)
/
insert into csi2.object_id_generator(class_name,attribute_name,id_value) values ('com.equant.csi.jdo.CRecurringCharge','recurringChargeId',1)
/
insert into csi2.object_id_generator(class_name,attribute_name,id_value) values ('com.equant.csi.jdo.CServiceElement','serviceElementId',1)
/
insert into csi2.object_id_generator(class_name,attribute_name,id_value) values ('com.equant.csi.jdo.CVersion','versionId',1)
/
