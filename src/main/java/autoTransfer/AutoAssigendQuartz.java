package com.jjb.cas.quartz;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.jjb.ecms.biz.cache.CacheContext;
import com.jjb.ecms.constant.AppDitDicConstant;
import com.jjb.ecms.infrastructure.TmDitDic;
import com.jjb.ecms.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.jjb.ecms.biz.util.AppCommonUtil;
import com.jjb.ecms.biz.util.LogPrintUtils;
import com.jjb.unicorn.facility.context.OrganizationContextHolder;

/**
 * 自动分配待办工作任务
 */
@Component
public class AutoAssigendQuartz {

    private static Logger logger = LoggerFactory.getLogger(AutoAssigendQuartz.class);

    @Autowired
    private CacheContext cacheContext;
    @Autowired
    private AutoAssigend autoAssigend;
    private static int counter = 0;
    @Value("#{env['assignQuartzIp'?:'']}")
    private String assignQuartzIp;
    @Autowired
    private AppCommonUtil appCommonUtil;

    public void execute() {
        appCommonUtil.setOrg(OrganizationContextHolder.getOrg());//设置系统机构号
        long ms = System.currentTimeMillis();
        logger.info("Quartz开始 第" + "(" + counter + ")" + "次自动分配......开始" + LogPrintUtils.printCommonStartLog(ms, null));
//        // 利用InetAddress类获取服务器信息
//        InetAddress netAddress = getInetAddress();
//        String hostIp = getHostIp(netAddress);
//        logger.info("host ip[" + hostIp + "],host name[" + getHostName(netAddress) + "]");

        // 开关-是否开启自动分配,为空或者为N的情况下不开启自动分配
        TmDitDic ditDic = cacheContext.getApplyOnlineOnOff(AppDitDicConstant.isAutoTransferOrNot);
        if (ditDic != null && StringUtils.isNotEmpty(ditDic.getRemark())
                && ditDic.getRemark().equals(AppDitDicConstant.onLinOff_on)) {
            logger.info("调用开关-Y,开启自动分配");
            autoAssigend.autoAssignee();
//          if (null != assignQuartzIp && null != hostIp) {
//            	logger.info("开始执行自动分配,host ip[" + hostIp + "],TokenId[" + ms + "]");
//          }
        } else {
            logger.info("调用开关-N,未开启自动分配");
        }
        logger.info("Quartz结束 第" + "(" + counter + ")" + "次自动分配......结束耗时["
                + (System.currentTimeMillis() - ms) + "]");
        counter++;
    }

    public static InetAddress getInetAddress() {

        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            logger.error("获取当前服务器地址信息异常", e);
        }
        return null;

    }

    public static String getHostIp(InetAddress netAddress) {
        if (null == netAddress) {
            return null;
        }
        String ip = netAddress.getHostAddress(); // get the ip address
        return ip;
    }

    public static String getHostName(InetAddress netAddress) {
        if (null == netAddress) {
            return null;
        }
        String name = netAddress.getHostName(); // get the host address
        return name;
    }

}
