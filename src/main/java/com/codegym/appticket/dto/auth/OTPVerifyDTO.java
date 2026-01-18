package com.codegym.appticket.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OTPVerifyDTO {
    private String otpCode;
}
