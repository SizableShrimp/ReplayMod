package com.replaymod.core.utils;

import java.util.regex.Pattern;

public class Patterns {
    public static final Pattern ALPHANUMERIC_UNDERSCORE = Pattern.compile("^[a-z0-9_]*$", Pattern.CASE_INSENSITIVE);
    public static final Pattern ALPHANUMERIC_COMMA = Pattern.compile("^[a-z0-9,]*$", Pattern.CASE_INSENSITIVE);
    public static final Pattern ALPHANUMERIC_SPACE_HYPHEN_UNDERSCORE = Pattern.compile("^[a-z0-9 \\-_]*$", Pattern.CASE_INSENSITIVE);
}
