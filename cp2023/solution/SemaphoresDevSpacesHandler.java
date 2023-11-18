package cp2023.solution;

import cp2023.base.ComponentId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class SemaphoresDevSpacesHandler{
    private final Integer size;
    private Map<Integer, Pair<Semaphore, ComponentId>> semSpacesMap;

    public SemaphoresDevSpacesHandler(Integer size) throws InterruptedException
    {
        this.size = size;
        semSpacesMap = new ConcurrentHashMap<>();

        for(int i = 0; i < size; i++)
            semSpacesMap.put(i, new Pair<>(new Semaphore(1, true), null));

    }

    public void acquire(Integer idx, ComponentId compId) throws InterruptedException
    {
        semSpacesMap.get(idx).first.acquire();
        semSpacesMap.get(idx).second = compId;
    }
    public void release(ComponentId compId)
    {
        for(int i = 0; i < size; i++)
        {
            if(semSpacesMap.get(i).second != null &&
                    semSpacesMap.get(i).second.equals(compId))
            {
                semSpacesMap.get(i).second = null;
                semSpacesMap.get(i).first.release();
                break;
            }
        }
    }
}
