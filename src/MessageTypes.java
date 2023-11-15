package src;

public enum MessageTypes {
    TASK_REQUEST,
    STATUS,
    LOGIN,
    NEW_TASK,
    TASK_SUCCESSFUL,
    TASK_FAILED;

    public String typeToString()
    {
        return switch (this) {
            case TASK_REQUEST -> "1";
            case LOGIN -> "2";
            case STATUS -> "3";
            case NEW_TASK -> "4";
            case TASK_SUCCESSFUL -> "5";
            case TASK_FAILED -> "6";
        };
    }

    public MessageTypes stringToType(String type)
    {
        return switch(type)
        {
            case "1" -> TASK_REQUEST;
            case "2" -> LOGIN;
            case "3" -> STATUS;
            case "4" -> NEW_TASK;
            case "5" -> TASK_SUCCESSFUL;
            case "6" -> TASK_FAILED;
            default -> null;
        };
    }
}

