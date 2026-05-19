package com.ouazzou.miniaws.modules.ai_ops.domain;

// Un "record" en Java est une classe légère parfaite pour transporter des données
public record VmDeploymentRequest(
        String nomServeur,
        Integer ramRecommandee,
        Integer cpuRecommande,
        String explication,
        String os
) {}