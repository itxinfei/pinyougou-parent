package com.pinyougou.cart.controller;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbAddress;
import com.pinyougou.user.service.AddressService;

import entity.PageResult;
import entity.Result;
/**
 * controller
 * @author Administrator
 *
 */
@RestController
@RequestMapping("/address")
public class AddressController {

	private static final Logger logger = Logger.getLogger(AddressController.class);

	@Reference
	private AddressService addressService;
	
	/**
	 * 返回全部列表
	 * @return
	 */
	@RequestMapping("/findAll")
	public List<TbAddress> findAll(){			
		return addressService.findAll();
	}
	
	
	/**
	 * 返回全部列表
	 * @return
	 */
	@RequestMapping("/findPage")
	public PageResult  findPage(int page,int rows){			
		return addressService.findPage(page, rows);
	}
	
	/**
	 * 增加
	 * @param address
	 * @return
	 */
	@RequestMapping("/add")
	public Result add(@RequestBody TbAddress address){
		try {
			addressService.add(address);
			return new Result(true, "增加成功");
		} catch (Exception e) {
			logger.error("增加地址失败", e);
			return new Result(false, "增加失败");
		}
	}
	
	/**
	 * 修改
	 * @param address
	 * @return
	 */
	@RequestMapping("/update")
	public Result update(@RequestBody TbAddress address){
		try {
			addressService.update(address);
			return new Result(true, "修改成功");
		} catch (Exception e) {
			logger.error("修改地址失败", e);
			return new Result(false, "修改失败");
		}
	}	
	
	/**
	 * 获取实体
	 * @param id
	 * @return
	 */
	@RequestMapping("/findOne")
	public TbAddress findOne(Long id){
		return addressService.findOne(id);		
	}
	
	/**
	 * 批量删除
	 * @param ids
	 * @return
	 */
	@RequestMapping("/delete")
	public Result delete(Long [] ids){
		try {
			addressService.delete(ids);
			return new Result(true, "删除成功"); 
		} catch (Exception e) {
			logger.error("删除地址失败", e);
			return new Result(false, "删除失败");
		}
	}
	
		/**
	 * 查询+分页
	 * @param address
	 * @param page
	 * @param rows
	 * @return
	 */
	@RequestMapping("/search")
	public PageResult search(@RequestBody TbAddress address, int page, int rows  ){
		return addressService.findPage(address, page, rows);		
	}
	
	@RequestMapping("/findListByLoginUser")
	public List<TbAddress> findListByLoginUser(){
		//获取登陆用户
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		return addressService.findListByUserId(username);
	}

	/**
	 * 设置默认地址
	 */
	@RequestMapping("/setDefault")
	public Result setDefault(Long id){
		try {
			String username = SecurityContextHolder.getContext().getAuthentication().getName();
			addressService.setDefault(username, id);
			return new Result(true, "设置成功");
		} catch (Exception e) {
			logger.error("设置默认地址失败", e);
			return new Result(false, "设置失败");
		}
	}

}
