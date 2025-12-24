app.service('orderService',function($http){
	this.search=function(page,rows,searchEntity){
		return $http.post('../order/search.do?page='+page+"&rows="+rows, searchEntity);
	}
	
	this.findOne=function(id){
		return $http.get('../order/findOne.do?id='+id);
	}
	
	this.update=function(entity){
		return $http.post('../order/update.do',entity);
	}
	
	this.delete=function(ids){
		return $http.get('../order/delete.do?ids='+ids);
	}
});