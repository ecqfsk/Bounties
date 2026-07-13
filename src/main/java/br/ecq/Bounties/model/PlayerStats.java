package br.ecq.Bounties.model;

public class PlayerStats {

    private double earned;
    private double spent;
    private int claims;
    private int placements;
    private int killsWithBounty;

    public double getEarned() {
        return earned;
    }

    public void addEarned(double amount) {
        if (amount > 0) {
            this.earned += amount;
        }
    }

    public void setEarned(double earned) {
        this.earned = Math.max(0, earned);
    }

    public double getSpent() {
        return spent;
    }

    public void addSpent(double amount) {
        if (amount > 0) {
            this.spent += amount;
        }
    }

    public void setSpent(double spent) {
        this.spent = Math.max(0, spent);
    }

    public int getClaims() {
        return claims;
    }

    public void addClaim() {
        this.claims++;
    }

    public void setClaims(int claims) {
        this.claims = Math.max(0, claims);
    }

    public int getPlacements() {
        return placements;
    }

    public void addPlacement() {
        this.placements++;
    }

    public void setPlacements(int placements) {
        this.placements = Math.max(0, placements);
    }

    public int getKillsWithBounty() {
        return killsWithBounty;
    }

    public void addKillWithBounty() {
        this.killsWithBounty++;
    }

    public void setKillsWithBounty(int killsWithBounty) {
        this.killsWithBounty = Math.max(0, killsWithBounty);
    }
}
