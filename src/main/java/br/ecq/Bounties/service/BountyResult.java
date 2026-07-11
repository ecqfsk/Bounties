package br.ecq.Bounties.service;

public enum BountyResult {
    SUCCESS,
    NO_PERMISSION,
    CANNOT_SELF,
    BYPASS,
    INSUFFICIENT_FUNDS,
    INVALID_AMOUNT,
    TOO_LOW,
    TOO_HIGH,
    PLAYER_NOT_FOUND
}