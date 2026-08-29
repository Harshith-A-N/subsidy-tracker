package com.subsidytracker.security.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.subsidytracker.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    private String email;
    private String password;
    private Role role;

    @JsonSetter("role")
    public void setRoleFromJson(Object roleObj) {
        if (roleObj == null) {
            this.role = null;
            return;
        }
        if (roleObj instanceof Role r) {
            this.role = r;
            return;
        }
        String str = roleObj.toString().trim().toUpperCase();
        for (Role r : Role.values()) {
            if (r.name().equalsIgnoreCase(str) || r.name().replace("_", "").equalsIgnoreCase(str.replace("_", ""))) {
                this.role = r;
                return;
            }
        }
        this.role = null;
    }
}
