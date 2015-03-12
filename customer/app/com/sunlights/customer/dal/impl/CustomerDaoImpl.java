package com.sunlights.customer.dal.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sunlights.common.AppConst;
import com.sunlights.common.DictConst;
import com.sunlights.common.dal.EntityBaseDao;
import com.sunlights.common.exceptions.ConverterException;
import com.sunlights.common.utils.ConverterUtil;
import com.sunlights.common.utils.DBHelper;
import com.sunlights.common.vo.MsgSettingVo;
import com.sunlights.customer.dal.CustomerDao;
import com.sunlights.customer.vo.CustomerVo;
import models.*;
import org.apache.commons.lang3.StringUtils;
import play.Logger;

import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Project: fsp</p>
 * <p>Title: CustomerManageDaoImpl.java</p>
 * <p>Description: </p>
 * <p>Copyright (c) 2014 Sunlights.cc</p>
 * <p>All Rights Reserved.</p>
 *
 * @author <a href="mailto:jiaming.wang@sunlights.cc">wangJiaMing</a>
 */

public class CustomerDaoImpl extends EntityBaseDao implements CustomerDao {
    public String getCustomerIdSeq() {
        Query query = em.createNativeQuery("SELECT nextval('cust_seq')");
        String cust_seq = query.getSingleResult().toString();
        return Strings.padStart(cust_seq, 10, '0');
    }

    //Customer
    @Override
    public Customer getCustomerByMobile(String mobile) {
        List<Customer> list = findBy(Customer.class, "mobile", mobile);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public Customer getCustomerByCustomerId(String customerId) {
        List<Customer> list = findBy(Customer.class, "customerId", customerId);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public Customer updateCustomer(Customer customer) {
        return update(customer);
    }

    @Override
    public Customer saveCustomer(Customer customer) {
        return create(customer);
    }

    public CustomerVo getCustomerVoByPhoneNo(String mobilePhoneNo, String deviceNo) {
        String sql = " select c.mobile,c.real_name,c.nick_name,c.email,c.identity_number," +
                " case when c.identity_typer = :identityTyper and c.identity_number is not null THEN '1' ELSE '0' END as certify," +
                " case when a.trade_password is null THEN '0' ELSE '1' END as tradePwdFlag," +
                " (select count(1) from c_bank_card bc where bc.customer_id = c.customer_id) as bankCardCount," +
                " c.customer_id" +
                " from c_customer c,f_basic_account a" +
                " where  c.customer_id = a.cust_id" +
                " and  c.mobile = :mobilePhoneNo";
        Logger.debug(sql);

        Query query = em.createNativeQuery(sql);
        query.setParameter("identityTyper", DictConst.CERTIFICATE_TYPE_1);
        query.setParameter("mobilePhoneNo", mobilePhoneNo);
        List<Object[]> list = query.getResultList();
        CustomerVo customerVo = transCustomerVo(list);
        if (customerVo != null) {//手势设置查询
            if (StringUtils.isNotEmpty(deviceNo)) {
                String deviceNoSql = "select cg from CustomerGesture cg where cg.customerId = :customerId and cg.deviceNo = :deviceNo order by cg.updateTime desc";
                query = em.createQuery(deviceNoSql, CustomerGesture.class);
                query.setParameter("customerId", customerVo.getCustomerId());
                query.setParameter("deviceNo", deviceNo);
                List<CustomerGesture> customerGestureList = query.getResultList();
                if (customerGestureList.isEmpty()) {
                    customerVo.setGestureOpened("0");
                    customerVo.setGestureSetted("0");
                } else {
                    customerVo.setGestureOpened(AppConst.STATUS_VALID.equals(customerGestureList.get(0).getStatus()) ? "1" : "0");
                    customerVo.setGestureSetted("1");
                }
            } else {
                customerVo.setGestureOpened("0");
                customerVo.setGestureSetted("0");
            }
        }


        return customerVo;
    }

    public CustomerVo getCustomerVoByIdCardNo(String idCardNo, String realName) {
        String sql = " select c.mobile,c.real_name,c.nick_name,c.email,c.identity_number," +
                " case when c.identity_typer = :identityTyper and c.identity_number is not null THEN '1' ELSE '0' END as certify," +
                " case when a.trade_password is null THEN '0' ELSE '1' END as tradePwdFlag," +
                " (select count(1) from c_bank_card bc where bc.customer_id = c.customer_id) as bankCardCount, " +
                "  c.customer_id " +
                " from    c_customer c,f_basic_account a" +
                " where   c.customer_id = a.cust_id" +
                " and     c.real_name = :realName" +
                " and     c.identity_typer = :identityTyper" +
                " and     c.identity_number = :idCardNo";

        Logger.debug(sql);

        Query query = em.createNativeQuery(sql);
        query.setParameter("identityTyper", DictConst.CERTIFICATE_TYPE_1);
        query.setParameter("idCardNo", idCardNo);
        query.setParameter("realName", realName);
        List list = query.getResultList();

        CustomerVo customerVo = transCustomerVo(list);
        if (customerVo != null) {
            customerVo.setGestureOpened("0");
            customerVo.setGestureSetted("0");
        }

        return customerVo;
    }

    public CustomerVo getCustomerVoByAuthenticationMobile(String mobile) {
        String sql = " select c.mobile,c.real_name,c.email,c.customer_id,c.authentication_id" +
                "  from c_customer c, c_authentication a " +
                " where c.authentication_id = a.id " +
                "  and  a.mobile = :mobile";
        Logger.debug(sql);

        Query query = em.createNativeQuery(sql);
        query.setParameter("mobile", mobile);
        List<Object[]> list = query.getResultList();
        if (list.isEmpty()) {
            return null;
        }

        String keys = "mobilePhoneNo,userName,email,customerId,authenticationId";
        List<CustomerVo> customerVos = ConverterUtil.convert(keys, list, CustomerVo.class);

        return customerVos.get(0);
    }

    private CustomerVo transCustomerVo(List<Object[]> list) {
        if (list.isEmpty()) {
            return null;
        }

        String keys = "mobilePhoneNo,userName,nickName,email,idCardNo,certify,tradePwdFlag,bankCardCount,customerId";
        List<CustomerVo> customerVos = ConverterUtil.convert(keys, list, CustomerVo.class);
        CustomerVo customerVo = customerVos.get(0);
        findShuMiAccount(customerVo);

        if (customerVo.getMobilePhoneNo() != null) {
            customerVo.setMobileDisplayNo(customerVo.getMobilePhoneNo().substring(0, 3) + "****" + customerVo.getMobilePhoneNo().substring(7));
        }
        if (AppConst.VALID_CERTIFY.equals(customerVo.getCertify())) {
            customerVo.setIdCardNo(customerVo.getIdCardNo().substring(0, 6) + "******" + customerVo.getIdCardNo().substring(14));
            customerVo.setUserName("*" + customerVo.getUserName().substring(1));
        }
        return customerVo;
    }

    private void findShuMiAccount(CustomerVo customerVo) {
        Query query = createNameQuery("findShuMiAccount", customerVo.getCustomerId());
        List<ShuMiAccount> list = query.getResultList();
        if (list.isEmpty()) {
            return;
        }
        try {
            ConverterUtil.fromEntity(customerVo, list.get(0));
        } catch (ConverterException e) {
            e.printStackTrace();
        }

    }


    //CustomerSession
    public CustomerSession findCustomerSessionByToken(String token, Timestamp nMin) {
        StringBuffer sb = new StringBuffer();
        sb.append("select c FROM CustomerSession c ");
        sb.append("where c.status = 'Y' ");
        sb.append(" /~ and c.token = {token} ~/ ");
        sb.append(" /~ and c.updateTime >= {nMin} ~/ ");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("EQS_token", token);
        params.put("GED_nMin", new Date(nMin.getTime()));

        List<CustomerSession> list = findByMap(sb.toString(), params);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public CustomerSession saveCustomerSession(CustomerSession customerSession) {
        return create(customerSession);
    }

    @Override
    public CustomerSession updateCustomerSession(CustomerSession customerSession) {
        return update(customerSession);
    }

    public CustomerSession findCustomerSessionByCustomerId(String customerId, String deviceNo) {
        Query query = createNameQuery("findCSByCustomerId", customerId, deviceNo);
        List<CustomerSession> list = query.getResultList();
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }


    //CustomerGesture
    @Override
    public CustomerGesture saveCustomerGesture(CustomerGesture customerGesture) {
        return create(customerGesture);
    }

    @Override
    public CustomerGesture updateCustomerGesture(CustomerGesture customerGesture) {
        return update(customerGesture);
    }

    public CustomerGesture findCustomerGestureByDeviceNo(String customerId, String deviceNo) {
        Query query = createNameQuery("findCGByCustomerId", customerId, deviceNo);
        List<CustomerGesture> list = query.getResultList();
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public Customer findRecommenderInfo(String customerId) {
        if (StringUtils.isEmpty(customerId)) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        String columns = "c2.customer_id  ";
        sb.append("select ").append(columns)
                .append("from c_customer c1 join c_customer c2 on c2.mobile = c1.recommend_phone where 1 = 1 and  /~c1.customer_id = {customerId}~/");

        Map<String, Object> filterMap = Maps.newHashMapWithExpectedSize(5);

        filterMap.put("EQS_customerId", customerId);
        List<String> resultRows = createNativeQueryByMap(sb.toString(), filterMap).getResultList();
        if (resultRows == null || resultRows.isEmpty()) {
            return null;
        }
        Customer customer = new Customer();
        customer.setCustomerId(resultRows.get(0));
        return customer;
    }


    @Override
    public List<String> findAliasByCustomerId(String customerId) {
        return createNameQuery("findAliasByCustomerId", customerId).getResultList();
    }

    @Override
    public List<MsgSettingVo> findRegistrationIdsByCustomerId(String customerId) {
        String sql = " SELECT distinct c.registration_id, c.device_no,c.customer_id,c.platform, " +
                "        CASE WHEN ( " +
                "              SELECT cs.status " +
                "              FROM c_customer_session cs " +
                "              WHERE cs.customer_id = c.customer_id " +
                "              AND cs.device_no = c.device_no " +
                "               ORDER BY cs.create_time DESC " +
                "               LIMIT 1 offset 0 " +
                "           ) = 'Y' THEN 'Y' ELSE 'N' END AS loginStatus " +
                "  FROM c_customer_msg_setting c " +
                " WHERE c.push_open_status = 'Y' " +
                "   and c.device_no is not null" +
                "   and c.registration_id is not null " +
                "   and c.customer_id = :customerId";


        Logger.debug(">>findRegistrationIdsByCustomerId:" + sql);

        Query query = em.createNativeQuery(sql);
        query.setParameter("customerId", customerId);
        List<Object[]> list = query.getResultList();

        String keys = "registrationId,deviceNo,customerId,platform,loginStatus";
        return ConverterUtil.convert(keys, list, MsgSettingVo.class);
    }

    @Override
    public CustomerMsgSetting findCustomerMsgSetting(String registrationId, String deviceNo) {
        List<CustomerMsgSetting> list = createNameQuery("findSettingByRegIdAndDeviceNo", registrationId, deviceNo).getResultList();
        return list.isEmpty() ? null : list.get(0);
    }


    @Override
    public CustomerMsgSetting updateCustomerMsgSetting(CustomerMsgSetting customerMsgSetting) {
        return update(customerMsgSetting);
    }

    @Override
    public CustomerMsgSetting createCustomerMsgSetting(CustomerMsgSetting customerMsgSetting) {
        return create(customerMsgSetting);
    }

    @Override
    public boolean validateHasFirstPurchase(String customerId) {
        String sql = "select count(1) from t_trade t where t.cust_id = :customerId and t.create_time > :currentTime";
        Query query = em.createNativeQuery(sql);
        query.setParameter("customerId", customerId);
        query.setParameter("currentTime", DBHelper.afterMinutes(DBHelper.getCurrentTime(), 60));//考虑到调用时间延迟，给1小时移除当前操作交易影响
        int count = Integer.valueOf(query.getSingleResult().toString());
        if (count == 0) {
            return false;
        }

        return true;
    }
}
