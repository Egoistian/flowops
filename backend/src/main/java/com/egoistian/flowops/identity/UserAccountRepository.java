package com.egoistian.flowops.identity;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class UserAccountRepository {
    private final JdbcClient jdbc;

    public UserAccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserAccount> findActive(String organizationKey, String email) {
        return jdbc.sql("""
                        select u.id, u.organization_id, u.email, u.display_name, u.password_hash
                        from users u
                        join organizations o on o.id = u.organization_id
                        where o.organization_key = :organizationKey
                          and o.status = 'ACTIVE'
                          and u.status = 'ACTIVE'
                          and lower(u.email) = lower(:email)
                        """)
                .param("organizationKey", organizationKey)
                .param("email", email)
                .query((rs, rowNum) -> new UserAccount(
                        rs.getObject("id", UUID.class),
                        rs.getObject("organization_id", UUID.class),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        rs.getString("password_hash"),
                        loadRoles(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class))))
                .optional();
    }

    private Set<String> loadRoles(UUID userId, UUID organizationId) {
        List<String> roles = jdbc.sql("""
                        select role
                        from user_roles
                        where user_id = :userId and organization_id = :organizationId
                        order by role
                        """)
                .param("userId", userId)
                .param("organizationId", organizationId)
                .query(String.class)
                .list();
        return new LinkedHashSet<>(roles);
    }

    public record UserAccount(
            UUID userId,
            UUID organizationId,
            String email,
            String displayName,
            String passwordHash,
            Set<String> roles) {
    }
}
