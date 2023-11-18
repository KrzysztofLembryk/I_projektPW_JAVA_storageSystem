package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class CycleHandler {
    Map<DeviceId, Queue<Pair<DeviceId, ComponentId>>> srcToDestGraph;
    //Semaphore mutex = new Semaphore(1, true);

    public CycleHandler(Map<DeviceId, Integer> devices)
    {
        srcToDestGraph = new ConcurrentHashMap<>();
        for(DeviceId devId : devices.keySet())
            srcToDestGraph.put(devId, new LinkedList<>());
    }
    private void queue_pushBack(DeviceId srcDev, DeviceId destDev, ComponentId compId)
    {
        // we need to make copies of destDev and CompId cause remove might delete
        // those objects contents, and they would be deleted inside other functions too
        // need checking
        ComponentId copyCompId = new ComponentId(compId.hashCode());
        DeviceId copyDestDev = new DeviceId(destDev.hashCode());

        srcToDestGraph.get(srcDev).add(new Pair<>(copyDestDev, copyCompId));
    }
    public void queue_removeFront(DeviceId srcDev, ComponentId compId)
    {
        if(!srcToDestGraph.get(srcDev).isEmpty() &&
                srcToDestGraph.get(srcDev).peek().second.equals(compId))
            srcToDestGraph.get(srcDev).remove();
    }

    private boolean myselfFirstInQueue(DeviceId srcDev, ComponentId compId)
    {
        return srcToDestGraph.get(srcDev).peek().second.equals(compId);
    }

    private Pair<Boolean, ComponentId> findCycle(DeviceId srcDev)
    {
        DeviceId currDev = srcDev;
        do
        {
            if(!srcToDestGraph.get(currDev).isEmpty())
            {
                currDev = srcToDestGraph.get(currDev).peek().first;
            }
            else
                return new Pair<>(false, new ComponentId(-1));
        }while(!currDev.equals(srcDev));

        currDev = srcToDestGraph.get(srcDev).peek().first;
        ComponentId compOnDest = srcToDestGraph.get(currDev).peek().second;

        return new Pair<>(true, compOnDest);
    }

    public Pair<Boolean, ComponentId> cycleExist(DeviceId srcDev, DeviceId destDev, ComponentId compId)
    {
        queue_pushBack(srcDev, destDev, compId);
        if(myselfFirstInQueue(srcDev, compId))
        {
            Pair<Boolean, ComponentId> isCycle = findCycle(srcDev);

            return isCycle;
        }
        return new Pair<>(false, new ComponentId(-1));
    }

}
