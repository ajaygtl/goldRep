-- changes for correct functioning after CR5030 comes out
-- CR5030 moves some sections from Main Page to Access Connection Page
-- this script does migration in CSI for the moved parts
SET echo ON
spool db_patch-v3.2.0_CR5030.log;

DECLARE
	CURSOR  cur_needdata IS
				SELECT
				--CSERVICECHANGEITEM  	
  				a.SERVICECHANGEITEMID AS sciSERVICECHANGEITEMID, 
  				a.CREATEDATE AS sciCREATEDATE, 
  				a.CREATEDBY AS sciCREATEDBY, 
  				a.LUPDDATE AS sciLUPDDATE, 
  				a.LUPDBY AS sciLUPDBY, 
  				a.CHANGETYPECODE AS sciCHANGETYPECODE, 
				a.SERVICEATTRIBUTEID AS sciSERVICEATTRIBUTEID, 
  				a.VERSIONSERVICEELEMENTID AS sciVERSIONSERVICEELEMENTID, 
  				a.SERVICEELEMENTID AS sciSERVICEELEMENTID, 
  				a.SERVICEELEMENTID_1 AS sciSERVICEELEMENTID_1,
				--CVERSIONSERVICEELEMENT
 				d.VERSIONSERVICEELEMENTID AS vceVERSIONSERVICEELEMENTID, 
				d.CREATEDATE AS vceCREATEDATE, 
  				d.CREATEDBY AS vceCREATEDBY, 
  				d.LUPDDATE AS vceLUPDDATE, 
  				d.LUPDBY AS vceLUPDBY, 
  				d.NEWELM AS vceNEWELM, 
  				d.VERSIONID AS vceVERSIONID, 
  				d.SERVICEELEMENTID AS vceSERVICEELEMENTID 
				FROM CSERVICECHANGEITEM a, CSERVICEATTRIBUTE b, CSERVICEELEMENT c, CVERSIONSERVICEELEMENT d, CVERSION e 
				WHERE a.SERVICEATTRIBUTEID=b.SERVICEATTRIBUTEID 
				AND a.VERSIONSERVICEELEMENTID=d.VERSIONSERVICEELEMENTID 
				AND d.SERVICEELEMENTID=c.SERVICEELEMENTID 
				AND e.VERSIONID=d.VERSIONID AND e.SERVICEHANDLE LIKE 'IP_VPN%' 
				AND c.SERVICEELEMENTCLASS='ServiceOptions' 
				AND b.SERVICEATTRIBUTENAME='Service Type';
	CURSOR cur_accesscon(ver NUMBER) IS
			   	SELECT  c.SERVICEELEMENTID 
				FROM  CSERVICEELEMENT c, CVERSIONSERVICEELEMENT d 
				WHERE  d.SERVICEELEMENTID=c.SERVICEELEMENTID
				AND d.VERSIONID=ver  
				AND c.SERVICEELEMENTCLASS='AccessConnection';
	CURSOR cur_versionelement(verid NUMBER) IS
		   		SELECT
				STATUSTYPECODE, 
  				STATUSDATE, 
  				CREATEDATE, 
  				LUPDDATE, 
  				CREATEDBY, 
  				LUPDBY
				FROM CVERSIONSERVICEELEMENTSTS
				WHERE VERSIONSERVICEELEMENTID=verid;  			
	Id NUMBER;						
BEGIN 
	 FOR lv_cur_needdata IN cur_needdata LOOP
	 	 FOR lv_cur_accesscon IN cur_accesscon(lv_cur_needdata.vceVERSIONID) LOOP
			 --Fill CVERSIONSERVICEELEMENT
			 SELECT SEQVERSIONSERVICEELEMENT.NEXTVAL INTO Id FROM dual;
			 INSERT INTO CVERSIONSERVICEELEMENT 
			 VALUES(
			  	Id, 
				lv_cur_needdata.vceCREATEDATE, 
  				lv_cur_needdata.vceCREATEDBY, 
  				lv_cur_needdata.vceLUPDDATE, 
  				lv_cur_needdata.vceLUPDBY, 
  				lv_cur_needdata.vceNEWELM, 
  				lv_cur_needdata.vceVERSIONID, 
  				lv_cur_accesscon.SERVICEELEMENTID
			 );
			 --Fill CVERSIONSERVICEELEMENTSTS
			 FOR lv_cur_versionelement IN cur_versionelement(lv_cur_needdata.vceVERSIONSERVICEELEMENTID) LOOP
			 	 INSERT INTO CVERSIONSERVICEELEMENTSTS
				 VALUES(
				 	SEQVERSIONSERVICEELEMENTSTS.NEXTVAL,
				 	lv_cur_versionelement.STATUSTYPECODE, 
  					lv_cur_versionelement.STATUSDATE, 
  					lv_cur_versionelement.CREATEDATE, 
  					lv_cur_versionelement.LUPDDATE, 
  					lv_cur_versionelement.CREATEDBY, 
  					lv_cur_versionelement.LUPDBY,
					id
				 );
			 END LOOP;
			 --Fill CSERVICECHANGEITEM
			 INSERT INTO CSERVICECHANGEITEM
			 VALUES(
			 		SEQSERVICECHANGEITEM.NEXTVAL,
			 		lv_cur_needdata.sciCREATEDATE, 
  			 		lv_cur_needdata.sciCREATEDBY, 
  			 		lv_cur_needdata.sciLUPDDATE, 
  			 		lv_cur_needdata.sciLUPDBY, 
  			 		lv_cur_needdata.sciCHANGETYPECODE, 
  			 		lv_cur_needdata.sciSERVICEATTRIBUTEID, 
  			 		id, 
  			 		lv_cur_needdata.sciSERVICEELEMENTID, 
  			 		lv_cur_needdata.sciSERVICEELEMENTID_1
			 );
		 END LOOP;
		 --Delete CSERVICECHANGEITEM
		 DELETE FROM CSERVICECHANGEITEM
		 WHERE SERVICECHANGEITEMID=lv_cur_needdata.sciSERVICECHANGEITEMID;
	END LOOP;	 
	COMMIT;
END;
/

spool OFF;
quit

