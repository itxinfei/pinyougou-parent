package util;

import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * JWT工具类 - Token生成与验证
 * <p>
 * JWT结构说明：
 * - Header: 算法类型（HS512）
 * - Payload: 用户信息（username/issuedAt/expiration）
 * - Signature: 签名（防止篡改）
 * <p>
 * Token有效期策略：
 * - Access Token: 2小时（短期，安全性高）
 * - Refresh Token: 7天（长期，用于刷新Access Token）
 * <p>
 * 认证流程：
 * 1. 用户登录成功后生成Access Token和Refresh Token
 * 2. 客户端在请求头携带：Authorization: Bearer <token>
 * 3. 服务端验证Token有效性（签名、过期时间）
 * 4. Token过期前30分钟，客户端使用Refresh Token刷新
 * <p>
 * Token失效机制：
 * - 自然过期：Token到达expiration时间自动失效
 * - 主动登出：将Token从Redis删除或加入黑名单
 * - 密码修改：删除该用户所有Token
 * - 封号处理：将用户ID加入黑名单
 * <p>
 * ⚠️ 安全注意事项：
 * 1. 密钥（SECRET）必须足够复杂（至少32位）
 * 2. 密钥不能硬编码，应从配置中心读取
 * 3. 密钥必须定期更换（建议90天）
 * 4. Token必须使用HTTPS传输
 * 5. 不要在Token中存放敏感信息（密码、身份证号等）
 * 6. 必须实现Token黑名单机制（防止已登出Token继续使用）
 * <p>
 * 🔴 当前实现缺陷：
 * 1. 密钥硬编码在代码中（安全风险）
 * 2. 未实现Refresh Token功能（generateRefreshToken已实现但未使用）
 * 3. 未实现Token黑名单查询方法
 * 4. 未实现Token刷新逻辑（refreshToken方法未完善）
 * <p>
 * 改进建议：
 * - 密钥配置：@Value("${jwt.secret}") 从配置文件读取
 * - 黑名单方法：addToBlacklist() / isInBlacklist()
 * - Token刷新：validateRefreshToken() + generateNewAccessToken()
 * - IP绑定：将登录IP存入Token并验证
 * - 设备绑定：支持多设备登录管理
 *
 * @author Administrator
 */
@Component
public class JwtUtils {

    /**
     * 密钥（应该放在配置文件中）
     */
    private static final String SECRET = "pinyougou_secret_key_2026_secure_token";

    /**
     * 有效期：2小时（单位：秒）
     */
    private static final long EXPIRATION = 2 * 60 * 60;

    /**
     * Refresh Token有效期：7天（单位：秒）
     */
    private static final long REFRESH_EXPIRATION = 7 * 24 * 60 * 60;

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
        return generateToken(username, EXPIRATION);
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
            .signWith(SignatureAlgorithm.HS512, SECRET);

        return builder.compact();
    }

    /**
     * 生成Refresh Token
     *
     * @param username 用户名
     * @return Refresh Token
     */
    public String generateRefreshToken(String username) {
        return generateToken(username, REFRESH_EXPIRATION);
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
                .setSigningKey(SECRET)
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
     * <p>
     * 验证失败场景：
     * - Token格式错误或签名无效（解析失败）
     * - Token已过期
     * - 签发人不匹配
     * - Token被篡改
     * <p>
     * ⚠️ 注意事项：
     * - 此方法不查询Redis黑名单（黑名单检查应在业务层单独实现）
     * - 只验证Token本身的合法性，不验证用户状态
     * - 建议在业务层增加：用户是否存在、是否被封禁、Token是否在黑名单
     * <p>
     * 性能优化：
     * - Token验证无需查询数据库（无IO操作）
     * - 适合在高并发场景使用
     * - 建议缓存公钥（当前使用对称加密HS512）
     *
     * @param token JWT Token
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) {
                return false;
            }

            // 检查是否过期
            Date expiration = claims.getExpiration();
            if (expiration == null || expiration.before(new Date())) {
                return false;
            }

            // 检查签发人
            String issuer = claims.getIssuer();
            if (!ISSUER.equals(issuer)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
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
