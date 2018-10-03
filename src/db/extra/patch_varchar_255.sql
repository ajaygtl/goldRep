
ALTER TABLE csi2.COnceOffCharge MODIFY (lupdBy VARCHAR2(255))
/
ALTER TABLE csi2.COnceOffCharge MODIFY (chgCatId VARCHAR2(255))
/
ALTER TABLE csi2.COnceOffCharge MODIFY (createdBy VARCHAR2(255))
/
ALTER TABLE csi2.COnceOffCharge MODIFY (discCode VARCHAR2(255))
/

ALTER TABLE csi2.CSystemUser MODIFY (systemUserName VARCHAR2(255))
/
ALTER TABLE csi2.CSystemUser MODIFY (systemCode VARCHAR2(255))
/
ALTER TABLE csi2.CSystemUser MODIFY (lupdBy VARCHAR2(255))
/
ALTER TABLE csi2.CSystemUser MODIFY (createdBy VARCHAR2(255))
/

ALTER TABLE csi2.CUsageCharge MODIFY (chgCatId VARCHAR2(255))
/
ALTER TABLE csi2.CUsageCharge MODIFY (createdBy VARCHAR2(255))
/
ALTER TABLE csi2.CUsageCharge MODIFY (lupdBy VARCHAR2(255))
/

ALTER TABLE csi2.CVersion MODIFY (description VARCHAR2(255))
/
ALTER TABLE csi2.CVersion MODIFY (ordHandle VARCHAR2(255))
/
ALTER TABLE csi2.CVersion MODIFY (ctrHandle VARCHAR2(255))
/
ALTER TABLE csi2.CVersion MODIFY (siteHandle VARCHAR2(255))
/
ALTER TABLE csi2.CVersion MODIFY (custHandle VARCHAR2(255))
/
ALTER TABLE csi2.CVersion MODIFY (endUserHandle VARCHAR2(255))
/
ALTER TABLE csi2.CVersion MODIFY (serviceHandle VARCHAR2(255))
/
ALTER TABLE csi2.CVersion MODIFY (ratingCurrency VARCHAR2(255))
/
ALTER TABLE csi2.CVersion MODIFY (createdBy VARCHAR2(255))
/
ALTER TABLE csi2.CVersion MODIFY (lupdBy VARCHAR2(255))
/

ALTER TABLE csi2.CVersionServiceElementSts MODIFY (createdBy VARCHAR2(255))
/
ALTER TABLE csi2.CVersionServiceElementSts MODIFY (lupdBy VARCHAR2(255))
/

ALTER TABLE csi2.CServiceElement MODIFY (usid VARCHAR2(255))
/
ALTER TABLE csi2.CServiceElement MODIFY (serviceElementName VARCHAR2(255))
/
ALTER TABLE csi2.CServiceElement MODIFY (description VARCHAR2(255))
/
ALTER TABLE csi2.CServiceElement MODIFY (prodId VARCHAR2(255))
/
ALTER TABLE csi2.CServiceElement MODIFY (lupdBy VARCHAR2(255))
/
ALTER TABLE csi2.CServiceElement MODIFY (createdBy VARCHAR2(255))
/

ALTER TABLE csi2.CServiceChangeItem MODIFY (createdBy VARCHAR2(255))
/
ALTER TABLE csi2.CServiceChangeItem MODIFY (lupdBy VARCHAR2(255))
/
ALTER TABLE csi2.CServiceChangeItem MODIFY (changeTypeCode VARCHAR2(255))
/

ALTER TABLE csi2.CRecurringCharge MODIFY (discCode VARCHAR2(255))
/
ALTER TABLE csi2.CRecurringCharge MODIFY (chgCatId VARCHAR2(255))
/
ALTER TABLE csi2.CRecurringCharge MODIFY (createdBy VARCHAR2(255))
/
ALTER TABLE csi2.CRecurringCharge MODIFY (lupdBy VARCHAR2(255))
/

ALTER TABLE csi2.CServiceAttribute MODIFY (serviceAttributeName VARCHAR2(255))
/
ALTER TABLE csi2.CServiceAttribute MODIFY (value VARCHAR2(255))
/
ALTER TABLE csi2.CServiceAttribute MODIFY (createdBy VARCHAR2(255))
/
ALTER TABLE csi2.CServiceAttribute MODIFY (lupdBy VARCHAR2(255))
/

ALTER TABLE csi2.CRefType MODIFY (createdBy VARCHAR2(255))
/
ALTER TABLE csi2.CRefType MODIFY (lupdBy VARCHAR2(255))
/

ALTER TABLE csi2.CChargeChangeItem MODIFY (changeTypeCode VARCHAR2(255))
/
ALTER TABLE csi2.CChargeChangeItem MODIFY (createdBy VARCHAR2(255))
/
ALTER TABLE csi2.CChargeChangeItem MODIFY (lupdBy VARCHAR2(255))
/
ALTER TABLE csi2.CChargeChangeItem MODIFY (discCode VARCHAR2(255))
/

ALTER TABLE csi2.CVersionServiceElement MODIFY (createdBy VARCHAR2(255))
/
ALTER TABLE csi2.CVersionServiceElement MODIFY (lupdBy VARCHAR2(255))
/
