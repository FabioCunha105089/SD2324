package src;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Status {
    private final int queueSize;
    private final List<String> workersInfo;

    public Status(int queueSize, List<String> workersInfo)
    {
        this.queueSize = queueSize;
        this.workersInfo = workersInfo;
    }

    public Status() {
        this.queueSize = -1;
        this.workersInfo = null;
    }

    public void serialize(DataOutputStream out) throws IOException
    {
        out.writeUTF(MessageTypes.STATUS.typeToString());
        if(this.queueSize != -1 && this.workersInfo != null)
        {
            out.writeInt(this.queueSize);
            out.writeInt(workersInfo.size());
            for (String workerInfo : workersInfo)
                out.writeUTF(workerInfo);
        }
        out.flush();
    }

    public static Status deserialize(DataInputStream in)
    {
        int queueSize = 0;
        List<String> workersInfo = new ArrayList<>();
        try{
            queueSize = in.readInt();
            int num = in.readInt();
            for (int i = 0; i <num; i++) {
               String info = in.readUTF();
               workersInfo.add(info);
            }
        }
        catch (IOException e)
        {
            System.out.println("Erro a ler status do servidor: " + e);
        }
        return new Status(queueSize, workersInfo);
    }

    public int getQueueSize() {
        return this.queueSize;
    }
    public List<String> getWorkersInfo() {
        return this.workersInfo;
    }
}
