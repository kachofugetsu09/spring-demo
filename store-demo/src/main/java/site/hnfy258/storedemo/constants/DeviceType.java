package site.hnfy258.storedemo.constants;

public enum DeviceType {
    PC("PC"),
    MOBILE("Mobile");

    private final String type;
    
    DeviceType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
}
