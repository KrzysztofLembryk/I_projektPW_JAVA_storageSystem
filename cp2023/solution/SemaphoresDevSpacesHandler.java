package cp2023.solution;

import cp2023.base.ComponentId;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

public class SemaphoresDevSpacesHandler {
    private Integer size;
    private SortedMap<Integer, Pair<Semaphore, ComponentId>> semSpacesMap;
    private Semaphore noSpaceSemaphore;
    private Integer howManyWaiting = 0;

    public SemaphoresDevSpacesHandler(Integer size) throws InterruptedException
    {
        semSpacesMap = new TreeMap<>();
        noSpaceSemaphore = new Semaphore(0, true);

        for(int i = 0; i < size; i++)
            semSpacesMap.put(i, new Pair<>(new Semaphore(1, true), null));

    }

    public void acquire(Integer idx) throws InterruptedException
    {
        semSpacesMap.get(idx).first.acquire();
    }
    public void noFreeSpaceAcquire() throws InterruptedException
    {
        howManyWaiting += 1;
        noSpaceSemaphore.acquire();
    }
    public void release(Integer idx)
    {
        semSpacesMap.get(idx).first.release();
    }
    public void release(ComponentId compId)
    {
        for(int i = 0; i < size; i++)
        {
            if(semSpacesMap.get(i).second != null &&
                    semSpacesMap.get(i).second.equals(compId))
            {
                semSpacesMap.get(i).first.release();
                break;
            }
        }
    }

    public void noFreeSpaceRelease()
    {
        if(howManyWaiting > 0)
            howManyWaiting -= 1;
        noSpaceSemaphore.release();
    }

    public Integer getHowManyWaiting() {
        return howManyWaiting;
    }
}
