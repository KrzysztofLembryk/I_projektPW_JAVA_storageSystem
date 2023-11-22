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
        // Sprawdzamy czy jakies urzadzenie ma zerowa pojemnosc, jesli takie urzadzenie
        // istnieje to rzucamy illegalArgException.
        if(deviceTotalSlots.isEmpty())
            throw new IllegalArgumentException("StorageSystemFactory - newSystem - areArgsCorrect - " +
                    "isDeviceMapCorrect - map is empty");

        for(Integer capacity : deviceTotalSlots.values())
            if(capacity == 0)
                throw new IllegalArgumentException("StorageSystemFactory - newSystem - areArgsCorrect - " +
                        "isDeviceMapCorrect - capacity of a device is 0");

    }
    private static void isComponentMapCorrect(Map<DeviceId, Integer> devTotalSlots,
                                              Map<ComponentId, DeviceId> compPlacement)
            throws IllegalArgumentException
    {
        // Sprawdzamy czy wyjsciowe przyporzadkowanie komponentow do device jest poprawne,
        // czyli czy komponenty sa na device ktore istnieja w naszym systemie.
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
        // Zliczamy ile jest komponentow na danym device i sprawdzamy czy
        // liczba komponentow nie przekracza device capacity. Wykonujemy
        // te funkcje po wykonaniu dwoch poprzednich funkcji sprawdzajacych
        // poprawnosc danych wiec wiemy ze komponenty sa przyporzadkowane
        // do istniejacych device i ze zadne device capacity != 0.
        Map<DeviceId, Integer> howManyComponentsOnDevices = new HashMap<>();

        for(DeviceId deviceID : deviceTotalSlots.keySet())
            howManyComponentsOnDevices.put(deviceID, 0);

        // Zliczamy liczbe elementow na device.
        for(DeviceId id : componentPlacement.values())
        {
            Integer val = howManyComponentsOnDevices.get(id);
            howManyComponentsOnDevices.put(id, val + 1);
        }

        // Sprawdzamy czy liczba elem na device <= capacity.
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
    }

}
