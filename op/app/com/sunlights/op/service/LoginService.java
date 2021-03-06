package com.sunlights.op.service;

import com.sunlights.common.MsgCode;
import com.sunlights.common.Severity;
import com.sunlights.common.dal.EntityBaseDao;
import com.sunlights.common.exceptions.BusinessRuntimeException;
import com.sunlights.common.vo.Message;
import com.sunlights.op.service.impl.EmailServiceImpl;
import com.sunlights.op.vo.EmailVo;
import com.sunlights.op.vo.MenuVo;
import com.sunlights.op.vo.UserVo;
import models.P;
import models.Resource;
import models.Role;
import models.User;
import org.apache.commons.lang3.StringUtils;
import play.Logger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Yuan on 2015/1/28.
 */
public class LoginService {

    private EntityBaseDao entityBaseDao = new EntityBaseDao();
    private EmailService emailService = new EmailServiceImpl();

    public static String MENU = "MENU";

    public UserVo login (UserVo userVo) {

        StringBuffer jpql = new StringBuffer();
        jpql.append(" select u from User u");
        jpql.append(" where u.deleted = false");
        jpql.append(" and u.username = ?1");

        List<User> users = entityBaseDao.find(jpql.toString(), userVo.getUsername());

        if (users.isEmpty()) {
            // 没有该用户
            throw new BusinessRuntimeException(new Message(Severity.ERROR, MsgCode.LOGIN_NOT_REGISTER_ERROR));
        }

        if (users.size() > 1) {
            // 该用户账号异常
            throw new BusinessRuntimeException(new Message(Severity.ERROR, MsgCode.LOGIN_ACCOUNT_UNUSUAL_ERROR));
        } else {
            User user = users.get(0);
            if (!"Y".equals(user.getStatus())) {
                // 该用户账号异常
                throw new BusinessRuntimeException(new Message(Severity.ERROR, MsgCode.LOGIN_ACCOUNT_UNUSUAL_ERROR));
            }
            if (!user.getPassword().equals(userVo.getPassword())) {
                // 密码错误
                throw new BusinessRuntimeException(new Message(Severity.ERROR, MsgCode.LOGIN_PASSWORD_INCORRECT_ERROR));
            }

            if (user.getLoginInd() == null) {
                user.setLoginInd("Y");
                userVo.setLoginInd("Y");
            }else{
                user.setLoginInd("N");
                userVo.setLoginInd("N");
            }
            entityBaseDao.update(user);
            return userVo;
        }

    }

    public UserVo getCurrentUser (String username) {
        User user = findUserByName(username);
        if (user == null) {
            return null;
        }
        UserVo userVo = new UserVo();
        userVo.setUsername(user.getUsername());
        userVo.setId(user.getId());
        userVo.setLoginInd(user.getLoginInd());
        P p = user.getP();
        if (p != null) {
            userVo.setZhName(p.getLastName() + p.getFirstName());
        }

        // List<Role> roles = getRoles(user.getUsername());
        // Logger.info("[roles]" + roles.size());

        List<Resource> resources = getResources(user.getUsername());
        Logger.info("[resources]" + resources.size());
        List<String> permissions = new ArrayList<String>();
        for (Resource resource : resources) {
            permissions.add(resource.getUri());
        }
        userVo.setPermissions(permissions);

        if (!resources.isEmpty()) {
            MenuVo menuVo = getMenuVo(resources.get(0), resources);
            userVo.setMenuVo(menuVo);
        }
        return userVo;
    }

    public void reset (UserVo userVo) {
        List<User> users = entityBaseDao.find("select u from User u where u.username = '" + userVo.getUsername().trim() + "'");
        if (users.isEmpty()) {
            throw new BusinessRuntimeException(new Message(Severity.ERROR, MsgCode.LOGIN_NOT_REGISTER_ERROR));
        } else {
            User user = users.get(0);

            if (StringUtils.isNotEmpty(userVo.getEmail())) {
                P p = user.getP();
                if (p == null || !userVo.getEmail().trim().equals(p.getEmail())) {
                    // 用户名和邮箱不匹配
                    throw new BusinessRuntimeException(new Message(Severity.ERROR, MsgCode.USER_NAME_NOT_MATCHED_EMAIL));
                }
                EmailVo emailVo = new EmailVo();
                emailVo.setSubject(MsgCode.OPERATOR_RESET_PWD.getMessage());
                List<String> tos = new ArrayList<String>();
                tos.add(userVo.getEmail().trim());
                emailVo.setTo(tos);
                String emailInfo = MessageFormat.format(MsgCode.OPERATOR_RESET_PWD_INFO.getMessage(), userVo.getUsername(), user.getPassword());
                emailVo.setBodyHtml(emailInfo);
                emailService.sendEmail(emailVo);

            }

            user.setPassword(userVo.getPassword().trim());
            user.setUpdateTime(new Date());
            entityBaseDao.update(user);
        }

    }

    private User findUserByName (String username) {
        StringBuffer jpql = new StringBuffer();
        jpql.append(" select u from User u");
        jpql.append(" where u.deleted = false");
        jpql.append(" and u.username = ?1");

        List<User> users = entityBaseDao.find(jpql.toString(), username);

        if (users.isEmpty()) {
            // 没有该用户
            throw new BusinessRuntimeException(new Message(Severity.ERROR, MsgCode.LOGIN_NOT_REGISTER_ERROR));
        }

        if (users.size() > 1) {
            // 该用户账号异常
            throw new BusinessRuntimeException(new Message(Severity.ERROR, MsgCode.LOGIN_ACCOUNT_UNUSUAL_ERROR));
        } else {
            return users.get(0);
        }

    }

    private List<Role> getRoles (String username) {
        if (username == null || username.trim().isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        String jpql = "select ur.role from UserRole ur where ur.deleted = false and ur.role.deleted = false and ur.user.username = ?1";
        List<Role> roles = entityBaseDao.find(jpql, username);

        return roles;
    }

    private List<Resource> getResources (String username) {
        if (username == null || username.trim().isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        StringBuffer jpql = new StringBuffer();
        jpql.append(" select distinct rsr.resource,rsr.resource.parentId,rsr.resource.seqNo");
        jpql.append(" from UserRole ur,RoleResource rsr");
        jpql.append(" where 1=1");
        jpql.append(" and ur.deleted = false");
        jpql.append(" and ur.role= rsr.role");
        jpql.append(" and ur.user.username = ?1");
        jpql.append(" and rsr.deleted = false");
        jpql.append(" and rsr.resource.deleted = false");
        jpql.append(" and rsr.role.deleted = false");
        jpql.append(" order by rsr.resource.parentId asc,rsr.resource.seqNo asc");

        List<Resource> resources = new ArrayList<Resource>();
        List<Object[]> rows = entityBaseDao.find(jpql.toString(), username);

        for (Object[] row : rows) {
            resources.add((Resource) row[0]);
        }
        return resources;
    }

    private MenuVo getMenuVo (Resource resource, List<Resource> resources) {
        MenuVo menuVo = new MenuVo(resource);
        for (Resource r : resources) {
            if (resource.getId() == r.getParentId()) {
                menuVo.getMenuVos().add(getMenuVo(r, resources));
            }
        }
        return menuVo;
    }
}
