<script type="text/javascript">
	<#--弹出框共用js-->
	function dialogInfo(title,width,height,url){
		a = top.dialog({
			title: title,
			width: width,
			height: height,
			url: url,
			oniframeload:function(){},
			button:
			[
			    {
			        value: '确定',
			        callback: function () {
			       		this.close();
			            return false;
			        },
			        autofocus: true
			    }
			]
		});
		a.showModal();
	}
</script>