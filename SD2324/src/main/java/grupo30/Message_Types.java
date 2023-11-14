package grupo30;

public enum Message_Types {
    TASK_REQUEST,
    STATUS,
    LOGIN;

    public String typeToString()
    {
        return switch (this) {
            case TASK_REQUEST -> "1";
            case LOGIN -> "2";
            case STATUS -> "3";
        };
    }

    public Message_Types stringToType(String type)
    {
        return switch(type)
        {
            case "1" -> TASK_REQUEST;
            case "2" -> LOGIN;
            case "3" -> STATUS;
            default -> null;
        };
    }
}

