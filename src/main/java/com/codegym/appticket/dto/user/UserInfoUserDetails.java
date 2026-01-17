package com.codegym.appticket.dto.user;

import com.codegym.appticket.entity.User;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserInfoUserDetails implements UserDetails {
    private String email;
    private String password;
    private List<GrantedAuthority> authorities;
    private User user;
    public UserInfoUserDetails(User user) {
        email = user.getEmail();
        password = user.getPassword();
        this.user = user;
        authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(java.util.stream.Collectors.toList());
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // isBlocked = true -> Locked -> NonLocked = false
        return user.getIsBlocked() == null || !user.getIsBlocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getEnabled() != null ? user.getEnabled() : true;
    }

    public User getUser() {
        return user;
    }
}
