package br.ecq.Bounties.model;

import java.util.UUID;

public class HistoryEntry {

    public enum Type {
        SET,
        ADD,
        REMOVE,
        CLAIM,
        EXPIRE,
        REFUND
    }

    private final Type type;
    private final long timestamp;
    private final UUID actor;
    private final String actorName;
    private final UUID target;
    private final String targetName;
    private final UUID extra;
    private final String extraName;
    private final double amount;

    public HistoryEntry(Type type, long timestamp, UUID actor, String actorName,
                        UUID target, String targetName, UUID extra, String extraName, double amount) {
        this.type = type;
        this.timestamp = timestamp;
        this.actor = actor;
        this.actorName = actorName != null ? actorName : "?";
        this.target = target;
        this.targetName = targetName != null ? targetName : "?";
        this.extra = extra;
        this.extraName = extraName != null ? extraName : "";
        this.amount = amount;
    }

    public Type getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public UUID getActor() {
        return actor;
    }

    public String getActorName() {
        return actorName;
    }

    public UUID getTarget() {
        return target;
    }

    public String getTargetName() {
        return targetName;
    }

    public UUID getExtra() {
        return extra;
    }

    public String getExtraName() {
        return extraName;
    }

    public double getAmount() {
        return amount;
    }

    public boolean involves(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        return uuid.equals(actor) || uuid.equals(target) || uuid.equals(extra);
    }
}
