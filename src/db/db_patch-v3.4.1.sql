SET echo ON
SET serveroutput on
spool db_patch-v3.4.1.LOG

DECLARE
cur_se_usid  VARCHAR2(50);
cur_vse_id   NUMBER(38);
cur_count     NUMBER(38);
insert_count NUMBER(38);

prev_se_usid  VARCHAR2(50);
srvattr_id    NUMBER(38);


CURSOR srvelem IS
  SELECT DISTINCT
       se.usid
     , vse.versionserviceelementid                                 
  FROM cserviceelement           se
     , cversionserviceelement    vse
     , cversionserviceelementsts vses
     , cservicechangeitem        i
     , cserviceattribute         t
 WHERE vse.serviceelementid = se.serviceelementid
   AND vse.versionserviceelementid = vses.versionserviceelementid
   AND vses.statustypecode in ('InProgress', 'Current')
   AND vse.versionserviceelementid = i.versionserviceelementid(+)
   AND i.serviceattributeid = t.serviceattributeid(+)
   AND se.SERVICEELEMENTCLASS = 'Transport'
   AND se.SOURCESYSTEM = 'CIS'
   AND 0 =
   (SELECT count(se1.USID)
   FROM cserviceelement           se1
     , cversionserviceelement    vse1
     , cversionserviceelementsts vses1
     , cservicechangeitem        i1
     , cserviceattribute         t1
 WHERE vse1.serviceelementid = se1.serviceelementid
   AND vse1.versionserviceelementid = vses1.versionserviceelementid
   AND se1.usid = se.USID
   AND vses1.statustypecode in ('InProgress', 'Current')
   AND vse1.versionserviceelementid = i1.versionserviceelementid(+)
   AND i1.serviceattributeid = t1.serviceattributeid(+)
   AND se1.SERVICEELEMENTCLASS = 'Transport'
   AND t1.SERVICEATTRIBUTENAME = 'Transport Type'
   AND se1.SOURCESYSTEM = 'CIS')
  order by se.usid, vse.versionserviceelementid;


BEGIN
	 DBMS_OUTPUT.enable(2000000);
	 prev_se_usid := 'BEGINING';
	 cur_count := 0;
	 insert_count := 0;
	 
	 OPEN srvelem;
	 
	 LOOP 
	 FETCH srvelem INTO cur_se_usid, cur_vse_id;
	 EXIT WHEN srvelem%NOTFOUND;
	 	 
	 IF cur_se_usid != prev_se_usid
            THEN
               
               select SEQSERVICEATTRIBUTE.nextval into srvattr_id from dual;
               
               insert into cserviceattribute (serviceattributeid, serviceattributename, value, createdate, createdby, lupddate, lupdby)
                                       values(srvattr_id, 'Transport Type', 'UNKNOWN', sysdate, 'CISADM', sysdate, 'CISADM');
               
               insert into cservicechangeitem (servicechangeitemid, createdate, createdby, lupddate, lupdby, serviceattributeid, versionserviceelementid) 
                                       values (seqservicechangeitem.nextval, sysdate, 'CISADM', sysdate, 'CISADM', srvattr_id, cur_vse_id);
               
               insert_count := insert_count + 1;
               DBMS_OUTPUT.PUT_LINE('[INFO] added usid = ' || cur_se_usid );
         END IF;	 
         
         prev_se_usid := cur_se_usid;
         cur_count := cur_count + 1;
         
         IF MOD(insert_count, 500) = 0 
            THEN
            	commit;
         END IF;
	 
	 END LOOP;
	 CLOSE srvelem;	
	 
	 commit;
	 
	 DBMS_OUTPUT.PUT_LINE('[INFO] Total records added: '||insert_count );
     	 DBMS_OUTPUT.PUT_LINE('[INFO] Total records handled: ' || cur_count ); 
END;
/
spool OFF;
SET echo OFF

