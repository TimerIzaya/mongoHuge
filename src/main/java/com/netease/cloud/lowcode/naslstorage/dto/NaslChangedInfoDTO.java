package com.netease.cloud.lowcode.naslstorage.dto;

import lombok.Data;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/7/6 10:29
 */

@Data
public class NaslChangedInfoDTO {
    private Long backendChangedTime;
    private Long webChangedTime;
}
