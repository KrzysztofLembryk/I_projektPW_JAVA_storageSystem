package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.ComponentTransfer;
import cp2023.base.DeviceId;
import cp2023.exceptions.TransferException;

import java.util.Map;

public class StorageSystem implements cp2023.base.StorageSystem {
    // deviceTotalSlots - stores info about how many components
    // device of given ID can store (deviceID --> capacity).
    private Map<DeviceId, Integer> deviceTotalSlots;

    // componentInDevicePlacement - remembers on which device given
    // component is stored.
    private Map<ComponentId, DeviceId> componentInDevicePlacement;
    public StorageSystem(Map<DeviceId, Integer> deviceTotalSlots,
                                Map<ComponentId, DeviceId> componentPlacement)
    {
        this.deviceTotalSlots = deviceTotalSlots;
        this.componentInDevicePlacement = componentPlacement;
    }

    @Override
    public void execute(ComponentTransfer transfer) throws TransferException {

    }
    protected Map<DeviceId, Integer> getDeviceSlotsMap()
    {
        return deviceTotalSlots;
    }
    protected Map<ComponentId, DeviceId> getComponentPlacementMap()
    {
        return componentInDevicePlacement;
    }
}
