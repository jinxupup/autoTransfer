package com.jjb.cas.quartz;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jjb.acl.access.service.AccessUserService;
import com.jjb.acl.infrastructure.TmAclBranch;
import com.jjb.acl.infrastructure.TmAclUser;
import com.jjb.ecms.biz.cache.CacheContext;
import com.jjb.ecms.biz.service.activiti.ActivitiService;
import com.jjb.ecms.biz.service.apply.ApplyQueryService;
import com.jjb.ecms.biz.service.commonDialog.ApplyHistoryService;
import com.jjb.ecms.biz.service.manage.ApplyTaskDetailsService;
import com.jjb.ecms.biz.service.param.SysParamService;
import com.jjb.ecms.biz.util.AppCommonUtil;
import com.jjb.ecms.biz.util.LogPrintUtils;
import com.jjb.ecms.constant.AppConstant;
import com.jjb.ecms.facility.dto.ApplyTaskDetailsDto;
import com.jjb.ecms.facility.dto.SimpleUser;
import com.jjb.ecms.infrastructure.TmAppAudit;
import com.jjb.ecms.infrastructure.TmAppAuditQuota;
import com.jjb.ecms.infrastructure.TmAppHistory;
import com.jjb.ecms.infrastructure.TmAppMain;
import com.jjb.ecms.infrastructure.TmDitDic;
import com.jjb.ecms.util.CollectionUtils;
import com.jjb.ecms.util.StringUtils;
import com.jjb.jyd.dictionary.enums.EcmsAuthority;
import com.jjb.jyd.dictionary.enums.RtfState;
import com.jjb.unicorn.facility.context.OrganizationContextHolder;


/**
 * @Description: 案件自动分配
 */
@Component
public class AutoAssigend implements Serializable {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static final long serialVersionUID = 1L;
    private static final String DIT_DIC_AUTO_CONFIG_STR = "autoTransfer";
    private static final String DIT_DIC_FILTER_USER_STR = "autoTransferSDuser";
//	private static final String AUTO_FILTER_USER_NO_DATA = "noData";

    @Autowired
    private TaskService taskService;
    @Autowired
    private ApplyQueryService applyQueryService;
    @Autowired
    private ApplyTaskDetailsService applyTaskDetailsService;
    //	@Autowired
//	private UnifiedParameterFacility parameterFacility;
    @Autowired
    private SysParamService sysParamService;
    @Autowired
    private ApplyHistoryService applyHistoryService;
    @Autowired
    private CacheContext cacheContext;
    @Autowired
    private AccessUserService accessUserService;
    //	@Autowired
//	private ApplyInputService applyInputService;
    @Autowired
    private AppCommonUtil appCommonUtil;
    @Autowired
    private ActivitiService activitiService;

    private Map<String, DicBean> isOpenAutoAssigneeMap = new HashMap<String, DicBean>();
    //	private static Map<String,Map<String, BMPUser>> activitiUserMap = new HashMap<String, Map<String, BMPUser>>();
    private Map<String, Map<String, List<SimpleUser>>> aclUserMap = new HashMap<String, Map<String, List<SimpleUser>>>();
    //	private Map<String, TmAclBranch> branchMap = new HashMap<String, TmAclBranch>();
    private Map<String, List<String>> filterUser = new HashMap<String, List<String>>();

    /**
     * 待办任务自动分配
     */
    public void autoAssignee() {
        appCommonUtil.setOrg(OrganizationContextHolder.getOrg());//设置系统机构号
        //获取自动分配的开关
        List<TmDitDic> tmDitDicList = getAllAutoAssignConfi();
        if (tmDitDicList != null && tmDitDicList.size() > 0) {
//			branchMap = new HashMap<String, TmAclBranch>();
            //循环遍历每一条配置,需找特定任务的未分配任务,再循环便利每一条未分配任务去分配
            for (int i = 0; i < tmDitDicList.size(); i++) {
                TmDitDic t1 = tmDitDicList.get(i);
                filterUser = new HashMap<String, List<String>>();
                aclUserMap = new HashMap<String, Map<String, List<SimpleUser>>>();
                if (t1.getTabName() != null && !t1.getTabName().equals("")
                        && t1.getRemark() != null && StringUtils.equals(t1.getRemark(), "Y")) {
                    OrganizationContextHolder.setOrg(t1.getOrg());
                    //获取特定任务的所有未分配任务列表(按照设置的优先派件规则来,如果没有则按默认来)
                    ApplyTaskDetailsDto applyTaskDetailsDto = new ApplyTaskDetailsDto();
                    applyTaskDetailsDto.setTaskDefKey(t1.getTabName());
                    List<ApplyTaskDetailsDto> unTaskList = new ArrayList<ApplyTaskDetailsDto>();
                    String pri = t1.getItemName();
                    StringBuffer sb = new StringBuffer();
                    boolean isUseSort = false;
                    if (StringUtils.isNotEmpty(pri)) {
//						( CASE WHEN M.APP_PROPERTY like '%V%' THEN 1 WHEN M.APP_PROPERTY like '%P%' THEN 2 END) asc ,
                        String[] priortys = pri.split("\\|");
                        sb.append(" (CASE ");
                        for (int j = 0; j < priortys.length; j++) {
                            String str = priortys[j];
                            //str : M.APP_PROPERTY like '%V%'
                            if (StringUtils.isNotEmpty(str)) {
                                isUseSort = true;
                                sb.append(" WHEN  M.APP_PROPERTY like ");
                                sb.append("'" + "%" + str + "%" + "'");
                                sb.append(" THEN ");
                                sb.append(j + 1);
                            }
                        }
                        sb.append(" END) asc , ");
                    }
                    if (isUseSort) {
                        applyTaskDetailsDto.setPriorit(sb.toString());
                    }
                    unTaskList = applyTaskDetailsService.getTaskUndistributedList(applyTaskDetailsDto);
                    if (unTaskList != null) {
                        logger.info("节点[" + t1.getItemName() + "]已开启自动分配[" + t1.getRemark() + "],平均数量[" + unTaskList.size() + "]");
                        //遍历每一条的未分配任务并进行分配
                        for (int j = 0; j < unTaskList.size(); j++) {
                            ApplyTaskDetailsDto unTask = unTaskList.get(j);
                            if (unTask == null) {
                                continue;
                            }
                            OrganizationContextHolder.setOrg(unTask.getOrg());
                            OrganizationContextHolder.setUserNo("sysauto");
                            try {
                                assignee(unTask.getTaskId(), unTask.getTaskDefKey(), unTask.getAppNo(), true);
                            } catch (Exception e) {
                                logger.error("自动分配失败,AppNo[" + unTask.toString() + "]", e);
                            }
                        }
                    }
                }
            }
            //置空值，待下次自动处理时在查
            isOpenAutoAssigneeMap = new HashMap<String, DicBean>();
            aclUserMap = new HashMap<String, Map<String, List<SimpleUser>>>();
//			branchMap = new HashMap<String, TmAclBranch>();
            filterUser = new HashMap<String, List<String>>();
        }
    }

    /**
     * 自动分配员工
    
     * //1-1.通过操作员提交任务发起的自动分配动作保留：录入复核退回、初审退回、终审退回、重审等共计4个状态操作。
     * //1-2.录入复核退回、初审退回、终审退回、重审等分配到原经办人手中（不考虑手头案件数量和是否自动分配，后续若新增退回操作，都是类似处理）
     * //1-3.如果原经办人权限或者发生岗位变更，则该任务变成待分配状态。
     * //1-4.如果是补件->初审，则分配至初审经办人队列（不考虑手头案件数量和是否自动分配）
     * <p>
     * //2.通过系统quartz定时任务发起的分配如下：
     * //2-1.查找配置，判断节点是否需要自动分配
     * //2-2找到拥有操作此流程的权限的员工，当员工为空，跳过分配步骤
     * //2-3过滤分行，过滤后员工为空，跳过分配步骤
     * //2-4如果是终审，根据额度配置过滤用户
     * //2-5统计查询当前节点的任务数
     * //2-6查找用户，不在统计查询列表中的用户优先分配
     * //2-7若6中未找到用户，此时找统计查询的第一个用户
     * //2-8如果是补件操作，则只能分配到录入时选择的受理网点中的补件操作员手中
     *
     * @param
     */
    @Transactional
    public void assignee(String taskId, String taskKey, String appNo, boolean isQuartz) {
        long start = System.currentTimeMillis();
        String userId = null;
        String opUser = OrganizationContextHolder.getUserNo();
        if (StringUtils.isEmpty(appNo)) {
            logger.error("无法正常分配，申请件编号[" + appNo + "] 为空");
            return;
        }
        // 获取自动分配配置信息
        DicBean dicBean = getAutoAssignConfi(taskKey);
        //如果是定时任务自动发起分配
        if (isQuartz) {
            // 判断是否自动分案
            if (dicBean == null || !dicBean.isAssign()) {
                return;
            }
        }
        logger.info("开始分配...taskId[" + taskId + "],taskKey[" + taskKey + "]" + LogPrintUtils.printAppNoLog(appNo, start, null));
        //先查询出任务
        List<Task> taskList = taskService.createTaskQuery().taskId(taskId).list();
        Task task = null;
        if (CollectionUtils.sizeGtZero(taskList)) {
            task = taskList.get(0);
        }
        TmAppMain appMain = applyQueryService.getTmAppMainByAppNo(appNo);
        if (appMain == null || appMain.getOwningBranch() == null) {
            logger.info("本次任务自动分配结束...没有找到该申请件, taskId[" + taskId + "],taskKey[" + taskKey
                    + "],rtfState[" + appMain.getRtfState() + "]" + LogPrintUtils.printAppNoEndLog(appNo, start, null) + "开始自动分配下一个任务");
            return;
        }
        // 获取拥有操作此流程的员工,此流程未分配员工，忽略分配步骤
        Map<String, List<SimpleUser>> usersMaps = filterEmployees(taskKey, appMain.getProductCd());
        if (usersMaps == null || usersMaps.size() == 0) {
            logger.info("本次任务自动分配结束...没有获取到操作此流程的员工, taskId[" + taskId + "],taskKey[" + taskKey
                    + "],rtfState[" + appMain.getRtfState() + "]" + LogPrintUtils.printAppNoEndLog(appNo, start, null) + "开始自动分配下一个任务");
            return;
        }
        //获取系统所有分支行
        List<String> branchIds = setOwningBranchVariable(appMain.getOwningBranch());
        // 筛选分行员工
        usersMaps = filterBranchUser(usersMaps, branchIds);
        if (usersMaps == null || usersMaps.size() == 0) {
            logger.info("本次任务自动分配结束...没有找到可分配用户列表, taskId[" + taskId + "],taskKey[" + taskKey
                    + "],rtfState[" + appMain.getRtfState() + "]" + LogPrintUtils.printAppNoEndLog(appNo, start, null) + "开始自动分配下一个任务");
            return;
        }
        List<SimpleUser> userList = new ArrayList<SimpleUser>();
        if (usersMaps != null && usersMaps.size() > 0) {
            for (List<SimpleUser> list : usersMaps.values()) {
                if (CollectionUtils.isNotEmpty(list)) {
                    userList.addAll(list);
                }
            }
        }
        if (CollectionUtils.isEmpty(userList)) {
            logger.info("自动分配结束...没有找到可分配用户列表, taskId[" + taskId + "],taskKey[" + taskKey
                    + "],rtfState[" + appMain.getRtfState() + "]" + LogPrintUtils.printAppNoEndLog(appNo, start, null));
            return;
        }
        // 补件
        if (taskKey.equals(EcmsAuthority.CAS_APPLY_PATCHBOLT.lab)) {
            userList = filterPathBoltUser(usersMaps, appMain);
        }
        // 终审
        if (taskKey.equals(EcmsAuthority.CAS_APPLY_FINALAUDIT.lab)) {
            userList = filterFinalFlowUser(userList, appMain, taskKey);
        }
        if (CollectionUtils.isEmpty(userList)) {
            logger.info("自动分配结束...没有找到可分配用户列表, taskId[" + taskId + "],taskKey[" + taskKey
                    + "],rtfState[" + appMain.getRtfState() + "]" + LogPrintUtils.printAppNoEndLog(appNo, start, null));
            return;
        }
        //如果是录入复核退回、初审退回、终审退回、重审到初审、补件完成到初审
        if (
                (appMain.getRtfState().equals(RtfState.B15.state) //申请复核退回
                || appMain.getRtfState().equals(RtfState.F07.state) //初审调查退回录入修改
                || appMain.getRtfState().equals(RtfState.K08.state) //终审退回电调
                || appMain.getRtfState().equals(RtfState.A30.state)  //行里说重审不要进原经办,重审完成到初审调查
                || appMain.getRtfState().equals(RtfState.G10.state) //补件完成(其实写这里并没什么卵用，等后期改了流程图就有用了)
                || appMain.getRtfState().equals(RtfState.F18.state)//电话调查退回初审
                || appMain.getRtfState().equals(RtfState.F03.state)//初审退回至预审

        )) {
/*			String hisAppNo = applyQueryService.getTmAppAuditByAppNo(appNo).getAppNoHis();
            String historyOpUser ="";
			if (StringUtils.isNotEmpty(hisAppNo)){
				 historyOpUser = getHisOpUser(appNo, hisAppNo, appMain);*/
            TmAppAudit histAudit = applyQueryService.getTmAppAuditByAppNo(appNo);
            String hisAppNo = null;
            if(histAudit!=null) {
                hisAppNo = histAudit.getAppNoHis();
            }
            String historyOpUser = getHisOpUser(appNo, hisAppNo, appMain);
            //正常流程往下流转时------
            logger.info("回原经办人分配...申请件编号[" + appNo + "],opUser[" + opUser + "],taskId[" + taskId + "],taskKey[" + taskKey
                    + "],rtfState[" + appMain.getRtfState() + "],hisAppNo[" + hisAppNo + "]");
            if (StringUtils.isNotBlank(historyOpUser) && isInUserList(userList, historyOpUser)) {
                //正常流程往下流转时------
                activitiService.assingneeTask(task, appMain, appNo, AppConstant.SYS_AUTO, historyOpUser);
                logger.info("回原经办人[" + historyOpUser + "]分配成功...申请件编号[" + appNo + "],opUser[" + opUser + "],taskId[" + taskId
                        + "],taskKey[" + taskKey + "],rtfState[" + appMain.getRtfState() + "]");
            } else {
                logger.info("回原经办人[" + historyOpUser + "]分配失败..(原经办人不在指定分配用户中).申请件编号[" + appNo + "],opUser[" + opUser + "],taskId[" + taskId
                        + "],taskKey[" + taskKey + "],rtfState[" + appMain.getRtfState() + "]");
            }
            return;
        } else if (!isQuartz) {
            return;
        }
        //正常流程往下流转时------
        logger.info("自动分配...申请件编号[" + appNo + "],opUser[" + opUser + "],taskId[" + taskId
                + "],taskKey[" + taskKey + "],rtfState[" + appMain.getRtfState() + "]");
        // 获取全部有当前状态任务的用户,按用户以及任务个数分组
        List<CandidateBean> cbList = getCandidate(taskKey);
        if (cbList != null && cbList.size() > 0) {
            // 先找手上没任务的
            for (int i = 0; i < userList.size(); i++) {
                SimpleUser user = userList.get(i);
                boolean isInCandidates = false;
                for (CandidateBean candidateBean : cbList) {
                    if (user != null && candidateBean.userId.equals(user.getUserNo())) {
                        isInCandidates = true;
                        continue;
                    }
                }
                if (isInCandidates == false) {
                    userId = user.getUserNo();
                    break;
                }
            }
            // 手上都有任务，就取任务数最少的人。数据库查询已完成排序工作
            if (userId == null) {
                // 此处应当取users中案件最少的人(得到cbList时实现),并且案件数量小于配置的最大数目
                for (CandidateBean candidateBean : cbList) {
                    for (int i = 0; i < userList.size(); i++) {
                        SimpleUser user = userList.get(i);
                        if (candidateBean != null && candidateBean.userId.equals(user.getUserNo())) {
                            // 保证取到第一条，及数量最小的;判断手上的案件数目，如果案件最少的员工案件数目也超过配置的最大数目，不分配
                            if (dicBean.getMax() == 0 || dicBean.getMax() > candidateBean.getCt()) {
                                userId = user.getUserNo();
                                if (userId != null) {
                                    //!设置任务转移记录
                                    activitiService.assingneeTask(task, appMain, appNo, AppConstant.SYS_AUTO, userId);
                                    logger.info("自动分配任务给操作员[" + userId + "]成功...,taskId[" + taskId + "],taskKey[" + taskKey
                                            + "],rtfState[" + appMain.getRtfState() + "]" + LogPrintUtils.printAppNoEndLog(appNo, start, null));
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // 当前手上都没任务，就去users中的第一个
            userId = userList.get(0).getUserNo();
        }
        if (userId != null) {
            logger.info("自动分配任务给操作员[" + userId + "]成功...,taskId[" + taskId + "],taskKey[" + taskKey
                    + "],rtfState[" + appMain.getRtfState() + "]" + LogPrintUtils.printAppNoEndLog(appNo, start, null));
            //!设置任务转移记录
            activitiService.assingneeTask(task, appMain, appNo, AppConstant.SYS_AUTO, userId);
        }
    }

    /**
     * 筛选分行出用户
     *
     * @param users
     * @param branchIds
     * @return
     */
    private Map<String, List<SimpleUser>> filterBranchUser(Map<String, List<SimpleUser>> users, List<String> branchIds) {
        Map<String, List<SimpleUser>> users2 = new HashMap<String, List<SimpleUser>>();
        for (int i = 0; i < branchIds.size(); i++) {
            users2.put(branchIds.get(i), users.get(branchIds.get(i)));
        }
        return users2;
    }

    /**
     * 筛选终审可见额度用户
     *
     * @param
     * @param
     * @param
     * @return
     */
    private List<SimpleUser> filterFinalFlowUser(List<SimpleUser> userList, TmAppMain tmAppMain, String taskKey) {
        List<SimpleUser> userList2 = new ArrayList<SimpleUser>();
        BigDecimal appLmt = tmAppMain.getAppLmt();
        if (appLmt == null) {
            appLmt = BigDecimal.ZERO;
        }

        BigDecimal chkLmt = tmAppMain.getChkLmt();
        if (chkLmt == null) {
            chkLmt = BigDecimal.ZERO;
        }
        if (CollectionUtils.isNotEmpty(userList)) {
            for (int i = 0; i < userList.size(); i++) {
                SimpleUser su = userList.get(i);
                TmAppAuditQuota quota = cacheContext.getTmAppAuditQuotaForCache(su.getUserNo(), taskKey);
                //该操作员设置了可见额度最低与最高
                if (quota != null && (quota.getVisibleMinimum() != null || quota.getVisibleMaximum() != null)) {
                    BigDecimal compareSrcAmt = tmAppMain.getSugLmt();
                    if (compareSrcAmt == null && tmAppMain.getChkLmt() != null) {
                        compareSrcAmt = tmAppMain.getChkLmt();
                    } else if (compareSrcAmt == null && tmAppMain.getAppLmt() != null) {
                        compareSrcAmt = tmAppMain.getAppLmt();
                    } else if (compareSrcAmt == null && tmAppMain.getAccLmt() != null) {
                        compareSrcAmt = tmAppMain.getAccLmt();
                    } else {
                        userList2.add(su);
                        continue;
                    }
                    boolean isOk = false;
                    if (quota.getVisibleMinimum() != null && compareSrcAmt.compareTo(quota.getVisibleMinimum()) >= 0) {
                        isOk = true;
                    }
                    if (quota.getVisibleMaximum() != null && compareSrcAmt.compareTo(quota.getVisibleMaximum()) <= 0) {
                        isOk = true;
                    }
                    if (isOk) {
                        userList2.add(su);
                    }
                }/*else { 如果未设置有效的终审人员可见额度，则不分配给此人
					userList2.add(su);
				}*/
            }
        }

        return userList2;
    }

    /**
     * 找到拥有当前节点权限的员工
     *
     * @param taskKey
     * @return
     */
    private Map<String, List<SimpleUser>> filterEmployees(String taskKey, String productCd) {
        Map<String, List<SimpleUser>> map1 = new HashMap<String, List<SimpleUser>>();

        //循环的取tm_dit_dic表中DIC_TYPE为autoTransferSDuser中的REMARK的用户名存入filterUser(屏蔽的用户)
        //如果filterUser里面有值则不再进入此方法
        if (filterUser.size() == 0) {
            TmDitDic ditDic = new TmDitDic();
            ditDic.setDicType(DIT_DIC_FILTER_USER_STR);
            ditDic.setTabName(taskKey);
            List<TmDitDic> ditDics = sysParamService.getTmDitDic(ditDic);
            List<String> sdUserList = new ArrayList<String>();
            for (int i = 0; i < ditDics.size(); i++) {
                TmDitDic tmDitDic = ditDics.get(i);
                if (tmDitDic != null) {
                    String sdUser = tmDitDic.getRemark();
                    if (StringUtils.isNotEmpty(sdUser)) {
                        sdUserList.add(sdUser);
                    }
                }
                filterUser.put("sdUserNo", sdUserList);
            }
        }

/*		//如果等于空或者里面有个值是noData，表示不再进入该方法获取配置参数
		if(filterUser.size()==0 || !filterUser.containsKey(AUTO_FILTER_USER_NO_DATA)){
			TmDitDic ditDic = new TmDitDic();
			ditDic.setDicType(DIT_DIC_FILTER_USER_STR);
			ditDic.setTabName(taskKey);
			List<TmDitDic> ditDics = sysParamService.getTmDitDic(ditDic);
			if(ditDics!=null && ditDics.size()>0){
				for (int i = 0; i < ditDics.size(); i++) {
					filterUser.put(taskKey+"-"+ditDics.get(i).getRemark(), ditDics.get(i).getRemark());
				}
			}
			//设置无值
			if(filterUser.size()==0){
				filterUser = new HashMap<String, String>();
				filterUser.put(AUTO_FILTER_USER_NO_DATA,AUTO_FILTER_USER_NO_DATA);
			}
		}*/

        if (aclUserMap != null && aclUserMap.containsKey(taskKey)) {
            map1 = aclUserMap.get(taskKey);
        } else {
            String[] authString = new String[]{};
            if (StringUtils.equals(taskKey, "applyinfo-finalaudit")) {
                authString = new String[]{"CAS_APPLY_FINALAUDIT"};
            }
            if (StringUtils.equals(taskKey, "applyinfo-check")) {
                authString = new String[]{"CAS_APPLY_BASIC_CHECK"};
            }
            if (StringUtils.equals(taskKey, "applyinfo-pre-check")) {
                authString = new String[]{"CAS_APPLY_PRE_CHECK"};
            }
            if (StringUtils.equals(taskKey, "applyinfo-patchbolt")) {
                authString = new String[]{"CAS_APPLY_PATCHBOLT"};
            }
            if (StringUtils.equals(taskKey, "applyinfo-telephone-survey")) {
                authString = new String[]{"CAS_APPLY_TEL_SURVEY"};
            }
            if (StringUtils.equals(taskKey, "applyinfo-input-modify")) {
                authString = new String[]{"CAS_APPLY_UPDATE"};
            }
            if (StringUtils.equals(taskKey, "applyinfo-review")) {
                authString = new String[]{"CAS_APPLY_REAUDIT"};
            }

            //获取所有有对应操作权限的用户
            List<TmAclUser> aclUsers = accessUserService.getUserMenus(authString);
            for (TmAclUser acluser : aclUsers) {
                //所有有对应操作权限的用户不在过滤列表中的用户才可以继续使用
                if (filterUser == null || filterUser.size() == 0 || (acluser.getUserNo() != null
                        && filterUser.get("sdUserNo") != null && !filterUser.get("sdUserNo").contains(acluser.getUserNo()))) {
                    SimpleUser su = new SimpleUser();
                    su.setUserNo(acluser.getUserNo());
                    su.setUserChName(acluser.getUserName());
                    su.setBranchId(acluser.getBranchCode());
                    if (StringUtils.isNotEmpty(acluser.getBranchCode())) {
                        if (map1 != null && map1.containsKey(acluser.getBranchCode())) {
                            List<SimpleUser> aclu2 = map1.get(acluser.getBranchCode());
                            aclu2.add(su);
                            map1.put(acluser.getBranchCode(), aclu2);
                        } else {
                            List<SimpleUser> aclu2 = new ArrayList<SimpleUser>();
                            aclu2.add(su);
                            map1.put(acluser.getBranchCode(), aclu2);
                        }
                    }
                }
            }
            if (aclUserMap == null) {
                aclUserMap = new LinkedHashMap<String, Map<String, List<SimpleUser>>>();
            }
            aclUserMap.put(taskKey, map1);
        }
        return map1;
    }

    /**
     * 获取自动分案配置信息
     *
     * @param taskKey
     * @return
     */
    private DicBean getAutoAssignConfi(String taskKey) {
        String org = OrganizationContextHolder.getOrg();
        if (isOpenAutoAssigneeMap != null && isOpenAutoAssigneeMap.containsKey(org + "-" + taskKey)) {
            return isOpenAutoAssigneeMap.get(org + "-" + taskKey);
        } else {
            List<TmDitDic> tmDitDicList = getAllAutoAssignConfi();
            if (tmDitDicList == null || tmDitDicList.size() < 1) {
                return null;
            }
            for (int i = 0; i < tmDitDicList.size(); i++) {
                TmDitDic t = tmDitDicList.get(i);
                if (t != null) {
                    //是否开启
                    String assign = t.getRemark();
                    //最大平均派件数
                    String maxNum = t.getFormName();
                    try {
                        DicBean dicBean = new DicBean();
                        dicBean.setAssign(false);
                        dicBean.setMax(0);
                        // 配置以竖线（|）分隔
                        dicBean.setAssign("Y".equals(assign));
                        dicBean.setMax(Integer.parseInt(maxNum));
                        isOpenAutoAssigneeMap.put(t.getOrg() + "-" + t.getTabName(), dicBean);
                    } catch (Exception e) {
                        logger.error(OrganizationContextHolder.getOrg() + "自动分案配置错误", e);
                    }
                }
            }
        }
        return isOpenAutoAssigneeMap.get(org + "-" + taskKey);
    }

    /**
     * 获取所有自动分配配置列表
     *
     * @return
     */
    private List<TmDitDic> getAllAutoAssignConfi() {
        TmDitDic ditDic = new TmDitDic();
        ditDic.setDicType(DIT_DIC_AUTO_CONFIG_STR);
        List<TmDitDic> tmDitDicList = sysParamService.getTmDitDic(ditDic);
        if (tmDitDicList == null || tmDitDicList.size() < 1) {
            return null;
        }

        Map<String, DicBean> mapDicBean = new HashMap<String, DicBean>();
        isOpenAutoAssigneeMap = new HashMap<String, DicBean>();
        for (int i = 0; i < tmDitDicList.size(); i++) {
            TmDitDic t = tmDitDicList.get(i);
            if (t != null) {
                //是否开启
                String assign = t.getRemark();
                //最大平均派件数
                String maxNum = t.getFormName();
                try {
                    DicBean dicBean = new DicBean();
                    dicBean.setAssign(false);
                    dicBean.setMax(0);
                    // 配置以竖线（|）分隔
                    dicBean.setAssign("Y".equals(assign));
                    dicBean.setMax(Integer.parseInt(maxNum));
                    isOpenAutoAssigneeMap.put(t.getOrg() + "-" + t.getTabName(), dicBean);
                    mapDicBean.put(t.getOrg() + "-" + t.getTabName(), dicBean);
                } catch (Exception e) {
                    logger.error(OrganizationContextHolder.getOrg() + "自动分案配置错误", e);
                }
            }
        }
        return tmDitDicList;
    }

    /**
     * 获取已分配清单
     *
     * @param taskKey
     * @return
     */
    public List<CandidateBean> getCandidate(String taskKey) {
        List<CandidateBean> list = new ArrayList<CandidateBean>();
        ApplyTaskDetailsDto applyTaskDetailsDto = new ApplyTaskDetailsDto();
        applyTaskDetailsDto.setTaskDefKey(taskKey);
        List<ApplyTaskDetailsDto> taskList = applyTaskDetailsService.getTaskCntBytaskKey(applyTaskDetailsDto);
        if (CollectionUtils.isEmpty(taskList)) {
            return list;
        }
        for (ApplyTaskDetailsDto applyTaskDetailsDto2 : taskList) {
            CandidateBean candidateBean = new CandidateBean();
            candidateBean.ct = (Integer) applyTaskDetailsDto2.getTaskCnt();
            candidateBean.taskKey = (String) applyTaskDetailsDto2.getTaskDefKey();
            candidateBean.userId = (String) applyTaskDetailsDto2.getOwner();
            list.add(candidateBean);
        }
        return list;
    }

    /**
     * 获取原经办人
     *
     * @param
     * @return
     */
    private String getHisOpUser(String appNo, String hisAppNo, TmAppMain appMain) {

        logger.info("获取原初审经办人,newAppNo[" + appNo + "], historyAppNo[" + hisAppNo + "]");
        List<String> rtfStateList = new ArrayList<String>();
        //如果是终审退回、重审等,，给终审退回和重审时找原初审经办人
        if (appMain.getRtfState().equals(RtfState.K08.state) || appMain.getRtfState().equals(RtfState.A30.state)) {
            // 如果是重审，则取历史申请编号得到初审经半人。
            if (hisAppNo != null && !hisAppNo.trim().equals("")) {
                appNo = hisAppNo;
            }
//			rtfStateList.add(RtfState.F06.toString());//初审调查拒绝
            rtfStateList.add(RtfState.F20.toString());//电话调查完成
        } else if (appMain.getRtfState().equals(RtfState.G10.state)
                || appMain.getRtfState().equals(RtfState.J05.state)) {//如果当前状态是补件完成,查找踢补件的初审原经办人
            rtfStateList.add(RtfState.F08.toString());//初审调查要求补件
        } else if (appMain.getRtfState().equals(RtfState.B15.state)) {//给录入复核退回时找录入完成原经办人
            rtfStateList.add(RtfState.A10.toString());//录入完成
        } else if (appMain.getRtfState().equals(RtfState.F07.state)) {//给录初审退回时找录入或者录入修改人手中
            //给录初审退回时找人工核查完成原经办人
            rtfStateList.add(RtfState.A10.toString());//人工核查完成
        } else if (appMain.getRtfState().equals(RtfState.F18.state)) {//电话调查退回
            rtfStateList.add(RtfState.F05.toString());//初审调查提交
        }
        if (rtfStateList == null || rtfStateList.isEmpty()) {
            return null;
        }
        TmAppHistory appHistory = new TmAppHistory();
        appHistory.setAppNo(appNo);
//		appHistory.setRtfState(rtfStateList.toString());
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("appNo", appHistory.getAppNo());
        map.put("rtfState_in", rtfStateList.toArray(new String[0]));
        map.put("_SORT_NAME", "createDate");
        map.put("_SORT_ORDER", "DESC");
        List<TmAppHistory> tmHistoryList = applyHistoryService.getAppHistroyByParam(map);
        if (tmHistoryList != null && tmHistoryList.size() > 0) {
            TmAppHistory hi1 = tmHistoryList.get(0);
            if (hi1 != null && hi1.getRtfState() != null) {
                return hi1.getOperatorId();
            }
        }

        return null;
    }


    /**
     * 判断所属人是否在筛选后的user集合中（此处不考虑操作员手中案件数量）
     *
     * @param
     * @param checkOperator
     * @return
     */
    private boolean isInUserList(List<SimpleUser> userList, String checkOperator) {
        if (CollectionUtils.sizeGtZero(userList)) {
            for (SimpleUser simpleUser : userList) {
                if (simpleUser.getUserNo().equals(checkOperator)) {
                    return true;
                }
            }
        }

        return false;
    }

    // 补件筛选出与录入时受理网点一致的补件员
    private List<SimpleUser> filterPathBoltUser(Map<String, List<SimpleUser>> users, TmAppMain tmAppMain) {
        String owningBranch = null;
        if (tmAppMain != null) {
            owningBranch = tmAppMain.getOwningBranch();// 得到申请件的受理网点
        }

        return users.get(owningBranch);
    }

    /**
     * 设置本行以及所有上级网点变量
     *
     * @param owningBranch
     * @param
     * @param
     */
    private List<String> setOwningBranchVariable(String owningBranch) {
        List<String> result = new ArrayList<String>();
        TmAclBranch curBran = cacheContext.getTmAclBranchByCode(owningBranch);
        if (curBran != null) {
            result.add(curBran.getBranchCode());
            TmAclBranch branch = null;
            String oBranch = curBran.getBranchCode();
            int i = 0;
            while (true) {
                branch = cacheContext.getTmAclBranchByCode(oBranch);
                if (branch == null) {
                    break;
                }
                oBranch = branch.getParentCode();
                result.add(oBranch);
                i++;
                if (i > 4) {
                    break;
                }
            }
        }
        return result;
    }


}
