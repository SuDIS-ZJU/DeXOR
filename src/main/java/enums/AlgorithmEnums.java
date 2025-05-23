package enums;

public enum AlgorithmEnums {
    GORILLA("Gorilla"),
    CHIMP("Chimp"),
    CHIMP128("Chimp128"),
    Elf("Elf"),
    ElfPlus("ElfPlus"),
    Camel("Camel"),
    DeXOR("DeXOR"),
    ALP("ALP"),
    ElfStar("ElfStar"),
    DPF("DPF");

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
