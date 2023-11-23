package src;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Status {
    private int mem;
    private int nQueue;

    private Status(int mem, int nQueue)
    {
        this.mem = mem;
        this.nQueue = nQueue;
    }

    public Status() {
        this.mem = -1;
        this.nQueue = -1;
    }

    public void serialize(DataOutputStream out) throws IOException
    {
        out.writeUTF(MessageTypes.STATUS.typeToString());
        if(this.mem != -1 && this.nQueue != -1)
        {
            out.writeInt(this.mem);
            out.writeInt(this.nQueue);
        }
    }

    public static Status deserialize(DataInputStream in)
    {
        int mem = 0;
        int nQueue = 0;
        try{
            mem = in.readInt();
            nQueue = in.readInt();
        }
        catch (IOException e)
        {
            System.out.println("Erro a ler status do servidor: " + e);
        }
        return new Status(mem, nQueue);
    }

    public void printStatus()
    {
        System.out.println("Memória disponível: " + mem + "\nTarefas pendentes: " + nQueue);
    }
}
