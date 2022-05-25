package com.netease.cloud.lowcode.naslstorage.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ActionDTO {
    private String path;
    private String action;
    private Map object;
}
