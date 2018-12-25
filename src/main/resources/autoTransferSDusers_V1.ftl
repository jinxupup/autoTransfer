<#include "/layout.ftl"/>
<@body>
    <@panel >
        <@panelBody>
            <@pureTable style="text-align: center;">
            <#--<caption>姓名:${(applyName)!} 证件号码:${(applyIdNo)!}</caption>-->
            <tr >
                <th width="50%" >用户名</th>
                <th width="50%" >用户姓名</th>
            </tr>

            <#if users??>
                <#list users as user>
                    <tr>
                        <td>${(user.userNo)!}</td>
                        <td>${(user.userName)!}</td>
                    </tr>
                </#list>
            </#if>
            </@pureTable>
        </@panelBody>
    </@panel>
<script>
</script>
</@body>



