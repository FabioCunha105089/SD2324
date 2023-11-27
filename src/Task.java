package src;

import java.io.*;

public class Task {
    private String taskName; //Nome da task, que é o nome do ficheiro de onde vem
    private int mem; //Memória que vai usar
    private byte[] task; //A task mesmo
    private int errorCode;
    private String errorMessage;

    public Task(String taskName, int mem, byte[] task)
    {
        this.task = task;
        this.mem = mem;
        this.taskName = taskName;
    }

    private Task(String taskName, byte[] task)
    {
        this.task = task;
        this.taskName = taskName;
    }

    private Task(String errorMessage, int errorCode)
    {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public void serialize(DataOutputStream out)
    {
        try{
            out.writeUTF(MessageTypes.TASK_REQUEST.typeToString());
            out.writeInt(this.mem);
            out.writeUTF(this.taskName);
            out.writeInt(this.task.length);
            out.write(this.task);
            out.flush();
        }
        catch (IOException e)
        {
            System.out.println("Erro a serializar task: " + e);
        }
    }

    public static Task deserialize(DataInputStream in, MessageTypes type) {
        if (type == MessageTypes.TASK_FAILED)
            return deserialize_failure(in);
        else
            return deserialize_success(in);
    }

    private static Task deserialize_success(DataInputStream in)
    {
        String taskName = "";
        byte[] task = null;
        try
        {
            taskName = in.readUTF();
            int taskSize = in.readInt();
            task = in.readNBytes(taskSize);
        }
        catch(IOException e)
        {
            System.out.println("Erro a ler resposta de task: " + e);
        }
        return new Task(taskName, task);
    }

    private static Task deserialize_failure(DataInputStream in)
    {
        String errorMsg = "";
        int errorCode = 0;
        try
        {
            errorCode = in.readInt();
            errorMsg = in.readUTF();
        }
        catch(IOException e)
        {
            System.out.println("Erro a ler resposta de task: " + e);
        }
        return new Task(errorMsg, errorCode);
    }

    public void writeResultToFile(String resultPath)
    {
        try {
            String path = this.taskName.concat("_Result");
            File file = new File(resultPath.concat(path));
            if (file.createNewFile()) {
                try (FileOutputStream writer = new FileOutputStream(file, false)) {
                    writer.write(this.task);
                }
            } else {
                throw new Exception("Não foi possível criar ficheiro.");
            }
        } catch (Exception e) {
            System.out.println("Erro a guardar resultados: " + e);
        }
    }

    public byte[] getTask(){
        return this.task;
    }

    public void printError()
    {
        System.out.println("Erro" + this.errorCode + " a executar task: " + this.errorMessage);
    }
}
