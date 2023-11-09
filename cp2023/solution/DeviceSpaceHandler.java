package cp2023.solution;

public class DeviceSpaceHandler {
    private Integer size;
    private DevSpacesTypes[] arrOfDevSpaces;
    public DeviceSpaceHandler(Integer size)
    {
        this.size = size;
        arrOfDevSpaces = new DevSpacesTypes[size];

        for(int i = 0; i < size; i++)
            arrOfDevSpaces[i] = DevSpacesTypes.FREE;
    }
    public Integer reserveFreeSpace()
    {
        for(int i = 0; i < size; i++)
        {
            if(arrOfDevSpaces[i] == DevSpacesTypes.FREE)
            {
                arrOfDevSpaces[i] = DevSpacesTypes.RESERVED;
                return i;
            }
        }

        return -1;
    }
    public void freeReservedSpace(Integer idx)
    {
        if(idx < size)
            arrOfDevSpaces[idx] = DevSpacesTypes.FREE;
    }
}
