package com.g42.platform.gms.notification.infrastructure.repository;

import com.g42.platform.gms.notification.infrastructure.entity.ZaloToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ZaloTokenRepo extends JpaRepository<ZaloToken, String> {

    ZaloToken findByState(String state);
    @Query("""
    select z from ZaloToken z where z.state=:state
    """)
    ZaloToken getZaloTokenByState(String state);

    ZaloToken getZaloTokensByStateEqualsIgnoreCase(String state);

    ZaloToken getZaloTokensByState(String state);

    /** Mọi dòng "active", mới nhất trước — dùng để dọn khi lỡ có nhiều hơn 1 dòng. */
    java.util.List<ZaloToken> findByStateOrderByIdDesc(String state);
}
