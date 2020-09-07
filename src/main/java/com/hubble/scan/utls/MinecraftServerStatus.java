package com.hubble.scan.utls;

import java.io.*;
import java.net.*;

public class MinecraftServerStatus
{
    public static final byte NUM_FIELDS = 6;
    public static final int DEFAULT_TIMEOUT = 5;
    private String address;
    private int port;
    private int timeout;
    private boolean serverUp;
    private String motd;
    private String version;
    private String currentPlayers;
    private String maximumPlayers;
    private long latency;

    public MinecraftServerStatus(String address, int port)
    {
        this(address, port, DEFAULT_TIMEOUT);
    }

    public MinecraftServerStatus(String address, int port, int timeout)
    {
        setAddress(address);
        setPort(port);
        setTimeout(timeout);
        refresh();
    }

    public boolean refresh()
    {
        String[] serverData;
        String rawServerData;
        try
        {
            Socket clientSocket = new Socket();
            long startTime = System.currentTimeMillis();
            clientSocket.connect(new InetSocketAddress(getAddress(), getPort()), timeout);
            setLatency(System.currentTimeMillis() - startTime);
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            byte[] payload = {(byte) 0xFE, (byte) 0x01};
            dos.write(payload, 0, payload.length);
            rawServerData = br.readLine();
            clientSocket.close();
        }
        catch(Exception e)
        {
            serverUp = false;
            return serverUp;
        }

        if(rawServerData == null)
            serverUp = false;
        else
        {
            serverData = rawServerData.split("\u0000\u0000\u0000");
            if(serverData != null && serverData.length >= NUM_FIELDS)
            {
                serverUp = true;
                setVersion(serverData[2].replace("\u0000", ""));
                setMotd(serverData[3].replace("\u0000", ""));
                setCurrentPlayers(serverData[4].replace("\u0000", ""));
                setMaximumPlayers(serverData[5].replace("\u0000", ""));
            }
            else
                serverUp = false;
        }
        return serverUp;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public int getTimeout()
    {
        return timeout * 1000;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout * 1000;
    }

    public String getMotd()
    {
        return motd;
    }

    public String getVersion()
    {
        return version;
    }

    public String getCurrentPlayers()
    {
        return currentPlayers;
    }

    public String getMaximumPlayers()
    {
        return maximumPlayers;
    }

    public long getLatency()
    {
        return latency;
    }

    public void setLatency(long latency)
    {
        this.latency = latency;
    }

    public void setMaximumPlayers(String maximumPlayers)
    {
        this.maximumPlayers = maximumPlayers;
    }

    public void setCurrentPlayers(String currentPlayers)
    {
        this.currentPlayers = currentPlayers;
    }

    public void setMotd(String motd)
    {
        this.motd = motd;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public boolean isServerUp()
    {
        return serverUp;
    }
}