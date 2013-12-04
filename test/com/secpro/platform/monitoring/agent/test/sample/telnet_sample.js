{
   "body": {
      "telnet": {
         "s": "ls",
         "c": "Last login: Wed Dec  4 11:00:35 2013 from baiyanwei^^ls^exit^[baiyanwei@baiyanwei ~]$ ls^2013-09-05-14-59-42.064-VirtualBox-583.log  gitTest\t       ultrapower^2013-09-05-14-59-53.053-VirtualBox-667.log  Music\t       url.txt^android\t\t\t\t\t    Pictures\t       Videos^backup\t\t\t\t\t    Public\t       VirtualBox VMs^baiyanwei.pem\t\t\t\t    research\t       workflow^connect_ec2_host.sh\t\t\t    secpro\t       workspace^Desktop\t\t\t\t\t    send_file_ec2.txt  Workspaces^Documents\t\t\t\t    software\t       yottaa^Downloads\t\t\t\t    Templates^[baiyanwei@baiyanwei ~]$ exit^logout^"
      }
   },
   "sid": "sid001",
   "ope": "telnet",
   "tid": "tid001",
   "ea": "1386126035312",
   "es": "1",
   "ec": "1045"
}

//说明:
body中存放各采集结果,以OPERTION的名称为KEY.
body.telnet.s 采集命令
body.telnet.c 采集结果

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