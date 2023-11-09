/*
 * University of Warsaw
 * Concurrent Programming Course 2023/2024
 * Java Assignment
 *
 * Author: Konrad Iwanicki (iwanicki@mimuw.edu.pl)
 */
package cp2023.solution;

import java.util.HashMap;
import java.util.Map;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;
import cp2023.base.StorageSystem;


public final class StorageSystemFactory {
    private static void isDeviceMapCorrect(Map<DeviceId, Integer> deviceTotalSlots) throws IllegalArgumentException
    {
        // we check if any device has 0 capacity, if such device exist we throw exception
        for(Integer capacity : deviceTotalSlots.values())
            if(capacity == 0)
                throw new IllegalArgumentException("StorageSystemFactory - newSystem - areArgsCorrect - " +
                        "isDeviceMapCorrect - capacity of a device is 0");

    }
    private static void isComponentMapCorrect(Map<DeviceId, Integer> devTotalSlots,
                                              Map<ComponentId, DeviceId> compPlacement)
            throws IllegalArgumentException
    {
        for(DeviceId devID : compPlacement.values())
        {
            if(!devTotalSlots.containsKey(devID))
                throw new IllegalArgumentException("StorageSystemFactory - newSystem - areArgsCorrect - " +
                        "isComponentMapCorrect - there exists a component with assigned deviceID that does not exist");
        }
    }
    private static void areThereTooManyComponentsOnDevice(Map<DeviceId, Integer> deviceTotalSlots,
                                                          Map<ComponentId, DeviceId> componentPlacement)
            throws IllegalArgumentException
    {
        Map<DeviceId, Integer> howManyComponentsOnDevices = new HashMap<>();

        for(DeviceId deviceID : deviceTotalSlots.keySet())
            howManyComponentsOnDevices.put(deviceID, 0);

        // we count how many components is currently on each device
        for(DeviceId id : componentPlacement.values())
        {
            Integer val = howManyComponentsOnDevices.get(id);
            howManyComponentsOnDevices.put(id, val + 1);
        }
        // we check if current number of components on device is <= maxNnbrOfComponents on device
        for(DeviceId id : deviceTotalSlots.keySet()) {
            Integer maxNumberOfDevices = deviceTotalSlots.get(id);
            Integer currentNumberOfDevices = howManyComponentsOnDevices.get(id);

            if (currentNumberOfDevices > maxNumberOfDevices)
                throw new IllegalArgumentException("StorageSystemFactory - newSystem - areArgsCorrect - " +
                        "number of components on device exceeds device's limit");
        }
    }
    private static void areArgumentsCorrect(Map<DeviceId, Integer> deviceTotalSlots,
                                               Map<ComponentId, DeviceId> componentPlacement) throws IllegalArgumentException
    {
        isDeviceMapCorrect(deviceTotalSlots);
        isComponentMapCorrect(deviceTotalSlots, componentPlacement);
        areThereTooManyComponentsOnDevice(deviceTotalSlots, componentPlacement);
    }
    public static StorageSystem newSystem(
            Map<DeviceId, Integer> deviceTotalSlots,
            Map<ComponentId, DeviceId> componentPlacement) throws IllegalArgumentException {

        areArgumentsCorrect(deviceTotalSlots, componentPlacement);

        return new cp2023.solution.StorageSystem(deviceTotalSlots, componentPlacement);

        // FIXME: implement
        //throw new RuntimeException("not implemented");
    }

}
