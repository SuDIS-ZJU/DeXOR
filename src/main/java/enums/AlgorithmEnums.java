package enums;

public enum AlgorithmEnums {
    GORILLA("Gorilla"),
    CHIMP("Chimp"),
    CHIMP128("Chimp128"),
    Elf("Elf"),
    ElfPlus("ElfPlus"),
    Camel("Camel"),
    ATDP("ATDP");

    private final String name;

    AlgorithmEnums(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    static public AlgorithmEnums CheckName(String name) throws Exception {
        for(AlgorithmEnums algorithm : AlgorithmEnums.values()){
            if(name.equalsIgnoreCase(algorithm.name)){
                return algorithm;
            }
        }
        throw new Exception("No such algorithm");
    }
}
