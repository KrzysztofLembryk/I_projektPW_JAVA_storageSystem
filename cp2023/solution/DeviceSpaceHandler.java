package cp2023.solution;

import cp2023.base.ComponentId;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class DeviceSpaceHandler {
    private Integer size;
    private SortedMap<Integer, Pair<DevSpacesTypes, ComponentId>> mapOfDevSpaces;
    private int spacesOccupied;
    public DeviceSpaceHandler(Integer size)
    {
        // occupied is less than size, StorageSysFactory ensures that
        this.size = size;
        this.spacesOccupied = 0;
        mapOfDevSpaces = new TreeMap<>();

        for(int i = 0; i < size; i++)
        {
           mapOfDevSpaces.put(i, new Pair<>(DevSpacesTypes.FREE, null));
        }

    }
    public Pair<Integer, DevSpacesTypes> reserveSpace(ComponentId compId)
    {
        // no free space, so we cannot reserve, so we need to wait on special semaphore
        // for first free space
        if(!existsFreeSpace())
            return new Pair<>(-1, DevSpacesTypes.OCCUPIED);


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

        // this return will never happen
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
