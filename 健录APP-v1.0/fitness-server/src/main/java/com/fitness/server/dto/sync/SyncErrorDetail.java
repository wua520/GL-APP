package com.fitness.server.dto.sync;

/**
 * 同步错误详情
 */
public class SyncErrorDetail {
    private String entityType;    // "workout", "diet_record", "body_record", "training_plan"
    private String localId;       // 客户端localId
    private String errorCode;     // 稳定的机器可判定错误码
    private String errorMessage;  // 用户可展示的稳定提示
    private boolean retryable;    // 客户端是否应保留并重试该条目

    public SyncErrorDetail() {}

    public SyncErrorDetail(String entityType, String localId, String errorCode, String ignoredDiagnostic) {
        this.entityType = entityType;
        this.localId = localId;
        this.errorCode = errorCode;
        this.errorMessage = defaultUserMessage(errorCode);
        this.retryable = isRetryableCode(errorCode);
    }

    private static boolean isRetryableCode(String code) {
        return "DB_ERROR".equals(code) || "NETWORK_ERROR".equals(code) || "UNKNOWN".equals(code);
    }

    private static String defaultUserMessage(String code) {
        return switch (code) {
            case "DUPLICATE" -> "该数据已同步，已跳过重复写入";
            case "VALIDATION_FAILED" -> "数据格式不正确，请修改后重试";
            case "DB_ERROR" -> "服务器暂时无法保存数据，请稍后重试";
            case "NETWORK_ERROR" -> "网络暂时不可用，请稍后重试";
            default -> "同步失败，请稍后重试";
        };
    }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getLocalId() { return localId; }
    public void setLocalId(String localId) { this.localId = localId; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public boolean isRetryable() { return retryable; }
    public void setRetryable(boolean retryable) { this.retryable = retryable; }
}
