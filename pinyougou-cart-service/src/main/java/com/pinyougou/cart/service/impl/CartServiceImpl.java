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
	
	/**
	 * 添加商品到购物车
	 * <p>
	 * 业务逻辑：
	 * 1. 参数校验（商品ID、数量合法性）
	 * 2. 查询商品信息（验证商品存在性和状态）
	 * 3. 库存校验（防止超卖）
	 * 4. 根据商家ID查找购物车（一个商家对应一个Cart）
	 * 5. 如果商品已在购物车中，则增加数量；否则新增商品
	 * 6. 更新商品总价（price × num）
	 * <p>
	 * 购物车数据结构：
	 * List<Cart>
	 *   ├─ Cart 1 (商家A)
	 *   │   ├─ sellerId: "1001"
	 *   │   ├─ sellerName: "联想旗舰店"
	 *   │   └─ orderItemList: [OrderItem, OrderItem, ...]
	 *   ├─ Cart 2 (商家B)
	 *   │   ├─ sellerId: "1002"
	 *   │   ├─ sellerName: "戴尔旗舰店"
	 *   │   └─ orderItemList: [OrderItem, ...]
	 * <p>
	 * 数量限制：
	 * - 单次添加数量：1-999件
	 * - 累计数量限制：同一商品最多999件（防止超卖）
	 * - 总购物车商品数：未限制（⚠️ 建议增加限制，防止内存溢出）
	 * <p>
	 * 异常处理：
	 * - ValidationException: 参数错误（ID为空、数量非法）
	 * - ResourceNotFoundException: 商品不存在
	 * - InsufficientStockException: 库存不足
	 * <p>
	 * 线程安全：
	 * - ⚠️ 当前实现非线程安全
	 * - 高并发场景下可能出现数据不一致
	 * - 建议：使用 synchronized 或分布式锁（Redis Lock）
	 *
	 * @param cartList 购物车列表（已有商品）
	 * @param itemId 商品SKU ID
	 * @param num 购买数量
	 * @return 更新后的购物车列表
	 */
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
				orderItem.setTotalFee(orderItem.getPrice().multiply(new BigDecimal(orderItem.getNum())));
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
		orderItem.setTotalFee(item.getPrice().multiply(new BigDecimal(num)));
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

	/**
	 * 合并购物车（用户登录时调用）
	 * <p>
	 * 业务场景：
	 * - 用户未登录时添加了商品到本地购物车（购物车1）
	 * - 用户登录时，登录前购物车已有商品（购物车2）
	 * - 需要将两个购物车合并到登录账号的购物车中
	 * <p>
	 * 合并策略：
	 * 1. 遍历购物车2的每个购物车和商品
	 * 2. 调用 addGoodsToCartList() 将商品添加到购物车1
	 * 3. 如果商品已存在，则增加数量（最多999件）
	 * 4. 如果商品不存在，则新增商品到对应商家的购物车
	 * <p>
	 * 合并规则：
	 * - 按商家分组：不同商家的商品分别存放
	 * - 相同商品合并：同一SKU的数量累加
	 * - 数量限制：合并后总数不超过999件（防止超卖）
	 * - 库存校验：合并时再次验证库存（addGoodsToCartList会检查）
	 * <p>
	 * 注意事项：
	 * - ⚠️ 当前实现有Bug：cartList1.addAll(cartList2) 被注释掉了
	 * - ⚠️ 性能问题：嵌套循环时间复杂度 O(n*m)，购物车商品多时性能差
	 * - ⚠️ 没有处理商品下架、库存变化的情况
	 * <p>
	 * 改进建议：
	 * - 使用 HashMap 优化查找性能（O(n+m)）
	 * - 添加商品状态验证（过滤已下架商品）
	 * - 添加库存变化提醒（库存不足时提示用户）
	 * - 合并后排序：按商家、按添加时间
	 *
	 * @param cartList1 登录用户的购物车（主购物车）
	 * @param cartList2 未登录时的本地购物车（待合并）
	 * @return 合并后的购物车列表
	 */
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
