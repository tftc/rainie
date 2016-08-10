package com.itiancai.galaxy.dts.domain;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by lsp on 16/7/28.
 */
@Entity
@Table(name = "dts_action")
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 主事务id
     */
    @Column(name = "tx_id", nullable = false)
    private String txId;

    /**
     * 幂等值
     */
    @Column(name = "instruction_id", nullable = false)
    private String instructionId;

    /**
     * 子事务id
     */
    @Column(name = "action_id", nullable = false)
    private String actionId;

    /**
     * 子事务状态(UNKNOWN,PREPARE,SUCCESS,FAIL)
     */
    @Column(name = "status", nullable = false)
    private int status;

    /**
     * 服务名称
     */
    @Column(name = "service_name", nullable = false)
    private String serviceName;

    /**
     * 数据创建时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "c_time")
    private Date cTime;

    /**
     * 数据修改时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "m_time")
    private Date mTime;

    /**
     * 请求参数
     */
    @Column(name = "context", nullable = false)
    private String context;

    public Action(){}

    public Action(String txId, String actionId, Status.Action status, String serviceName, Date cTime,
                  Date mTime, String context, String instructionId){
        this.txId = txId;
        this.actionId = actionId;
        this.status = status.getStatus();
        this.serviceName = serviceName;
        this.cTime = cTime;
        this.mTime = mTime;
        this.context = context;
        this.instructionId = instructionId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getInstructionId() {
        return instructionId;
    }

    public void setInstructionId(String instructionId) {
        this.instructionId = instructionId;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public Status.Action getStatus() {
        return Status.Action.getStatus(status);
    }

    public void setStatus(Status.Action status) {
        this.status = status.getStatus();
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Date getcTime() {
        return cTime;
    }

    public void setcTime(Date cTime) {
        this.cTime = cTime;
    }

    public Date getmTime() {
        return mTime;
    }

    public void setmTime(Date mTime) {
        this.mTime = mTime;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}