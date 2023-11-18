package cp2023.solution;

import cp2023.base.ComponentId;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class DeviceSpaceHandler {
    private Integer size;
    private Map<Integer, Pair<DevSpacesTypes, ComponentId>> mapOfDevSpaces;
    private int howManyWaitingInQueue = 0;
    private Semaphore semaphoreWaitForReleased = new Semaphore(0, true);
    public DeviceSpaceHandler(Integer size)
    {
        // occupied is less than size, StorageSysFactory ensures that
        this.size = size;
        mapOfDevSpaces = new HashMap<>();

        for(int i = 0; i < size; i++)
        {
           mapOfDevSpaces.put(i, new Pair<>(DevSpacesTypes.FREE, null));
        }

    }
    private int initIdx = 0;
    protected Integer init_spaces_reservation(ComponentId compId)
    {
        if(initIdx < size)
        {
            if(mapOfDevSpaces.get(initIdx).first == DevSpacesTypes.FREE)
            {
                mapOfDevSpaces.get(initIdx).first = DevSpacesTypes.OCCUPIED;
                mapOfDevSpaces.get(initIdx).second = compId;
                initIdx += 1;
                return initIdx - 1;
            }
        }
        return -1;
    }

    public Integer reserveSpace(ComponentId compId)
            throws InterruptedException
    {
        for(int i = 0; i < size; i++) {
            if (mapOfDevSpaces.get(i).first == DevSpacesTypes.FREE) {

                mapOfDevSpaces.get(i).first = DevSpacesTypes.OCCUPIED;
                mapOfDevSpaces.get(i).second = compId;
                return i;
            }
        }
        // this return will never happen but java doesn't know that
        System.out.println("Jakims cudem devSpaceHandler return W reserveSpace po for sie zrobilo");
        return -1;
    }
    public void freeSpace(ComponentId compId) throws InterruptedException
    {
        // we free occupied space by us
        for(int i = 0; i < size; i++)
        {
            if(mapOfDevSpaces.get(i).second != null &&
                    mapOfDevSpaces.get(i).second.equals(compId))
            {
                mapOfDevSpaces.get(i).second = null;
                mapOfDevSpaces.get(i).first = DevSpacesTypes.FREE;

                break;
            }
        }
    }
}
