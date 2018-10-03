SET echo ON
spool db_patch-v3.3.0.LOG

DECLARE
cur_se_id   NUMBER(38);
cur_vse_id   NUMBER(38);
CURSOR srvelem IS
  SELECT UNIQUE se.SERVICEELEMENTID
       FROM  CSERVICEELEMENT se, CVERSIONSERVICEELEMENT vse, CVERSION v
       WHERE se.SERVICEELEMENTID = vse.SERVICEELEMENTID
         AND vse.VERSIONID = v.VERSIONID
         AND se.SERVICEELEMENTCLASS = 'ServiceOptions'
         AND v.SERVICEHANDLE != 'CSP_PURPLE'
  	   AND SUBSTR(se.USID,1,3)= 'SO:'
         AND 0 < (SELECT COUNT(se1.SERVICEELEMENTID)
                    FROM  CSERVICEELEMENT se1,
                          CVERSIONSERVICEELEMENT vse1,
                          CVERSION v1
                    WHERE se1.SERVICEELEMENTID != se.SERVICEELEMENTID
                          AND se1.SERVICEELEMENTID = vse1.SERVICEELEMENTID
                          AND vse1.VERSIONID = v1.VERSIONID
                          AND se1.SERVICEELEMENTCLASS = 'ServiceOptions'
                          AND v1.SERVICEHANDLE != 'CSP_PURPLE'
                          AND v1.CUSTHANDLE = v.CUSTHANDLE
                          AND v1.SITEHANDLE = v.SITEHANDLE
                          AND v1.SERVICEHANDLE = v.SERVICEHANDLE
                 );

CURSOR ver_serv_elem( seID IN NUMBER) IS
         SELECT versionServiceElementID FROM CVersionServiceElement
              WHERE (serviceelementID = seID);

BEGIN
	 OPEN srvelem;
	 
	 <<se>>
	 LOOP
	 
	 	 FETCH srvelem INTO cur_se_id;
		 EXIT se WHEN srvelem%NOTFOUND;
		 
		 OPEN ver_serv_elem(cur_se_id);

		 <<vse>>
		 LOOP
		 
		 	 FETCH ver_serv_elem INTO cur_vse_id;
			 EXIT vse WHEN ver_serv_elem%NOTFOUND;
			 
   			 DELETE FROM CCHARGECHANGEITEM WHERE VERSIONSERVICEELEMENTID = cur_vse_id;
			 
			 DELETE FROM CSERVICECHANGEITEM WHERE VERSIONSERVICEELEMENTID = cur_vse_id;
			 
			 DELETE FROM CVERSIONSERVICEELEMENTSTS WHERE VERSIONSERVICEELEMENTID = cur_vse_id;
			 
			 DELETE FROM CVERSIONSERVICEELEMENT WHERE VERSIONSERVICEELEMENTID = cur_vse_id;
			    
		 END LOOP vse;

		 CLOSE ver_serv_elem;
		 
		DBMS_OUTPUT.PUT_LINE('[INFO] Deleting ServiceElementId = ' || cur_se_id );		 
		
		DELETE FROM CSERVICEELEMENT WHERE SERVICEELEMENTID = cur_se_id;

	 END LOOP se;
	 
	 CLOSE srvelem;
END;
/

DECLARE
cur_se_id   NUMBER(38);
new_usid    VARCHAR2(255);
old_usid    VARCHAR2(255);
CURSOR usid_cursor IS
    SELECT UNIQUE se.SERVICEELEMENTID, 'SO::' || v.CUSTHANDLE || '::' || v.SITEHANDLE || '::' || v.SERVICEHANDLE 
    FROM  CSERVICEELEMENT se,
          CVERSIONSERVICEELEMENT vse,
          CVERSION v
    WHERE se.SERVICEELEMENTID = vse.SERVICEELEMENTID
      AND vse.VERSIONID = v.VERSIONID
      AND se.SERVICEELEMENTCLASS = 'ServiceOptions'
      AND v.SERVICEHANDLE != 'CSP_PURPLE';
BEGIN
    OPEN usid_cursor;
    LOOP
        FETCH usid_cursor INTO cur_se_id, new_usid;
        EXIT WHEN usid_cursor%NOTFOUND;
        DBMS_OUTPUT.PUT_LINE('[INFO] Updating ' || cur_se_id || ' record to new usid = "' || new_usid || '"');
        UPDATE CSERVICEELEMENT SET USID = new_usid WHERE ServiceElementID = cur_se_id AND USID != new_usid;
    END LOOP;
    CLOSE usid_cursor;
     
    COMMIT;
END;
/

-- change column length
ALTER TABLE CCISEXTRACTLOG MODIFY (
NOTE	VARCHAR2(2000) );

-- create new audit table
DROP TABLE CAUDIT;
CREATE TABLE CAUDIT
(
  AUDIT_TABLENAME    VARCHAR2(50),
  AUDIT_PRIMARY_KEY  NUMBER,
  AUDIT_OLD_VALUE    VARCHAR2(2000),
  AUDIT_NEW_VALUE    VARCHAR2(2000),
  AUDIT_TIME	   DATE
)
TABLESPACE csi_data
STORAGE    (
            INITIAL          512K
            NEXT             512K
            PCTINCREASE      0
           )
;


-- migrate usid for Nasbackup

update cserviceelement
set usid = 'NASBKP: '||usid
where serviceelementclass = 'NASBackup'
and usid not like 'NASBKP: %' 
and 'NASBKP: '||usid not in
(	  select usid
	  from cserviceelement
	  where usid like 'NASBKP:%'	  
	  );

commit;

spool OFF;

quit
