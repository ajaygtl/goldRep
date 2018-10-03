set echo on
spool db_patch-v2.0.0.log

/* ==============================================================*/
/*  Index: IDX_CVERSIONSERVICEELEMENT_03                        */
/* ==============================================================*/
create index IDX_CVERSIONSERVICEELEMENT_03 on CVERSIONSERVICEELEMENT (
    VERSIONSERVICEELEMENTID ASC,
    VERSIONID ASC
) storage (
    initial 40K
    next 40K
) tablespace CSI_INDEX;

/*==============================================================*/
/* Create All Sequences                                         */
/*==============================================================*/


set serveroutput on 
declare
    v_CHARGECHANGEITEMID            number(38);
    v_ONCEOFFCHARGEID               number(38);
    v_RECURRINGCHARGEID             number(38);
    v_SERVICEATTRIBUTEID            number(38);
    v_SERVICECHANGEITEMID           number(38);
    v_SERVICEELEMENTID              number(38);
    v_SYSTEMUSERID                  number(38);
    v_USAGECHARGEID                 number(38);
    v_VERSIONID                     number(38);
    v_VERSIONSERVICEELEMENTID       number(38);
    v_VERSIONSERVICEELEMENTSTSID    number(38);
    stm                             varchar2(4000);
begin
    select nvl(max(CHARGECHANGEITEMID),0)+2 into v_CHARGECHANGEITEMID from CCHARGECHANGEITEM;
    stm := 'create sequence SEQCHARGECHANGEITEM  increment by 1 start with ' || v_CHARGECHANGEITEMID;
    dbms_output.put_line('executing ' || stm);
    execute immediate stm;

    select nvl(max(ONCEOFFCHARGEID),0)+2 into v_ONCEOFFCHARGEID from CONCEOFFCHARGE;
    stm := 'create sequence SEQONCEOFFCHARGE increment by 1 start with ' || v_ONCEOFFCHARGEID;
    dbms_output.put_line('executing ' || stm);
    execute immediate stm;

    select nvl(max(RECURRINGCHARGEID),0)+2 into v_RECURRINGCHARGEID from CRECURRINGCHARGE;
    stm := 'create sequence SEQRECURRINGCHARGE increment by 1 start with '||v_RECURRINGCHARGEID;
    dbms_output.put_line('executing ' || stm);
    execute immediate stm;

    select nvl(max(SERVICEATTRIBUTEID),0)+2 into v_SERVICEATTRIBUTEID from CSERVICEATTRIBUTE;
    stm := 'create sequence SEQSERVICEATTRIBUTE increment by 1 start with ' || v_SERVICEATTRIBUTEID;
    dbms_output.put_line('executing ' || stm);
    execute immediate stm;

    select nvl(max(SERVICECHANGEITEMID),0)+2 into v_SERVICECHANGEITEMID from CSERVICECHANGEITEM;
    stm := 'create sequence SEQSERVICECHANGEITEM increment by 1 start with ' || v_SERVICECHANGEITEMID;
    dbms_output.put_line('executing ' || stm);
    execute immediate stm;

    select nvl(max(SERVICEELEMENTID),0)+2 into v_SERVICEELEMENTID from CSERVICEELEMENT;
    stm := 'create sequence SEQSERVICEELEMENT increment by 1 start with ' || v_SERVICEELEMENTID;
    dbms_output.put_line('executing ' || stm);
    execute immediate stm;

    select nvl(max(SYSTEMUSERID),0)+2 into v_SYSTEMUSERID from CSYSTEMUSER;
    stm := 'create sequence SEQSYSTEMUSER increment by 1 start with ' || v_SYSTEMUSERID;
    dbms_output.put_line('executing ' || stm);
    execute immediate stm;

    select nvl(max(USAGECHARGEID),0)+2 into v_USAGECHARGEID from CUSAGECHARGE;
    stm := 'create sequence SEQUSAGECHARGE increment by 1 start with ' || v_USAGECHARGEID;
    dbms_output.put_line('executing ' || stm);
    execute immediate stm;

    select nvl(max(VERSIONID),0)+2 into v_VERSIONID from CVERSION;
    stm := 'create sequence SEQVERSION increment by 1 start with ' || v_VERSIONID;
    dbms_output.put_line('executing ' || stm);
    execute immediate stm;

    select nvl(max(VERSIONSERVICEELEMENTID),0)+2 into v_VERSIONSERVICEELEMENTID from CVERSIONSERVICEELEMENT;
    stm := 'create sequence SEQVERSIONSERVICEELEMENT increment by 1 start with '||v_VERSIONSERVICEELEMENTID;
    dbms_output.put_line('executing ' || stm);
    execute immediate stm;

    select nvl(max(VERSIONSERVICEELEMENTSTSID),0)+2 into v_VERSIONSERVICEELEMENTSTSID from CVERSIONSERVICEELEMENTSTS;
    stm := 'create sequence SEQVERSIONSERVICEELEMENTSTS increment by 1 start with '||v_VERSIONSERVICEELEMENTSTSID;
    dbms_output.put_line('executing ' || stm);
    execute immediate stm;

exception
    when others then
        raise_application_error(-20001, 'error while creating the sequenceses:  ' || substr(sqlerrm, 1, 200));
end;
/

spool off;


