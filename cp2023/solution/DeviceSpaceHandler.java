package cp2023.solution;

import cp2023.base.ComponentId;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class DeviceSpaceHandler {
    private final Integer size;
    private final ComponentId freeSpace = new ComponentId(Integer.MIN_VALUE);
    private Map<Integer, ComponentId> mapOfDevSpaces;
    public DeviceSpaceHandler(Integer size)
    {
        this.size = size;
        mapOfDevSpaces = new ConcurrentHashMap<>();

        for(int i = 0; i < size; i++)
        {
            System.out.println("chuj");
            // I assume that devices ids are positive numbers, so Integer Min_Val
            // means that slot is free
            mapOfDevSpaces.put(i,  freeSpace);
        }

    }
    private int initIdx = 0;
    protected Integer init_spaces_reservation(ComponentId compId)
    {
        if(initIdx < size)
        {
            if(mapOfDevSpaces.get(initIdx).equals(freeSpace))
            {
                mapOfDevSpaces.put(initIdx,  compId);
                initIdx += 1;
                return initIdx - 1;
            }
        }
        return -1;
    }

    public Integer reserveSpace(ComponentId compId)
            throws InterruptedException
    {
        // najpierw sprawdzamy czy juz nie mamy miejsca w destdev
        // moglo sie tak zdarzyc gdy byl cykl
        for(int i = 0; i < size; i++) {
            if (mapOfDevSpaces.get(i).equals(compId)) {
                return i;
            }
        }
        for(int i = 0; i < size; i++) {
            if (mapOfDevSpaces.get(i).equals(freeSpace)) {
                mapOfDevSpaces.put(i, compId);
                return i;
            }
        }
        // this return will never happen but java doesn't know that
        System.out.println("Jakims cudem devSpaceHandler return W reserveSpace po for sie zrobilo");
        return -1;
    }
    public Integer reserveSpaceCycle(ComponentId newCompId, ComponentId oldCompId)
    {
        for(int i = 0; i < size; i++) {
            if(mapOfDevSpaces.get(i).equals(oldCompId)){
                mapOfDevSpaces.put(i, newCompId);
                return i;
            }
        }
        return -1;
    }
    public void freeSpace(ComponentId compId)
    {
        // we free occupied space by us
        for(int i = 0; i < size; i++)
        {
            if(mapOfDevSpaces.get(i).equals(compId))
            {
                mapOfDevSpaces.put(i, freeSpace);
                break;
            }
        }
    }
}
