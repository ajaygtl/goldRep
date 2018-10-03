set echo on
spool db_patch-v2.1.4.log

/* ==============================================================*/
/*  table: csi_properties                                        */
/* ==============================================================*/
CREATE TABLE csi_properties         (
    name    VARCHAR2(255)   NOT NULL,
    value   VARCHAR2(255)   NOT NULL);

INSERT INTO csi_properties (name, value) VALUES ('CSI_VERSION', '2.1.4.');

/*==============================================================*/
/* Migrate all Global CSP versions from ServiceOption to        */
/* CSP class type                                               */
/*==============================================================*/
UPDATE CServiceElement SET ServiceElementClass = 'CSP' WHERE ServiceElementID in (
       SELECT UNIQUE
           se.ServiceElementID
       FROM
          CVersion v,
          CVersionServiceElement vse,
          CServiceElement se
       WHERE
          v.VersionID = vse.VersionID AND
          vse.ServiceElementID = se.ServiceElementID AND
          v.ServiceHandle = 'CSP_PURPLE' AND
          v.SiteHandle = 'GLOBAL' AND
          se.ServiceElementClass = 'ServiceOptions'
);

/*=================================================================*/
/* 1. Migrate all Global CSP USID to "display" style               */
/* 2. Migrate all Service names from product name to display name  */
/*=================================================================*/
CREATE OR REPLACE PROCEDURE tmp_update_csp_usid(oldserviceid IN VARCHAR2, newserviceid IN VARCHAR2) IS
    cur_usid    VARCHAR2(255);
    new_usid    VARCHAR2(255);
    cur_se      NUMBER(38);
    CURSOR usid_cursor (oldserviceid VARCHAR2, newserviceid VARCHAR2) IS
        SELECT ServiceElementID, USID FROM CServiceElement WHERE usid LIKE 'CSP_%: ' || oldserviceid AND usid NOT LIKE 'CSP_%: ' || newserviceid;
BEGIN
     OPEN usid_cursor(oldserviceid, newserviceid);

     LOOP
         FETCH usid_cursor INTO cur_se, cur_usid;
         EXIT WHEN usid_cursor%NOTFOUND;

         new_usid := SUBSTR(cur_usid, 1, INSTR(cur_usid, ': ' || oldserviceid)+1) || newserviceid;

         DBMS_OUTPUT.put_line('[INFO] Updating ' || cur_se || ' record id, where old usid = "' || cur_usid || '" and new usid = "' || new_usid || '"');

         UPDATE
             CServiceElement
         SET
             USID = new_usid
         WHERE
             ServiceElementID = cur_se;
     END LOOP;

     CLOSE usid_cursor;

     UPDATE
         CServiceAttribute
     SET
         Value = newserviceid
     WHERE
         ServiceAttributeName = 'Global CSP Service' AND
         Value = oldserviceid;
END;
/

BEGIN
     tmp_update_csp_usid('ATM_PURPLE', 'ATM');
     tmp_update_csp_usid('FRAME_RELAY_PURPLE', 'Frame Relay');
     tmp_update_csp_usid('IP_VPN', 'IP VPN');
     tmp_update_csp_usid('MANAGED_ROUTER_CPE_LANAS', 'LAN Access');
     tmp_update_csp_usid('VOICE_IPVPN_PURPLE', 'Voice VPN');

     COMMIT;
END;
/

DROP PROCEDURE tmp_update_csp_usid;

spool off;

quit
