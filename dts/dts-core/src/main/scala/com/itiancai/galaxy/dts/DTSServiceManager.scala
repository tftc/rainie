package com.itiancai.galaxy.dts

import java.util.Date

import com.itiancai.galaxy.dts.dao.{ActionDao, ActivityDao}
import com.itiancai.galaxy.dts.domain._
import com.itiancai.galaxy.dts.repository.DTSRepository
import com.itiancai.galaxy.dts.utils.{NameResolver, RecoveryClientFactory}
import com.twitter.finagle.http.{Method, Request, Version}
import com.twitter.util.Future
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import scala.collection.JavaConversions._

@Service
class DTSServiceManager {
  @Autowired
  val clientFactory: RecoveryClientFactory = null
  @Autowired
  val actionDao: ActionDao = null
  @Autowired
  val activityDao: ActivityDao = null
  @Autowired
  val idGenerator: IdGenerator = null
  @Autowired
  val dtsRepository: DTSRepository = null

  val logger = LoggerFactory.getLogger(getClass)

  /**
    * 开启主事务,流程如下
    * 1.判断name对应的bean是否存在,且缓存
    * 2.生成tx_id,tx_id并存放在域中
    * 3.组装数据并保存
    *
    * @param bizId        业务id
    * @param businessType 服务名称
    * @param timeOut      超时时间
    * @throws Exception
    */
  @Transactional(value = "dtsTransactionManager")
  def startActivity(bizId: String, businessType: String, timeOut: Int):String ={
    val txId: String = idGenerator.getTxId(businessType)
    val activity: Activity = new Activity(txId, Status.Activity.UNKNOWN, businessType, new Date, timeOut, new Date, 0, bizId)
    activityDao.save(activity)
    txId
  }

  /**
    * 开启子事务,流程如下:
    * 1.判断name对应的bean及tx_id是否存在,且缓存
    * 2.组装Action 保存数据
    *
    * @param instructionId 幂等id
    * @param serviceName   服务名称
    * @param context       请求参数json
    */
  @Transactional(value = "dtsTransactionManager")
  def startAction(instructionId: String, serviceName: String, context: String): String = {
    val actionId: String = idGenerator.getActionId(serviceName)
    val action: Action = new Action(TXIdLocal.current_txId, actionId, Status.Action.UNKNOWN, serviceName,
      new Date(), new Date(), context, instructionId)
    actionDao.save(action)
    action.getActionId
  }

  @Transactional(value = "dtsTransactionManager")
  def finishAction(status: Status.Action, actionId: String): Unit ={
    val action = actionDao.findByActionId(actionId)
    if(Option(action).isDefined){
      actionDao.updateStatus(actionId, status.getStatus, action.getStatus)
    }
  }

  /**
    * 主事务处理成功,子事务做commit 流程:
    * 1.tx_id获取当前主事务数据,修改数据状态
    * 2.变更子事务状态
    * 3.isImmediately true action回调对应得commit/rollback
    *
    * @param status        方法名称 SUCCESS FAIL
    * @param isImmediately 是否实时处理
    * @throws Exception
    */
  def finishActivity(status: Status.Activity, isImmediately: Boolean) {
    val txId = TXIdLocal.current_txId
    logger.error(s"finishActivity tx:${txId} start")
    //修改Activity状态
    val flag = dtsRepository.updateActivityStatus(txId, status)
    if(flag && isImmediately) {
      finishActivity(txId, status)
    } else {
      logger.warn(s"activity tx:${txId} status had changed")
    }
  }

  /**
    * 完成主事务
    * @param txId
    * @param status
    */
  def finishActivity(txId: String, status: Status.Activity): Unit = {
    if(dtsRepository.handleTX(txId)) {
      val finishActions_f = finishActions(txId, status)
      finishActions_f.map(flag => {
        //如果子事务都处理成功
        if (flag) {
          //修改Activity完成标志
          dtsRepository.finishActivity(txId)
          logger.info(s"tx:${txId} finish success")
        } else {
          logger.warn(s"tx:${txId} finish fail")
        }
      }).handle({
        case t:Throwable => {
          logger.warn(s"reclaimTX tx:[${txId}]", t)
          dtsRepository.reclaimTX(txId)
        }
      })
    }
  }

  /**
    * 完成子事务(提交或回滚)
    *
    * @param txId
    * @param status
    * @return
    */
  def finishActions(txId: String, status: Status.Activity): Future[Boolean] = {
    def finishAction(action: Action): Future[Boolean] = {
      //resolve name
      val (sysName, moduleName, serviceName) = NameResolver.eval(action.getServiceName)
      //get client
      logger.info(s"sysName:${sysName}, moduleName:${moduleName} serviceName:${serviceName}")
      val client_f = clientFactory.getClient(sysName, moduleName)
      val method = if (status == Status.Activity.SUCCESS) "commit" else "rollback" //提交 | 回滚
      val path = s"${NameResolver.ACTION_HANDLE_PATH}?id=${action.getInstructionId}&name=${serviceName}&method=${method}"
      val request = Request(Version.Http11, Method.Get, path)
      val actionStatus = if (status == Status.Activity.SUCCESS) Status.Action.SUCCESS else Status.Action.FAIL
      logger.info(s"finishAction ${sysName}-${serviceName}")
      client_f.flatMap(client => {
        client(request).map(response => {
          val result = response.contentString == "true"
          if(!result) {
            logger.info(s"finishAction:[${action.getId}] ${sysName}-${serviceName} fail, response:${response.contentString}")
            false
          } else {
            logger.info(s"finishAction ${sysName}-${serviceName} success")
            val flag = {
              if (dtsRepository.finishAction(action.getActionId, actionStatus)) {
                true
              } else {  //确认action status是否修改成功
                val entity = actionDao.findByActionId(action.getActionId)
                entity.getStatus == status
              }
            }
            if (flag) {
              logger.info(s"action [${action.getId}] updateStatus ${actionStatus} success")
            } else {
              logger.warn(s"action [${action.getId}] status had changed")
            }
            flag //子事务处理成功
          }
        }).handle({
          case t: Throwable => {
            logger.error(s"action [${action.getId}] execute ${method} fail.", t)
            false //子事务失败
          }
        })
      }).handle({
        case t: Throwable => {
          logger.error(s"client error sysName:${sysName}, moduleName:${moduleName}.", t)
          false
        }
      })
    }
    val finishList_f = Future collect actionDao.listByTxId(txId).map(finishAction)
    finishList_f.map(!_.contains(false))
  }
}
