{
   "body": {
      "error": {
         "message": "Authentication failed.",
         "entry": {
            "mda": {
               "username": "baiyanwei",
               "password": "pswd"
            },
            "con": "ls",
            "tpt": "22",
            "sid": "sid001",
            "cat": 1386125883296,
            "sat": 1386125883296,
            "reg": "HB",
            "ope": "ssh",
            "tid": "tid001",
            "tip": "192.168.18.66"
         },
         "code": "0",
         "type": "operation"
      }
   },
   "sid": "sid001",
   "ope": "ssh",
   "tid": "tid001",
   "ea": "1386125883301",
   "es": "1",
   "ec": "2304"
}

//说明:
body中存放各采集异常结果,以"ope":采集命令的异常信息.
body.error 采集错误信息
body.error.message 具体异常信息
body.error.entry 异常时执行的调度信息.
body.error.code 异常代码
body.error.type 异常类别,目前分为mca/operation 两种.

tid 任务信息ID 对应MSU_SCHEDULE.TASK_ID/VARCHAR2(50) NOT NULL,
sid 任务调度编号 对应MSU_SCHEDULE.SCHEDULE_ID/VARCHAR2(50) NOT NULL, 主键
ope 任务操作 对应MSU_SCHEDULE.OPERATION/VARCHAR2(50) NOT NULL,
ea 任务执行时间 对应MSU_SCHEDULE.EXECUTE_AT/NUMBER(20),
es 任务执行状态 对应MSU_SCHEDULE.EXECUTE_STATUS/NUMBER(1),
ec 任务执行时长 对应MSU_SCHEDULE.EXECUTE_COST/NUMBER(20),
body.error 采集异常信息 MSU_SCHEDULE.EXECUTE_DESCRIPTION/VARCHAR2(500), 直接将body.error的值存入即可.

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
