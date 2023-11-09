package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.ComponentTransfer;
import cp2023.base.DeviceId;
import cp2023.exceptions.TransferException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class StorageSystem implements cp2023.base.StorageSystem {
    // deviceTotalSlots - stores info about how many components
    // device of given ID can store (deviceID --> capacity).
    private Map<DeviceId, Integer> deviceTotalSlots;

    // deviceSpacseMap - knows if there are free to use spaces on device or not
    private Map<DeviceId, DeviceSpaceHandler> deviceSpacesMap;

    // componentInDevicePlacement - remembers on which device given
    // component is stored.
    private Map<ComponentId, DeviceId> componentInDevicePlacement;


     // isCompBeingTransfered - knows if given component is currently transfered,
     // since only one transfer for component can be commissioned
    private Map<ComponentId, Boolean> isCompBeingTransfered;

    // semaphoreIsCompBeingTransfered - allows only one thread to change value in
    // isCompBeingTransfered map, because it could happen that two threads simultaneously
    // would want to transfer given componenent and simultaneously would check
    // if given component was being transfered and at that very point it wasnt being transfered
    // so both threads would be able to transfer it
    private Map<ComponentId, Semaphore> semaphoreIsCompBeingTransfered;
    private Semaphore semaphoreForTransfer;
    public StorageSystem(Map<DeviceId, Integer> deviceTotalSlots,
                                Map<ComponentId, DeviceId> componentPlacement)
    {
        this.deviceTotalSlots = deviceTotalSlots;
        this.componentInDevicePlacement = componentPlacement;
        isCompBeingTransfered = new HashMap<>();
        semaphoreIsCompBeingTransfered = new HashMap<>();
        semaphoreForTransfer = new Semaphore(1, true);
        deviceSpacesMap = new ConcurrentHashMap<>();;

        for(DeviceId devId : deviceTotalSlots.keySet())
        {
            deviceSpacesMap.put(devId, new DeviceSpaceHandler(deviceTotalSlots.get(devId)));
        }

        for(ComponentId compId : componentPlacement.keySet())
        {
            isCompBeingTransfered.put(compId, false);
            semaphoreIsCompBeingTransfered.put(compId, new Semaphore(1, true));
        }

    }

    @Override
    public void execute(ComponentTransfer transfer) throws TransferException {

        // zanim zrobimy transfer prepare musimy miec

    }
    protected Map<DeviceId, Integer> getDeviceSlotsMap()
    {
        return deviceTotalSlots;
    }
    protected Map<ComponentId, DeviceId> getCompPlacementMap()
    {
        return componentInDevicePlacement;
    }
    protected Map<ComponentId, Semaphore> getSemaphoreCompTransfered()
    {
        return semaphoreIsCompBeingTransfered;
    }
    protected Map<ComponentId, Boolean> getIsCompBeingTransfered()
    {
        return isCompBeingTransfered;
    }
    protected Semaphore getSemaphoreForTransfer()
    {
        return semaphoreForTransfer;
    }
}
