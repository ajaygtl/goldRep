-- select distinct versionid from cversion where ordhandle == OrderHandle and ordertype != CSIConstants.ORDER_TYPE_EXISTING
-- select distinct versionid from cversion where ordhandle = 'TEST-ORD-001' and ordertype != 'Existing'
-- select distinct
--	   versionserviceelementid
-- from 
--	 cversionserviceelement 
-- where 
--	  versionid in 
--	  			(select versionid from cversion where ordhandle = 'TEST-ORD-001' and ordertype != 'Existing')
-- select distinct versionserviceelementid from cversionserviceelement where versionid in (select versionid from cversion where ordhandle = 'TEST-ORD-001' and ordertype != 'Existing')
-- select distinct chargechangeitemid from cchargechangeitem where versionserviceelementid in (select versionserviceelementid from cversionserviceelement where versionid in (select versionid from cversion where ordhandle = 'TEST-ORD-001' and ordertype != 'Existing')) 

-- select usagechargeid from cusagecharge where usagechargeid in (select distinct usagechargeid from cchargechangeitem where versionserviceelementid in (select versionserviceelementid from cversionserviceelement where versionid in (select versionid from cversion where ordhandle = 'TEST-ORD-001' and ordertype != 'Existing')))
-- select recurringchargeid from crecurringcharge where recurringchargeid in (select distinct recurringchargeid from cchargechangeitem where versionserviceelementid in (select versionserviceelementid from cversionserviceelement where versionid in (select versionid from cversion where ordhandle = 'TEST-ORD-001' and ordertype != 'Existing')))
-- select onceoffchargeid from conceoffcharge where onceoffchargeid in (select distinct onceoffchargeid from cchargechangeitem where versionserviceelementid in (select versionserviceelementid from cversionserviceelement where versionid in (select versionid from cversion where ordhandle = 'TEST-ORD-001' and ordertype != 'Existing')))

SELECT ServiceChangeItemID, ServiceElementID , ServiceElementID_1 , VersionServiceElementID , ServiceAttributeID FROM   CServiceChangeItem 
WHERE  ServiceChangeItemID in 
	   (SELECT ServiceChangeItemID 
	   FROM CVersionServiceElement) 
