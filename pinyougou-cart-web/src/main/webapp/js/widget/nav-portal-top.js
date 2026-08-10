
/*购物车效果*/
function hovercar() {
	$("#shopcar").mouseover(function() {
		$("#shopcarlist").show("fast");
	});
	$("#shopcarlist").mouseleave(function() {
		$("#shopcarlist").hide("fast");
	});
}

/*自动完成*/
$(function(){

	// ✅ 从后端API获取搜索建议标签，替换硬编码数据
	$.ajax({
		url: 'search/findKeywords.do',
		type: 'GET',
		success: function(data) {
			if (data && data.length > 0) {
				$("#autocomplete").autocomplete({
					source: data
				});
			} else {
				// 后端无数据时使用默认标签
				$("#autocomplete").autocomplete({
					source: ["手机", "电脑", "平板", "耳机", "笔记本", "显示器", "键盘", "鼠标"]
				});
			}
		},
		error: function() {
			// 请求失败时使用默认标签
			$("#autocomplete").autocomplete({
				source: ["手机", "电脑", "平板", "耳机", "笔记本", "显示器", "键盘", "鼠标"]
			});
		}
	});

});
