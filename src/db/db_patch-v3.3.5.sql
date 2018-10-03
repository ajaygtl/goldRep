SET echo ON
spool db_patch_-v3.3.5.log

UPDATE  CSERVICEATTRIBUTE SET value='true'
         WHERE SERVICEATTRIBUTENAME='Modem Already in Place'   AND
         UPPER(value) IN ('1601R + WIC-1B-S/T', '1601R + WIC1BS/T','4083663',
                                         'CISCO 2501', 'CS2620', 'FOR DECNET TRAFFIC', 'ROUTER TYPE 1601-R',
                                         'USE EXISTING', 'Y', 'YES - CS2611', 'YES - FOR OOBM',
                                         'YES-NO CHANGE', 'SECONDARY X MCS-IAC','YA','YEA','YES','TRUE','USE EXISITNG');



UPDATE CSERVICEATTRIBUTE SET value='false'
         WHERE SERVICEATTRIBUTENAME='Modem Already in Place' AND
         UPPER(value) IN ('-', '.', '38 FLOOR', 'N/A', 'NA', 'NO - ROUTER SWAP TO BE DONE',
                                        'NULL', 'NEW', 'NO', 'ADD NM-4B-S/T', 'NONE', 'PARTIAL',
                                        'PLEASE ADD A NM-4E1-IMA', 'PLEASE ORDER', 'CASCADED', 'FALSE', 'UNKNOWN','DISCONNECT');

COMMIT;

spool off;
quit
