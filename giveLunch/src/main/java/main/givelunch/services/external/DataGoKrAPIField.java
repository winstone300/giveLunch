package main.givelunch.services.external;

public enum DataGoKrAPIField {
    FOODNAME("FOOD_NM_KR"),
    CATEGORY("FOOD_OR_NM"),
    SERVINGSIZE("SERVING_SIZE"),
    CALORIES("AMT_NUM1"),
    PROTEIN("AMT_NUM3"),
    FAT("AMT_NUM4"),
    CARBOHYDRATE("AMT_NUM6");

    private final String key;

    DataGoKrAPIField(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
