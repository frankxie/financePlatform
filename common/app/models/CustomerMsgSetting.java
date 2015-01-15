package models;

import com.sunlights.common.AppConst;

import javax.persistence.*;
import java.util.Date;

/**
 * <p>Project: thirdpartyservice</p>
 * <p>Title: CustomerMsgSetting.java</p>
 * <p>Description: </p>
 * <p>Copyright (c) 2014 Sunlights.cc</p>
 * <p>All Rights Reserved.</p>
 *
 * @author <a href="mailto:jiaming.wang@sunlights.cc">wangJiaMing</a>
 */
@Entity
@Table(name = "c_customer_msg_setting")
@NamedQueries({
        @NamedQuery(name="findAliasByGroupId",query="select cms.alias from CustomerGroup cg,CustomerMsgSetting cms where cg.customerId = cms.customerId and cms.pushOpenStatus = 'Y' and cg.id = ?1"),
        @NamedQuery(name="findAliasByCustomerId",query="select cms.alias from CustomerMsgSetting cms where cms.pushOpenStatus = 'Y' and cms.customerId = ?1"),
        @NamedQuery(name="findRegistrationIdsByCustomerId",query="select cms.registrationId from CustomerMsgSetting cms where cms.pushOpenStatus = 'Y' and cms.customerId = ?1"),
        @NamedQuery(name="findSettingByRegistrationId",query="select cms from CustomerMsgSetting cms where cms.pushOpenStatus = 'Y' and cms.registrationId = ?1")
})
public class CustomerMsgSetting extends IdEntity {
    @Column(name = "customer_id", length = 30)
    private String customerId;
    private String tag;
    private String alias;
    @Column(name = "registration_id")
    private String registrationId;
    @Column(name = "push_open_status", length = 1)
    private String pushOpenStatus = AppConst.STATUS_VALID;//Y开启、N关闭推送
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time")
    private Date createTime;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time")
    private Date updateTime;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public String getPushOpenStatus() {
        return pushOpenStatus;
    }

    public void setPushOpenStatus(String pushOpenStatus) {
        this.pushOpenStatus = pushOpenStatus;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

}