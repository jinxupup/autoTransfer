<#include "/layout.ftl"/>
<@body>
    <@gotop/>
    <@panel head="分配管理">
        <@panelBody>
            <@form id="transForm" cols="3">
                <@row>
                    <@field label="任务名" >
                        <@dictSelect id="taskName" dicttype="TaskName" showcode="false"  nullable="true"  name="query.taskName" readonly="true" value="${(taskName)!}"/>
                    </@field>
                    <@field label="平均派件数量">
                        <@input name="query.num"  value="${(num)!}"/>
                    </@field>

                    <@field label="是否开启">
                        <@dictSelect dicttype="Indicator" name="query.msgcategory"  value="${(Indicator)!}" />
                    </@field>
                </@row>
                <@row>
                    <@field label="优先派件顺序">
                        <@input name="query.priorityType" value="${(priorityType)!}"/>
                    </@field>
                    <@field label="屏蔽的用户" field_ar="24" label_ar="7" input_ar="29">
                        <@multipleSelect id="SDuser" name="SDuser" options=cas_('tableMap','getAllUser','')
                        nullable="true" showfilter="true" showcode="true" value="${(SDusers)!}"/>
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
        </@panelBody>
    </@panel>
<script>

<#--    var autoTransfer = function(){
        $.ajax({
            url:"${base}/autoTransfer/actAutoTransfer",
            type:"post",
            dataType : "json",
            data:$("#transForm").serialize(),
            success:function(ref){
                if(ref.s){
                    alert(ref.msg);
                    location.reload(true);
                }else{
                    alert(ref.msg);
                    location.reload(true)
                }
            }
        });
    }  -->
</script>
</@body>
