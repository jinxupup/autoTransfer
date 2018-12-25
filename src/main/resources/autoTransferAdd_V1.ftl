<#include "/layout.ftl"/>
<@body>
    <@gotop/>
    <@panel head="分配管理">
        <@panelBody>
            <@form id="transForm" cols="4">
                <@row>
                    <@field label="任务名" >
                        <@dictSelect id="taskName" dicttype="TaskName" nullable="true" showcode="false"  name="query.taskName" />
                    </@field>
                    <@field label="平均派件数量">
                        <@input name="query.num" />
                    </@field>
                    <@field label="屏蔽的用户" >
                        <@multipleSelect  id="SDuser" name="SDuser" options=cas_('tableMap','getAllUser','') nullable="false" showfilter="true" showcode="true" />
                    </@field>
                    <@field label="是否开启">
                        <@dictSelect dicttype="Indicator" name="query.msgcategory" nullable="true"/>
                    </@field>
                </@row>
                <@row>
                    <@field label="优先派件顺序" field_ar="20" label_ar="8" input_ar="28">
                        <@textarea name="query.priorityType"  rows="1" valid={"notEmpty":"true"} value="V|K|N|C"/>
                    </@field>
                </@row>
                <@row>
                    <@toolbar align="center">
                        <@ajaxButton id = "subBtn" name=" 提交 " fa="send" url="autoTransfer/actAutoTransfer"
                        form_id="transForm"  style="margin-right: 15px;" success_url="autoTransfer/autoTransferPage"/>
                        <@backButton name="返回" fa="undo" style="margin-left:5px;margin-right:10px;"/>
                    </@toolbar>
                </@row>
            </@form>
        </br>
        <div style="margin-top: 15px; border: solid 1px #c7c1c1; width:100%;padding:2px 5px;color:#ff0000">
            <h4 style="color:#0cd913;">友情提示：</h4>
            <p>V-VIP客户;C-征信不良;N-新客户;K-快速审批, 优先派件顺序及格式例如: V|K|N|C (以" | "作为分隔)</p>
        </div>
        </@panelBody>
    </@panel>
<script>

</script>
</@body>
