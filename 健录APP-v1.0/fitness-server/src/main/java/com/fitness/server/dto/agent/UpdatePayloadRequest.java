package com.fitness.server.dto.agent;

/**
 * 更新操作草案请求
 */
public class UpdatePayloadRequest {
    private String payloadJson;

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}
