SET SCAN ON VERIFY OFF

SPOOL schema_install.lst
CLEAR SCREEN
PROMPT *************************************************************
PROMPT *              INSTALLATION WITH USER CREATION              *
PROMPT *            This script performs the operations            *
PROMPT *              which require SYSTEM privileges              *
PROMPT *************************************************************
PROMPT
PROMPT *  Provide the Following Information
PROMPT *  *********************************
PROMPT
PROMPT *        User Information
PROMPT *        ****************
PROMPT
ACCEPT USER_NAME CHAR DEFAULT csi2 PROMPT '*    New User Login (default csi2): '
ACCEPT USER_PASS CHAR DEFAULT csi2 PROMPT '*    New User Password (default csi2): '
PROMPT
PROMPT *        DataBase Information
PROMPT *        ********************
PROMPT
ACCEPT SERVICE_NAME       CHAR PROMPT '*    Global Database Name: '
ACCEPT INDEX_TBLSPACE CHAR DEFAULT CSI_INDEX PROMPT '*    Index Tablespace Name (default CSI_INDEX): '
ACCEPT DEF_TBLSPACE   CHAR DEFAULT CSI_DATA PROMPT '*    Default User''s Tablespace Name (default CSI_DATA): '

WHENEVER SQLERROR EXIT SUCCESS
PROMPT
PROMPT *        Login as DBA
PROMPT *        ************
PROMPT
ACCEPT DBA_LOGIN      CHAR DEFAULT sys PROMPT '*    DBA Login (default sys): '
ACCEPT DBA_PASSWD     CHAR             PROMPT '*    Password: ' HIDE

PROMPT
PROMPT *        Connecting as &&DBA_LOGIN@&&SERVICE_NAME

CONNECT &&DBA_LOGIN/&&DBA_PASSWD@&&SERVICE_NAME;
WHENEVER SQLERROR EXIT SUCCESS
PROMPT
PROMPT *        DataBase Installation Started
PROMPT

CREATE USER &&USER_NAME IDENTIFIED BY &&USER_PASS
DEFAULT TABLESPACE &&DEF_TBLSPACE;
WHENEVER SQLERROR EXIT SUCCESS

GRANT CONNECT, RESOURCE TO &&USER_NAME;
WHENEVER SQLERROR EXIT SUCCESS

@csi_create_database.sql

ACCEPT any_key CHAR PROMPT 'Press [Enter] to exit'
SPOOL OFF
QUIT
