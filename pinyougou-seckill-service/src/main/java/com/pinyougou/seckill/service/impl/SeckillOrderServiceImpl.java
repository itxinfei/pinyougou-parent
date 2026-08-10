package com.pinyougou.seckill.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbSeckillGoodsMapper;
import com.pinyougou.mapper.TbSeckillOrderMapper;
import com.pinyougou.pojo.TbSeckillGoods;
import com.pinyougou.pojo.TbSeckillOrder;
import com.pinyougou.pojo.TbSeckillOrderExample;
import com.pinyougou.pojo.TbSeckillOrderExample.Criteria;
import com.pinyougou.seckill.service.SeckillOrderService;
import entity.PageResult;
import com.pinyougou.exception.InsufficientStockException;
import com.pinyougou.exception.ResourceNotFoundException;
import com.pinyougou.exception.ValidationException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import util.IdWorker;

import java.util.Date;
import java.util.List;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {

    private static final Logger logger = Logger.getLogger(SeckillOrderServiceImpl.class);

    @Autowired
    private TbSeckillOrderMapper seckillOrderMapper;

    /**
     * 查询全部
     */
    @Override
    public List<TbSeckillOrder> findAll() {
        return seckillOrderMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbSeckillOrder> page = (Page<TbSeckillOrder>) seckillOrderMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 增加
     */
    @Override
    @Transactional
    public void add(TbSeckillOrder seckillOrder) {
        seckillOrderMapper.insert(seckillOrder);
    }


    /**
     * 修改
     */
    @Override
    @Transactional
    public void update(TbSeckillOrder seckillOrder) {
        seckillOrderMapper.updateByPrimaryKey(seckillOrder);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbSeckillOrder findOne(Long id) {
        return seckillOrderMapper.selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     */
    @Override
    @Transactional
    public void delete(Long[] ids) {
        for (Long id : ids) {
            seckillOrderMapper.deleteByPrimaryKey(id);
        }
    }

    /**
     * @param seckillOrder
     * @param pageNum      当前页 码
     * @param pageSize     每页记录数
     * @return
     */
    @Override
    public PageResult findPage(TbSeckillOrder seckillOrder, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbSeckillOrderExample example = new TbSeckillOrderExample();
        Criteria criteria = example.createCriteria();

        if (seckillOrder != null) {
            if (seckillOrder.getUserId() != null && seckillOrder.getUserId().length() > 0) {
                criteria.andUserIdLike("%" + seckillOrder.getUserId() + "%");
            }
            if (seckillOrder.getSellerId() != null && seckillOrder.getSellerId().length() > 0) {
                criteria.andSellerIdLike("%" + seckillOrder.getSellerId() + "%");
            }
            if (seckillOrder.getStatus() != null && seckillOrder.getStatus().length() > 0) {
                criteria.andStatusLike("%" + seckillOrder.getStatus() + "%");
            }
            if (seckillOrder.getReceiverAddress() != null && seckillOrder.getReceiverAddress().length() > 0) {
                criteria.andReceiverAddressLike("%" + seckillOrder.getReceiverAddress() + "%");
            }
            if (seckillOrder.getReceiverMobile() != null && seckillOrder.getReceiverMobile().length() > 0) {
                criteria.andReceiverMobileLike("%" + seckillOrder.getReceiverMobile() + "%");
            }
            if (seckillOrder.getReceiver() != null && seckillOrder.getReceiver().length() > 0) {
                criteria.andReceiverLike("%" + seckillOrder.getReceiver() + "%");
            }
            if (seckillOrder.getTransactionId() != null && seckillOrder.getTransactionId().length() > 0) {
                criteria.andTransactionIdLike("%" + seckillOrder.getTransactionId() + "%");
            }
        }
        Page<TbSeckillOrder> page = (Page<TbSeckillOrder>) seckillOrderMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private TbSeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private IdWorker idWorker;

    /**
     * @param seckillId
     * @param userId
     */
    @Override
    public void submitOrder(Long seckillId, String userId) {
        if(seckillId==null||seckillId<=0){
            throw new ValidationException("秒杀商品ID不能为空且必须大于0");
        }
        if(userId==null||userId.trim().isEmpty()){
            throw new ValidationException("用户ID不能为空");
        }

        TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps("seckillGoods").get(seckillId);
        if (seckillGoods == null) {
            throw new ResourceNotFoundException("秒杀商品不存在，商品ID："+seckillId);
        }

        // ✅ 修复：检查重复秒杀 + 扣减库存合并为一个Lua脚本原子执行
        // 避免先扣库存后发现重复导致库存回滚的竞态窗口
        String seckillUserKey = "seckill:user:" + seckillId;
        String luaScript =
                "local userKey = KEYS[2] " +
                "local userId = ARGV[2] " +
                "if redis.call('SISMEMBER', userKey, userId) == 1 then return -2 end " +
                "local stock = redis.call('HGET', KEYS[1], ARGV[1]) " +
                "if stock == false or tonumber(stock) <= 0 then return -1 end " +
                "local newStock = redis.call('HINCRBY', KEYS[1], ARGV[1], -1) " +
                "redis.call('SADD', userKey, userId) " +
                "return newStock";
        Long remainingStock = (Long) redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
                java.util.Arrays.asList("seckillGoods", seckillUserKey),
                String.valueOf(seckillId), userId);
        if (remainingStock == null || remainingStock == -1) {
            throw new InsufficientStockException("秒杀商品已经被抢光");
        }
        if (remainingStock == -2) {
            throw new ValidationException("您已经秒杀过该商品，请勿重复秒杀");
        }

        // 扣减成功，更新本地对象
        seckillGoods.setStockCount(remainingStock.intValue());
        if (remainingStock == 0) {
            seckillGoodsMapper.updateByPrimaryKey(seckillGoods);
            redisTemplate.boundHashOps("seckillGoods").delete(seckillId);
            logger.info("商品同步到数据库...");
        }

        // 设置秒杀用户记录过期时间（秒杀活动结束后7天自动清理）
        redisTemplate.boundSetOps(seckillUserKey).expire(7, java.util.concurrent.TimeUnit.DAYS);

        TbSeckillOrder seckillOrder = new TbSeckillOrder();
        seckillOrder.setId(idWorker.nextId());
        seckillOrder.setSeckillId(seckillId);
        seckillOrder.setMoney(seckillGoods.getCostPrice());
        seckillOrder.setUserId(userId);
        seckillOrder.setSellerId(seckillGoods.getSellerId());
        seckillOrder.setCreateTime(new Date());
        seckillOrder.setStatus("0");
        redisTemplate.boundHashOps("seckillOrder").put(userId, seckillOrder);
        // 设置秒杀订单过期时间（30分钟内未支付自动取消）
        redisTemplate.boundHashOps("seckillOrder").expire(30, java.util.concurrent.TimeUnit.MINUTES);
        logger.info("保存订单成功(redis)，订单ID：" + seckillOrder.getId());
    }

    /**
     * @param userId
     * @return
     */
    @Override
    public TbSeckillOrder searchOrderFromRedisByUserId(String userId) {
        return (TbSeckillOrder) redisTemplate.boundHashOps("seckillOrder").get(userId);
    }

    /**
     * @param userId
     * @param orderId
     * @param transactionId
     */
    @Override
    public void saveOrderFromRedisToDb(String userId, Long orderId, String transactionId) {
        if(userId==null||userId.trim().isEmpty()){
            throw new ValidationException("用户ID不能为空");
        }
        if(orderId==null||orderId<=0){
            throw new ValidationException("订单ID不能为空且必须大于0");
        }
        if(transactionId==null||transactionId.trim().isEmpty()){
            throw new ValidationException("交易ID不能为空");
        }

        TbSeckillOrder seckillOrder = searchOrderFromRedisByUserId(userId);
        if (seckillOrder == null) {
            throw new ResourceNotFoundException("不存在订单，用户ID："+userId);
        }
        if (!seckillOrder.getId().equals(orderId)) {
            throw new ValidationException("订单号不符，期望订单ID："+orderId+"，实际订单ID："+seckillOrder.getId());
        }
        seckillOrder.setPayTime(new Date());
        seckillOrder.setStatus("1");
        seckillOrder.setTransactionId(transactionId);
        seckillOrderMapper.insert(seckillOrder);
        redisTemplate.boundHashOps("seckillOrder").delete(userId);

    }

    /**
     * @param userId
     * @param orderId
     */
    @Override
    public void deleteOrderFromRedis(String userId, Long orderId) {
        //1.查询出缓存中的订单
        TbSeckillOrder seckillOrder = searchOrderFromRedisByUserId(userId);
        if (seckillOrder != null) {
            //2.删除缓存中的订单
            redisTemplate.boundHashOps("seckillOrder").delete(userId);
            //3.库存回退 - 使用Lua脚本保证原子性
            String luaScript = "local stock = redis.call('HGET', KEYS[1], ARGV[1]) " +
                    "if stock == false then " +
                    "  redis.call('HSET', KEYS[1], ARGV[1], 1) " +
                    "  return 1 " +
                    "end " +
                    "local newStock = redis.call('HINCRBY', KEYS[1], ARGV[1], 1) return newStock";
            redisTemplate.execute(
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
                    java.util.Collections.singletonList("seckillGoods"),
                    String.valueOf(seckillOrder.getSeckillId()));
            logger.info("订单取消：" + orderId);
        }
    }
}
