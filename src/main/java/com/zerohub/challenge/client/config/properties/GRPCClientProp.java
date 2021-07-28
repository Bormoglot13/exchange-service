package com.zerohub.challenge.client.config.properties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GRPCClientProp {

    private Integer port;
    private Long channelShutdownTimeoutMs;
}
