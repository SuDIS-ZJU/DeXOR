package Experiment;

public enum VerificationMode {
    BITWISE("bitwise"),
    PRECISION("precision"),
    NONE("none");

    private final String configName;

    VerificationMode(String configName) {
        this.configName = configName;
    }

    public String getConfigName() {
        return configName;
    }

    public static VerificationMode parse(String value) {
        if (value == null) return BITWISE;
        String normalized = value.trim().toLowerCase();
        switch (normalized) {
            case "none":
            case "no-verify":
            case "false":
                return NONE;
            case "precision":
            case "original-precision":
                return PRECISION;
            case "bitwise":
            case "raw-bit":
            case "true":
                return BITWISE;
            default:
                throw new IllegalArgumentException("Unknown verification mode: " + value);
        }
    }
}
