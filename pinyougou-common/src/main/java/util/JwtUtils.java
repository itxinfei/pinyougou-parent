package util;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * JWT工具类 - Token生成与验证
 *
 * @author Administrator
 */
@Component
public class JwtUtils {

    private static final Logger logger = Logger.getLogger(JwtUtils.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Token黑名单Redis Key前缀
     */
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * ✅ 从配置文件读取JWT签名密钥（替代硬编码）
     */
    @Value("${jwt.secret:pinyougou_secure_key_2026_change_in_production}")
    private String secret;

    /**
     * ✅ 从配置文件读取Access Token有效期（默认2小时）
     */
    @Value("${jwt.expiration:7200}")
    private long expiration;

    /**
     * ✅ 从配置文件读取Refresh Token有效期（默认7天）
     */
    @Value("${jwt.refreshExpiration:604800}")
    private long refreshExpiration;

    /**
     * 主题（Subject）
     */
    private static final String SUBJECT = "pinyougou_user";

    /**
     * 签发人（Issuer）
     */
    private static final String ISSUER = "pinyougou";

    /**
     * 生成Token
     *
     * @param username 用户名
     * @return JWT Token
     */
    public String generateToken(String username) {
        return generateToken(username, expiration);
    }

    /**
     * 生成Token（自定义有效期）
     *
     * @param username 用户名
     * @param expiration 有效期（秒）
     * @return JWT Token
     */
    public String generateToken(String username, long expiration) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expirationDate = new Date(now + expiration * 1000);

        JwtBuilder builder = Jwts.builder()
            .setSubject(SUBJECT)
            .setIssuer(ISSUER)
            .setIssuedAt(issuedAt)
            .setExpiration(expirationDate)
            .setAudience(username) // 观众（用户名）
            .signWith(SignatureAlgorithm.HS512, secret);

        return builder.compact();
    }

    /**
     * 生成Refresh Token
     *
     * @param username 用户名
     * @return Refresh Token
     */
    public String generateRefreshToken(String username) {
        return generateToken(username, refreshExpiration);
    }

    /**
     * 解析Token
     *
     * @param token JWT Token
     * @return Claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证Token是否有效
     * <p>
     * 验证流程：
     * 1. 解析Token（验证签名和格式）
     * 2. 检查是否过期（expiration < now）
     * 3. 检查签发人（issuer）
     * 4. 检查Token是否在黑名单中
     * <p>
     * 验证失败场景：
     * - Token格式错误或签名无效（解析失败）
     * - Token已过期
     * - 签发人不匹配
     * - Token被篡改
     * - Token在黑名单中（已登出）
     * <p>
     * 性能优化：
     * - Token验证无需查询数据库（无IO操作）
     * - 黑名单查询只需一次Redis查询
     * - 适合在高并发场景使用
     *
     * @param token JWT Token
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            // 1. 验证Token签名和格式
            Claims claims = parseToken(token);
            if (claims == null) {
                return false;
            }

            // 2. 检查是否过期
            Date expiration = claims.getExpiration();
            if (expiration == null || expiration.before(new Date())) {
                return false;
            }

            // 3. 检查签发人
            String issuer = claims.getIssuer();
            if (!ISSUER.equals(issuer)) {
                return false;
            }

            // 4. 检查Token是否在黑名单
            if (isTokenInBlacklist(token)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查Token是否在黑名单中
     * <p>
     * 黑名单机制：
     * - Key: token:blacklist:{token}
     * - Value: "1"（任意值，仅用于标识存在）
     * - 过期时间：Token剩余有效期
     * <p>
     * @param token JWT Token
     * @return true-在黑名单中，false-不在黑名单中
     */
    public boolean isTokenInBlacklist(String token) {
        try {
            String blacklistKey = BLACKLIST_PREFIX + token;
            return redisTemplate.hasKey(blacklistKey);
        } catch (Exception e) {
            logger.error("查询Token黑名单失败", e);
            return false;
        }
    }

    /**
     * 将Token加入黑名单
     * <p>
     * 加入黑名单场景：
     * - 用户主动登出
     * - 管理员强制登出（踢人）
     * - 密码修改后作废旧Token
     * <p>
     * @param token JWT Token
     * @return true-加入成功，false-加入失败
     */
    public boolean addTokenToBlacklist(String token) {
        try {
            // 1. 解析Token
            Claims claims = parseToken(token);
            if (claims == null) {
                return false;
            }

            // 2. 获取过期时间
            Date expiration = claims.getExpiration();
            if (expiration == null) {
                return false;
            }

            // 3. 计算剩余有效期（秒）
            long remainingTime = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            if (remainingTime <= 0) {
                // Token已过期，无需加入黑名单
                return false;
            }

            // 4. 加入黑名单，并设置过期时间
            String blacklistKey = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(blacklistKey, "1", remainingTime, TimeUnit.SECONDS);

            return true;
        } catch (Exception e) {
            logger.error("加入Token黑名单失败", e);
            return false;
        }
    }

    /**
     * 根据用户名删除Token
     *
     * @param username 用户名
     */
    public void deleteTokenByUsername(String username) {
        try {
            String tokenKey = "token:" + username;
            redisTemplate.delete(tokenKey);
        } catch (Exception e) {
            logger.error("删除Token失败: " + username, e);
        }
    }

    /**
     * 从Token中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return claims.getAudience();
    }

    /**
     * 从Token中获取签发时间
     *
     * @param token JWT Token
     * @return 签发时间
     */
    public Date getIssuedAtFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getIssuedAt() : null;
    }

    /**
     * 从Token中获取过期时间
     *
     * @param token JWT Token
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getExpiration() : null;
    }

    /**
     * 检查Token是否即将过期（剩余时间小于30分钟）
     *
     * @param token JWT Token
     * @return true-即将过期，false-未过期
     */
    public boolean isTokenExpiringSoon(String token) {
        Date expiration = getExpirationDateFromToken(token);
        if (expiration == null) {
            return true;
        }

        long remainingTime = expiration.getTime() - System.currentTimeMillis();
        return remainingTime < 30 * 60 * 1000; // 30分钟
    }

    /**
     * 刷新Token
     *
     * @param oldToken 旧Token
     * @return 新Token
     */
    public String refreshToken(String oldToken) {
        if (!validateToken(oldToken)) {
            return null;
        }

        String username = getUsernameFromToken(oldToken);
        return generateToken(username);
    }

    /**
     * 获取Token剩余有效时间（秒）
     *
     * @param token JWT Token
     * @return 剩余秒数
     */
    public long getRemainingSeconds(String token) {
        Date expiration = getExpirationDateFromToken(token);
        if (expiration == null) {
            return 0;
        }

        long remaining = expiration.getTime() - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000 : 0;
    }
}
