package util;

import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * JWT工具类
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
