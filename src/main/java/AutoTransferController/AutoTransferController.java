package com.jjb.ecms.app.controller.param;

import com.jjb.acl.biz.dao.TmAclUserDao;
import com.jjb.acl.infrastructure.TmAclUser;
import com.jjb.ecms.app.controller.utils.PagePathConstant;
import com.jjb.ecms.biz.cache.CacheContext;
import com.jjb.ecms.biz.service.param.SysParamService;
import com.jjb.ecms.infrastructure.TmDitDic;
import com.jjb.ecms.util.StringUtils;
import com.jjb.unicorn.facility.model.Json;
import com.jjb.unicorn.facility.model.Page;
import com.jjb.unicorn.facility.model.Query;
import com.jjb.unicorn.web.controller.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName AutoTransferController
 * @Description TODO 案件自动分配配置
 * @Author smh
 * Date 2018/12/18 11:14
 * Version 1.0
 */

@Controller
@RequestMapping("/autoTransfer")
public class AutoTransferController extends BaseController {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private static final String DIT_DIC_AUTO_TRANS_SDUER = "autoTransferSDuser";
    private static final String DIT_DIC_AUTO_TRANS = "autoTransfer";

    @Autowired
    private CacheContext cacheContext;
    @Autowired
    private SysParamService sysParamService;
    @Autowired
    private TmAclUserDao tmAclUserDao;

    /**
     * @Author smh
     * @Description TODO 跳转到配置页面
     * @Date 2018/12/18 14:27
     */
    @RequestMapping("/autoTransferPage")
    public String autoTransferPage() {
        return cacheContext.getPageConfig(PagePathConstant.autoTransferPage);
    }

    /**
     * @Author smh
     * @Description TODO 提交自动分配 先删除再添加
     * @Date 2018/12/18 14:28
     */
    @Transactional
    @ResponseBody
    @RequestMapping("/actAutoTransfer")
    public Json autoTransfer() {
        Json json = Json.newSuccess();
        Query query = null;
        try {
            String SDuser = "";
            String[] SDusers = getParas("SDuser[]");
            if (SDusers == null) {
                SDuser = getPara("SDuser");
            }
            query = getQuery();
            String taskName = String.valueOf(query.get("taskName"));
            //删除原来的配置以及屏蔽组  根据dic_type以及tab_name
            List<TmDitDic> tmDitDicQureyList = new ArrayList<TmDitDic>();
            TmDitDic tmDitDicDelete = new TmDitDic();
            tmDitDicDelete.setTabName(taskName);
            tmDitDicDelete.setDicType(DIT_DIC_AUTO_TRANS_SDUER);
            //屏蔽组
            List<TmDitDic> tmDitDicSDuerList = sysParamService.getTmDitDic(tmDitDicDelete);
            tmDitDicDelete.setTabName(taskName);
            tmDitDicDelete.setDicType(DIT_DIC_AUTO_TRANS);
            //配置
            List<TmDitDic> tmDitDicConfig = sysParamService.getTmDitDic(tmDitDicDelete);
            tmDitDicQureyList.addAll(tmDitDicSDuerList);
            tmDitDicQureyList.addAll(tmDitDicConfig);
            if (tmDitDicQureyList != null && tmDitDicQureyList.size() != 0) {
                for (TmDitDic tmDitDicDle : tmDitDicQureyList) {
                    Integer id = tmDitDicDle.getId();
                    if (StringUtils.isNotEmpty(String.valueOf(id))) {
                        sysParamService.deleteTmDitDic(id);
                    }
                }
            }
            //存入屏蔽的用户(单个用户)
            if (StringUtils.isNotEmpty(SDuser)) {
                TmDitDic tmDitDicSDuers = new TmDitDic();
                tmDitDicSDuers.setDicType(DIT_DIC_AUTO_TRANS_SDUER);
                tmDitDicSDuers.setTabName(taskName);
                tmDitDicSDuers.setRemark(SDuser);
                tmDitDicSDuers.setTypeName("自动分配屏蔽用户组");
                sysParamService.saveTmDitDic(tmDitDicSDuers);
            }
            //存入屏蔽的用户(多个用户)
            if (SDusers != null && SDusers.length != 0) {
                for (int i = 0; i < SDusers.length; i++) {
                    TmDitDic tmDitDicSDuers = new TmDitDic();
                    tmDitDicSDuers.setDicType(DIT_DIC_AUTO_TRANS_SDUER);
                    tmDitDicSDuers.setTabName(taskName);
                    tmDitDicSDuers.setRemark(SDusers[i]);
                    tmDitDicSDuers.setTypeName("自动分配屏蔽用户组");
                    sysParamService.saveTmDitDic(tmDitDicSDuers);
                }
            }
            //存入分配的配置信息
            TmDitDic tmDitDicInfo = new TmDitDic();
            tmDitDicInfo.setDicType(DIT_DIC_AUTO_TRANS);
            tmDitDicInfo.setTabName(taskName);
            tmDitDicInfo.setFormName(String.valueOf(query.get("num")));
            tmDitDicInfo.setRemark(String.valueOf(query.get("msgcategory")));
            tmDitDicInfo.setItemName(String.valueOf(query.get("priorityType")));
            tmDitDicInfo.setTypeName("自动分配类型");
            sysParamService.saveTmDitDic(tmDitDicInfo);
            // throw new Exception();
        } catch (Exception e) {
            logger.error("自动分配配置异常", e);
            json.setFail("自动分配配置异常");
            json.setS(false);
        }
        return json;
    }


    /**
     * @Author smh
     * @Description TODO  展示自动分配任务列表
     * @Date 2018/12/20 11:44
     */
    @ResponseBody
    @RequestMapping("/autoTransferList")
    public Page<TmDitDic> list() {
        Page<TmDitDic> page = getPage(TmDitDic.class);
        page = sysParamService.getListPage(page);
        logger.info("查询的记录条数是：" + page.getRows().size());
        return page;
    }

    /**
     * @Author smh
     * @Description TODO 自动分配新增配置
     * @Date 2018/12/20 15:01
     */
    @RequestMapping("/autoTransferAdd")
    public String autoTransferAddPage() {
        return cacheContext.getPageConfig(PagePathConstant.autoTransferAddPage);
    }

    /**
     * @Author smh
     * @Description TODO  查看屏蔽用户组
     * @Date 2018/12/20 15:45
     */
    @RequestMapping("/querySDusers")
    public String querySDusers() {
        String tabName = getPara("tabName");
        TmDitDic ditDic = new TmDitDic();
        ditDic.setTabName(tabName);
        ditDic.setDicType(DIT_DIC_AUTO_TRANS_SDUER);
        List<TmDitDic> ditDics = sysParamService.getTmDitDic(ditDic);
        List<TmAclUser> users = new ArrayList<TmAclUser>();
        for (int i = 0; i < ditDics.size(); i++) {
            TmDitDic tmDitDic = ditDics.get(i);
            if (tmDitDic != null) {
                String sdUser = tmDitDic.getRemark();
                TmAclUser tmAclUser = new TmAclUser();
                tmAclUser = tmAclUserDao.getUserByUserNo(sdUser);
                users.add(tmAclUser);
            }
        }
        setAttr("users", users);
        return cacheContext.getPageConfig(PagePathConstant.autoTransferSDusers);
    }

    /**
     * @Author smh
     * @Description TODO  删除配置信息(删除对应的配置及屏蔽用户组)
     * @Date 2018/12/20 16:52
     */
    @ResponseBody
    @RequestMapping("/autoTransferDelete")
    public Json autoTransferDelete() {
        Json json = Json.newSuccess();
        try {
            String taskName = getPara("tabName");
            List<TmDitDic> tmDitDicQureyList = new ArrayList<TmDitDic>();
            TmDitDic tmDitDicDelete = new TmDitDic();
            //屏蔽组
            tmDitDicDelete.setTabName(taskName);
            tmDitDicDelete.setDicType(DIT_DIC_AUTO_TRANS_SDUER);
            List<TmDitDic> tmDitDicSDuerList = sysParamService.getTmDitDic(tmDitDicDelete);
            //配置
            tmDitDicDelete.setTabName(taskName);
            tmDitDicDelete.setDicType(DIT_DIC_AUTO_TRANS);
            List<TmDitDic> tmDitDicConfig = sysParamService.getTmDitDic(tmDitDicDelete);
            tmDitDicQureyList.addAll(tmDitDicSDuerList);
            tmDitDicQureyList.addAll(tmDitDicConfig);
            if (tmDitDicQureyList != null && tmDitDicQureyList.size() != 0) {
                for (TmDitDic tmDitDicDle : tmDitDicQureyList) {
                    Integer id = tmDitDicDle.getId();
                    if (StringUtils.isNotEmpty(String.valueOf(id))) {
                        sysParamService.deleteTmDitDic(id);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("删除失败", e);
            json.setFail("删除失败");
            json.setS(false);
        }
        return json;
    }

    /**
     * @Author smh
     * @Description TODO  修改页面
     * @Date 2018/12/20 17:21
     */
    @RequestMapping("/autoTransferupdate")
    public String autoTransferupdate() {
        String tabName = getPara("tabName");
        TmDitDic ditDic = new TmDitDic();
        ditDic.setTabName(tabName);
        ditDic.setDicType(DIT_DIC_AUTO_TRANS);
        //配置信息
        List<TmDitDic> tmDitDicConfig = sysParamService.getTmDitDic(ditDic);
        String taskName="";
        String num="";
        String Indicator="";
        String priorityType="";
        if (tmDitDicConfig != null && tmDitDicConfig.size() != 0) {
            for (TmDitDic tmDitDic : tmDitDicConfig) {
                 taskName = tmDitDic.getTabName();
                 num=tmDitDic.getFormName();
                 Indicator=tmDitDic.getRemark();
                 priorityType=tmDitDic.getItemName();
            }
        }
        setAttr("taskName", taskName);
        setAttr("num", num);
        setAttr("Indicator", Indicator);
        setAttr("priorityType", priorityType);
        //屏蔽组
        StringBuilder SDusers = new StringBuilder();
        TmDitDic tmDitDic = new TmDitDic();
        tmDitDic.setTabName(tabName);
        tmDitDic.setDicType(DIT_DIC_AUTO_TRANS_SDUER);
        List<TmDitDic> tmDitDicQureyList = sysParamService.getTmDitDic(tmDitDic);
        List<String> SDuserList = new ArrayList<String>();
        for (TmDitDic dic:tmDitDicQureyList){
            String SDuser=dic.getRemark();
            SDusers = SDusers.append(SDuser+",");
        }
        setAttr("SDusers", SDusers.toString());
        return cacheContext.getPageConfig(PagePathConstant.autoTransferUpdate);
    }


}
