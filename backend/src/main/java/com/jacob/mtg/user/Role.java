package com.jacob.mtg.user;

public enum Role {
    USER,
    ADMIN,
    /** Shared recruiter account: read/write its own data, but cannot change email/password or delete itself. */
    DEMO
}
