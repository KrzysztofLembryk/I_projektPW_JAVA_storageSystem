package cp2023.solution;

public class DeviceSpaceHandler {
    private Integer size;
    private DevSpacesTypes[] arrOfDevSpaces;
    private int spacesReserved;
    public DeviceSpaceHandler(Integer size)
    {
        this.size = size;
        this.spacesReserved = 0;
        arrOfDevSpaces = new DevSpacesTypes[size];

        for(int i = 0; i < size; i++)
            arrOfDevSpaces[i] = DevSpacesTypes.FREE;
    }
    public boolean reserveFreeSpace()
    {
        for(int i = 0; i < size; i++)
        {
            if(arrOfDevSpaces[i] == DevSpacesTypes.FREE)
            {
                arrOfDevSpaces[i] = DevSpacesTypes.RESERVED;
                spacesReserved += 1;
                return true;
            }
        }

        return false;
    }
    public boolean freeReservedSpace()
    {
        for(int i = 0; i < size; i++)
        {
            if(arrOfDevSpaces[i] == DevSpacesTypes.RESERVED)
            {
                arrOfDevSpaces[i] = DevSpacesTypes.FREE;
                spacesReserved -= 1;
                return true;
            }
        }
        return false;
    }

    public boolean existsFreeSpace()
    {
        return spacesReserved != size;
    }
}
