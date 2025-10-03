package com.veely.dto.payslip;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class PayslipUploadResult {
    private int processed;
    private int stored;
    private int unmatched;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> unmatchedCodes = new ArrayList<>();

    public void addError(String message) {
        errors.add(message);
    }

    public void addUnmatched(String message) {
        unmatched++;
        unmatchedCodes.add(message);
    }
}
