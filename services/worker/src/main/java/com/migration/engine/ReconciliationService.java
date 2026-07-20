package com.migration.engine;

public class ReconciliationService {

    public ReconciliationResult reconcile(long sourceCount, long written, long skipped, long updated,
                                          boolean coldPhase) {
        long accounted = written + skipped + updated;
        boolean passed;
        if (coldPhase) {
            // cold: skipped duplicates OK, written + skipped should equal source
            passed = (written + skipped) == sourceCount || accounted >= sourceCount;
        } else {
            // hot: updates count as handled
            passed = accounted == sourceCount || written + updated == sourceCount;
        }
        return new ReconciliationResult(sourceCount, written, skipped, updated, passed);
    }

    public record ReconciliationResult(long sourceCount, long written, long skipped, long updated, boolean passed) {}
}
