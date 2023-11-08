/*
 * University of Warsaw
 * Concurrent Programming Course 2023/2024
 * Java Assignment
 *
 * Author: Konrad Iwanicki (iwanicki@mimuw.edu.pl)
 */
package cp2023.solution;

import java.util.Map;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;
import cp2023.base.StorageSystem;


public final class StorageSystemFactory {
    private static boolean isDeviceMapCorrect(Map<DeviceId, Integer> deviceTotalSlots)
    {
        System.out.println("ciul");
        return true;
    }
    private static boolean isComponentMapCorrect(Map<ComponentId, DeviceId> componentPlacement)
    {
        return true;
    }
    public static StorageSystem newSystem(
            Map<DeviceId, Integer> deviceTotalSlots,
            Map<ComponentId, DeviceId> componentPlacement) {
        return new cp2023.solution.StorageSystem(deviceTotalSlots, componentPlacement);
        // FIXME: implement
        //throw new RuntimeException("not implemented");
    }

}
