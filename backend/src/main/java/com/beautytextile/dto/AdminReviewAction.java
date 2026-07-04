package com.beautytextile.dto;

/** Admin action on a review (approve / reject / reply). */
public record AdminReviewAction(
        String action,    // APPROVE | REJECT | REPLY | DELETE
        String reply      // only used for REPLY action
) {}
