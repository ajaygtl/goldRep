SET SCAN ON VERIFY OFF

SPOOL install.lst
CLEAR SCREEN
PROMPT *************************************************************
PROMPT *             INSTALLATION WITHOUT USER CREATION            *
PROMPT *************************************************************
PROMPT
PROMPT *  Provide the Following Information
PROMPT *  *********************************
PROMPT
PROMPT *        User Information
PROMPT *        ****************
PROMPT
ACCEPT USER_NAME CHAR DEFAULT csi2 PROMPT '*    User Login (default csi2): '
ACCEPT USER_PASS CHAR DEFAULT csi2 PROMPT '*    User Password (default csi2): '
PROMPT
PROMPT *        DataBase Information
PROMPT *        ********************
PROMPT
ACCEPT SERVICE_NAME   CHAR PROMPT '*    Global Database Name: '
ACCEPT INDEX_TBLSPACE CHAR DEFAULT CSI_INDEX PROMPT '*    Index Tablespace Name (default CSI_INDEX): '

WHENEVER SQLERROR EXIT FAILURE

PROMPT
PROMPT *        DataBase Installation Started
PROMPT

@csi_create_database.sql

ACCEPT any_key CHAR PROMPT 'Press [Enter] to exit'
SPOOL OFF
QUIT
