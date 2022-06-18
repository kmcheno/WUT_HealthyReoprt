package com.ming.healthyreport.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PostDTO {
    private boolean status;
    private String data;
}
