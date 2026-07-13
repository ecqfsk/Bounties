package br.ecq.Bounties.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Recompensa ativa em um alvo, com contribuidores e expiracao.
 */
public class BountyData {

    private final UUID target;
    private final Map<UUID, Double> contributors = new LinkedHashMap<UUID, Double>();
    private long createdAt;
    private long expiresAt;
    private long lastUpdated;

    public BountyData(UUID target) {
        this.target = target;
        this.createdAt = System.currentTimeMillis();
        this.lastUpdated = this.createdAt;
        this.expiresAt = 0L;
    }

    public UUID getTarget() {
        return target;
    }

    public double getAmount() {
        double total = 0;
        for (double value : contributors.values()) {
            total += value;
        }
        return total;
    }

    public Map<UUID, Double> getContributors() {
        return Collections.unmodifiableMap(contributors);
    }

    public boolean hasContributor(UUID uuid) {
        return contributors.containsKey(uuid) && contributors.get(uuid) > 0;
    }

    public double getContribution(UUID uuid) {
        Double value = contributors.get(uuid);
        return value != null ? value : 0;
    }

    public int getContributorCount() {
        return contributors.size();
    }

    public void setContribution(UUID contributor, double amount) {
        if (amount <= 0) {
            contributors.remove(contributor);
        } else {
            contributors.put(contributor, amount);
        }
        touch();
    }

    public void addContribution(UUID contributor, double amount) {
        if (amount <= 0) {
            return;
        }
        setContribution(contributor, getContribution(contributor) + amount);
    }

    /**
     * Substitui toda a recompensa por um unico contribuidor.
     */
    public void replaceWith(UUID contributor, double amount) {
        contributors.clear();
        if (amount > 0) {
            contributors.put(contributor, amount);
        }
        touch();
    }

    public double removeContribution(UUID contributor) {
        Double removed = contributors.remove(contributor);
        touch();
        return removed != null ? removed : 0;
    }

    public Map<UUID, Double> clearAndGetContributors() {
        Map<UUID, Double> copy = new LinkedHashMap<UUID, Double>(contributors);
        contributors.clear();
        touch();
        return copy;
    }

    public boolean isEmpty() {
        return contributors.isEmpty() || getAmount() <= 0;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean hasExpiration() {
        return expiresAt > 0;
    }

    public boolean isExpired(long now) {
        return hasExpiration() && now >= expiresAt;
    }

    public long getMillisUntilExpire(long now) {
        if (!hasExpiration()) {
            return -1;
        }
        return Math.max(0, expiresAt - now);
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void touch() {
        this.lastUpdated = System.currentTimeMillis();
    }

    /** Contribuidor desconhecido (dados legados sem dono). Nao recebe reembolso. */
    public static final UUID UNKNOWN_CONTRIBUTOR = new UUID(0L, 0L);

    /**
     * Carrega formato legado: so o valor total, sem contribuidores conhecidos.
     */
    public static BountyData legacy(UUID target, double amount) {
        BountyData data = new BountyData(target);
        if (amount > 0) {
            data.contributors.put(UNKNOWN_CONTRIBUTOR, amount);
        }
        return data;
    }

    public static boolean isUnknownContributor(UUID uuid) {
        return uuid == null || UNKNOWN_CONTRIBUTOR.equals(uuid);
    }
}
