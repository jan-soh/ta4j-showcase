package de.jansoh.rsistrategy.service;

public enum AlgoStatus {
    FINISHED,
    CANCELED;

    public static boolean isFinished(String status) {
        return status.equals(FINISHED.name());
    }

    ;

    public static boolean isCancelled(String status) {
        return status.equals(CANCELED.name());
    }

    ;
}
