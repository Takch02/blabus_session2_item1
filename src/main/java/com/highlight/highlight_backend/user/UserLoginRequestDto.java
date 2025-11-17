package com.highlight.highlight_backend.user;


import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserLoginRequestDto {

    @Column(nullable = false)
    @Size(min = 6, max = 15)
    private String userId;

    @Size(min = 8, max = 15)
    @Column(nullable = false)
    private String password;

}
