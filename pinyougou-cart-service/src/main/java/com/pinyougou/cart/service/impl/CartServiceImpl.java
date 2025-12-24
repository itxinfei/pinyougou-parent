package com.pinyougou.cart.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.exception.InsufficientStockException;
import com.pinyougou.exception.ResourceNotFoundException;
import com.pinyougou.exception.ValidationException;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojo.group.Cart;
@Service
public class CartServiceImpl implements CartService {

	private static final Logger logger = Logger.getLogger(CartServiceImpl.class);

	@Autowired
	private TbItemMapper itemMapper;
	
	@Override
	public List<Cart> addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num) {
		
		if(itemId==null||itemId<=0){
			throw new ValidationException("商品ID不能为空且必须大于0");
		}
		if(num==null||num<=0){
			throw new ValidationException("商品数量不能为空且必须大于0");
		}
		if(num>999){
			throw new ValidationException("单次购买数量不能超过999件");
		}
		
		TbItem item = itemMapper.selectByPrimaryKey(itemId);
		if(item==null){
			throw new ResourceNotFoundException("商品不存在");
		}
		if(!item.getStatus().equals("1")){
			throw new ValidationException("商品状态不合法");
		}
		if(item.getStockCount()==null||item.getStockCount()<=0){
			throw new InsufficientStockException("商品库存不足");
		}
		if(num>item.getStockCount()){
			throw new InsufficientStockException("购买数量超过库存，当前库存："+item.getStockCount());
		}
		
		String sellerId = item.getSellerId();
		
		Cart cart = searchCartBySellerId(cartList,sellerId);
		
		if(cart==null){
			
			cart=new Cart();
			cart.setSellerId(sellerId);
			cart.setSellerName(item.getSeller());			
			List<TbOrderItem> orderItemList=new ArrayList();
			TbOrderItem orderItem = createOrderItem(item,num);			
			orderItemList.add(orderItem);			
			cart.setOrderItemList(orderItemList);
			
			cartList.add(cart);
			
		}else{
			TbOrderItem orderItem = searchOrderItemByItemId(cart.getOrderItemList(),itemId);
			if(orderItem==null){
				orderItem=createOrderItem(item,num);
				cart.getOrderItemList().add(orderItem);				
				
			}else{
				int newNum=orderItem.getNum()+num;
				if(newNum>999){
					throw new ValidationException("单次购买数量不能超过999件");
				}
				if(newNum>item.getStockCount()){
					throw new InsufficientStockException("购买数量超过库存，当前库存："+item.getStockCount());
				}
				orderItem.setNum(newNum);
				orderItem.setTotalFee( new BigDecimal(orderItem.getPrice().doubleValue()*orderItem.getNum() ) );
				if(orderItem.getNum()<=0){
					cart.getOrderItemList().remove(orderItem);					
				}
				if(cart.getOrderItemList().size()==0){
					cartList.remove(cart);
				}				
			}
			
		}
		
		return cartList;
	}
	
	/**
	 * 根据商家ID在购物车列表中查询购物车对象
	 * @param cartList
	 * @param sellerId
	 * @return
	 */
	private Cart searchCartBySellerId(List<Cart> cartList,String sellerId){
		for(Cart cart:cartList){
			if(cart.getSellerId().equals(sellerId)){
				return cart;
			}
		}
		return null;		
	}
	
	/**
	 * 根据skuID在购物车明细列表中查询购物车明细对象
	 * @param orderItemList
	 * @param itemId
	 * @return
	 */
	public TbOrderItem searchOrderItemByItemId(List<TbOrderItem> orderItemList,Long itemId){
		for(TbOrderItem orderItem:orderItemList){
			if(orderItem.getItemId().longValue()==itemId.longValue()){
				return orderItem;
			}			
		}
		return null;
	}
	
	/**
	 * 创建购物车明细对象
	 * @param item
	 * @param num
	 * @return
	 */
	private TbOrderItem createOrderItem(TbItem item,Integer num){
		//创建新的购物车明细对象
		TbOrderItem orderItem=new TbOrderItem();
		orderItem.setGoodsId(item.getGoodsId());
		orderItem.setItemId(item.getId());
		orderItem.setNum(num);
		orderItem.setPicPath(item.getImage());
		orderItem.setPrice(item.getPrice());
		orderItem.setSellerId(item.getSellerId());
		orderItem.setTitle(item.getTitle());
		orderItem.setTotalFee(  new BigDecimal(item.getPrice().doubleValue()*num) );
		return orderItem;
	}
	
	@Autowired
	private RedisTemplate redisTemplate;

	@Override
	public List<Cart> findCartListFromRedis(String username) {
		logger.info("从redis中提取购物车" + username);
		List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(username);
		if(cartList==null){
			cartList=new ArrayList();
		}		
		return cartList;
	}

	@Override
	public void saveCartListToRedis(String username, List<Cart> cartList) {
		logger.info("向redis中存入购物车" + username);
		redisTemplate.boundHashOps("cartList").put(username, cartList);
		
	}

	@Override
	public List<Cart> mergeCartList(List<Cart> cartList1, List<Cart> cartList2) {
		// cartList1.addAll(cartList2);  不能简单合并 		
		for(Cart cart:cartList2){
			for( TbOrderItem orderItem :cart.getOrderItemList() ){
				cartList1=addGoodsToCartList(cartList1,orderItem.getItemId(),orderItem.getNum());
			}
		}
		return cartList1;		
	}

}
