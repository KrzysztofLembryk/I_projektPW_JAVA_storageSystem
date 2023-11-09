package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;
import cp2023.exceptions.*;

import java.util.Map;

public class ComponentTransfer implements cp2023.base.ComponentTransfer {
    private final StorageSystem storageSystem;
    private final ComponentId compId;
    private final DeviceId srcDevId;
    private final DeviceId destDevId;
    private boolean isPrepared;
    private boolean isPerformed;
    private final Thread myThread;
    TypeOfTransfer transferType;


    private void isTransferOK() throws DeviceDoesNotExist, ComponentDoesNotNeedTransfer,
            ComponentAlreadyExists, ComponentDoesNotExist
    {
        if(transferType == TypeOfTransfer.ADD)
        {
            if(!storageSystem.getDeviceSlotsMap().containsKey(destDevId))
                throw new DeviceDoesNotExist(destDevId);
            if(storageSystem.getCompPlacementMap().containsKey(compId))
                throw new ComponentAlreadyExists(compId);
        }
        else if(transferType == TypeOfTransfer.REMOVE)
        {
            if(!storageSystem.getDeviceSlotsMap().containsKey(srcDevId))
                throw new DeviceDoesNotExist(srcDevId);
            if(!storageSystem.getCompPlacementMap().containsKey(compId))
                throw new ComponentDoesNotExist(compId, srcDevId);
        }

        else
        {

        }

    }
    public ComponentTransfer(TypeOfTransfer type, StorageSystem storSys,
                             ComponentId compId, DeviceId srcDevID,
                             DeviceId destDevID)
            throws ComponentIsBeingOperatedOn, DeviceDoesNotExist,
                ComponentDoesNotNeedTransfer, ComponentAlreadyExists, ComponentDoesNotExist
    {
        try
        {
            this.storageSystem = storSys;
            this.transferType = type;
            this.compId = compId;
            this.srcDevId = srcDevID;
            this.destDevId = destDevID;
            this.isPrepared = false;
            this.isPerformed = false;
            this.myThread = Thread.currentThread();

            // PROSTSZA WERSJA Z JEDNYM SEMAPHOREM - NA RAZIE NIE WIEM JAK Z WIELOMA ZROBIC
            storageSystem.getSemaphoreForNewComp().acquire();

            boolean compIdExist = storageSystem.getCompPlacementMap().containsKey(compId);

            // we check if compID is in map that stores whether given compID is being transfered
//            if(compIdExist)
//                storageSystem.getSemaphoreCompTransfered().get(compId).acquire();

            if(!compIdExist)
            {
                // if there is no such compID this means we either add new comp
                // so we have another semaphore for adding components,
                // we also put (compID, false) to map that checks
                //storageSystem.getSemaphoreForNewComp().acquire();

                storageSystem.getIsCompBeingTransfered().put(compId, false);
            }


            isTransferOK();
            if(!storageSystem.getIsCompBeingTransfered().get(compId))
                storageSystem.getIsCompBeingTransfered().put(compId, true);
            else
                throw new ComponentIsBeingOperatedOn(compId);

//            if(compIdExist)
//                storageSystem.getSemaphoreCompTransfered().get(compId).release();
//            else
//                storageSystem.getSemaphoreForNewComp().release();

            storageSystem.getSemaphoreForNewComp().release();
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException("panic: unexpected thread interruption");
        }

    }
    @Override
    public ComponentId getComponentId()
    {
        return  compId;
    }
    @Override
    public DeviceId getSourceDeviceId()
    {
        Map<ComponentId, DeviceId> compInDevicePlacement = storageSystem.getCompPlacementMap();

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

        // at the very end of perform method we need to inform system that
        // transfer at given component has ended, and thus we can create another transfer
        storageSystem.getIsCompBeingTransfered().put(compId, false);
    }
}
