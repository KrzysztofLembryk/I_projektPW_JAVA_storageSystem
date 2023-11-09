package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;
import cp2023.exceptions.ComponentDoesNotExist;

import java.util.Map;

public class ComponentTransfer implements cp2023.base.ComponentTransfer {
    private final StorageSystem storageSystem;
    private final ComponentId compId;
    private final DeviceId srcDevId;
    private final DeviceId destDevId;
    private boolean isPrepared;
    private boolean isPerformed;
    private final Thread myThread;
    public ComponentTransfer(StorageSystem storSys, ComponentId compId, DeviceId srcDevID, DeviceId destDevID)
    {
        this.storageSystem = storSys;
        this.compId = compId;
        this.srcDevId = srcDevID;
        this.destDevId = destDevID;
        this.isPrepared = false;
        this.isPerformed = false;
        this.myThread = Thread.currentThread();
    }
    @Override
    public ComponentId getComponentId()
    {
        return  compId;
    }
    @Override
    public DeviceId getSourceDeviceId()
    {
        Map<ComponentId, DeviceId> compInDevicePlacement = storageSystem.getComponentPlacementMap();

        // jesli komponentu nie ma w mapie komponent -> urzadzenie, to znaczy ze dodajemy
        // nowy komponent do systemu
        if(!compInDevicePlacement.containsKey(compId))
            return null;
        else
            return srcDevId;
    }
    @Override
    public DeviceId getDestinationDeviceId()
    {
        return destDevId;
    }
    @Override
    public void prepare()
    {

    }

    @Override
    public void perform()
    {

    }
}
