package com.fitness.server.service;

import com.fitness.server.dto.sync.SyncErrorDetail;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncFailureContractTest {

    @Test
    void exposesStableUserMessageInsteadOfDatabaseDiagnostic() {
        SyncErrorDetail error = new SyncErrorDetail(
            "training_plan", "42", "DB_ERROR", "SQLSTATE[HY000]: internal database detail"
        );

        assertEquals("DB_ERROR", error.getErrorCode());
        assertEquals("服务器暂时无法保存数据，请稍后重试", error.getErrorMessage());
        assertTrue(error.isRetryable());
    }

    @Test
    void classifiesExceptionsByTypeRatherThanTheirMessageText() throws Exception {
        assertEquals("DUPLICATE", classify(new DuplicateKeyException("arbitrary message")));
        assertEquals("VALIDATION_FAILED", classify(new DataIntegrityViolationException("no constraint text")));
        assertEquals("DB_ERROR", classify(new java.sql.SQLException("no SQL keyword")));
        assertEquals("VALIDATION_FAILED", classify(new IllegalArgumentException("not a validation word")));
        assertEquals("UNKNOWN", classify(new IllegalStateException("database duplicate constraint")));
    }

    @Test
    void marksOnlyTransientFailuresAsRetryable() {
        SyncErrorDetail validation = new SyncErrorDetail("diet_record", "7", "VALIDATION_FAILED", "ignored");
        SyncErrorDetail duplicate = new SyncErrorDetail("diet_record", "7", "DUPLICATE", "ignored");

        assertFalse(validation.isRetryable());
        assertFalse(duplicate.isRetryable());
    }

    private String classify(Exception exception) throws Exception {
        Method method = SyncService.class.getDeclaredMethod("classifyError", Exception.class);
        method.setAccessible(true);
        return (String) method.invoke(new SyncService(), exception);
    }
}
