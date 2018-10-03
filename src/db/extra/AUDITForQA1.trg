CREATE OR REPLACE TRIGGER cserviceattribute_audit
  BEFORE INSERT OR UPDATE OR DELETE ON CServiceAttribute
  For each row  
BEGIN
  
  IF UPDATING THEN
    insert into caudit values ('CSERVICEATTRIBUTE', :old.serviceattributeid, :old.serviceattributename||'||'||:old.value, :new.serviceattributename||'|'||:new.value, sysdate);
  END IF;
  
  IF INSERTING THEN
    insert into caudit values ('CSERVICEATTRIBUTE', :new.serviceattributeid, null, :new.serviceattributename||'||'||:new.value, sysdate);
  END IF;  
  
  IF DELETING THEN
    insert into caudit values ('CSERVICEATTRIBUTE', :old.serviceattributeid, :old.serviceattributename||'||'||:old.value, null, sysdate);
  END IF;  
  
END;
/

