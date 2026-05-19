package com.ouazzou.miniaws.modules.monitoring.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VmMetrics {
    private Integer vmId;
    private double cpuUsagePercentage;
    private double ramUsagePercentage;
    private String status;
}