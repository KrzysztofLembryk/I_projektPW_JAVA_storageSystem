package cp2023.solution;

import cp2023.base.ComponentId;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class DeviceSpaceHandler {
    private Integer size;
    private Map<Integer, Pair<DevSpacesTypes, ComponentId>> mapOfDevSpaces;
    private int spacesOccupied;
    private int howManyWaitingInQueue = 0;
    private Semaphore semaphoreWaitForReleased = new Semaphore(0, true);
    private Semaphore waitingQueueSemaphore;
    public DeviceSpaceHandler(Integer size, Semaphore waitingQSem)
    {
        // occupied is less than size, StorageSysFactory ensures that
        this.waitingQueueSemaphore = waitingQSem;
        this.size = size;
        this.spacesOccupied = 0;
        mapOfDevSpaces = new HashMap<>();

        for(int i = 0; i < size; i++)
        {
           mapOfDevSpaces.put(i, new Pair<>(DevSpacesTypes.FREE, null));
        }

    }

    public Pair<Integer, DevSpacesTypes> freeQueue_and_reserveSpace(ComponentId compId)
            throws InterruptedException
    {
        // najpierw pierwszenstwo mieli ci co czekali na semaforze na jakiekolwiek miejsce
        while(existsFreeSpace() && howManyWaitingInQueue > 0)
        {
            howManyWaitingInQueue -= 1;
            // robimy dziedziczenie sekcji krytycznej:
            waitingQueueSemaphore.release();
            semaphoreWaitForReleased.acquire();
        }

        // no free space, so we cannot reserve, so we need to wait on special semaphore
        // for first free space
        if(!existsFreeSpace())
        {
            howManyWaitingInQueue += 1;
            return new Pair<>(-1, DevSpacesTypes.OCCUPIED);
        }


        for(int i = 0; i < size; i++)
        {
            if(mapOfDevSpaces.get(i).first == DevSpacesTypes.FREE)
            {
                mapOfDevSpaces.get(i).first = DevSpacesTypes.OCCUPIED;
                mapOfDevSpaces.get(i).second = compId;
                spacesOccupied += 1;
                return new Pair<>(i, DevSpacesTypes.FREE);
            }
            if(mapOfDevSpaces.get(i).first == DevSpacesTypes.OK_TO_RESERVE)
            {
                mapOfDevSpaces.get(i).first = DevSpacesTypes.OCCUPIED;
                mapOfDevSpaces.get(i).second = compId;
                spacesOccupied += 1;
                return new Pair<>(i, DevSpacesTypes.OK_TO_RESERVE);
            }
        }

        // this return will never happen but java doesn't know that
        System.out.println("Jakims cudem devSpaceHandler return W freeQueue_reserve  po for sie zrobilo");
        return new Pair<>(-1, null);
    }

    public Pair<Integer, DevSpacesTypes> freedThread_reserveSpace(ComponentId compId)
    {
        // no free space, so we cannot reserve, so we need to wait on special semaphore
        // for first free space

        for(int i = 0; i < size; i++)
        {
            if(mapOfDevSpaces.get(i).first == DevSpacesTypes.FREE)
            {
                mapOfDevSpaces.get(i).first = DevSpacesTypes.OCCUPIED;
                mapOfDevSpaces.get(i).second = compId;
                spacesOccupied += 1;
                semaphoreWaitForReleased.release();
                return new Pair<>(i, DevSpacesTypes.FREE);
            }
            if(mapOfDevSpaces.get(i).first == DevSpacesTypes.OK_TO_RESERVE)
            {
                mapOfDevSpaces.get(i).first = DevSpacesTypes.OCCUPIED;
                mapOfDevSpaces.get(i).second = compId;
                spacesOccupied += 1;
                semaphoreWaitForReleased.release();
                return new Pair<>(i, DevSpacesTypes.OK_TO_RESERVE);
            }
        }

        // this return will never happen
        System.out.println("Jakims cudem devSpaceHandler return po for sie zrobilo");
        return new Pair<>(-1, null);
    }
    public void freeSpace(Integer idx)
    {
        spacesOccupied -= 1;
        mapOfDevSpaces.get(idx).first = DevSpacesTypes.FREE;
        mapOfDevSpaces.get(idx).second = null;
    }
    public void freeSpace(ComponentId compId)
    {
        // we free first occupied space
        for(int i = 0; i < size; i++)
        {
            if(mapOfDevSpaces.get(i).second != null &&
                    mapOfDevSpaces.get(i).second.equals(compId))
            {
                spacesOccupied -= 1;
                mapOfDevSpaces.get(i).first = DevSpacesTypes.FREE;
                mapOfDevSpaces.get(i).second = null;
                break;
            }
        }


    }
    public void okToReserveSpace(Integer idx)
    {
        spacesOccupied -= 1;
        mapOfDevSpaces.get(idx).first = DevSpacesTypes.OK_TO_RESERVE;
        mapOfDevSpaces.get(idx).second = null;
    }
    private boolean existsFreeSpace()
    {
        return spacesOccupied != size;
    }
}
