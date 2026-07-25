package com.eazicut.api.users.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.eazicut.api.users.entity.User;

/**
 * Adapts a domain {@link User} to Spring Security's {@link UserDetails}
 * contract without dragging Spring Security types into the domain.
 *
 * <p>Kept in the {@code users.security} package (not on the {@code User}
 * entity itself) so the entity stays framework-free and can be mapped
 * into DTOs, JSON, etc. without pulling in {@code spring-security-core}.
 *
 * <p>Exposes the underlying {@link #user()} so downstream consumers
 * (JWT issuance in Stage 3, {@code /auth/me} in Stage 6) can reach the
 * id, display name, and role without a second lookup.
 */
public record UserDetailsAdapter(User user) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().authority()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    /**
     * Spring's contract expects a "username" — we return the normalised
     * email ({@code email_lower}) so downstream principal comparisons
     * stay case-insensitive without extra normalisation.
     */
    @Override
    public String getUsername() {
        return user.getEmailLower();
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return user.isEnabled(); }
}
