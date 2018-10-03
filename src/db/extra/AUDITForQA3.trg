CREATE OR REPLACE TRIGGER cverserelmsts_audit
  BEFORE INSERT OR UPDATE OR DELETE ON CVersionServiceElementSts
  For each row  
BEGIN
  
  IF UPDATING THEN
    insert into caudit values ('CVERSIONSERVICESTS', :old.versionserviceelementstsid, :old.statustypecode, :new.statustypecode, sysdate);
  END IF;
  
  IF INSERTING THEN
    insert into caudit values ('CVERSIONSERVICESTS', :new.versionserviceelementstsid, null, :new.statustypecode, sysdate);
  END IF;  
  
  IF DELETING THEN
    insert into caudit values ('CVERSIONSERVICESTS', :old.versionserviceelementstsid, :old.statustypecode, null, sysdate);
  END IF;  
  
END;
/
