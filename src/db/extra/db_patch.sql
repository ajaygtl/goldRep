
CREATE INDEX idx_cversion_01 ON cversion(sitehandle, custhandle);
CREATE INDEX idx_cversion_02 ON cversion(sitehandle, custhandle, servicehandle);

CREATE INDEX idx_cversionservelem_01 ON cversionserviceelement(createdate);
CREATE INDEX idx_cversionservelem_02 ON cversionserviceelement(lupddate);

CREATE INDEX idx_vsests_01 ON cversionserviceelementsts(statustypecode);
CREATE INDEX idx_vsests_02 ON cversionserviceelementsts(statusdate);

CREATE INDEX idx_charge_01 ON cchargechangeitem(chargelevel);

