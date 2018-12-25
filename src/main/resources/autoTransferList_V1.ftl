<#include "/layout.ftl"/>
<@body>
    <@panel head="自动分配管理">
        <@panelBody  style="text-align: center;">
            <div style="float:right"><@href href="autoTransfer/autoTransferAdd" name=" 新增分配任务" fa=""/>&nbsp;&nbsp;</div>
            <@table id="dataTable" url="autoTransfer/autoTransferList" page_size="25">
                <@th title="任务名" field="tabName" />
                <@th title="平均派件数量" field="formName" />
                <@th title="是否开启" field="remark" />
                <@th title="优先派件顺序" field="itemName" />
                <@th title="屏蔽用户组" render="true">
                    <@button click="update('{{row.tabName}}')" name="查看屏蔽用户详情" style="margin-right:15px"/>
                </@th>
                <@th title="操作" render="true" >
                    <@href href="autoTransfer/autoTransferupdate?tabName={{row.tabName}}" name="修改" />
                    &nbsp;&nbsp;
                    <@ajaxButton  confirm="确定删除此记录？" sense="danger"  name="删除" url="autoTransfer/autoTransferDelete?tabName={{row.tabName}}" success_url="autoTransfer/autoTransferPage" />
                </@th>
            </@table>

        </@panelBody>
    </@panel>
    <#include "dialogJs.ftl"/>
<script>
/*    $("#update").on('click',function(){
    d = top.dialog({
        width:600,
        title: '屏蔽用户组',
        height:600,
        url:"autoTransfer/querySDusers?tabName={{row.tabName}}"
        oniframeload:function(){},
        onclose:function () {},
    });
    d.showModal();
    });*/

    var update = function(tabName){
        var url = "${base}/autoTransfer/querySDusers?tabName="+tabName;
        dialogInfo('【屏蔽用户组】',400,300,url);
    }
</script>
</@body>
