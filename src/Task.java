package src;

import java.io.*;

public class Task {
    private final String taskId; //Nome da task, que é o nome do ficheiro de onde vem
    private String client;
    private int mem; //Memória que vai usar
    private byte[] task; //A task mesmo
    private int errorCode;
    private String errorMessage;

    public Task(String taskId, int mem, byte[] task)
    {
        this.task = task;
        this.mem = mem;
        this.taskId = taskId;
    }

    public Task(String client, String taskId, int mem, byte[] task)
    {
        this.client = client;
        this.task = task;
        this.mem = mem;
        this.taskId = taskId;
    }

    private Task(String taskId, byte[] task)
    {
        this.task = task;
        this.taskId = taskId;
    }

    private Task(String taskId, String errorMessage, int errorCode)
    {
        this.taskId = taskId;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public Task(String client, String taskId, byte[] task) {
        this.client = client;
        this.taskId = taskId;
        this.task = task;
    }

    public Task(String client, String taskId, String errorMsg, int errorCode) {
        this.client = client;
        this.taskId = taskId;
        this.errorMessage = errorMsg;
        this.errorCode = errorCode;
    }

    public void serialize(DataOutputStream out) throws IOException {
        out.writeUTF(MessageTypes.TASK_REQUEST.typeToString());
        out.writeInt(this.mem);
        out.writeUTF(this.taskId);
        out.writeInt(this.task.length);
        out.write(this.task);
        out.flush();
    }

    public void serializeToWorker(DataOutputStream out) throws IOException {
        out.writeUTF(this.client);
        out.writeUTF(this.taskId);
        out.writeInt(this.task.length);
        out.write(this.task);
        out.flush();
    }

    public void serializeToClient(DataOutputStream out, MessageTypes type) throws IOException {
        if (type.equals(MessageTypes.TASK_FAILED))
            serialize_failure(out, type);
        else
            serialize_success(out, type);
    }

    private void serialize_failure(DataOutputStream out, MessageTypes type) throws IOException {
        out.writeUTF(type.typeToString());
        out.writeUTF(this.taskId);
        out.writeInt(this.errorCode);
        out.writeUTF(this.errorMessage);
        out.flush();
    }

    private void serialize_success(DataOutputStream out, MessageTypes type) throws IOException {
        out.writeUTF(type.typeToString());
        out.writeUTF(this.taskId);
        out.writeInt(this.task.length);
        out.write(this.task);
        out.flush();
    }

    public static Task deserializeFromServer(DataInputStream in, MessageTypes type) throws IOException {
        if (type.equals(MessageTypes.TASK_FAILED))
            return deserializeFromServerFailure(in);
        else
            return deserializeFromServerSuccess(in);
    }

    private static Task deserializeFromServerFailure(DataInputStream in) throws IOException {
        String taskId = in.readUTF();
        int errorCode = in.readInt();
        String errorMessage = in.readUTF();

        return new Task(taskId, errorMessage, errorCode);
    }

    private static Task deserializeFromServerSuccess(DataInputStream in) throws IOException {
        String taskId = in.readUTF();
        int taskLength = in.readInt();
        byte[] task = in.readNBytes(taskLength);

        return new Task(taskId, task);
    }

    public static Task deserialize(DataInputStream in, MessageTypes type) {
        if (type.equals(MessageTypes.TASK_FAILED))
            return deserialize_failure(in);
        else
            return deserialize_success(in);
    }

    public static Task deserialize(DataInputStream in, String client) {
        return deserialize_request(in, client);
    }

    private static Task deserialize_request(DataInputStream in, String client) {
        String taskId = "";
        byte[] task = null;
        int mem = 0;
        try
        {
            mem = in.readInt();
            taskId = in.readUTF();
            int taskSize = in.readInt();
            task = in.readNBytes(taskSize);
        }
        catch(IOException e)
        {
            System.out.println("Erro a ler resposta de task: " + e);
        }
        return new Task(client, taskId, mem, task);
    }

    private static Task deserialize_success(DataInputStream in)
    {
        String client = "";
        String taskId = "";
        byte[] task = null;

        try
        {
            client = in.readUTF();
            taskId = in.readUTF();
            int taskLength = in.readInt();
            task = in.readNBytes(taskLength);
        }
        catch(IOException e)
        {
            System.out.println("Erro a ler resposta de task: " + e);
        }
        return new Task(client, taskId, task);
    }

    private static Task deserialize_failure(DataInputStream in)
    {
        String errorMsg = "";
        String client = "";
        String taskId = "";
        int errorCode = 0;
        try
        {
            client = in.readUTF();
            taskId = in.readUTF();
            errorCode = in.readInt();
            errorMsg = in.readUTF();
        }
        catch(IOException e)
        {
            System.out.println("Erro a ler resposta de task: " + e);
        }
        return new Task(client, taskId, errorMsg, errorCode);
    }

    public void writeResultToFile(String resultPath, MessageTypes type)
    {
        try {
            String path = this.taskId.concat("_Result.gz");
            if(type.equals(MessageTypes.TASK_FAILED))
                path = this.taskId.concat("_Result.txt");
            File file = new File(resultPath.concat(path));
                try (FileOutputStream writer = new FileOutputStream(file, false)) {
                    if (type.equals(MessageTypes.TASK_SUCCESSFUL)) {
                        writer.write(this.task);
                    }else {
                        writer.write(("Error code: " + this.errorCode + "\nError message: " + this.errorMessage).getBytes());
                    }
                }
        } catch (Exception e) {
            System.err.println("Erro a guardar resultados: " + e);
        }
    }

    public int getMem(){
        return this.mem;
    }
    public String getTaskId() {
        return this.taskId;
    }
    public String getClient() {
        return this.client;
    }
}
