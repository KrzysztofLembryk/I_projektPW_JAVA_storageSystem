package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class CycleHandler {
    // destDev_QueueSrcDevGraph - to kolejka trzymajaca dla danego destDev
    // transfery z srcDev jakie chca na niego przyjsc w kolejnosci przychodzenia
    Map<DeviceId, Queue<Pair<DeviceId, ComponentId>>> destDev_srcDevQueue;
    //Semaphore mutex = new Semaphore(1, true);

    public CycleHandler(Map<DeviceId, Integer> devices)
    {
        destDev_srcDevQueue = new ConcurrentHashMap<>();
        for(DeviceId devId : devices.keySet())
            destDev_srcDevQueue.put(devId, new LinkedList<>());
    }
    private void queue_pushBack(DeviceId srcDev, DeviceId destDev, ComponentId compId)
    {
        // we need to make copies of destDev and CompId cause remove might delete
        // those objects contents, and they would be deleted inside other functions too
        // need checking
        ComponentId copyCompId = new ComponentId(compId.hashCode());
        // jesli srcDev to null to mamy transfer ADD
        DeviceId copySrcDev = null;
        if(srcDev != null)
             copySrcDev = new DeviceId(srcDev.hashCode());

        destDev_srcDevQueue.get(destDev).add(new Pair<>(copySrcDev, copyCompId));
    }
    public void queue_removeFront(DeviceId destDev, ComponentId compId)
    {
        if(!destDev_srcDevQueue.get(destDev).isEmpty() &&
                destDev_srcDevQueue.get(destDev).peek().second.equals(compId))
            destDev_srcDevQueue.get(destDev).remove();
    }

    // this queue remove will be used in transfer REMOVE, cause remove doesnt know
    // component that it is freeing space for.
    public void queue_removeFront(DeviceId destDev)
    {
        if(!destDev_srcDevQueue.get(destDev).isEmpty())
            destDev_srcDevQueue.get(destDev).remove();
    }
    private boolean myselfFirstInQueue(DeviceId destDev, ComponentId compId)
    {
        if(!destDev_srcDevQueue.get(destDev).isEmpty())
            return destDev_srcDevQueue.get(destDev).peek().second.equals(compId);
        return false;
    }

    private Pair<Boolean, ComponentId> findCycle(DeviceId destDev)
    {
        DeviceId currDev = destDev;
        do
        {
            if(!destDev_srcDevQueue.get(currDev).isEmpty())
            {
                currDev = destDev_srcDevQueue.get(currDev).peek().first;
                // jesli po przejsciu po krawedzi mamy null to znaczy ze na
                // destDev pierwszym czekajacym transferem jest ADD, a on nie
                // bedzie w cyklu, wiec cyklu nie ma
                if(currDev == null)
                    return new Pair<>(false, new ComponentId(-1));
            }
            else
                return new Pair<>(false, new ComponentId(-1));
        }while(!currDev.equals(destDev));

        currDev = destDev_srcDevQueue.get(destDev).peek().first;
        ComponentId compOnDest = destDev_srcDevQueue.get(currDev).peek().second;

        return new Pair<>(true, compOnDest);
    }
    private void removeCycle(DeviceId destDev)
    {
        DeviceId currDev = destDev_srcDevQueue.get(destDev).peek().first;
        DeviceId tempDev;
        do
        {
                tempDev = destDev_srcDevQueue.get(currDev).peek().first;
                destDev_srcDevQueue.get(currDev).remove();
                currDev = tempDev;

        }while(!currDev.equals(destDev));

        destDev_srcDevQueue.get(destDev).remove();
    }

    public Pair<Boolean, ComponentId> cycleExist(DeviceId srcDev, DeviceId destDev, ComponentId compId)
    {
        queue_pushBack(srcDev, destDev, compId);
        if(myselfFirstInQueue(destDev, compId))
        {
            Pair<Boolean, ComponentId> isCycle = findCycle(destDev);
            if(isCycle.first)
            {
                // jesli jest cykl to go usuwamy zeby nastepne transfery nie mialy
                // starych informacji, bo po znalezieniu cyklu w execute zaczynamy go
                // ogarniac
                removeCycle(destDev);
            }
            return isCycle;
        }
        return new Pair<>(false, new ComponentId(-1));
    }

}
