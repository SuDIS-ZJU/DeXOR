package enums;

public enum AlgorithmEnums {
    GORILLA("Gorilla"),
    CHIMP("Chimp"),
    CHIMP128("Chimp128"),
    Elf("Elf"),
    ElfPlus("ElfPlus"),
    Camel("Camel"),
    DeXOR("DeXOR"),
    DeXORPlus("DeXORPlus"),
    ALP("ALP"),
    ElfStar("ElfStar"),
    SElfStar("SElfStar"),
    LZ4("LZ4"),
    ZSTD("Zstd"),
    SNAPPY("Snappy"),
    SIMPIECE("SimPiece"),
    MIXPIECE("MixPiece"),
    SZ3("SZ3"),
    SZ3V2("SZ3v2"),
    AXE("Axe");

    private final String name;

    AlgorithmEnums(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    static public AlgorithmEnums CheckName(String name) {
        for(AlgorithmEnums algorithm : AlgorithmEnums.values()){
            if(name.equalsIgnoreCase(algorithm.name)){
                return algorithm;
            }
        }
        return null;
    }
}
