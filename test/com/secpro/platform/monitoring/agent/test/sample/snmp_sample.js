{
   "body": {
      "snmp": {
         "s": "",
         "c": ""
      }
   },
   "tip": "192.168.18.66",
   "reg": "0311"
   "sid": "sid001",
   "ope": "ssh",
   "tid": "tid001",
   "ea": "1386126035312",
   "es": "1",
   "ec": "1045"
}
//说明:
body中存放各采集结果,以OPERTION的名称为KEY.
body.snmp.s 采集MIB
body.snmp.c 采集结果

tip 采集目标IP地址
reg 任务签发区域 对应MSU_SCHEDULE.REGION/VARCHAR2(50) NOT NULL,
tid 任务信息ID 对应MSU_SCHEDULE.TASK_ID/VARCHAR2(50) NOT NULL,
sid 任务调度编号 对应MSU_SCHEDULE.SCHEDULE_ID/VARCHAR2(50) NOT NULL, 主键
ope 任务操作 对应MSU_SCHEDULE.OPERATION/VARCHAR2(50) NOT NULL,
ea 任务执行时间 对应MSU_SCHEDULE.EXECUTE_AT/NUMBER(20),
es 任务执行状态 对应MSU_SCHEDULE.EXECUTE_STATUS/NUMBER(1),
ec 任务执行时长 对应MSU_SCHEDULE.EXECUTE_COST/NUMBER(20),


//
CREATE TABLE MSU_SCHEDULE
(
  TASK_ID                  VARCHAR2(50) NOT NULL,
  SCHEDULE_ID         VARCHAR2(50) NOT NULL,
  SCHEDULE_POINT      NUMBER(20) NOT NULL,
  CREATE_AT           NUMBER(20) NOT NULL,
  REGION      VARCHAR2(50) NOT NULL,
  OPERATION   VARCHAR2(50) NOT NULL,
  FETCH_AT            NUMBER(20),
  FETCH_BY            VARCHAR2(50),
  EXECUTE_AT          NUMBER(20),
  EXECUTE_COST        NUMBER(20),
  EXECUTE_STATUS      NUMBER(1),
  EXECUTE_DESCRIPTION VARCHAR2(500)
)
;