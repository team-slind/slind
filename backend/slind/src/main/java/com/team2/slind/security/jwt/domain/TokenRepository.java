package com.team2.slind.security.jwt.domain;

import com.team2.slind.security.jwt.dto.RefreshTokenDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class TokenRepository {

    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenExpirationPeriod;
    private final RedisTemplate redisTemplate;


    public TokenRepository(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    public void saveRefreshToken(RefreshTokenDto refreshTokenDto) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        //memberId를 key로 refreshToken을 value로 저장
        valueOperations.set(String.valueOf(refreshTokenDto.getMemberId()), String.valueOf(refreshTokenDto.getRefreshToken()));

        redisTemplate.expire(String.valueOf(refreshTokenDto.getRefreshToken()), refreshTokenExpirationPeriod, TimeUnit.MICROSECONDS);


    }

    public Optional<RefreshTokenDto> findByMemberId(String memberId) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        Optional<String> refreshTokenOptional = Optional.ofNullable(valueOperations.get(memberId));

        //refreshToken이 redis에 존재하지 않음
        if (refreshTokenOptional.isEmpty()) {
            return Optional.empty();
        }

        //redis에 존재하면 RefreshTokenDto 반환
        return Optional.of(new RefreshTokenDto(memberId, refreshTokenOptional.get()));

    }

    public void deleteByMemberId(String memberId) {
        redisTemplate.delete(memberId);
    }

    public void deleteByAccessToken(String accessToken){
        redisTemplate.delete(accessToken);
    }
    public Optional<String> findByKey(String key) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        Optional<String> valueOptional = Optional.ofNullable(valueOperations.get(key));

        if (valueOptional.isEmpty()) return Optional.empty();
        return valueOptional;
    }

    public void saveBlackList(String accessToken, Long expiration) {
        redisTemplate.opsForValue().set(accessToken, "logout", expiration, TimeUnit.MILLISECONDS);
    }
}
