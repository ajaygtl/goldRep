/*==============================================================*/
/* DBMS name:      ORACLE Version 8i2 (8.1.6)                   */
/* Created on:     6/20/2004 6:00:47 PM                         */
/*==============================================================*/


alter table CCHARGECHANGEITEM
   drop constraint FK_CCHARGEC_REF_CCHRG_CONCEOFF;

alter table CCHARGECHANGEITEM
   drop constraint FK_CCHARGEC_REF_CCHRG_CRECURRI;

alter table CCHARGECHANGEITEM
   drop constraint FK_CCHARGEC_REF_CCHRG_CSERVICE;

alter table CCHARGECHANGEITEM
   drop constraint FK_CCHARGEC_REF_CCHRG_CUSAGECH;

alter table CCHARGECHANGEITEM
   drop constraint FK_CCHARGEC_REF_CCHRG_CVERSION;

alter table CSERVICECHANGEITEM
   drop constraint FK_CSERVICE_REF_CSERV_CVERSION;

alter table CSERVICECHANGEITEM
   drop constraint FK_CSERV_REF_CSERV_CSERVICE_01;

alter table CSERVICECHANGEITEM
   drop constraint FK_CSERV_REF_CSERV_CSERVICE_02;

alter table CSERVICECHANGEITEM
   drop constraint FK_CSERV_REF_CSERV_CSERVICE_03;

alter table CVERSION
   drop constraint FK_CVERSION_REF_CVERS_CSYSTEMU;

alter table CVERSIONSERVICEELEMENT
   drop constraint FK_CVERSION_REF_CVERS_CSERVICE;

alter table CVERSIONSERVICEELEMENT
   drop constraint FK_CVERS_REF_CVERS_CVERSION_01;

alter table CVERSIONSERVICEELEMENTSTS
   drop constraint FK_CVERS_REF_CVERS_CVERSION_02;

drop view VIW_VERSION_ELEMENT_STATUS;

drop view VIW_VERSION_ELEMENT_ATTRIBUTE;

drop view VIW_STATUS;

drop view VIW_CHARGE;

drop index FK_CCHARGECHANGEITEM_01;

drop index FK_CCHARGECHANGEITEM_02;

drop index FK_CCHARGECHANGEITEM_03;

drop index FK_CCHARGECHANGEITEM_04;

drop index FK_CCHARGECHANGEITEM_05;

drop index IDX_CCHARGECHANGEITEM_01;

drop index IDX_CIS;

drop index FK_CSERVICECHANGEITEM_01;

drop index FK_CSERVICECHANGEITEM_02;

drop index FK_CSERVICECHANGEITEM_03;

drop index FK_CSERVICECHANGEITEM_04;

drop index IDX_SOURCESYSTEM;

drop index UK_CSERVICEELEMENT_USID;

drop index FK_CVERSION_01;

drop index IDX_CVERSION_01;

drop index IDX_CVERSION_02;

drop index FK_CVERSIONSERVICEELEMENT_01;

drop index FK_CVERSIONSERVICEELEMENT_02;

drop index IDX_CVERSIONSERVICEELEMENT_01;

drop index IDX_CVERSIONSERVICEELEMENT_02;

drop index IDX_CVERSIONSERVICEELEMENT_03;

drop index FK_CVERSIONSERVICEELEMSTS_01;

drop index IDX_CVERSIONSERVICEELEMSTS_01;

drop index IDX_CVERSIONSERVICEELEMSTS_02;

drop table CCHARGECHANGEITEM cascade constraints;

drop table CMAPCISTOCSI cascade constraints;

drop table CONCEOFFCHARGE cascade constraints;

drop table CRECURRINGCHARGE cascade constraints;

drop table CSERVICEATTRIBUTE cascade constraints;

drop table CSERVICECHANGEITEM cascade constraints;

drop table CSERVICEELEMENT cascade constraints;

drop table CSYSTEMUSER cascade constraints;

drop table CUSAGECHARGE cascade constraints;

drop table CVERSION cascade constraints;

drop table CVERSIONSERVICEELEMENT cascade constraints;

drop table CVERSIONSERVICEELEMENTSTS cascade constraints;

drop sequence SEQCHARGECHANGEITEM;

drop sequence SEQONCEOFFCHARGE;

drop sequence SEQRECURRINGCHARGE;

drop sequence SEQREFTYPE;

drop sequence SEQSERVICEATTRIBUTE;

drop sequence SEQSERVICECHANGEITEM;

drop sequence SEQSERVICEELEMENT;

drop sequence SEQSYSTEMUSER;

drop sequence SEQUSAGECHARGE;

drop sequence SEQVERSION;

drop sequence SEQVERSIONSERVICEELEMENT;

drop sequence SEQVERSIONSERVICEELEMENTSTS;

create sequence SEQCHARGECHANGEITEM
increment by 1
start with 1;

create sequence SEQONCEOFFCHARGE
increment by 1
start with 1;

create sequence SEQRECURRINGCHARGE
increment by 1
start with 1;

create sequence SEQREFTYPE
increment by 1
start with 1;

create sequence SEQSERVICEATTRIBUTE
increment by 1
start with 1;

create sequence SEQSERVICECHANGEITEM
increment by 1
start with 1;

create sequence SEQSERVICEELEMENT
increment by 1
start with 1;

create sequence SEQSYSTEMUSER
increment by 1
start with 1;

create sequence SEQUSAGECHARGE
increment by 1
start with 1;

create sequence SEQVERSION
increment by 1
start with 1;

create sequence SEQVERSIONSERVICEELEMENT
increment by 1
start with 1;

create sequence SEQVERSIONSERVICEELEMENTSTS
increment by 1
start with 1;

/*==============================================================*/
/* Table: CCHARGECHANGEITEM                                     */
/*==============================================================*/


create table CCHARGECHANGEITEM  (
   CHARGECHANGEITEMID   NUMBER(38)                       not null,
   CHANGETYPECODE       VARCHAR2(255),
   CHARGELEVEL          VARCHAR2(50),
   CREATEDATE           DATE,
   CREATEDBY            VARCHAR2(255)                    not null,
   LUPDBY               VARCHAR2(255),
   DISCCODE             VARCHAR2(255),
   LUPDDATE             DATE,
   ONCEOFFCHARGEID      NUMBER(38),
   RECURRINGCHARGEID    NUMBER(38),
   SERVICEATTRIBUTEID   NUMBER(38),
   USAGECHARGEID        NUMBER(38),
   VERSIONSERVICEELEMENTID NUMBER(38)                       not null,
   constraint PK_CCHARGECHANGEITEM primary key (CHARGECHANGEITEMID)
         using index
       tablespace CSI_INDEX
)
tablespace CSI_DATA;

/*==============================================================*/
/* Index: FK_CCHARGECHANGEITEM_01                               */
/*==============================================================*/
create index FK_CCHARGECHANGEITEM_01 on CCHARGECHANGEITEM (
   ONCEOFFCHARGEID ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: FK_CCHARGECHANGEITEM_02                               */
/*==============================================================*/
create index FK_CCHARGECHANGEITEM_02 on CCHARGECHANGEITEM (
   RECURRINGCHARGEID ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: FK_CCHARGECHANGEITEM_03                               */
/*==============================================================*/
create index FK_CCHARGECHANGEITEM_03 on CCHARGECHANGEITEM (
   SERVICEATTRIBUTEID ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: FK_CCHARGECHANGEITEM_04                               */
/*==============================================================*/
create index FK_CCHARGECHANGEITEM_04 on CCHARGECHANGEITEM (
   USAGECHARGEID ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: FK_CCHARGECHANGEITEM_05                               */
/*==============================================================*/
create index FK_CCHARGECHANGEITEM_05 on CCHARGECHANGEITEM (
   VERSIONSERVICEELEMENTID ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: IDX_CCHARGECHANGEITEM_01                              */
/*==============================================================*/
create index IDX_CCHARGECHANGEITEM_01 on CCHARGECHANGEITEM (
   CHARGELEVEL ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Table: CMAPCISTOCSI                                          */
/*==============================================================*/


create table CMAPCISTOCSI  (
   CMAPCISTOCSIID       NUMBER(38)                       not null,
   CISTABLE             VARCHAR2(80)                     not null,
   CISCOLUMN            VARCHAR2(80)                     not null,
   CSINAME              VARCHAR2(80)                     not null,
   CSICLASS             VARCHAR2(80)                     not null,
   CSIPARENT            VARCHAR2(80),
   constraint PK_CMAPCISTOCSI primary key (CMAPCISTOCSIID)
)
tablespace CSI_DATA
tablespace CSI_DATA;

comment on column CMAPCISTOCSI.CISCOLUMN is
'
';

/*==============================================================*/
/* Index: IDX_CIS                                               */
/*==============================================================*/
create unique index IDX_CIS on CMAPCISTOCSI (
   CISTABLE ASC,
   CISCOLUMN ASC
)
storage
(
    initial 512K
    next 512K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Table: CONCEOFFCHARGE                                        */
/*==============================================================*/


create table CONCEOFFCHARGE  (
   ONCEOFFCHARGEID      NUMBER(38)                       not null,
   AMOUNT               NUMBER,
   CREATEDATE           DATE,
   LUPDDATE             DATE,
   LUPDBY               VARCHAR2(255),
   CHGCATID             VARCHAR2(255),
   CREATEDBY            VARCHAR2(255)                    not null,
   DISCCODE             VARCHAR2(255),
   constraint PK_CONCEOFFCHARGE primary key (ONCEOFFCHARGEID)
         using index
       tablespace CSI_INDEX
)
tablespace CSI_DATA;

/*==============================================================*/
/* Table: CRECURRINGCHARGE                                      */
/*==============================================================*/


create table CRECURRINGCHARGE  (
   RECURRINGCHARGEID    NUMBER(38)                       not null,
   AMOUNT               NUMBER,
   DISCCODE             VARCHAR2(255),
   CHGCATID             VARCHAR2(255),
   CREATEDATE           DATE,
   LUPDDATE             DATE,
   CREATEDBY            VARCHAR2(255)                    not null,
   LUPDBY               VARCHAR2(255),
   constraint PK_CRECURRINGCHARGE primary key (RECURRINGCHARGEID)
         using index
       tablespace CSI_INDEX
)
tablespace CSI_DATA;

/*==============================================================*/
/* Table: CSERVICEATTRIBUTE                                     */
/*==============================================================*/


create table CSERVICEATTRIBUTE  (
   SERVICEATTRIBUTEID   NUMBER(38)                       not null,
   SERVICEATTRIBUTENAME VARCHAR2(255),
   VALUE                VARCHAR2(255),
   PRODATTRID           VARCHAR2(50),
   CREATEDATE           DATE,
   CREATEDBY            VARCHAR2(255)                    not null,
   LUPDDATE             DATE,
   LUPDBY               VARCHAR2(255),
   constraint PK_CSERVICEATTRIBUTE primary key (SERVICEATTRIBUTEID)
         using index
       tablespace CSI_INDEX
)
tablespace CSI_DATA;

/*==============================================================*/
/* Table: CSERVICECHANGEITEM                                    */
/*==============================================================*/


create table CSERVICECHANGEITEM  (
   SERVICECHANGEITEMID  NUMBER(38)                       not null,
   CREATEDATE           DATE,
   CREATEDBY            VARCHAR2(255)                    not null,
   LUPDDATE             DATE,
   LUPDBY               VARCHAR2(255),
   CHANGETYPECODE       VARCHAR2(255),
   SERVICEATTRIBUTEID   NUMBER(38),
   VERSIONSERVICEELEMENTID NUMBER(38)                       not null,
   SERVICEELEMENTID     NUMBER(38),
   SERVICEELEMENTID_1   NUMBER(38),
   constraint PK_CSERVICECHANGEITEM primary key (SERVICECHANGEITEMID)
         using index
       tablespace CSI_INDEX
)
tablespace CSI_DATA;

/*==============================================================*/
/* Index: FK_CSERVICECHANGEITEM_01                              */
/*==============================================================*/
create index FK_CSERVICECHANGEITEM_01 on CSERVICECHANGEITEM (
   SERVICEATTRIBUTEID ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: FK_CSERVICECHANGEITEM_02                              */
/*==============================================================*/
create index FK_CSERVICECHANGEITEM_02 on CSERVICECHANGEITEM (
   VERSIONSERVICEELEMENTID ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: FK_CSERVICECHANGEITEM_03                              */
/*==============================================================*/
create index FK_CSERVICECHANGEITEM_03 on CSERVICECHANGEITEM (
   SERVICEELEMENTID ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: FK_CSERVICECHANGEITEM_04                              */
/*==============================================================*/
create index FK_CSERVICECHANGEITEM_04 on CSERVICECHANGEITEM (
   SERVICEELEMENTID_1 ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Table: CSERVICEELEMENT                                       */
/*==============================================================*/


create table CSERVICEELEMENT  (
   SERVICEELEMENTID     NUMBER(38)                       not null,
   USID                 VARCHAR2(255)                    not null,
   SERVICEELEMENTCLASS  VARCHAR2(50)                     not null,
   SERVICEELEMENTNAME   VARCHAR2(255),
   DESCRIPTION          VARCHAR2(255),
   CREATIONDATE         DATE,
   GRANDFATHERDATE      DATE,
   STARTBILLINGDATE     DATE,
   ENDBILLINGDATE       DATE,
   PRODID               VARCHAR2(255),
   CREATEDATE           DATE,
   LUPDDATE             DATE,
   LUPDBY               VARCHAR2(255),
   CREATEDBY            VARCHAR2(255)                    not null,
   SOURCESYSTEM         VARCHAR2(5)                      not null,
   constraint PK_CSERVICEELEMENT primary key (SERVICEELEMENTID)
         using index
       tablespace CSI_INDEX
)
tablespace CSI_DATA;

/*==============================================================*/
/* Index: UK_CSERVICEELEMENT_USID                               */
/*==============================================================*/
create unique index UK_CSERVICEELEMENT_USID on CSERVICEELEMENT (
   USID ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: IDX_SOURCESYSTEM                                      */
/*==============================================================*/
create index IDX_SOURCESYSTEM on CSERVICEELEMENT (
   SOURCESYSTEM ASC
)
storage
(
    initial 512K
    next 512K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Table: CSYSTEMUSER                                           */
/*==============================================================*/


create table CSYSTEMUSER  (
   SYSTEMUSERID         NUMBER(38)                       not null,
   SYSTEMUSERNAME       VARCHAR2(255)                    not null,
   SYSTEMCODE           VARCHAR2(255),
   CREATEDATE           DATE,
   LUPDDATE             DATE,
   LUPDBY               VARCHAR2(255),
   CREATEDBY            VARCHAR2(255)                    not null,
   constraint PK_CSYSTEMUSER primary key (SYSTEMUSERID)
         using index
       tablespace CSI_INDEX
)
tablespace CSI_DATA;

/*==============================================================*/
/* Table: CUSAGECHARGE                                          */
/*==============================================================*/


create table CUSAGECHARGE  (
   USAGECHARGEID        NUMBER(38)                       not null,
   CHGCATID             VARCHAR2(255),
   CREATEDATE           DATE,
   CREATEDBY            VARCHAR2(255)                    not null,
   LUPDDATE             DATE,
   LUPDBY               VARCHAR2(255),
   constraint PK_CUSAGECHARGE primary key (USAGECHARGEID)
         using index
       tablespace CSI_INDEX
)
tablespace CSI_DATA;

/*==============================================================*/
/* Table: CVERSION                                              */
/*==============================================================*/


create table CVERSION  (
   VERSIONID            NUMBER(38)                       not null,
   DESCRIPTION          VARCHAR2(255),
   SBAHANDLE            NUMBER(38)                       not null,
   ORDHANDLE            VARCHAR2(255),
   CTRHANDLE            VARCHAR2(255),
   SITEHANDLE           VARCHAR2(255),
   ORDERTYPE            VARCHAR2(50),
   CUSTHANDLE           VARCHAR2(255),
   ENDUSERHANDLE        VARCHAR2(255),
   SERVICEHANDLE        VARCHAR2(255),
   RATINGCURRENCY       VARCHAR2(255),
   EXCHANGERATE         FLOAT,
   CREATEDATE           DATE,
   CREATEDBY            VARCHAR2(255)                    not null,
   LUPDDATE             DATE,
   LUPDBY               VARCHAR2(255),
   ORDERSTATUS          VARCHAR2(50),
   SYSUSERID            NUMBER(38),
   SYSTEMUSERID         NUMBER(38)                       not null,
   constraint PK_CVERSION primary key (VERSIONID)
         using index
       tablespace CSI_INDEX
)
tablespace CSI_DATA;

/*==============================================================*/
/* Index: FK_CVERSION_01                                        */
/*==============================================================*/
create index FK_CVERSION_01 on CVERSION (
   SYSTEMUSERID ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: IDX_CVERSION_01                                       */
/*==============================================================*/
create index IDX_CVERSION_01 on CVERSION (
   SITEHANDLE ASC,
   CUSTHANDLE ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: IDX_CVERSION_02                                       */
/*==============================================================*/
create index IDX_CVERSION_02 on CVERSION (
   SITEHANDLE ASC,
   CUSTHANDLE ASC,
   SERVICEHANDLE ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Table: CVERSIONSERVICEELEMENT                                */
/*==============================================================*/


create table CVERSIONSERVICEELEMENT  (
   VERSIONSERVICEELEMENTID NUMBER(38)                       not null,
   CREATEDATE           DATE,
   CREATEDBY            VARCHAR2(255)                    not null,
   LUPDDATE             DATE,
   LUPDBY               VARCHAR2(255),
   NEWELM               VARCHAR2(50),
   VERSIONID            NUMBER(38)                       not null,
   SERVICEELEMENTID     NUMBER(38)                       not null,
   constraint PK_CVERSIONSERVICEELEMENT primary key (VERSIONSERVICEELEMENTID)
         using index
       tablespace CSI_INDEX
)
tablespace CSI_DATA;

/*==============================================================*/
/* Index: FK_CVERSIONSERVICEELEMENT_01                          */
/*==============================================================*/
create index FK_CVERSIONSERVICEELEMENT_01 on CVERSIONSERVICEELEMENT (
   VERSIONID ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: FK_CVERSIONSERVICEELEMENT_02                          */
/*==============================================================*/
create index FK_CVERSIONSERVICEELEMENT_02 on CVERSIONSERVICEELEMENT (
   SERVICEELEMENTID ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: IDX_CVERSIONSERVICEELEMENT_01                         */
/*==============================================================*/
create index IDX_CVERSIONSERVICEELEMENT_01 on CVERSIONSERVICEELEMENT (
   CREATEDATE ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: IDX_CVERSIONSERVICEELEMENT_02                         */
/*==============================================================*/
create index IDX_CVERSIONSERVICEELEMENT_02 on CVERSIONSERVICEELEMENT (
   LUPDDATE ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: IDX_CVERSIONSERVICEELEMENT_03                         */
/*==============================================================*/
create index IDX_CVERSIONSERVICEELEMENT_03 on CVERSIONSERVICEELEMENT (
   VERSIONSERVICEELEMENTID ASC,
   VERSIONID ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Table: CVERSIONSERVICEELEMENTSTS                             */
/*==============================================================*/


create table CVERSIONSERVICEELEMENTSTS  (
   VERSIONSERVICEELEMENTSTSID NUMBER(38)                       not null,
   STATUSTYPECODE       VARCHAR2(50)                     not null,
   STATUSDATE           DATE,
   CREATEDATE           DATE,
   LUPDDATE             DATE,
   CREATEDBY            VARCHAR2(255)                    not null,
   LUPDBY               VARCHAR2(255),
   VERSIONSERVICEELEMENTID NUMBER(38)                       not null,
   constraint PK_CVERSIONSERVICEELEMENTSTS primary key (VERSIONSERVICEELEMENTSTSID)
         using index
       tablespace CSI_INDEX
)
tablespace CSI_DATA;

/*==============================================================*/
/* Index: FK_CVERSIONSERVICEELEMSTS_01                          */
/*==============================================================*/
create index FK_CVERSIONSERVICEELEMSTS_01 on CVERSIONSERVICEELEMENTSTS (
   VERSIONSERVICEELEMENTID ASC
);

/*==============================================================*/
/* Index: IDX_CVERSIONSERVICEELEMSTS_01                         */
/*==============================================================*/
create index IDX_CVERSIONSERVICEELEMSTS_01 on CVERSIONSERVICEELEMENTSTS (
   STATUSTYPECODE ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* Index: IDX_CVERSIONSERVICEELEMSTS_02                         */
/*==============================================================*/
create index IDX_CVERSIONSERVICEELEMSTS_02 on CVERSIONSERVICEELEMENTSTS (
   STATUSDATE ASC
)
storage
(
    initial 40K
    next 40K
)
tablespace CSI_INDEX;

/*==============================================================*/
/* View: VIW_CHARGE                                             */
/*==============================================================*/
create or replace view VIW_CHARGE(VSE_ID, SA_ID, CCI_CHARGE_LEVEL, CCI_CHANGE_CODE, OOC_AMOUNT, OOC_CHGCATID, OOC_DISCCODE, RC_AMOUNT, RC_CHGCATID, RC_DISCCODE, UC_CHGCATID) as
SELECT cci.versionserviceelementid             vse_id
     , cci.serviceattributeid                  sa_id
     , cci.chargelevel                         cci_charge_level
     , cci.changetypecode                      cci_change_code
     , ooc.amount                              ooc_amount
     , ooc.chgcatid                            ooc_chgcatid
     , ooc.disccode                            ooc_disccode
     , rc.amount                               rc_amount
     , rc.chgcatid                             rc_chgcatid
     , rc.disccode                             rc_disccode
     , uc.chgcatid                             uc_chgcatid
  FROM cchargechangeitem                       cci
     , conceoffcharge                          ooc
     , crecurringcharge                        rc
     , cusagecharge                            uc
 WHERE cci.onceoffchargeid = ooc.onceoffchargeid(+)
   AND cci.recurringchargeid = rc.recurringchargeid(+)
   AND cci.usagechargeid = uc.usagechargeid(+);

/*==============================================================*/
/* View: VIW_STATUS                                             */
/*==============================================================*/
create or replace view VIW_STATUS(VSE_ID, STATUS_CODE, STATUS_DATE) as
SELECT base.versionserviceelementid            vse_id
     , base.statustypecode                     status_code
     , base.statusdate                         status_date
  FROM cversionserviceelementsts               base
 WHERE base.statustypecode = 'ReadyForService'
    OR base.statustypecode = 'ReadyForBilling'
    OR base.statustypecode = 'OrderDisconnect'
 ORDER BY
       base.versionserviceelementid;

/*==============================================================*/
/* View: VIW_VERSION_ELEMENT_ATTRIBUTE                          */
/*==============================================================*/
create or replace view VIW_VERSION_ELEMENT_ATTRIBUTE(V_CUSTHANDLE, V_SITEHANDLE, V_SERVICEHANDLE, V_ORDHANDLE, V_ORDERSTATUS, V_ORDERTYPE, V_CTRHANDLE, V_SBAHANDLE, V_ENDUSERHANDLE, V_EXCHANGERATE, V_CREATEDATE, VSE_VERSIONSERVICEELEMENTID, VSE_CREATEDATE, VSE_LUPDDATE, VSES_STATUSTYPECODE, SE_SERVICEELEMENTID, SE_USID, SE_SERVICEELEMENTCLASS, SE_SERVICEELEMENTNAME, SE_DESCRIPTION, SE_CREATIONDATE, SE_GRANDFATHERDATE, SE_STARTBILLINGDATE, SE_ENDBILLINGDATE, SE_PRODID, U_SYSTEMUSERNAME, U_SYSTEMCODE, I_SERVICEATTRIBUTEID, I_LUPDDATE, I_CHANGETYPECODE, T_SERVICEATTRIBUTEID, T_SERVICEATTRIBUTENAME, T_VALUE) as
SELECT v.custhandle                v_custhandle
     , v.sitehandle                v_sitehandle
     , v.servicehandle             v_servicehandle
     , v.ordhandle                 v_ordhandle
     , v.orderstatus               v_orderstatus
     , v.ordertype                 v_ordertype
     , v.ctrhandle                 v_ctrhandle
     , v.sbahandle                 v_sbahandle
     , v.enduserhandle             v_enduserhandle
     , v.exchangerate              v_exchangerate
     , v.createdate                v_createdate
     , vse.versionserviceelementid vse_versionserviceelementid
     , vse.createdate              vse_createdate
     , vse.lupddate                vse_lupddate
     , vses.statustypecode         vses_statustypecode
     , se.serviceelementid         se_serviceelementid
     , se.usid                     se_usid
     , se.serviceelementclass      se_serviceelementclass
     , se.serviceelementname       se_serviceelementname
     , se.description              se_description
     , se.creationdate             se_creationdate
     , se.grandfatherdate          se_grandfatherdate
     , se.startbillingdate         se_startbillingdate
     , se.endbillingdate           se_endbillingdate
     , se.prodid                   se_prodid
     , su.systemusername           u_systemusername
     , su.systemcode               u_systemcode
     , i.serviceattributeid        i_serviceattributeid
     , i.lupddate                  i_lupddate
     , i.changetypecode            i_changetypecode
     , t.serviceattributeid        t_serviceattributeid
     , t.serviceattributename      t_serviceattributename
     , t.value                     t_value
  FROM cversion                  v
     , cserviceelement           se
     , cversionserviceelement    vse
     , cversionserviceelementsts vses
     , csystemuser               su
     , cservicechangeitem        i
     , cserviceattribute         t
 WHERE v.versionid = vse.versionid
   AND v.systemuserid = su.systemuserid
   AND vse.serviceelementid = se.serviceelementid
   AND vse.versionserviceelementid = vses.versionserviceelementid
   AND vses.statustypecode in ('InProgress', 'Current', 'Disconnect', 'Delete')
   AND vse.versionserviceelementid = i.versionserviceelementid(+)
   AND i.serviceattributeid = t.serviceattributeid(+);

/*==============================================================*/
/* View: VIW_VERSION_ELEMENT_STATUS                             */
/*==============================================================*/
create or replace view VIW_VERSION_ELEMENT_STATUS as
SELECT v.custhandle                v_custhandle
     , v.sitehandle                v_sitehandle
     , v.servicehandle             v_servicehandle
     , v.ordhandle                 v_ordhandle
     , vse.serviceelementid        se_serviceelementid
     , vse.lupddate                vse_lupddate
     , vses.statustypecode         vses_statustypecode
  FROM cversion                  v
     , cversionserviceelement    vse
     , cversionserviceelementsts vses
 WHERE v.versionid = vse.versionid
   AND vse.versionserviceelementid = vses.versionserviceelementid
   AND vses.statustypecode in ('InProgress', 'Current', 'Disconnect', 'Delete');

alter table CCHARGECHANGEITEM
   add constraint FK_CCHARGEC_REF_CCHRG_CONCEOFF foreign key (ONCEOFFCHARGEID)
      references CONCEOFFCHARGE (ONCEOFFCHARGEID) not deferrable;

alter table CCHARGECHANGEITEM
   add constraint FK_CCHARGEC_REF_CCHRG_CRECURRI foreign key (RECURRINGCHARGEID)
      references CRECURRINGCHARGE (RECURRINGCHARGEID) not deferrable;

alter table CCHARGECHANGEITEM
   add constraint FK_CCHARGEC_REF_CCHRG_CSERVICE foreign key (SERVICEATTRIBUTEID)
      references CSERVICEATTRIBUTE (SERVICEATTRIBUTEID) not deferrable;

alter table CCHARGECHANGEITEM
   add constraint FK_CCHARGEC_REF_CCHRG_CUSAGECH foreign key (USAGECHARGEID)
      references CUSAGECHARGE (USAGECHARGEID) not deferrable;

alter table CCHARGECHANGEITEM
   add constraint FK_CCHARGEC_REF_CCHRG_CVERSION foreign key (VERSIONSERVICEELEMENTID)
      references CVERSIONSERVICEELEMENT (VERSIONSERVICEELEMENTID) not deferrable;

alter table CSERVICECHANGEITEM
   add constraint FK_CSERVICE_REF_CSERV_CVERSION foreign key (VERSIONSERVICEELEMENTID)
      references CVERSIONSERVICEELEMENT (VERSIONSERVICEELEMENTID) not deferrable;

alter table CSERVICECHANGEITEM
   add constraint FK_CSERV_REF_CSERV_CSERVICE_01 foreign key (SERVICEATTRIBUTEID)
      references CSERVICEATTRIBUTE (SERVICEATTRIBUTEID) not deferrable;

alter table CSERVICECHANGEITEM
   add constraint FK_CSERV_REF_CSERV_CSERVICE_02 foreign key (SERVICEELEMENTID)
      references CSERVICEELEMENT (SERVICEELEMENTID) not deferrable;

alter table CSERVICECHANGEITEM
   add constraint FK_CSERV_REF_CSERV_CSERVICE_03 foreign key (SERVICEELEMENTID_1)
      references CSERVICEELEMENT (SERVICEELEMENTID) not deferrable;

alter table CVERSION
   add constraint FK_CVERSION_REF_CVERS_CSYSTEMU foreign key (SYSTEMUSERID)
      references CSYSTEMUSER (SYSTEMUSERID) not deferrable;

alter table CVERSIONSERVICEELEMENT
   add constraint FK_CVERSION_REF_CVERS_CSERVICE foreign key (SERVICEELEMENTID)
      references CSERVICEELEMENT (SERVICEELEMENTID) not deferrable;

alter table CVERSIONSERVICEELEMENT
   add constraint FK_CVERS_REF_CVERS_CVERSION_01 foreign key (VERSIONID)
      references CVERSION (VERSIONID) not deferrable;

alter table CVERSIONSERVICEELEMENTSTS
   add constraint FK_CVERS_REF_CVERS_CVERSION_02 foreign key (VERSIONSERVICEELEMENTID)
      references CVERSIONSERVICEELEMENT (VERSIONSERVICEELEMENTID) not deferrable;

