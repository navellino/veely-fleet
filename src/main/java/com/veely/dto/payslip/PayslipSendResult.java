package com.veely.dto.payslip;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class PayslipSendResult {
    private int requested;
    private int sent;
    private int skipped;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> skippedMessages = new ArrayList<>();

    public void addError(String message) {
        errors.add(message);
    }

    public void addSkipped(String message) {
        skipped++;
        skippedMessages.add(message);
    }
}
