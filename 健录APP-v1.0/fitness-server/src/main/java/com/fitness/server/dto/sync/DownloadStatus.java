package com.fitness.server.dto.sync;

import java.util.ArrayList;
import java.util.List;

/**
 * 下载状态
 */
public class DownloadStatus {
    private boolean success;       // 整体是否成功
    private int totalItems;        // 总条目数
    private int successItems;      // 成功条目数
    private int failedItems;       // 失败条目数
    private List<SyncErrorDetail> errors;  // 失败详情

    public DownloadStatus() {
        this.errors = new ArrayList<>();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    public int getSuccessItems() { return successItems; }
    public void setSuccessItems(int successItems) { this.successItems = successItems; }
    public int getFailedItems() { return failedItems; }
    public void setFailedItems(int failedItems) { this.failedItems = failedItems; }
    public List<SyncErrorDetail> getErrors() { return errors; }
    public void setErrors(List<SyncErrorDetail> errors) { this.errors = errors; }

    public void addError(SyncErrorDetail error) {
        this.errors.add(error);
        this.failedItems++;
    }

    public void incrementSuccess() {
        this.successItems++;
    }

    public void computeSuccess() {
        this.success = (this.failedItems == 0);
    }
}
