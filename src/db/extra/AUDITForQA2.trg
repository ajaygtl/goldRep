CREATE OR REPLACE TRIGGER cversion_audit
  BEFORE INSERT OR UPDATE OR DELETE ON CVersion
  For each row  
BEGIN
  
  IF UPDATING THEN
    insert into caudit values ('CVERSION', :old.versionid, :old.servicehandle||'||'||:old.sitehandle, :new.servicehandle||'|'||:new.sitehandle, sysdate);
  END IF;
  
  IF INSERTING THEN
    insert into caudit values ('CVERSION', :new.versionid, null, :new.servicehandle||'|'||:new.sitehandle, sysdate);
  END IF;  
  
  IF DELETING THEN
    insert into caudit values ('CVERSION', :old.versionid, :old.servicehandle||'||'||:old.sitehandle, null, sysdate);
  END IF;  
  
END;

/

