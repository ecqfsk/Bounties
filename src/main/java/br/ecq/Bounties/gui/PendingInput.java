package br.ecq.Bounties.gui;

import java.util.UUID;

public class PendingInput {

    private final UUID targetUuid;
    private final String targetName;
    private final boolean addMode;

    public PendingInput(UUID targetUuid, String targetName, boolean addMode) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.addMode = addMode;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public boolean isAddMode() {
        return addMode;
    }
}